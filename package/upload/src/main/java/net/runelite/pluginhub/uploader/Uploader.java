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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Comparator;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Uploader
{
	public static void main(String... args) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
	{
		String diffJSON = Files.asCharSource(new File("/tmp/manifest_diff"), StandardCharsets.UTF_8)
			.read();
		ManifestDiff diff = Util.GSON.fromJson(diffJSON, ManifestDiff.class);

		try (UploadConfiguration uploadConfig = new UploadConfiguration().fromEnvironment(Util.readRLVersion()))
		{
			SigningConfiguration signingConfig = SigningConfiguration.fromEnvironment();

			PluginHubManifest.ManifestFull manifestFull = new PluginHubManifest.ManifestFull();
			if (!diff.isIgnoreOldManifest() || (diff.getOldManifestVersion() != null && !diff.getCopyFromOld().isEmpty()))
			{
				String version = diff.getOldManifestVersion();
				if (version == null)
				{
					version = Util.readRLVersion();
				}

				manifestFull = uploadConfig.getManifest(version, signingConfig);
			}

			if (diff.isIgnoreOldManifest())
			{
				manifestFull.getJars().removeIf(m -> !diff.getCopyFromOld().contains(m.getInternalName()));
				manifestFull.getDisplay().removeIf(m -> !diff.getCopyFromOld().contains(m.getInternalName()));
			}

			manifestFull.getJars().removeIf(m -> diff.getRemove().contains(m.getInternalName()));
			manifestFull.getDisplay().removeIf(m -> diff.getRemove().contains(m.getInternalName()));

			manifestFull.getJars().addAll(diff.getAddJarData());
			manifestFull.getDisplay().addAll(diff.getAddDisplayData());

			manifestFull.getJars().sort(Comparator.comparing(m -> m.getInternalName()));
			manifestFull.getDisplay().sort(Comparator.comparing(m -> m.getInternalName()));

			PluginHubManifest.ManifestLite manifestLite = new PluginHubManifest.ManifestLite();
			manifestLite.setJars(manifestFull.getJars());

			putSigned(UploadConfiguration.MANIFEST_TYPE_FULL, manifestFull, uploadConfig, signingConfig);
			putSigned(UploadConfiguration.MANIFEST_TYPE_LITE, manifestLite, uploadConfig, signingConfig);
		}
	}

	private static void putSigned(String manifestType, Object manifest, UploadConfiguration uploadConfig, SigningConfiguration signingConfig)
		throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
	{
		byte[] data = Util.GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);
		byte[] sig = signingConfig.sign(data);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new DataOutputStream(out).writeInt(sig.length);
		out.write(sig);
		out.write(data);
		byte[] signed = out.toByteArray();

		try (Response res = uploadConfig.getClient().newCall(new Request.Builder()
				.url(uploadConfig.getRoot().newBuilder()
					.addPathSegment(UploadConfiguration.DIR_MANIFEST)
					.addPathSegment(uploadConfig.getRuneLiteVersion() + manifestType)
					.build())
				.put(RequestBody.create(null, signed))
				.build())
			.execute())
		{
			Util.check(res);
		}
	}
}
