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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Assert;
import org.junit.Test;

public class UploadConfigurationTest
{
	public static final String TEST_SIGNING_KEY = "-----BEGIN PRIVATE KEY-----\n" +
		"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDw78Jgex/z/Wqp\n" +
		"CHLYYOQvD2yfOog2UJO7dZ8USRjDOUY6TVjqMg1aHNeI5USKG3jNKchw513PAfqO\n" +
		"j36S3I3CEv30kUBB/bgirG0YV/vJtuTcfiAa4Hl3JIKCDRi5gLkgMON8TYZs/afA\n" +
		"LVkR8ZFLlTlxLGE0VAReKXZH69poRdQLcAhRybvWWPKywWFXU4yfZzTIkQs82PXN\n" +
		"UyClxsOtEGYLlZ50oov6hx+YMPJaXdVOA1Ly01iBkB7gVQnCb/dCJqilNZab+5ja\n" +
		"xep4MPLBa39+dpGDECB8al2siYMEFWRT8eY8RAknQa6tL6ubUccHZEmuFT+n+II0\n" +
		"Zl6UPg/bAgMBAAECggEAaTD0l3UKJVd96uDSa2AaH+XHEdnXQId7iHu5AX1Mf2eR\n" +
		"HsFIUa+anr465/zZKMcHveNBLPIGxetiPj2uEGaUyafLEq0b9fPVIeZQFzHKr23X\n" +
		"i+DRGYrp3TemdytKoSrvKHvPxiR+zTUNuVzTJ39lZS94jc3HfrYz1fyaNJpnl+AT\n" +
		"6neUBfjXLUJYGzlLVouFIhsVywsF5Hk7N2UnSmQRdrFvgGE0IRUMqicXFuv6agxf\n" +
		"NI3Bdqqnzgfel26v7OKocbuolS2Zcr4hyPJdTtrNs5pf9tt2fIkV4P6sTJiVUSyw\n" +
		"TEHiCyZZyYDi4qr6F2yNkl/Ew6jNHcUA+XuTDE4OCQKBgQD79AnLckE9xIm1W+mN\n" +
		"4qjsZQ5Lxt7wQdXZ9Lovo+VUDJ7/X0qnND0n62OIO1yLW8PgOEzu3xb6f3sBGTp2\n" +
		"Yt+4QQmVRga5qHZ58pu3/P3YXM+C95X+x6GcIwisFH+8KEOFoFfGq3jbOUbF0uXt\n" +
		"LpuaDiEMR95+96gyTTc9qmTy1wKBgQD0zmxhRgWmhJqFTb2LLvY7E6G7BuVDys4A\n" +
		"lDrwNMklcw5LKabR539LbSEXx06fwQMlDyeY0NaRlx2DEFdgTH4CW7Nq/baHjuWl\n" +
		"Hq+4PJZvvBC9Ti9yWmDw0lcCJm4y0Kv78yIIwmtG+4LIru6l1/02+iChtYGEA540\n" +
		"801mgRGunQKBgQCYRU0GH+8+HWH8safdkHb3J7wUIATsv103dKhx0mPvABG31SeR\n" +
		"Fgk/7wsgcn/j2XnwMRaN51ZD3nfAmjazBd6fxO69wKyf2CiCWxWxhL0F3lGrnWaR\n" +
		"rKUHcET1ew4X8V2djOJ/t3I7S8pyFJvRVLHF0XQ3r9fQdGy6ueAA7NJF0QKBgBwQ\n" +
		"YfpQxasOPoyTmewPySiCmqLPKo83+5+zXoJU+s4xP208bCRaDoy+CPIp5giIXuzr\n" +
		"rNVm84IjOb3hrLKcckGg85OLXFZz+j2QpAJR58kNXTnmcagBVmWlJ1ZWw4FNzLmI\n" +
		"aNlqOFQd1yNccn1Oonef+weuwBc7NvLJBZF/sGA9AoGACDOQErTZC2HmiqgLGwqs\n" +
		"cg1MKU6a5gIpo7/zhR8beU4zKZfRMqmASI5KCA1JEGEnlJHyvGUwQcAx5Eu/JVXo\n" +
		"ckOxZ50guxkBMUHvEi6EIOKRsCqVbgVM6/HEYMj5z8VVn32vN1VYFk7Ng461RSgL\n" +
		"PTrpFFdp8oDxvgezLBFzqd8=\n" +
		"-----END PRIVATE KEY-----";

	@Test
	public void createClientWithCredentials() throws IOException, InterruptedException
	{
		MockWebServer server = new MockWebServer();

		server.enqueue(new MockResponse().setResponseCode(520).setBody("some cloudflare html"));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

		OkHttpClient client = new UploadConfiguration()
			.setClient("Aladdin:open sesame")
			.getClient();

		try (Response res = client.newCall(new Request.Builder()
			.put(RequestBody.create(null, "foo"))
			.url(server.url("/"))
			.build())
			.execute())
		{
			Assert.assertEquals(res.code(), 200);
			Assert.assertEquals(res.body().string(), "ok");
		}

		RecordedRequest r2 = server.takeRequest();
		Assert.assertEquals(r2.getHeader("Authorization"), "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
	}

	@Test
	public void testSigning() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		UploadConfiguration cfg = new UploadConfiguration()
			.setKey(TEST_SIGNING_KEY);

		byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
		byte[] sig;
		{
			Signature s = Signature.getInstance("SHA256withRSA");
			s.initSign(cfg.getKey());
			s.update(data);
			sig = s.sign();
		}

		{
			Signature s = Signature.getInstance("SHA256withRSA");
			s.initVerify(cfg.getCert());
			s.update(data);
			Assert.assertTrue(s.verify(sig));
		}

		{
			Signature s = Signature.getInstance("SHA256withRSA");
			s.initVerify(cfg.getCert());
			s.update("moo".getBytes(StandardCharsets.UTF_8));
			Assert.assertFalse(s.verify(sig));
		}
	}
}