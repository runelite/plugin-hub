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
package net.runelite.pluginhub.uploader;

import com.google.common.base.Strings;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

@Getter
@Accessors(chain = true)
public class UploadConfiguration implements Closeable
{
	public static final String DIR_JAR = "jar";
	public static final String DIR_API = "api";
	public static final String DIR_ICON = "icon";
	public static final String DIR_SOURCE = "source";
	public static final String DIR_LOG = "log";
	public static final String DIR_MANIFEST = "manifest";

	public static final String MANIFEST_TYPE_FULL = "_full.js";
	public static final String MANIFEST_TYPE_LITE = "_lite.js";

	private OkHttpClient client;

	@Getter
	private HttpUrl root;

	@Getter
	private String runeLiteVersion;

	public UploadConfiguration fromEnvironment(String runeLiteVersion)
	{
		String prNo = System.getenv("PACKAGE_IS_PR");
		if (prNo != null && !prNo.isEmpty() && !"false".equalsIgnoreCase(prNo))
		{
			return this;
		}

		setClient(System.getenv("REPO_CREDS"));

		String uploadRepoRootStr = System.getenv("REPO_ROOT");
		if (!Strings.isNullOrEmpty(uploadRepoRootStr))
		{
			root = HttpUrl.parse(uploadRepoRootStr);
		}
		this.runeLiteVersion = runeLiteVersion;

		return this;
	}

	public boolean isComplete()
	{
		return client != null && root != null;
	}

	public UploadConfiguration setClient(String credentials)
	{
		String repoAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		client = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				Request userAgentRequest = chain.request()
					.newBuilder()
					.header("User-Agent", "RuneLite-PluginHub-Package/2")
					.header("Authorization", repoAuth)
					.build();

				Response res = null;
				for (int attempts = 0; attempts < 2; attempts++)
				{
					res = chain.proceed(userAgentRequest);
					if (res.code() == 520)
					{
						res.close();
						continue;
					}

					break;
				}

				return res;
			})
			.build();

		return this;
	}

	public void put(HttpUrl path, File data) throws IOException
	{
		try (Response res = client.newCall(new Request.Builder()
				.url(path)
				.put(RequestBody.create(null, data))
				.build())
			.execute())
		{
			Util.check(res);
		}
	}

	public void putMkDirs(HttpUrl path, File data) throws IOException
	{
		mkdirs(path.newBuilder()
			.removePathSegment(path.pathSize() - 1)
			.build());
		put(path, data);
	}

	public void mkdirs(HttpUrl url) throws IOException
	{
		for (int i = 0; i < 2; i++)
		{
			try (Response res = client.newCall(new Request.Builder()
					.url(url.newBuilder()
						.addPathSegment("/")
						.build())
					.method("MKCOL", null)
					.build())
				.execute())
			{
				if (res.code() == 409 && i == 0)
				{
					mkdirs(url.newBuilder()
						.removePathSegment(url.pathSize() - 1)
						.build());

					continue;
				}

				// even though 405 is method not allowed, if your webdav
				// it actually means this url already exists
				if (res.code() != 405)
				{
					Util.check(res);
				}

				return;
			}
		}
	}

	public PluginHubManifest.ManifestFull getManifest(String version, SigningConfiguration signingConfig) throws IOException
	{
		try (Response res = getClient().newCall(new Request.Builder()
				.url(getRoot().newBuilder()
					.addPathSegment(DIR_MANIFEST)
					.addPathSegment(version + MANIFEST_TYPE_FULL)
					.addQueryParameter("c", System.nanoTime() + "")
					.build())
				.get()
				.build())
			.execute())
		{
			Util.check(res);

			BufferedSource src = res.body().source();

			byte[] signature = new byte[src.readInt()];
			src.readFully(signature);

			byte[] data = src.readByteArray();
			if (signingConfig != null && !signingConfig.verify(signature, data))
			{
				throw new RuntimeException("Unable to verify external plugin manifest");
			}

			return Util.GSON.fromJson(new String(data, StandardCharsets.UTF_8), PluginHubManifest.ManifestFull.class);
		}
		catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close()
	{
		if (client != null)
		{
			client.connectionPool().evictAll();
		}
	}
}
