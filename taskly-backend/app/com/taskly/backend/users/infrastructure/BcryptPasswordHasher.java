package com.taskly.backend.users.infrastructure;

import com.taskly.backend.users.domain.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

public class BcryptPasswordHasher implements PasswordHasher {

	private static final String VERSION = "2b";

	private static final int SALT_BYTES = 16;

	private static final int MAX_PASSWORD_BYTES = 72;

	private final int rounds;

	private final SecureRandom random = new SecureRandom();

	public BcryptPasswordHasher(int rounds) {
		this.rounds = rounds;
	}

	@Override
	public String hash(String plain) {
		byte[] salt = new byte[SALT_BYTES];
		random.nextBytes(salt);
		return OpenBSDBCrypt.generate(VERSION, plain.getBytes(StandardCharsets.UTF_8), salt, rounds);
	}

	@Override
	public boolean matches(String plain, String hash) {
		byte[] password = plain.getBytes(StandardCharsets.UTF_8);
		if (hash == null || password.length > MAX_PASSWORD_BYTES) {
			return false;
		}
		try {
			return OpenBSDBCrypt.checkPassword(hash, password);
		}
		catch (RuntimeException error) {
			return false;
		}
	}
}
