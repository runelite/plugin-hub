#!/usr/bin/env python3

"""
Copyright (c) 2019 Abex
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""

import argparse
import os
import re
import sys
from collections import OrderedDict
from pyclbr import Function
from string import Template

templatedir = os.path.join(os.path.dirname(
    os.path.realpath(__file__)), "templateplugin")
pwd = os.getcwd()
parser = argparse.ArgumentParser()


def strip_plugin(string: str) -> str:
    re_strip_plugin = r"(?i)[ _-]*plugin$"
    return re.sub(pattern=re_strip_plugin, repl="", string=string)


def reformat(string: str, string_replacement: Function) -> str:

    if string_replacement != to_spaces:
        pattern = r"[^a-zA-Z0-9_ -]"
        string = re.sub(pattern=pattern, repl="", string=string)

    if re.match(r".*[ _-]", string):
        pattern = r"(?:^|[ _.-]+)([^ _.-]+)"
        return re.sub(pattern=pattern, repl=lambda m: string_replacement(m.group(1)), string=string).strip(" -_")

    pattern = r"((?:^.|[A-Z0-9]+)(?:[a-z0-9]+|$))"
    return re.sub(pattern=pattern, repl=lambda m: string_replacement(m.group(1)), string=string).strip(" -_")


def to_spaces(string: str) -> str:
    return " " + string.capitalize()


def to_camelcase(string: str) -> str:
    return string.capitalize()


def to_dashes(string: str) -> str:
    return "-" + string.lower()


def to_lowercase(string: str) -> str:
    return string.lower()


def string_function(strfun):
    if isinstance(strfun, str):
        return strfun
    return strfun()


def entry_complete() -> bool:
    """ Requests verification from the user for the entered fields """
    i = input("Is the information above correct? [y/n] ").lower()
    if (i == "") or (i == "y"):
        return True
    if (i == "n"):
        return False


ordered_steps = OrderedDict(
    [
        (
            "name",
            {
                "ask": True,
                "desc": "The name your plugin will be shown by in menus",
                "value": "Foo Bazzer",
                "strip_plugin": True,
            },
        ),
        (
            "package",
            {
                "ask": True,
                "desc": "The java package your plugin will be in. Typically a reversed domain name",
                "value": "com.example.foobazzer",
                "strip_plugin": True,
            },
        ),
        (
            "author",
            {
                "ask": True,
                "desc": "Who wrote/maintains this plugin",
                "value": "John Doe",
            },
        ),
        (
            "description",
            {
                "ask": True,
                "desc": "A short string describing the plugin",
                "value": "Adds a bazzer to the foo",
            },
        ),
        (
            "version",
            {
                "desc": "The initial version number of the plugin",
                "value": "1.0-SNAPSHOT",
            },
        ),
        (
            "plugin_prefix",
            {
                "desc": "The name of the your plugin's main class, without the 'Plugin' suffix",
                "value": lambda: reformat(ordered_steps["name"]["value"], to_camelcase),
                "strip_plugin": True,
            },
        ),
        (
            "artifact_id",
            {
                "desc": "The name of the maven artifact",
                "value": lambda: reformat(ordered_steps["name"]["value"], to_dashes),
            },
        ),
        (
            "group_id",
            {
                "desc": "The group of the maven artifact",
                "value": lambda: ordered_steps["package"]["value"],
            },
        ),
        (
            "plugin_config_group",
            {
                "desc": "The prefix used to store config keys",
                "value": lambda: reformat(ordered_steps["name"]["value"], to_lowercase),
                "strip_plugin": True,
            },
        ),
    ]
)


def query_user(askAll: bool = True, ordered_steps: dict = ordered_steps) -> dict:
    """ Queries user for ordered steps """
    for key, var in ordered_steps.items():
        if askAll or ("ask" in var and var["ask"]):
            print(var["desc"])
            print("[" + string_function(var["value"]) + "]")
            val = input(key + ": ")
            if val:
                if "strip_plugin" in var and var["strip_plugin"]:
                    val = strip_plugin(val)
                var["value"] = val

    print(" ")
    for key, var in ordered_steps.items():
        print('{} = "{}"'.format(key, string_function(var["value"])))
    return ordered_steps


# checks active directory
pwdIsEmpty = len(os.listdir(pwd)) == 0
if pwdIsEmpty:
    ordered_steps["name"]["value"] = strip_plugin(
        reformat(os.path.basename(pwd), to_spaces))

# set up parser arguments
parser.add_argument("--noninteractive",
                    dest="noninteractive", action="store_true")
parser.add_argument("--output_directory", dest="output_directory")
for key, var in ordered_steps.items():
    parser.add_argument("--" + key, dest=key, help=var["desc"])
args = vars(parser.parse_args())

# sets run as noninteractive and removes ordered steps
noninteractive = args["noninteractive"]
if noninteractive and pwdIsEmpty:
    ordered_steps["name"]["ask"] = False

for key, var in ordered_steps.items():
    if args[key] == None:
        continue
    val = args[key]
    if "strip_plugin" in var and var["strip_plugin"]:
        val = strip_plugin(val)
    var["ask"], var["value"] = False, val

# queries user for form data
if not noninteractive:
    ordered_steps = query_user(askAll=False)
    while True:
        if entry_complete():
            break
        ordered_steps = query_user(askAll=True)

# set output directory
outdir = args["output_directory"]
if outdir == None:
    if pwdIsEmpty:
        outdir = pwd
    else:
        outdir = os.path.join(pwd, string_function(
            ordered_steps["artifact_id"]["value"]))

# set mappings
mappings = {}
for key, var in ordered_steps.items():
    mappings[key] = string_function(var["value"])
mappings["package_path"] = mappings["package"].replace(".", os.path.sep)

# set runelite version
with open(os.path.join(templatedir, "../runelite.version"), "rt") as runelite_version:
    mappings["runelite_version"] = runelite_version.read().strip()

# convert form data into plugin template
for root, dir, files in os.walk(templatedir):
    for file in files:
        try:
            infi = os.path.join(root, file)
            outfi = os.path.join(os.path.relpath(root, templatedir), file)
            outfi = outfi.replace("_(", "${").replace(")_", "}")
            outfi = Template(outfi).substitute(mappings)
            outfi = os.path.join(outdir, outfi)
            os.makedirs(os.path.dirname(outfi), exist_ok=True)
            # we need binary mode to not do that stupid crlf bullshit on windows
            with open(infi, "rb") as ifd:
                if file.endswith(".jar") or file.startswith("gradlew"):
                    with open(outfi, "wb") as ofd:
                        ofd.write(ifd.read())
                else:
                    contents = Template(ifd.read().decode(
                        "utf-8")).substitute(mappings)
                    with open(outfi, "wb") as ofd:
                        ofd.write(contents.encode("utf-8"))
        except ValueError as ex:
            raise ValueError(infi) from ex
