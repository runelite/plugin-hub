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

import com.google.common.base.Strings;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecorderPlugin implements Plugin
{
	@Override
	public String getName()
	{
		return "RuneLiteAPIRecorder";
	}

	@Override
	public void init(JavacTask task, String... args)
	{
		String buildDir = System.getenv("runelite.pluginhub.package.buildDir");
		if (Strings.isNullOrEmpty(buildDir))
		{
			return;
		}

		RecordingTreeScanner scanner = new RecordingTreeScanner(task);

		task.addTaskListener(new TaskListener()
		{
			@Override
			public void finished(TaskEvent e)
			{
				switch (e.getKind())
				{
					case ANALYZE:
						if (log.isDebugEnabled())
						{
							log.info("{}", PrintingScanner.print(e.getCompilationUnit()));
						}
						try
						{
							scanner.scan(e.getCompilationUnit(), null);
						}
						catch (Exception ex)
						{
							log.warn("failed to scan", ex);
							scanner.setPartial(true);
						}
						break;
					case COMPILATION:
						if (!scanner.isPartial())
						{
							try (FileOutputStream fos = new FileOutputStream(new File(buildDir, "api")))
							{
								scanner.getApi().encode(fos);
							}
							catch (IOException ex)
							{
								throw new RuntimeException(ex);
							}
						}
						break;
				}
			}
		});
	}
}
