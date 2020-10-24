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
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
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
	private static final Pattern PLUGIN_INTERNAL_NAME_TEST = Pattern.compile("^[a-z0-9-]+$");
	private static final Pattern REPOSITORY_TEST = Pattern.compile("^https://github\\.com/.*\\.git$");
	private static final Pattern COMMIT_TEST = Pattern.compile("^[a-fA-F0-9]{40}$");

	private static final File TMP_ROOT;
	private static final File GRADLE_HOME;

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
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Getter
	private final String internalName;

	private final File buildDirectory;

	@VisibleForTesting
	final File repositoryDirectory;

	private final File jarFile;
	private final File iconFile;

	@Getter
	private final File logFile;

	@Getter
	private FileOutputStream log;

	@Nullable
	private final String warning;

	private final String repositoryURL;
	private final String commit;

	@Getter
	private final ExternalPluginManifest manifest = new ExternalPluginManifest();

	@Getter
	@Setter
	private long buildTimeMS;

	public Plugin(File pluginCommitDescriptor) throws IOException, DisabledPluginException, PluginBuildException
	{
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

		if (!REPOSITORY_TEST.matcher(repositoryURL).matches())
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

		commit = (String) cd.remove("commit");
		if (!COMMIT_TEST.matcher(commit).matches())
		{
			throw PluginBuildException.of(internalName, "commit must be a full 40 character sha1sum")
				.withFileLine(pluginCommitDescriptor, "commit=" + commit);
		}

		warning = (String) cd.remove("warning");

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
		iconFile = new File(repositoryDirectory, "icon.png");
	}

	public void download() throws IOException, PluginBuildException
	{
		Process gitclone = new ProcessBuilder("git", "clone", "--config", "advice.detachedHead=false", this.repositoryURL, repositoryDirectory.getAbsolutePath())
			.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
			.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
			.start();
		Util.waitAndCheck(this, gitclone, "git clone", 2, TimeUnit.MINUTES);


		Process gitcheckout = new ProcessBuilder("git", "checkout", commit + "^{commit}")
			.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
			.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
			.directory(repositoryDirectory)
			.start();
		Util.waitAndCheck(this, gitcheckout, "git checkout", 2, TimeUnit.MINUTES);
	}

	public void build(String runeliteVersion) throws IOException, PluginBuildException
	{
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
					"runelite.pluginhub.package.lib", new File(Packager.PACKAGE_ROOT, "initLib/build/libs/initLib.jar").toString(),
					"runelite.pluginhub.package.buildDir", buildDirectory.getAbsolutePath(),
					"runelite.pluginhub.package.runeliteVersion", runeliteVersion))
				.setJvmArguments("-Xmx768M", "-XX:+UseParallelGC")
				.setStandardOutput(log)
				.setStandardError(log)
				.forTasks("runelitePluginHubPackage", "runelitePluginHubManifest")
				.withCancellationToken(cancel.token())
				.run(new ResultHandler<Void>()
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

	public void assembleManifest() throws IOException, PluginBuildException
	{
		manifest.setInternalName(internalName);
		manifest.setCommit(commit);
		manifest.setWarning(warning);

		{
			Properties chunk = loadProperties(new File(buildDirectory, "chunk.properties"));

			manifest.setVersion(chunk.getProperty("version"));
			if (Strings.isNullOrEmpty(manifest.getVersion()))
			{
				throw new IllegalStateException("version in empty");
			}
		}

		{
			long size = jarFile.length();
			if (size > 10 * 1024 * 1024)
			{
				throw PluginBuildException.of(this, "the output jar is {}MiB, which is above our limit of 10MiB", size / (1024 * 1024));
			}
			manifest.setSize((int) size);
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

			synchronized (ImageIO.class)
			{
				try
				{
					Objects.requireNonNull(ImageIO.read(iconFile));
				}
				catch (Exception e)
				{
					throw PluginBuildException.of(this, "icon is invalid", e)
						.withFile(iconFile);
				}
			}

			manifest.setHasIcon(true);
		}

		Set<String> pluginClasses = new HashSet<>();
		Set<String> jarClasses = new HashSet<>();
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

					byte[] classData = ByteStreams.toByteArray(jis);
					new ClassReader(classData).accept(new ClassVisitor(Opcodes.ASM7)
					{
						boolean extendsPlugin;
						String name;

						@SneakyThrows
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
						{
							if (version > Opcodes.V1_8 && !fileName.startsWith("META-INF/versions"))
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
					}, ClassReader.SKIP_FRAMES);
				}
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
				if (!Strings.isNullOrEmpty(supportStr))
				{
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
	}

	public void upload(UploadConfiguration uploadConfig) throws IOException
	{
		HttpUrl pluginRoot = uploadConfig.getUploadRepoRoot().newBuilder()
			.addPathSegment(internalName)
			.build();

		uploadConfig.put(
			pluginRoot.newBuilder().addPathSegment(commit + ".jar").build(),
			jarFile);

		if (manifest.isHasIcon())
		{
			uploadConfig.put(
				pluginRoot.newBuilder().addPathSegment(commit + ".png").build(),
				iconFile);
		}
	}

	public void uploadLog(UploadConfiguration uploadConfig) throws IOException
	{
		try
		{
			log.close();
			log = null;
		}
		catch (IOException ignored)
		{
		}

		uploadConfig.put(uploadConfig.getUploadRepoRoot()
				.newBuilder()
				.addPathSegment(internalName)
				.addPathSegment(commit + ".log")
				.build(),
			logFile);
	}

	public void writeLog(String format, Object... args) throws IOException
	{
		FormattingTuple fmt = MessageFormatter.arrayFormat(format, args);
		log.write(fmt.getMessage().getBytes(StandardCharsets.UTF_8));
		Throwable t = fmt.getThrowable();
		if (t != null)
		{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(log, StandardCharsets.UTF_8));
			pw.println(t.getMessage());
			t.printStackTrace(pw);
			pw.flush();
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
