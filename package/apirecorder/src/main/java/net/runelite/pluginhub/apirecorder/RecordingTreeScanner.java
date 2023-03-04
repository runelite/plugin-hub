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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Opcodes;

/**
 * Records all the API consumed of any nodes passed through it
 */
@Slf4j
class RecordingTreeScanner extends TreePathScanner<Void, Void>
{
	@Getter
	private final API api = new API();

	private final Map<String, Boolean> jvmClassCache = new HashMap<>();

	private final Trees trees;
	private final Elements elements;
	private final Types types;

	@Getter
	@Setter
	private boolean partial;

	public RecordingTreeScanner(JavacTask task)
	{
		this.trees = Trees.instance(task);
		this.elements = task.getElements();
		this.types = task.getTypes();
	}

	@Override
	public Void visitImport(ImportTree node, Void unused)
	{
		return null;
	}

	@Override
	public Void visitPackage(PackageTree node, Void unused)
	{
		// we don't want to record the pkg ident
		return null;
	}

	@Override
	public Void visitIdentifier(IdentifierTree node, Void unused)
	{
		recordElement(trees.getElement(getCurrentPath()));
		return null;
	}

	@Override
	public Void visitMemberSelect(MemberSelectTree node, Void unused)
	{
		recordElement(trees.getElement(getCurrentPath()));
		return super.visitMemberSelect(node, unused);
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, Void unused)
	{
		recordElement(trees.getElement(getCurrentPath()));
		return super.visitMethodInvocation(node, unused);
	}

	@Override
	public Void visitNewClass(NewClassTree node, Void unused)
	{
		recordElement(trees.getElement(getCurrentPath()));
		return super.visitNewClass(node, unused);
	}

	@SneakyThrows
	private boolean shouldRecord(Element element)
	{
		if (element instanceof TypeElement)
		{
			// there isn't a particularly nice way to find where a symbol is resolved from in the public api
			JavaFileObject classfile = ((Symbol.ClassSymbol) element).classfile;
			if (classfile == null || classfile.getKind() == JavaFileObject.Kind.SOURCE)
			{
				return false;
			}

			TypeMirror tm = element.asType();
			if (tm instanceof DeclaredType)
			{
				Element e = ((DeclaredType) tm).asElement();
				String fqn = elements.getBinaryName((TypeElement) e).toString();
				return jvmClassCache.computeIfAbsent(fqn, name ->
					ClassLoader.getPlatformClassLoader()
						.getResource(fqn.replace('.', '/') + ".class") == null);
			}
			else
			{
				unexpected(tm);
			}
		}
		else if (element instanceof PackageElement)
		{
			return false;
		}
		else
		{
			if (element.getEnclosingElement() != null && element.getEnclosingElement() != element)
			{
				return shouldRecord(element.getEnclosingElement());
			}
			else
			{
				unexpected(element);
			}
		}
		return false;
	}

	private void recordElement(Element element)
	{
		if (element == null)
		{
			unexpected(element);
		}

		if (!shouldRecord(element))
		{
			return;
		}

		if (element instanceof ExecutableElement)
		{
			ExecutableElement et = (ExecutableElement) element;
			StringBuilder desc = new StringBuilder();
			boolean ok = true;
			String n;
			desc.append('(');
			for (VariableElement t : et.getParameters())
			{
				n = typeDescriptor(t);
				ok &= n != null;
				desc.append(n);
			}
			desc.append(')');
			n = typeDescriptor(et.getReturnType());
			ok &= n != null;
			desc.append(n);

			if (ok)
			{
				api.recordMethod(
					modifiers(et),
					typeDescriptor(et.getEnclosingElement()),
					element.getSimpleName(),
					desc.toString());
			}
		}
		else if (element instanceof VariableElement)
		{
			switch (element.getKind())
			{
				case FIELD:
				case ENUM_CONSTANT:
					if ("class".equals(element.getSimpleName().toString()))
					{
						return;
					}

					api.recordField(
						modifiers(element),
						typeDescriptor(element.getEnclosingElement()),
						element.getSimpleName(),
						typeDescriptor(element.asType()),
						((VariableElement) element).getConstantValue());
					break;
				case RESOURCE_VARIABLE:
				case LOCAL_VARIABLE:
				case PARAMETER:
					return;
				default:
					unexpected(element);
					return;
			}
		}
		else if (element instanceof TypeElement)
		{
			api.recordClass(
				modifiers(element),
				typeDescriptor(element));
		}
		else
		{
			unexpected(element);
		}
	}

	private int modifiers(Element e)
	{
		// element.getModifiers is not complete, so we use the internal api
		// however some of it's bits are different from the classfile format's
		long flags = ((Symbol) e).flags_field;
		if ((flags & Flags.VARARGS) != 0)
		{
			flags |= Opcodes.ACC_VARARGS;
		}
		if (((flags & Flags.DEFAULT) != 0) && ((flags & Opcodes.ACC_INTERFACE) == 0))
		{
			flags &= ~Opcodes.ACC_ABSTRACT;
		}
		return (int) flags;
	}

	private String typeDescriptor(Element element)
	{
		if (element instanceof TypeElement || element instanceof VariableElement)
		{
			return typeDescriptor(element.asType());
		}
		else
		{
			unexpected(element);
			return null;
		}
	}

	private String typeDescriptor(TypeMirror type)
	{
		switch (type.getKind())
		{
			case VOID:
				return "V";
			case BOOLEAN:
				return "Z";
			case CHAR:
				return "C";
			case BYTE:
				return "B";
			case SHORT:
				return "S";
			case INT:
				return "I";
			case FLOAT:
				return "F";
			case LONG:
				return "J";
			case DOUBLE:
				return "D";
		}
		if (type instanceof ArrayType)
		{
			ArrayType at = (ArrayType) type;
			return "[" + typeDescriptor(at.getComponentType());
		}
		else if (type instanceof DeclaredType)
		{
			Element e = ((DeclaredType) type).asElement();
			// returns the fqn, not binary name
			String fqn = elements.getBinaryName((TypeElement) e).toString();
			return "L" + fqn.replace('.', '/') + ";";
		}
		else if (type instanceof TypeVariable)
		{
			return typeDescriptor(types.erasure(type));
		}
		else
		{
			unexpected(type);
			return null;
		}
	}

	private void unexpected(Element element)
	{
		if (element == null)
		{
			log.warn("unexpected null", new Exception());
		}
		else
		{
			log.warn("Unexpected {} {} {}", element.getKind(), element.getClass(), element, new Exception());
		}

		unexpected();
	}

	private void unexpected(TypeMirror mirror)
	{
		if (mirror == null)
		{
			log.warn("unexpected null type", new Exception());
		}
		else
		{
			log.warn("Unexpected {} {} {}", mirror.getKind(), mirror.getClass(), mirror, new Exception());
		}

		unexpected();
	}

	private void unexpected()
	{
		partial = true;
		TreePath p = getCurrentPath();
		for (int i = 0; p.getParentPath() != null && i < 1; p = p.getParentPath(), i++) ;
		log.info("{}", PrintingScanner.print(p.getLeaf()));
	}
}
