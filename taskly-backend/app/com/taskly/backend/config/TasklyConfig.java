package com.taskly.backend.config;

import com.typesafe.config.Config;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TasklyConfig {

	public record App(String level) {

		public boolean production() {
			return "production".equals(level);
		}
	}

	public record Auth(String secret, Duration tokenTtl, String cookieName, String cookieDomain, int bcryptRounds) {
	}

	public record Storage(String publicDir, long avatarMaxBytes) {

		public Path publicPath() {
			return Path.of(publicDir).toAbsolutePath().normalize();
		}

		public long avatarMaxMegabytes() {
			return avatarMaxBytes / (1024 * 1024);
		}
	}

	public record Seed(boolean allowProduction) {
	}

	private final App app;

	private final String clientUrl;

	private final Auth auth;

	private final Storage storage;

	private final Seed seed;

	@Inject
	public TasklyConfig(Config config) {
		this.app = new App(config.getString("taskly.app.level"));
		this.clientUrl = config.getString("taskly.client-url");
		this.auth = new Auth(
				config.getString("taskly.auth.secret"),
				config.getDuration("taskly.auth.token-ttl"),
				config.getString("taskly.auth.cookie-name"),
				config.getString("taskly.auth.cookie-domain"),
				config.getInt("taskly.auth.bcrypt-rounds"));
		this.storage = new Storage(
				config.getString("taskly.storage.public-dir"),
				config.getBytes("taskly.storage.avatar-max-size"));
		this.seed = new Seed(config.getBoolean("taskly.seed.allow-production"));
		validate();
	}

	public App app() {
		return app;
	}

	public String clientUrl() {
		return clientUrl;
	}

	public Auth auth() {
		return auth;
	}

	public Storage storage() {
		return storage;
	}

	public Seed seed() {
		return seed;
	}

	private void validate() {
		List<String> errors = new ArrayList<>();
		if (app.level().isBlank()) {
			errors.add("APP_LEVEL must not be blank");
		}
		if (clientUrl.isBlank()) {
			errors.add("CLIENT_URL must not be blank");
		}
		if (auth.secret().isBlank()) {
			errors.add("AUTH_SECRET is required");
		}
		else if (auth.secret().length() < 32) {
			errors.add("AUTH_SECRET must be at least 32 characters long");
		}
		if (auth.cookieName().isBlank()) {
			errors.add("taskly.auth.cookie-name must not be blank");
		}
		if (auth.tokenTtl().isZero() || auth.tokenTtl().isNegative()) {
			errors.add("taskly.auth.token-ttl must be positive");
		}
		if (auth.bcryptRounds() < 4 || auth.bcryptRounds() > 31) {
			errors.add("BCRYPT_ROUNDS must be between 4 and 31");
		}
		if (storage.publicDir().isBlank()) {
			errors.add("PUBLIC_DIR must not be blank");
		}
		if (storage.avatarMaxBytes() <= 0) {
			errors.add("taskly.storage.avatar-max-size must be positive");
		}
		if (!errors.isEmpty()) {
			throw new IllegalStateException("Invalid configuration: " + String.join("; ", errors));
		}
	}
}
