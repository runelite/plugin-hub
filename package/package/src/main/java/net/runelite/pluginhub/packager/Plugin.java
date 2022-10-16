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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import net.runelite.pluginhub.apirecorder.API;
import net.runelite.pluginhub.apirecorder.ClassRecorder;
import net.runelite.pluginhub.uploader.ExternalPluginManifest;
import net.runelite.pluginhub.uploader.UploadConfiguration;
import net.runelite.pluginhub.uploader.Util;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class Plugin implements Closeable
{
	private static final long MIB = 1024 * 1024;
	private static final long MAX_JAR_SIZE = 10 * MIB;
	private static final long MAX_SRC_SIZE = 10 * MIB;

	private static final Pattern PLUGIN_INTERNAL_NAME_TEST = Pattern.compile("^[a-z0-9-]+$");
	private static final Pattern REPOSITORY_TEST = Pattern.compile("^(https://github\\.com/.*)\\.git$");
	private static final Pattern COMMIT_TEST = Pattern.compile("^[a-fA-F0-9]{40}$");

	private static final String SUFFIX_JAR = ".jar";
	private static final String SUFFIX_SOURCES = "-sources.zip";
	private static final String SUFFIX_API = ".api";
	private static final String SUFFIX_ICON = ".png";

	private static final File TMP_ROOT;
	private static final File GRADLE_HOME;

	private static final API DISALLOWED_API;

	static
	{
		ImageIO.setUseCache(false);

		try
		{
			TMP_ROOT = Files.createTempDirectory("pluginhub-package").toFile();
			TMP_ROOT.deleteOnExit();

			GRADLE_HOME = new File(com.google.common.io.Files.asCharSource(new File(Packager.PACKAGE_ROOT, "build/gradleHome"), StandardCharsets.UTF_8).read().trim());
			if (!GRADLE_HOME.exists())
			{
				throw new RuntimeException("gradle home has moved");
			}

			try (InputStream is = Packager.class.getResourceAsStream("disallowed-apis.txt"))
			{
				DISALLOWED_API = API.decodePlain(is);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private final File pluginCommitDescriptor;

	@Getter
	private final String internalName;

	private final File buildDirectory;

	@VisibleForTesting
	final File repositoryDirectory;

	private final File jarFile;
	private final File srcZipFile;
	private final File iconFile;

	@Getter
	private final File apiFile;

	@Getter
	private final File logFile;

	@Getter
	private FileOutputStream log;

	@Nullable
	private final String warning;

	private final String repositoryURL;
	private final String commit;
	private final String defaultSupportURL;

	@Getter
	private final ExternalPluginManifest manifest = new ExternalPluginManifest();

	@Getter
	@Setter
	private long buildTimeMS;

	public Plugin(File pluginCommitDescriptor) throws IOException, DisabledPluginException, PluginBuildException
	{
		this.pluginCommitDescriptor = pluginCommitDescriptor;
		internalName = pluginCommitDescriptor.getName();
		if (!PLUGIN_INTERNAL_NAME_TEST.matcher(internalName).matches())
		{
			throw PluginBuildException.of(internalName, "invalid plugin file name \"{}\"", internalName)
				.withHelp("plugin file names must be lowercase alphanumeric + dashes. try: \""
					+ internalName.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "\"")
				.withFile(pluginCommitDescriptor);
		}

		Properties cd = loadProperties(pluginCommitDescriptor);

		String disabled = cd.getProperty("disabled");
		if (!Strings.isNullOrEmpty(disabled))
		{
			throw new DisabledPluginException(internalName, disabled);
		}

		repositoryURL = (String) cd.remove("repository");
		if (repositoryURL == null)
		{
			throw PluginBuildException.of(internalName, "repository is missing from {}", pluginCommitDescriptor)
				.withFile(pluginCommitDescriptor);
		}

		Matcher repoMatcher = REPOSITORY_TEST.matcher(repositoryURL);
		if (!repoMatcher.matches())
		{
			throw PluginBuildException.of(internalName, "repository is not an accepted url")
				.withFileLine(pluginCommitDescriptor, "repository=" + repositoryURL)
				.withHelp(() ->
				{
					if (!repositoryURL.startsWith("https"))
					{
						return "repositories must be https clone urls, not git:";
					}
					if (!repositoryURL.contains("github"))
					{
						return "repositories must be hosted on GitHub.com";
					}
					if (!repositoryURL.endsWith(".git"))
					{
						return "repository must be a clone url ~ it should end with .git";
					}
					return null;
				});
		}
		String repoRoot = repoMatcher.group(1);

		commit = (String) cd.remove("commit");
		if (!COMMIT_TEST.matcher(commit).matches())
		{
			throw PluginBuildException.of(internalName, "commit must be a full 40 character sha1sum")
				.withFileLine(pluginCommitDescriptor, "commit=" + commit);
		}

		defaultSupportURL = repoRoot + "/tree/" + commit;

		warning = (String) cd.remove("warning");
		cd.remove("authors");

		for (Map.Entry<Object, Object> extra : cd.entrySet())
		{
			throw PluginBuildException.of(internalName, "unexpected key in commit descriptor")
				.withFileLine(pluginCommitDescriptor, extra.getKey() + "=" + extra.getValue());
		}

		buildDirectory = new File(TMP_ROOT, internalName);
		if (!buildDirectory.mkdirs())
		{
			throw new RuntimeException("Unable to create temp directory");
		}
		repositoryDirectory = new File(buildDirectory, "repo");
		logFile = new File(buildDirectory, "log");
		log = new FileOutputStream(logFile, true);
		jarFile = new File(buildDirectory, "plugin.jar");
		apiFile = new File(buildDirectory, "api");
		srcZipFile = new File(buildDirectory, "source.zip");
		iconFile = new File(repositoryDirectory, "icon.png");
	}

	private void waitAndCheck(Process process, String name, long timeout, TimeUnit timeoutUnit) throws PluginBuildException
	{
		try
		{
			if (!process.waitFor(timeout, timeoutUnit))
			{
				process.destroy();
				throw PluginBuildException.of(this, name + " failed to complete in a reasonable time");
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}

		if (process.exitValue() != 0)
		{
			throw PluginBuildException.of(this, name + " exited with " + process.exitValue());
		}
	}

	public boolean rebuildNeeded(UploadConfiguration uploadConfig, String previousVersion, API currentApi) throws IOException
	{
		if (previousVersion == null)
		{
			return true;
		}

		HttpUrl oldPluginRoot = uploadConfig.getVersionlessRoot().newBuilder()
			.addPathSegment(previousVersion)
			.addPathSegment(internalName)
			.build();

		try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
			.url(oldPluginRoot.newBuilder()
				.addPathSegment(commit + SUFFIX_API)
				.build())
			.get()
			.build()).execute())
		{
			if (res.code() == 404)
			{
				return true;
			}
			Util.check(res);

			String missing = API.decode(res.body().byteStream())
				.missingFrom(currentApi)
				.collect(Collectors.joining("\n"));

			if (!missing.isEmpty())
			{
				writeLog("API changed; rebuild needed. changed:\n{}\n", missing);
				return true;
			}

			HttpUrl pluginRoot = uploadConfig.getUploadRepoRoot().newBuilder()
				.addPathSegment(internalName)
				.build();

			uploadConfig.mkdirs(pluginRoot);
			uploadConfig.copy(oldPluginRoot, pluginRoot, commit + SUFFIX_JAR, true);
			uploadConfig.copy(oldPluginRoot, pluginRoot, commit + SUFFIX_API, true);
			uploadConfig.copy(oldPluginRoot, pluginRoot, commit + SUFFIX_SOURCES, true);
			uploadConfig.copy(oldPluginRoot, pluginRoot, commit + SUFFIX_ICON, false);

			return false;
		}
		catch (UncheckedIOException | IOException e)
		{
			writeLog("failed to check api compatibility\n", e);
			return true;
		}
	}

	public void download() throws IOException, PluginBuildException
	{
		Process gitclone = new ProcessBuilder("git", "clone",
			"--config", "advice.detachedHead=false",
			"--filter", "tree:0", "--no-checkout",
			this.repositoryURL, repositoryDirectory.getAbsolutePath())
			.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
			.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
			.start();
		waitAndCheck(gitclone, "git clone", 2, TimeUnit.MINUTES);


		Process gitcheckout = new ProcessBuilder("git", "checkout", commit + "^{commit}")
			.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
			.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
			.directory(repositoryDirectory)
			.start();
		waitAndCheck(gitcheckout, "git checkout", 2, TimeUnit.MINUTES);
	}

	public void build(String runeliteVersion) throws IOException, PluginBuildException
	{
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(repositoryDirectory.toPath(), "**.{gradle,gradle.kts}"))
		{
			for (Path path : ds)
			{
				String badLine = MoreFiles.asCharSource(path, StandardCharsets.UTF_8)
					.lines()
					.filter(l -> l.codePoints().map(cp ->
					{
						if (cp == '\t')
						{
							return 8;
						}
						else if (cp > 127)
						{
							// any special char is counted as 4 because there are some very wide special characters
							return 4;
						}
						return 1;
					}).sum() > 120)
					.findAny()
					.orElse(null);
				if (badLine != null)
				{
					throw PluginBuildException.of(this, "All gradle files must wrap at 120 characters or less")
						.withFileLine(path.toFile(), badLine);
				}
			}
		}

		try (
			CountingOutputStream cos = new CountingOutputStream(new FileOutputStream(srcZipFile));
			ZipOutputStream zos = new ZipOutputStream(cos))
		{
			@Value
			class Entry
			{
				Path path;
				String zipPath;
				long length;
			}

			List<Entry> core = new ArrayList<>();
			List<Entry> extras = new ArrayList<>();
			Files.walkFileTree(repositoryDirectory.toPath(), new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
				{
					if (dir.toString().contains(".git"))
					{
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
				{
					String zipPath = repositoryDirectory.toPath().relativize(path).toString().replace('\\', '/');
					(zipPath.contains(".gradle") || zipPath.startsWith("src/main/") ? core : extras)
						.add(new Entry(path, zipPath, path.toFile().length()));
					return FileVisitResult.CONTINUE;
				}
			});

			core.sort(Comparator.comparing(Entry::getZipPath));
			for (Entry e : core)
			{
				ZipEntry ze = new ZipEntry(e.zipPath);
				zos.putNextEntry(ze);
				Files.copy(e.path, zos);
				zos.closeEntry();
			}

			extras.sort(Comparator.comparing(Entry::getLength).thenComparing(Entry::getZipPath));
			for (Entry e : extras)
			{
				if (cos.getCount() + e.length > MAX_SRC_SIZE)
				{
					writeLog("File \"{}\" is skipped from the source archive as it would make it too big ({} MiB)\n", e.zipPath, e.length / MIB);
					continue;
				}

				ZipEntry ze = new ZipEntry(e.zipPath);
				zos.putNextEntry(ze);
				Files.copy(e.path, zos);
				zos.closeEntry();
			}
		}

		try (InputStream is = Plugin.class.getResourceAsStream("verification-metadata.xml"))
		{
			File metadataFile = new File(repositoryDirectory, "gradle/verification-metadata.xml");
			metadataFile.getParentFile().mkdir();
			Files.copy(is, metadataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		try (ProjectConnection con = GradleConnector.newConnector()
			.forProjectDirectory(repositoryDirectory)
			.useInstallation(GRADLE_HOME)
			.connect())
		{
			CancellationTokenSource cancel = GradleConnector.newCancellationTokenSource();
			BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
			String buildSuccess = "success";

			con.newBuild()
				.withArguments(
					"--no-build-cache",
					"--console=plain",
					"--init-script", new File("./package/target_init.gradle").getAbsolutePath())
				.setEnvironmentVariables(ImmutableMap.of(
					"runelite.pluginhub.package.apirecorder", new File(Packager.PACKAGE_ROOT, "apirecorder/build/libs/apirecorder.jar").toString(),
					"runelite.pluginhub.package.buildDir", buildDirectory.getAbsolutePath(),
					"runelite.pluginhub.package.runeliteVersion", runeliteVersion))
				.setJvmArguments("-Xmx768M", "-XX:+UseParallelGC")
				.setStandardOutput(log)
				.setStandardError(log)
				.forTasks("runelitePluginHubPackage", "runelitePluginHubManifest")
				.withCancellationToken(cancel.token())
				.run(new ResultHandler<>()
				{
					@Override
					public void onComplete(Void result)
					{
						queue.add(buildSuccess);
					}

					@Override
					public void onFailure(GradleConnectionException failure)
					{
						queue.add(failure);
					}
				});
			log.flush();

			Object output = queue.poll(5, TimeUnit.MINUTES);
			if (output == null)
			{
				cancel.cancel();
				throw PluginBuildException.of(this, "build did not complete within 5 minutes");
			}
			if (output == buildSuccess)
			{
				return;
			}
			else if (output instanceof GradleConnectionException)
			{
				throw PluginBuildException.of(this, "build failed", output);
			}
			throw new IllegalStateException(output.toString());
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void assembleManifest(boolean disallowedFatal) throws IOException, PluginBuildException
	{
		manifest.setInternalName(internalName);
		manifest.setCommit(commit);
		manifest.setWarning(warning);

		{
			Properties chunk = loadProperties(new File(buildDirectory, "chunk.properties"));

			String version = chunk.getProperty("version");
			if (Strings.isNullOrEmpty(version))
			{
				throw new IllegalStateException("version in empty");
			}

			if (version.endsWith("SNAPSHOT"))
			{
				version = commit.substring(0, 8);
			}

			manifest.setVersion(version);
		}

		{
			long size = jarFile.length();
			if (size > MAX_JAR_SIZE)
			{
				throw PluginBuildException.of(this, "the output jar is {}MiB, which is above our limit of 10MiB", size / MIB);
			}
			if (size > (MAX_JAR_SIZE * 8) / 10)
			{
				writeLog("warning: the output jar is {}MiB, which is nearing our limit of 10MiB\n", size / MIB);
			}
			manifest.setSize((int) size);
		}

		{
			long size = srcZipFile.length();
			if (size > MAX_SRC_SIZE + MIB) // allow the header to be a bit bigger
			{
				throw PluginBuildException.of(this, "the source archive is {}MiB, which is above our limit of 10MiB", size / MIB);
			}
		}

		manifest.setHash(com.google.common.io.Files.asByteSource(jarFile)
			.hash(Hashing.sha256())
			.toString());

		if (iconFile.exists())
		{
			long size = iconFile.length();
			if (size > 256 * 1024)
			{
				throw PluginBuildException.of(this, "icon.png is {}KiB, which is above our limit of 256KiB", size / 1024)
					.withFile(iconFile);
			}

			BufferedImage bimg;
			synchronized (ImageIO.class)
			{
				try
				{
					bimg = Objects.requireNonNull(ImageIO.read(iconFile));
				}
				catch (Exception e)
				{
					throw PluginBuildException.of(this, "icon is invalid", e)
						.withFile(iconFile);
				}
			}

			if (bimg.getWidth() * bimg.getHeight() > 50 * 100)
			{
				if (disallowedFatal)
				{
					throw PluginBuildException.of(this, "icon.png is too high-resolution. It should be 48x72 px")
						.withFile(iconFile);
				}
				else
				{
					writeLog("icon.png is too high-resolution. It should be 48x72 px\n");
				}
			}
			else
			{
				manifest.setHasIcon(true);
			}
		}

		Set<String> pluginClasses = new HashSet<>();
		Set<String> jarClasses = new HashSet<>();
		{
			ClassRecorder builtinApi = new ClassRecorder();

			try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile)))
			{
				for (JarEntry je; (je = jis.getNextJarEntry()) != null; )
				{
					String fileName = je.getName();
					if (!fileName.endsWith(".class"))
					{
						continue;
					}

					boolean isMultiRelease = fileName.startsWith("META-INF/versions");
					byte[] classData = ByteStreams.toByteArray(jis);

					try
					{
						new ClassReader(classData).accept(new ClassVisitor(Opcodes.ASM7, builtinApi)
						{
							boolean extendsPlugin;
							String name;

							@SneakyThrows
							@Override
							public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
							{
								if ((version & 0xFFFF) > Opcodes.V1_8
									&& !(isMultiRelease || fileName.endsWith("module-info.class")))
								{
									throw PluginBuildException.of(Plugin.this, "plugins must be Java 1.8 compatible")
										.withFile(fileName);
								}

								jarClasses.add(name.replace('/', '.'));

								extendsPlugin = "net/runelite/client/plugins/Plugin".equals(superName);
								this.name = name;
								super.visit(version, access, name, signature, superName, interfaces);
							}

							@Override
							public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
							{
								if ("Lnet/runelite/client/plugins/PluginDescriptor;".equals(descriptor) && extendsPlugin)
								{
									pluginClasses.add(name.replace('/', '.'));
								}

								return null;
							}
						}, ClassReader.SKIP_CODE);
					}
					catch (IllegalArgumentException e)
					{
						if (isMultiRelease)
						{
							// allow multirelease classes to not be parsable by asm, they may be too new
							continue;
						}

						throw e;
					}
				}
			}

			if (apiFile.exists())
			{
				// we can record api symbols from the plugin's own dependencies, we need to strip those
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try (FileInputStream fis = new FileInputStream(apiFile))
				{
					API api = API.decode(fis);
					API.encode(out, api.missingFrom(builtinApi.getApi()));
					String disallowed = DISALLOWED_API.in(api)
						.collect(Collectors.joining("\n"));
					if (!disallowed.isEmpty())
					{
						if (disallowedFatal)
						{
							throw PluginBuildException.of(this, "plugin uses terminally deprecated APIs:\n{}", disallowed);
						}
						else
						{
							writeLog("plugin uses terminally deprecated APIs:\n{}\n", disallowed);
						}
					}
				}
				Files.write(apiFile.toPath(), out.toByteArray());
			}
		}

		{
			File propFile = new File(repositoryDirectory, "runelite-plugin.properties");
			if (!propFile.exists())
			{
				throw PluginBuildException.of(this, "runelite-plugin.properties must exist in the root of your repo");
			}
			Properties props = loadProperties(propFile);

			{
				String displayName = (String) props.remove("displayName");
				if (Strings.isNullOrEmpty(displayName))
				{
					throw PluginBuildException.of(this, "\"displayName\" must be set")
						.withFile(propFile);
				}
				manifest.setDisplayName(displayName);
			}

			{
				String author = (String) props.remove("author");
				if (Strings.isNullOrEmpty(author))
				{
					throw PluginBuildException.of(this, "\"author\" must be set")
						.withFile(propFile);
				}
				manifest.setAuthor(author);
			}

			{
				String supportStr = (String) props.remove("support");
				if (Strings.isNullOrEmpty(supportStr))
				{
					supportStr = this.defaultSupportURL;
				}

				try
				{
					manifest.setSupport(new URL(supportStr));
				}
				catch (MalformedURLException e)
				{
					throw PluginBuildException.of(this, "support url is malformed", e)
						.withFileLine(propFile, "support=" + supportStr);
				}
			}

			manifest.setDescription((String) props.remove("description"));

			{
				String tagsStr = (String) props.remove("tags");
				if (!Strings.isNullOrEmpty(tagsStr))
				{
					manifest.setTags(Splitter.on(",")
						.omitEmptyStrings()
						.trimResults()
						.splitToList(tagsStr)
						.toArray(new String[0]));
				}
			}

			{
				String pluginsStr = (String) props.remove("plugins");
				if (pluginsStr == null)
				{
					throw PluginBuildException.of(this, "\"plugins\" must be set")
						.withFile(propFile);
				}

				List<String> plugins = Splitter.on(CharMatcher.anyOf(",:;"))
					.omitEmptyStrings()
					.trimResults()
					.splitToList(pluginsStr);

				if (plugins.isEmpty())
				{
					throw PluginBuildException.of(this, "No plugin classes listed")
						.withHelp(() ->
						{
							String m = "You must list your plugin class names in the plugin descriptor";
							if (!pluginClasses.isEmpty())
							{
								m += "\nPerhaps you wanted plugins=" + String.join(", ", pluginClasses);
							}
							return m;
						})
						.withFileLine(propFile, "plugins=" + pluginsStr);
				}

				manifest.setPlugins(plugins.toArray(new String[0]));

				for (String className : plugins)
				{
					if (pluginClasses.contains(className))
					{
						continue;
					}

					if (jarClasses.contains(className))
					{
						throw PluginBuildException.of(this, "Plugin class \"{}\" is not a valid Plugin", className)
							.withHelp("All plugins must extend Plugin an have an @PluginDescriptor")
							.withFileLine(propFile, "plugins=" + pluginsStr);
					}

					Set<String> unusedPlugins = new HashSet<>(pluginClasses);
					unusedPlugins.removeAll(plugins);

					throw PluginBuildException.of(this,
							"Plugin class \"{}\" is missing from the output jar", className)
						.withHelp(unusedPlugins.isEmpty()
							? "All plugins must extend Plugin an have an @PluginDescriptor"
							: ("Perhaps you wanted " + String.join(", ", unusedPlugins)))
						.withFileLine(propFile, "plugins=" + pluginsStr);
				}
			}

			if (props.size() != 0)
			{
				writeLog("warning: unused props in runelite-plugin.properties: {}\n", props.keySet());
			}
		}

		realPluginChecks();
	}

	// Tests don't run this as the example plugin will fail these on purpose
	protected void realPluginChecks() throws IOException, PluginBuildException
	{
		{
			Process gitlog = new ProcessBuilder("git", "log", "--follow", "--format=%ct", "--", pluginCommitDescriptor.getAbsolutePath())
				.redirectOutput(ProcessBuilder.Redirect.PIPE)
				.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
				.directory(pluginCommitDescriptor.getParentFile())
				.start();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(gitlog.getInputStream())))
			{
				String line = br.readLine();
				manifest.setLastUpdatedAt(Long.parseLong(line));

				String lastLine = line;
				for (; (line = br.readLine()) != null; )
				{
					lastLine = line;
				}
				manifest.setCreatedAt(Long.parseLong(lastLine));
			}
			waitAndCheck(gitlog, "git log ", 30, TimeUnit.SECONDS);
		}

		if (!new File(repositoryDirectory, "LICENSE").exists())
		{
			if (manifest.getLastUpdatedAt() < 1604534400)
			{
				writeLog("Missing LICENSE file. This will become fatal in the future\n");
			}
			else
			{
				throw PluginBuildException.of(this, "Missing LICENSE file")
					.withHelp("All plugins must be licensed under a license that allows us to freely distribute the plugin jar standalone.\n" +
						"We recommend the BSD 2 Clause license.");
			}
		}
	}

	public void upload(UploadConfiguration uploadConfig) throws IOException
	{
		HttpUrl pluginRoot = uploadConfig.getUploadRepoRoot().newBuilder()
			.addPathSegment(internalName)
			.build();

		uploadConfig.put(
			pluginRoot.newBuilder().addPathSegment(commit + SUFFIX_JAR).build(),
			jarFile);

		if (apiFile.exists())
		{
			uploadConfig.put(
				pluginRoot.newBuilder().addPathSegment(commit + SUFFIX_API).build(),
				apiFile);
		}

		uploadConfig.put(
			pluginRoot.newBuilder().addPathSegment(commit + SUFFIX_SOURCES).build(),
			srcZipFile);

		if (manifest.isHasIcon())
		{
			uploadConfig.put(
				pluginRoot.newBuilder().addPathSegment(commit + SUFFIX_ICON).build(),
				iconFile);
		}
	}

	public String uploadLog(UploadConfiguration uploadConfig) throws IOException
	{
		try
		{
			log.close();
			log = null;
		}
		catch (IOException ignored)
		{
		}

		HttpUrl url = uploadConfig.getUploadRepoRoot()
			.newBuilder()
			.addPathSegment(internalName)
			.addPathSegment(commit + ".log")
			.build();
		uploadConfig.put(url, logFile);

		return url.toString();
	}

	public void writeLog(String format, Object... args) throws IOException
	{
		FormattingTuple fmt = MessageFormatter.arrayFormat(format, args);
		log.write(fmt.getMessage().getBytes(StandardCharsets.UTF_8));
		Throwable t = fmt.getThrowable();
		if (t != null)
		{
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			t.printStackTrace(pw);
			pw.flush();

			Writer w = new OutputStreamWriter(log, StandardCharsets.UTF_8);
			boolean collapsing = false;
			for (String line : Splitter.on('\n').split(caw.toString()))
			{
				boolean collapse = line.startsWith("\tat org.gradle.");
				if (collapse && !collapsing)
				{
					w.write("\t...\n");
				}
				if (!collapse)
				{
					w.write(line);
					w.write('\n');
				}
				collapsing = collapse;
			}
			w.flush();
		}
		log.flush();
	}

	static Properties loadProperties(File path) throws IOException
	{
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(path))
		{
			props.load(fis);
		}
		return props;
	}

	@Override
	public void close() throws IOException
	{
		if (log != null)
		{
			log.close();
		}
		MoreFiles.deleteRecursively(buildDirectory.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
	}
}
