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

import os
import sys
from collections import OrderedDict
import argparse
import re
from string import Template

templatedir = os.path.join(os.path.dirname(os.path.realpath(__file__)), "templateplugin")

def strip_plugin(str):
	return re.sub(r"(?i)[ _-]*plugin$", "", str)

def reformat(str, replacer):
	if replacer != to_spaces:
		str=re.sub(r"[^a-zA-Z0-9_ -]", "", str)
	if re.match(r".*[ _-]", str):
		return re.sub(r"(?:^|[ _.-]+)([^ _.-]+)", lambda m:replacer(m.group(1)), str).strip(" -_")
	return re.sub(r"((?:^.|[A-Z0-9]+)(?:[a-z0-9]+|$))", lambda m:replacer(m.group(1)), str).strip(" -_")

def to_spaces(seg):
	return " " + seg.capitalize()

def to_camelcase(seg):
	return seg.capitalize()

def to_dashes(seg):
	return "-" + seg.lower()

def to_lowercase(seg):
	return seg.lower()

def strfun(strfun):
	if isinstance(strfun, str):
		return strfun
	return strfun()

subs = OrderedDict([
	("name", {
		"ask": True,
		"desc": "The name your plugin will be shown by in menus",
		"value": "Foo Bazzer",
		"strip_plugin": True,
	}),
	("package", {
		"ask": True,
		"desc": "The java package your plugin will be in. Typically a reversed domain name",
		"value": "com.examaple.foobazzer",
		"strip_plugin": True,
	}),
	("author", {
		"ask": True,
		"desc": "Who wrote/maintains this plugin",
		"value": "John Doe",
	}),
	("description", {
		"ask": True,
		"desc": "A short string describing the plugin",
		"value": "Adds a bazzer to the foo",
	}),
	("version", {
		"desc": "The initial version number of the plugin",
		"value": "1.0-SNAPSHOT",
	}),
	("plugin_prefix", {
		"desc": "The name of the your plugin's main class, without the 'Plugin' suffix",
		"value": lambda: reformat(subs["name"]["value"], to_camelcase),
		"strip_plugin": True
	}),
	("artifact_id", {
		"desc": "The name of the maven artifact",
		"value": lambda: reformat(subs["name"]["value"], to_dashes),
	}),
	("group_id", {
		"desc": "The group of the maven artifact",
		"value": lambda: subs["package"]["value"],
	}),
	("plugin_config_group", {
		"desc": "The prefix used to store config keys",
		"value": lambda: reformat(subs["name"]["value"], to_lowercase),
		"strip_plugin": True
	})
])

pwd = os.getcwd()
pwdIsEmpty = len(os.listdir(pwd)) == 0
if pwdIsEmpty:
	subs["name"]["value"] = strip_plugin(reformat(os.path.basename(pwd), to_spaces))

parser = argparse.ArgumentParser()
parser.add_argument("--noninteractive", dest = "noninteractive", action='store_true')
parser.add_argument("--output_directory", dest = "output_directory")
for key, var in subs.items():
	parser.add_argument("--" + key, dest = key, help = var["desc"])
args = vars(parser.parse_args())

noninteractive = args["noninteractive"]

if noninteractive and pwdIsEmpty:
	subs["name"]["ask"] = False

for key, var in subs.items():
	if args[key] != None:
		val = args[key]
		if "strip_plugin" in var and var["strip_plugin"]:
			val = strip_plugin(val)
		var["value"] = val
		var["ask"] = False

askAll = False
while True:
	for key, var in subs.items():
		if askAll or ("ask" in var and var["ask"]):
			if noninteractive:
				print("\"{}\" was not specified in noninteractive mode".format(key))
				sys.exit(1)
			print(var["desc"])
			print("[" + strfun(var["value"]) + "]")
			val = input(key + ": ")
			if val:
				if "strip_plugin" in var and var["strip_plugin"]:
					val = strip_plugin(val)
				var["value"] = val

	askAll = True

	print("")
	for key, var in subs.items():
		print("{} = \"{}\"".format(key, strfun(var["value"])))
	def input_yes():
		while True:
			inp = input("Is this ok? [Yn]").lower()
			if inp == "" or inp == "y":
				return True
			if inp == "n":
				return False
	if noninteractive or input_yes():
		break

outdir = args["output_directory"]
if outdir == None:
	if pwdIsEmpty:
		outdir = pwd
	else:
		outdir = os.path.join(pwd, strfun(subs["artifact_id"]["value"]))

mappings = {}
for key, var in subs.items():
	mappings[key] = strfun(var["value"])
mappings["package_path"] = mappings["package"].replace(".", os.path.sep)
with open(os.path.join(templatedir, "../runelite.version"), "rt") as fi:
	mappings["runelite_version"] = fi.read().strip()

for root, dir, files in os.walk(templatedir):
	for file in files:
		try:
			infi = os.path.join(root, file)
			outfi = os.path.join(os.path.relpath(root, templatedir), file)
			outfi = outfi.replace("_(", "${").replace(")_", "}")
			outfi = Template(outfi).substitute(mappings)
			outfi = os.path.join(outdir, outfi)
			os.makedirs(os.path.dirname(outfi), exist_ok=True)
			with open(infi, "rb") as ifd: # we need binary mode to not do that stupid crlf bullshit on windows
				if file.endswith(".jar") or file.startswith("gradlew"):
					with open(outfi, "wb") as ofd:
						ofd.write(ifd.read())
				else:
					contents = Template(ifd.read().decode("utf-8")).substitute(mappings)
					with open(outfi, "wb") as ofd:
						ofd.write(contents.encode("utf-8"))
		except ValueError as ex:
			raise ValueError(infi) from ex