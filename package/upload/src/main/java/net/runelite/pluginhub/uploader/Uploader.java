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

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

public class Uploader
{
	private static final String MANIFEST_NAME = "manifest.js";

	public static void main(String... args) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
	{
		Gson gson = new Gson();

		String diffJSON = Files.asCharSource(new File("/tmp/manifest_diff"), StandardCharsets.UTF_8)
			.read();
		ManifestDiff diff = gson.fromJson(diffJSON, ManifestDiff.class);

		try (UploadConfiguration uploadConfig = new UploadConfiguration().fromEnvironment(Util.readRLVersion()))
		{
			SigningConfiguration signingConfig = SigningConfiguration.fromEnvironment();

			List<ExternalPluginManifest> manifests = new ArrayList<>();
			if (!diff.isIgnoreOldManifest() || (diff.getOldManifestVersion() != null && !diff.getCopyFromOld().isEmpty()))
			{
				String version = diff.getOldManifestVersion();
				if (version == null)
				{
					version = Util.readRLVersion();
				}
				try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
					.url(uploadConfig.getVersionlessRoot().newBuilder()
						.addPathSegment(version)
						.addPathSegment(MANIFEST_NAME)
						.addQueryParameter("c", System.nanoTime() + "")
						.build())
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
						if (!signingConfig.verify(signature, data))
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

			if (diff.isIgnoreOldManifest())
			{
				manifests.removeIf(m -> !diff.getCopyFromOld().contains(m.getInternalName()));
			}

			manifests.removeIf(m -> diff.getRemove().contains(m.getInternalName()));
			manifests.addAll(diff.getAdd());
			manifests.sort(Comparator.comparing(ExternalPluginManifest::getInternalName));

			{
				byte[] data = gson.toJson(manifests).getBytes(StandardCharsets.UTF_8);
				byte[] sig = signingConfig.sign(data);

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				new DataOutputStream(out).writeInt(sig.length);
				out.write(sig);
				out.write(data);
				byte[] manifest = out.toByteArray();

				try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
					.url(uploadConfig.getUploadRepoRoot().newBuilder()
						.addPathSegment(MANIFEST_NAME)
						.build())
					.put(RequestBody.create(null, manifest))
					.build())
					.execute())
				{
					Util.check(res);
				}
			}
		}
	}
}
