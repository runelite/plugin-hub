/*
 * Copyright (c) 2020 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.pluginhub.packager;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.function.Supplier;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class PluginBuildException extends Exception
{
	private String file;
	private String line;
	private String help;

	private PluginBuildException(String message, Throwable throwable)
	{
		super(message, throwable);
	}

	public static PluginBuildException of(String internalName, String message, Object... args)
	{
		FormattingTuple fmt = MessageFormatter.arrayFormat(internalName + ": " + message, args);
		return new PluginBuildException(fmt.getMessage(), fmt.getThrowable());
	}

	public static PluginBuildException of(Plugin plugin, String message, Object... args)
	{
		FormattingTuple fmt = MessageFormatter.arrayFormat(plugin.getInternalName() + ": " + message, args);
		return new PluginBuildException(fmt.getMessage(), fmt.getThrowable());
	}

	public PluginBuildException withFileLine(File file, String line)
	{
		return withFileLine(file.toString(), line);
	}

	public PluginBuildException withFile(File file)
	{
		return withFileLine(file, null);
	}

	public PluginBuildException withFileLine(String file, String line)
	{
		this.file = file;
		this.line = line;
		return this;
	}

	public PluginBuildException withFile(String file)
	{
		return withFileLine(file, null);
	}

	public PluginBuildException withHelp(String help)
	{
		this.help = help;
		return this;
	}

	public PluginBuildException withHelp(Supplier<String> help)
	{
		this.help = help.get();
		return this;
	}

	public String getHelpText()
	{
		StringBuilder sb = new StringBuilder();

		if (this.file != null)
		{
			sb.append("in file ").append(this.file);
			if (this.line != null)
			{
				sb.append(":\n").append(line);
			}
			sb.append("\n");
		}

		if (this.help != null)
		{
			sb.append(help);
		}

		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream s)
	{
		super.printStackTrace(s);
		s.println("\n");
		s.println(getMessage());
		s.println(getHelpText());
	}

	@Override
	public void printStackTrace(PrintWriter s)
	{
		super.printStackTrace(s);
		s.println("\n");
		s.println(getMessage());
		s.println(getHelpText());
	}
}
