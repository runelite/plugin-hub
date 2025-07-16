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

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.pluginhub.uploader.ManifestDiff;
import net.runelite.pluginhub.uploader.PluginHubManifest;
import net.runelite.pluginhub.uploader.UploadConfiguration;
import net.runelite.pluginhub.uploader.Util;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
public class Packager implements Closeable
{
	private static final File PLUGIN_ROOT = new File("./plugins");
	public static final File PACKAGE_ROOT = new File("./package/").getAbsoluteFile();
	private static final File ARTIFACT_DIR = new File("/tmp/jars");

	private Semaphore apiCheckSemaphore = new Semaphore(8);
	private Semaphore downloadSemaphore = new Semaphore(2);
	private Semaphore buildSemaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
	private Semaphore uploadSemaphore = new Semaphore(2);

	private final List<File> buildList;

	@Getter
	private final String runeliteVersion;

	@Setter
	private String apiFilesVersion;
	private PluginHubManifest.ManifestFull oldManifest;
	private Map<String, PluginHubManifest.JarData> oldJarData = ImmutableMap.of();
	private Map<String, PluginHubManifest.DisplayData> oldDisplayData = ImmutableMap.of();

	@Getter
	private final UploadConfiguration uploadConfig = new UploadConfiguration();

	private final AtomicInteger numDone = new AtomicInteger(0);
	private final int numTotal;

	@Setter
	private boolean alwaysPrintLog;

	@Getter
	private boolean failed;

	private boolean isIncrementalRebuild;

	private final StringBuilder buildSummary = new StringBuilder();

	private ManifestDiff diff = new ManifestDiff();

	public Packager(List<File> buildList) throws IOException
	{
		this.buildList = buildList;
		this.numTotal = buildList.size();
		this.runeliteVersion = Util.readRLVersion();
	}

