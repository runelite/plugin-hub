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
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrintingScanner extends TreeScanner<Void, Void>
{
	private final StringBuilder sb = new StringBuilder();
	private int tabs = 0;

	public static String print(Tree n)
	{
		PrintingScanner s = new PrintingScanner();
		s.scan(n, null);
		return s.toString();
	}

	public void indent()
	{
		for (int i = 0; i < tabs; i++)
		{
			sb.append('\t');
		}
	}

	@Override
	public Void scan(Tree tree, Void unused)
	{
		indent();
		if (tree == null)
		{
			sb.append("null\n");
			return null;
		}
		sb.append(tree.getKind()).append("(\n");
		tabs++;
		super.scan(tree, unused);
		tabs--;
		indent();
		sb.append(")\n");
		return null;
	}

	@Override
	public Void visitIdentifier(IdentifierTree node, Void unused)
	{
		indent();
		sb.append(node.getName()).append('\n');
		return super.visitIdentifier(node, unused);
	}

	@Override
	public Void visitMemberSelect(MemberSelectTree node, Void unused)
	{
		super.visitMemberSelect(node, unused);
		indent();
		sb.append(node.getIdentifier()).append('\n');
		return null;
	}

	@Override
	public String toString()
	{
		return sb.toString();
	}
}
