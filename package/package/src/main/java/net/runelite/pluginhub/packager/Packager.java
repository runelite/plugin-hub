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
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

@Slf4j
public class Packager
{
	private static final File PLUGIN_ROOT = new File("./plugins");
	public static final File PACKAGE_ROOT = new File("./package/").getAbsoluteFile();

	private Semaphore downloadSemaphore = new Semaphore(2);
	private Semaphore buildSemaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
	private Semaphore uploadSemaphore = new Semaphore(2);

	private final List<File> buildList;

	@Getter
	private final String runeliteVersion;

	@Getter
	private final UploadConfiguration uploadConfig = new UploadConfiguration();

	private final AtomicInteger numDone = new AtomicInteger(0);
	private final int numTotal;

	@Setter
	private boolean ignoreOldManifest;

	@Setter
	private boolean alwaysPrintLog;

	public Packager(List<File> buildList) throws IOException
	{
		this.buildList = buildList;
		this.numTotal = buildList.size();
		this.runeliteVersion = readRLVersion();
	}

	Set<ExternalPluginManifest> newManifests = Sets.newConcurrentHashSet();
	Set<String> remove = Sets.newConcurrentHashSet();

	public void buildPlugins()
		throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException
	{
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

		if (uploadConfig.isComplete())
		{
			Gson gson = new Gson();
			HttpUrl manifestURL = uploadConfig.getUploadRepoRoot().newBuilder()
				.addPathSegment("manifest.js")
				.build();

			List<ExternalPluginManifest> manifests = new ArrayList<>();
			if (!ignoreOldManifest)
			{
				try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
					.url(manifestURL)
					.get()
					.build())
					.execute())
				{
					if (res.code() != 404)
					{
						Util.check(res);

						BufferedSource src = res.body().source();

						byte[] signature = new byte[src.readInt()];
						src.readFully(signature);

						byte[] data = src.readByteArray();
						Signature s = Signature.getInstance("SHA256withRSA");
						s.initVerify(uploadConfig.getCert());
						s.update(data);

						if (!s.verify(signature))
						{
							throw new RuntimeException("Unable to verify external plugin manifest");
						}

						manifests = gson.fromJson(new String(data, StandardCharsets.UTF_8),
							new TypeToken<List<ExternalPluginManifest>>()
							{
							}.getType());
					}
				}
			}

			manifests.removeIf(m -> remove.contains(m.getInternalName()));
			manifests.addAll(newManifests);
			manifests.sort(Comparator.comparing(ExternalPluginManifest::getInternalName));

			{
				byte[] data = gson.toJson(manifests).getBytes(StandardCharsets.UTF_8);
				Signature s = Signature.getInstance("SHA256withRSA");
				s.initSign(uploadConfig.getKey());
				s.update(data);
				byte[] sig = s.sign();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				new DataOutputStream(out).writeInt(sig.length);
				out.write(sig);
				out.write(data);
				byte[] manifest = out.toByteArray();

				try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
					.url(manifestURL)
					.put(RequestBody.create(null, manifest))
					.build())
					.execute())
				{
					Util.check(res);
				}
			}

			uploadConfig.getClient().connectionPool().evictAll();
		}
	}


	private void buildPlugin(File plugin)
	{
		remove.add(plugin.getName());

		if (!plugin.exists())
		{
			return;
		}

		try (Plugin p = new Plugin(plugin))
		{
			try
			{
				try (Closeable ignored = acquireDownload(p))
				{
					p.download();
				}
				try (Closeable ignored = acquireBuild(p))
				{
					p.build(runeliteVersion);
					p.assembleManifest();
				}
				if (uploadConfig.isComplete())
				{
					try (Closeable ignored = acquireUpload(p))
					{
						p.upload(uploadConfig);
					}

					// outside the semaphore so the timing gets uploaded too
					p.uploadLog(uploadConfig);
				}

				newManifests.add(p.getManifest());
				log.info("{}: done in {}ms [{}/{}]", p.getInternalName(), p.getBuildTimeMS(), numDone.get() + 1, numTotal);
			}
			catch (PluginBuildException e)
			{
				p.writeLog("package failed\n", e);
				if (!alwaysPrintLog)
				{
					Files.asCharSource(p.getLogFile(), StandardCharsets.UTF_8).copyTo(System.out);
				}

				if (uploadConfig.isComplete())
				{
					p.uploadLog(uploadConfig);
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
			log.info("{}", e.getMessage());
		}
		catch (PluginBuildException e)
		{
			log.info("", e);
		}
		catch (Exception e)
		{
			log.warn("{}: crashed the build script: ", plugin.getName(), e);
		}
		finally
		{
			numDone.addAndGet(1);
		}
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

	public static String readRLVersion() throws IOException
	{
		return Files.asCharSource(new File("./runelite.version"), StandardCharsets.UTF_8).read().trim();
	}


	public static void main(String... args) throws Exception
	{
		boolean isBuildingAll = false;
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
		else if (!Strings.isNullOrEmpty(System.getenv("PACKAGE_COMMIT_RANGE")))
		{
			Process gitdiff = new ProcessBuilder("git", "diff", "--name-only", System.getenv("PACKAGE_COMMIT_RANGE"))
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
				new ProcessBuilder(new File(PACKAGE_ROOT, "gradlew").getAbsolutePath(), "--console=plain", "test")
					.directory(PACKAGE_ROOT)
					.inheritIO()
					.start()
					.waitFor();
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

		Packager pkg = new Packager(buildList);
		pkg.getUploadConfig().fromEnvironment(pkg.getRuneliteVersion());
		pkg.setAlwaysPrintLog(!pkg.getUploadConfig().isComplete());
		pkg.setIgnoreOldManifest(isBuildingAll);
		pkg.buildPlugins();
	}

	static List<File> listAllPlugins()
	{
		return Arrays.asList(PLUGIN_ROOT.listFiles());
	}
}