	public void buildPlugins() throws IOException
	{
		if (apiFilesVersion != null)
		{
			try
			{
				oldManifest = uploadConfig.getManifest(apiFilesVersion, null);
				oldJarData = oldManifest.getJars()
					.stream().collect(ImmutableMap.toImmutableMap(PluginHubManifest.JarData::getInternalName, Function.identity()));
				oldDisplayData = oldManifest.getDisplay()
					.stream().collect(ImmutableMap.toImmutableMap(PluginHubManifest.DisplayData::getInternalName, Function.identity()));
				diff.setOldManifestVersion(apiFilesVersion);
			}
			catch (RuntimeException | IOException e)
			{
				log.info("Unable to download previous manifest, doing full build", e);
				apiFilesVersion = null;
			}
		}

		if (uploadConfig.isComplete())
		{
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_JAR).build());
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_ICON).build());
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_API).build());
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_LOG).build());
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_SOURCE).build());
			uploadConfig.mkdirs(uploadConfig.getRoot().newBuilder().addPathSegment(UploadConfiguration.DIR_MANIFEST).build());
		}

		ARTIFACT_DIR.mkdirs();

		Queue<File> buildQueue = Queues.synchronizedQueue(new ArrayDeque<>(buildList));
		List<Thread> buildThreads = IntStream.range(0, 8)
			.mapToObj(v ->
			{
				Thread t = new Thread(() ->
				{
					for (File plugin; (plugin = buildQueue.poll()) != null; )
					{
						buildPlugin(plugin);
					}
				});
				t.start();
				return t;
			}).collect(Collectors.toList());

		for (Thread buildThread : buildThreads)
		{
			try
			{
				buildThread.join();
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}

		String diffJSON = Util.GSON.toJson(diff);
		log.debug("manifest change: {}", diffJSON);

		try (FileOutputStream fos = new FileOutputStream("/tmp/manifest_diff"))
		{
			fos.write(diffJSON.getBytes(StandardCharsets.UTF_8));
		}
	}

	private void buildPlugin(File plugin)
	{
		diff.getRemove().add(plugin.getName());

		if (!plugin.exists())
		{
			return;
		}

		try (Plugin p = new Plugin(plugin))
		{
			try
			{
				PluginHubManifest.JarData oldJarData = this.oldJarData.get(p.getInternalName());
				if (isIncrementalRebuild && oldJarData != null)
				{
					try (Closeable ignored = acquireAPICheck(p))
					{
						if (!p.rebuildNeeded(uploadConfig, oldJarData))
						{
							diff.getCopyFromOld().add(p.getInternalName());
							diff.getRemove().remove(p.getInternalName());
							return;
						}
					}
				}
				try (Closeable ignored = acquireDownload(p))
				{
					p.download();
				}
				try (Closeable ignored = acquireBuild(p))
				{
					p.build(runeliteVersion, alwaysPrintLog);
				}
				String logURL = "";
				if (uploadConfig.isComplete())
				{
					try (Closeable ignored = acquireUpload(p))
					{
						p.upload(uploadConfig);
					}

					// outside the semaphore so the timing gets uploaded too
					logURL = p.uploadLog(uploadConfig);
				}

				p.copyArtifacts(ARTIFACT_DIR);

				diff.getAddJarData().add(p.getJarData());
				diff.getAddDisplayData().add(p.getDisplayData());
				log.info("{}: done in {}ms [{}/{}]", p.getInternalName(), p.getBuildTimeMS(), numDone.get() + 1, numTotal);

				if (!p.getApiFile().exists())
				{
					logToSummary("{} failed to write the api record: {}", p.getInternalName(), logURL);
				}
			}
			catch (PluginBuildException e)
			{
				failed = true;
				p.writeLog("package failed\n", e);
				if (!alwaysPrintLog)
				{
					Files.asCharSource(p.getLogFile(), StandardCharsets.UTF_8).copyTo(System.out);
				}

				PluginHubManifest.DisplayData oldDisplayData = this.oldDisplayData.get(p.getInternalName());

				if (uploadConfig.isComplete())
				{
					String logURL = p.uploadLog(uploadConfig);
					if (oldDisplayData != null && oldDisplayData.getBuildFailAt() != null)
					{
						long daysFailed = Instant.ofEpochSecond(oldDisplayData.getBuildFailAt()).until(Instant.now(), ChronoUnit.DAYS);
						logToSummary("{} failed ({} days): {}", p.getInternalName(), daysFailed, logURL);
					}
					else
					{
						logToSummary("{} failed: {}", p.getInternalName(), logURL);
					}
				}
				else
				{
					logToSummary("{} failed", p.getInternalName());
				}

				if (oldDisplayData != null)
				{
					if (oldDisplayData.getBuildFailAt() == null)
					{
						oldDisplayData.setBuildFailAt(Instant.now().getEpochSecond());
					}

					oldDisplayData.setUnavailableReason(null);
					diff.getAddDisplayData().add(oldDisplayData);
				}
			}
			finally
			{
				if (alwaysPrintLog)
				{
					Files.asCharSource(p.getLogFile(), StandardCharsets.UTF_8).copyTo(System.out);
				}
			}
		}
		catch (DisabledPluginException e)
		{
			if (e.isIncludeInUnavailable())
			{
				PluginHubManifest.DisplayData oldDisplayData = this.oldDisplayData.get(e.getInternalName());
				if (oldDisplayData == null)
				{
					logToSummary("unavailible plugin {} wasn't in the previous manifest", e.getInternalName());
				}
				else
				{
					oldDisplayData.setUnavailableReason(e.getReason());
					diff.getAddDisplayData().add(oldDisplayData);
				}
			}
		}
		catch (PluginBuildException e)
		{
			failed = true;
			logToSummary("", e);
		}
		catch (Exception e)
		{
			failed = true;
			logToSummary("{}: crashed the build script: ", plugin.getName(), e);
		}
		finally
		{
			numDone.addAndGet(1);
		}
	}

	private void logToSummary(String message, Object... args)
	{
		log.info(message, args);
		FormattingTuple fmt = MessageFormatter.arrayFormat(message, args);
		synchronized (buildSummary)
		{
			buildSummary.append(fmt.getMessage()).append('\n');
		}
	}

	public String getBuildSummary()
	{
		return buildSummary.toString();
	}

	private Closeable acquireAPICheck(Plugin plugin)
	{
		return section(plugin, "apicheck", apiCheckSemaphore);
	}

	private Closeable acquireDownload(Plugin plugin)
	{
		return section(plugin, "download", downloadSemaphore);
	}

	private Closeable acquireBuild(Plugin plugin)
	{
		return section(plugin, "build", buildSemaphore);
	}

	private Closeable acquireUpload(Plugin plugin)
	{
		return section(plugin, "upload", uploadSemaphore);
	}

	private Closeable section(Plugin p, String name, Semaphore s)
	{
		try
		{
			s.acquire();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		Stopwatch time = Stopwatch.createStarted();
		return () ->
		{
			long ms = time.stop()
				.elapsed(TimeUnit.MILLISECONDS);
			p.setBuildTimeMS(p.getBuildTimeMS() + ms);
			p.writeLog("{}: {}ms\n", name, ms);
			s.release();
		};
	}

	public void setIsIncrementalRebuild(boolean incremental)
	{
		this.isIncrementalRebuild = incremental;
		diff.setIgnoreOldManifest(incremental);
	}

	@Override
	public void close()
	{
		uploadConfig.close();
	}

	public static void main(String... args) throws Exception
	{
		boolean isBuildingAll = false;
		boolean testFailure = false;

		String apiFilesVersion = System.getenv("API_FILES_VERSION");
		if (apiFilesVersion != null)
		{
			apiFilesVersion = apiFilesVersion.trim();
			if (apiFilesVersion.isEmpty())
			{
				apiFilesVersion = null;
			}
		}

		String range = System.getenv("PACKAGE_COMMIT_RANGE");

		List<File> buildList;
		if (args.length != 0)
		{
			buildList = Stream.of(args)
				.map(File::new)
				.collect(Collectors.toList());
		}
		else if ("ALL".equals(System.getenv("FORCE_BUILD")))
		{
			buildList = listAllPlugins();
			isBuildingAll = true;
		}
		else if (!Strings.isNullOrEmpty(System.getenv("FORCE_BUILD")))
		{
			buildList = StreamSupport.stream(
					Splitter.on(',')
						.trimResults()
						.omitEmptyStrings()
						.split(System.getenv("FORCE_BUILD"))
						.spliterator(), false)
				.map(name -> new File(PLUGIN_ROOT, name))
				.collect(Collectors.toList());
		}
		else if (!Strings.isNullOrEmpty(range))
		{
			Process gitdiff = new ProcessBuilder("git", "diff", "--name-only", range)
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();

			boolean doAll = false;
			boolean doPackageTests = false;
			buildList = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(gitdiff.getInputStream())))
			{
				for (String line; (line = br.readLine()) != null; )
				{
					if ("runelite.version".equals(line))
					{
						doAll = true;
					}
					else if (line.startsWith("plugins/"))
					{
						buildList.add(new File(line));
					}
					else if (line.startsWith("package/") || line.startsWith("templateplugin/") || line.startsWith("create_new_plugin.py"))
					{
						doPackageTests = true;
					}
				}
			}

			if (doPackageTests)
			{
				testFailure |= new ProcessBuilder(new File(PACKAGE_ROOT, "gradlew").getAbsolutePath(), "--console=plain", "test")
					.directory(PACKAGE_ROOT)
					.inheritIO()
					.start()
					.waitFor() != 0;
				testFailure |= new ProcessBuilder(new File(PACKAGE_ROOT, "gradlew").getAbsolutePath(), "--console=plain", ":verifyAll")
					.directory(new File(PACKAGE_ROOT, "verification-template"))
					.inheritIO()
					.start()
					.waitFor() != 0;
			}

			if (doAll)
			{
				isBuildingAll = true;
				buildList = listAllPlugins();
			}

			gitdiff.waitFor(1, TimeUnit.SECONDS);
			if (gitdiff.exitValue() != 0)
			{
				throw new RuntimeException("git diff exited with " + gitdiff.exitValue());
			}
		}
		else
		{
			throw new RuntimeException("missing env vars");
		}

		if (apiFilesVersion == null && !Strings.isNullOrEmpty(range))
		{
			String commit = range.substring(0, range.indexOf(".."));
			Process gitShow = new ProcessBuilder("git", "show", commit + ":runelite.version")
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();

			apiFilesVersion = new String(ByteStreams.toByteArray(gitShow.getInputStream()), StandardCharsets.UTF_8)
				.trim();

			gitShow.waitFor(1, TimeUnit.SECONDS);
			if (gitShow.exitValue() != 0)
			{
				throw new RuntimeException("git show exited with " + gitShow.exitValue());
			}
		}

		boolean failed;
		try (Packager pkg = new Packager(buildList))
		{
			pkg.getUploadConfig().fromEnvironment(pkg.getRuneliteVersion());
			pkg.setAlwaysPrintLog(!pkg.getUploadConfig().isComplete());
			pkg.setIsIncrementalRebuild(isBuildingAll);
			pkg.setApiFilesVersion(apiFilesVersion);
			pkg.buildPlugins();
			failed = pkg.isFailed();
			if (isBuildingAll)
			{
				String summary = pkg.getBuildSummary();
				if (!summary.isEmpty())
				{
					log.info("Failures:\n{}", summary);
				}
				if (!failed)
				{
					log.info("All plugins succeeded");
				}
			}
		}

		if (testFailure || (failed && !isBuildingAll))
		{
			System.exit(1);
		}
	}

	static List<File> listAllPlugins()
	{
		return Arrays.asList(PLUGIN_ROOT.listFiles());
	}
}
