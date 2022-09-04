/*
 * Copyright (c) 2021 Abex
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
package net.runelite.pluginhub.apirecorder;

import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.objectweb.asm.Opcodes;

@RequiredArgsConstructor
public class API
{
	@Getter
	private final Set<String> apis;

	public API()
	{
		this(new HashSet<>());
	}

	public static void encode(OutputStream os, Stream<String> stream) throws IOException
	{
		DeflaterOutputStream dos = new DeflaterOutputStream(os);
		Writer out = new OutputStreamWriter(dos, StandardCharsets.UTF_8);
		stream.sorted()
			.forEach(it -> write(out, it));
		out.flush();
		dos.finish();
	}

	@SneakyThrows
	private static void write(Writer w, String s)
	{
		w.write(s);
		w.write('\n');
	}

	public static API decode(InputStream is)
	{
		return decodePlain(new InflaterInputStream(is));
	}

	public static API decodePlain(InputStream is)
	{
		return new API(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
			.lines()
			.filter(line -> !line.isEmpty())
			.collect(ImmutableSet.toImmutableSet()));
	}

	public void encode(OutputStream os) throws IOException
	{
		encode(os, apis.stream());
	}

	public Stream<String> missingFrom(API other)
	{
		return apis.stream()
			.filter(a -> !other.getApis().contains(a));
	}

	public Stream<String> in(API other)
	{
		return apis.stream()
			.filter(a -> other.getApis().contains(a));
	}

	public static String modifiersToString(int modifiers, boolean member)
	{
		String s = "";
		if (Modifier.isAbstract(modifiers))
		{
			s += "a";
		}
		if (!member && Modifier.isInterface(modifiers))
		{
			s += "i";
		}
		if (Modifier.isPublic(modifiers))
		{
			s += "b";
		}
		else if (Modifier.isProtected(modifiers))
		{
			s += "t";
		}
		else if (Modifier.isPrivate(modifiers))
		{
			s += "v";
		}
		else
		{
			s += "g";
		}
		if (member && Modifier.isStatic(modifiers))
		{
			s += "s";
		}
		if (Modifier.isFinal(modifiers))
		{
			s += "f";
		}
		return s;
	}

	public void recordClass(int modifiers, String descriptor)
	{
		if (descriptor != null)
		{
			apis.add(descriptor + modifiersToString(modifiers, false));
		}
	}

	public void recordMethod(int modifiers, String classDescriptor, CharSequence name, String descriptor)
	{
		if (classDescriptor != null & descriptor != null)
		{
			String mod = modifiersToString(modifiers, true);
			if ((modifiers & Opcodes.ACC_VARARGS) != 0)
			{
				mod += "v";
			}
			apis.add(classDescriptor + "." + name + descriptor + ":" + mod);
		}
	}

	public void recordField(int modifiers, String classDescriptor, CharSequence name, String descriptor, Object constantValue)
	{
		if (classDescriptor != null && name != null && descriptor != null)
		{
			String mods = modifiersToString(modifiers, true);
			apis.add(classDescriptor + "." + name + ":" + descriptor + ":" + mods + ":" + (constantValue == null ? "" : constantValue));
		}
	}
}
