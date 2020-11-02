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

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Getter
@Accessors(chain = true)
public class UploadConfiguration
{
	private RSAPrivateCrtKey key;
	private PublicKey cert;
	private OkHttpClient client;

	@Setter
	private HttpUrl uploadRepoRoot;

	public UploadConfiguration fromEnvironment(String runeliteVersion)
	{
		String prNo = System.getenv("PACKAGE_IS_PR");
		if (prNo != null && !prNo.isEmpty() && !"false".equalsIgnoreCase(prNo))
		{
			return this;
		}

		setKey(System.getenv("SIGNING_KEY"));
		setClient(System.getenv("REPO_CREDS"));

		String uploadRepoRootStr = System.getenv("REPO_ROOT");
		if (!Strings.isNullOrEmpty(uploadRepoRootStr))
		{
			uploadRepoRoot = HttpUrl.parse(uploadRepoRootStr)
				.newBuilder()
				.addPathSegment(runeliteVersion)
				.build();
		}

		return this;
	}

	public boolean isComplete()
	{
		return key != null && cert != null && client != null && uploadRepoRoot != null;
	}

	public UploadConfiguration setKey(String keyStr)
	{
		if (keyStr == null)
		{
			key = null;
			cert = null;
			return this;
		}

		try
		{
			KeyFactory kf = KeyFactory.getInstance("RSA");

			byte[] pkcs8 = Base64.getMimeDecoder().decode(keyStr
				.replace("\\n", "\n")
				.replaceAll(" |-----(BEGIN|END) PRIVATE KEY-----(\n?)", ""));
			key = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
			cert = kf.generatePublic(new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent()));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			throw new RuntimeException(e);
		}

		return this;
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
}
