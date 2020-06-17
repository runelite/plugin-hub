#!/bin/bash

# Copyright (c) 2019 Abex
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

source repo_config.sh

set -e -x

PLUGINFILE="$1"
[ -s "$PLUGINFILE" ]
PLUGIN_ID=$(basename "$PLUGINFILE")

# check valid plugin id
[[ $PLUGIN_ID =~ ^[a-z0-9-]+$ ]]

SCRIPT_HOME="$(cd "$(dirname "$0")" ; pwd -P)"

RUNELITE_VERSION="$(cat "$SCRIPT_HOME/runelite.version")"

# read in the plugin descriptor
disabled=
# shellcheck disable=SC2162
while read LINE || [[ -n "$LINE" ]]; do
	[[ $LINE =~ ^(repository|commit|disabled|warning)=(.*)$ ]]
	eval "${BASH_REMATCH[1]}=\"${BASH_REMATCH[2]}\""
done < "$PLUGINFILE"
[ -z "$disabled" ] || exit 0

# must be a https github repo
[[ $repository =~ ^https://github.com/.*\.git$ ]]

# we must have a full 40 char sha1sum
[[ $commit =~ ^[a-fA-F0-9]{40}+$ ]]

# we need gradle 6.2 for dependency verification
GRADLE_VER=gradle-6.2
if [[ ! -e "/tmp/$GRADLE_VER/bin/gradle" ]]; then
	wget -q -O/tmp/gradle.zip "https://services.gradle.org/distributions/$GRADLE_VER-bin.zip"
	echo 'b93a5f30d01195ec201e240f029c8b42d59c24086b8d1864112c83558e23cf8a */tmp/gradle.zip' | shasum -a256 -c
	unzip -q /tmp/gradle.zip -d /tmp/
	[[ -e "/tmp/$GRADLE_VER/bin/gradle" ]]
fi
export GRADLE_HOME="/tmp/$GRADLE_VER/"
export PATH="$GRADLE_HOME/bin:$PATH"

BUILDDIR="$(mktemp -d /tmp/external-plugin.XXXXXXXX)"
trap "rm -rf ""$BUILDDIR""" EXIT
pushd "$BUILDDIR"

git clone -c 'advice.detachedHead=false' "$repository" "repo"
pushd "repo"
git checkout "$commit^{commit}"

SIGNING_KEY="" REPO_CREDS="" gradle \
	--no-build-cache \
	--parallel \
	--console=plain \
	--init-script="$SCRIPT_HOME/package.gradle" \
	-DrlpluginRuneLiteVersion="$RUNELITE_VERSION" \
	-DrlpluginOutputDirectory="$BUILDDIR" \
	-DrlpluginPluginID="$PLUGIN_ID" \
	-DrlpluginCommit="$commit" \
	-DrlpluginWarning="$warning" \
	rlpluginPackageJar rlpluginEmitManifest

[ -s "$BUILDDIR/plugin.jar" ]
[ -s "$BUILDDIR/plugin.manifest" ]

cat "$BUILDDIR/plugin.manifest"

[[ "${TRAVIS_PULL_REQUEST:-false}" == "false" ]] || exit 0

LOCATION="$REPO_ROOT/$RUNELITE_VERSION/$PLUGIN_ID/$commit"

ICON_UPLOAD=()
if [ -e "icon.png" ]; then
	ICON_UPLOAD=("--upload-file" "icon.png" "$LOCATION.png")
fi

curl --fail --retry 5 \
	--user "$REPO_CREDS" \
	--upload-file "$BUILDDIR/plugin.manifest" "$LOCATION.manifest" \
	--upload-file "$BUILDDIR/plugin.jar" "$LOCATION.jar" \
	"${ICON_UPLOAD[@]}"

echo "Build Success"
