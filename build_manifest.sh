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

[[ "${TRAVIS_PULL_REQUEST:-false}" == "false" ]] || exit 0

RUNELITE_VERSION="$(cat "runelite.version")"

MANIFEST="$(mktemp /tmp/manifest.XXXXXXXX)"
trap "rm -rf ""$MANIFEST*""" EXIT
MANIFEST_DIR="$MANIFEST.sub/"
mkdir "$MANIFEST_DIR"

MANIFEST_CHUNK_DOWNLOAD=()

for PLUGINFILE in plugins/*; do
	# read in the plugin descriptor
	disabled=
	# shellcheck disable=SC2162
	while read LINE || [[ -n "$LINE" ]]; do
		[[ $LINE =~ ^(repository|commit|disabled|warning)=(.*)$ ]]
		eval "${BASH_REMATCH[1]}=\"${BASH_REMATCH[2]}\""
	done < "$PLUGINFILE"
	[ -z "$disabled" ] || continue

	PLUGIN_ID=$(basename "$PLUGINFILE")
	LOCATION="$REPO_ROOT/$RUNELITE_VERSION/$PLUGIN_ID/$commit"
	MANIFEST_CHUNK_DOWNLOAD+=('--output' "$MANIFEST_DIR/$PLUGIN_ID" "$LOCATION.manifest")
done

curl --fail --retry 5 \
	"${MANIFEST_CHUNK_DOWNLOAD[@]}" || true

IS_FIRST=true
echo "[" > "$MANIFEST"
for MANIFEST_CHUNK in "$MANIFEST_DIR"/*; do
	if [[ "$IS_FIRST" != true ]]; then
		echo "," >> "$MANIFEST"
	fi
	IS_FIRST=
	cat "$MANIFEST_CHUNK" >> "$MANIFEST"
done
echo "]" >> "$MANIFEST"

# shellcheck disable=SC2059
openssl dgst -sha256 -sign <(set +x; printf -- "$SIGNING_KEY") -out "$MANIFEST.sig" "$MANIFEST"

perl -e "print pack('N', -s \"$MANIFEST.sig\")" > "$MANIFEST.out"
cat "$MANIFEST.sig" >> "$MANIFEST.out"
cat "$MANIFEST" >> "$MANIFEST.out"

curl --fail --retry 5 \
	--user "$REPO_CREDS" \
	--upload-file "$MANIFEST.out" "$REPO_ROOT/$RUNELITE_VERSION/manifest.js"

echo "Build Success"