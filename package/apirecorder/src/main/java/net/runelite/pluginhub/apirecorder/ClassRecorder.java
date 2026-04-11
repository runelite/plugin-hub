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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Records the API provided by any classes passed through it
 */
public class ClassRecorder extends ClassVisitor
{
	@Getter
	private final API api = new API();

	private String className;

	public ClassRecorder()
	{
		super(Opcodes.ASM7);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
	{
		this.className = "L" + name + ";";
		api.recordClass(access, className);
		api.recordClassHierarchy(this.className, "L" + superName + ";");
		for (String iface : interfaces)
		{
			api.recordClassHierarchy(this.className, "L" + iface + ";");
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value)
	{
		if (!Modifier.isPrivate(access))
		{
			api.recordField(access, className, name, descriptor, value);
		}
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
	{
		if (!Modifier.isPrivate(access))
		{
			api.recordMethod(access, className, name, descriptor);
		}
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	public void recordClass(File jarFile) throws IOException
	{
		try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile)))
		{
			for (JarEntry je; (je = jis.getNextJarEntry()) != null; )
			{
				String fileName = je.getName();
				if (!fileName.endsWith(".class"))
				{
					continue;
				}

				ClassReader cr = new ClassReader(jis);
				cr.accept(this, ClassReader.SKIP_CODE);
			}
		}
	}

	public static void main(String... classes) throws IOException
	{
		var cr = new ClassRecorder();
		Iterator<String> args = List.of(classes).iterator();
		File out = new File(args.next());
		for (String fi; args.hasNext(); )
		{
			fi = args.next();
			cr.recordClass(new File(fi));
		}
		try (OutputStream os = new FileOutputStream(out))
		{
			cr.getApi().encode(os);
		}
	}
}
