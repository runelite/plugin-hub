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

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class SigningConfiguration
{
	private final RSAPrivateCrtKey key;
	private final PublicKey cert;

	public static SigningConfiguration fromEnvironment()
	{
		return new SigningConfiguration(System.getenv("SIGNING_KEY"));
	}

	public SigningConfiguration(String keyStr)
	{
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
	}

	public byte[] sign(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		Signature s = Signature.getInstance("SHA256withRSA");
		s.initSign(key);
		s.update(data);
		return s.sign();
	}

	public boolean verify(byte[] sig, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		Signature s = Signature.getInstance("SHA256withRSA");
		s.initVerify(cert);
		s.update(data);
		return s.verify(sig);
	}
}
