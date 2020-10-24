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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Response;

public class Util
{
	private Util()
	{
	}

	public static void waitAndCheck(Plugin plugin, Process process, String name, long timeout, TimeUnit timeoutUnit) throws PluginBuildException
	{
		try
		{
			if (!process.waitFor(timeout, timeoutUnit))
			{
				process.destroy();
				throw PluginBuildException.of(plugin, name + " failed to complete in a reasonable time");
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}

		if (process.exitValue() != 0)
		{
			throw PluginBuildException.of(plugin, name + " exited with " + process.exitValue());
		}
	}

	public static void check(Response res) throws IOException
	{
		if ((res.code() / 100) != 2)
		{
			throw new IOException(res.request().url() + ": " + res.code() + " " + res.message());
		}
	}
}
