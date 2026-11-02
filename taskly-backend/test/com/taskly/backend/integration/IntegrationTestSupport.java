package com.taskly.backend.integration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithServer;

public abstract class IntegrationTestSupport extends WithServer {

	static final Path PUBLIC_DIR = createPublicDir();

	@Override
	protected Application provideApplication() {
		TestDatabase database = TestDatabase.INSTANCE;
		return new GuiceApplicationBuilder()
				.configure("db.default.url", database.jdbcUrl)
				.configure("db.default.username", database.username)
				.configure("db.default.password", database.password)
				.configure("taskly.auth.secret", "integration-test-secret-integration-test-secret")
				.configure("taskly.auth.bcrypt-rounds", 4)
				.configure("taskly.storage.public-dir", PUBLIC_DIR.toString())
				.configure("play.http.secret.key", "integration-test-play-secret-integration-test-play-secret")
				.build();
	}

	ApiClient client() {
		return new ApiClient("http://127.0.0.1:" + port);
	}

	record Registered(ApiClient client, String id, String username, String email, String password) {
	}

	Registered register() {
		ApiClient client = client();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String username = "user_" + suffix;
		String email = suffix + "@mail.com";
		String password = "Password123!";
		ApiResponse response = client.post("/api/v1/auth/signup",
				ApiClient.json("username", username, "email", email, "password", password));
		if (response.status() != 200) {
			throw new IllegalStateException("Signup failed: " + response.body());
		}
		return new Registered(client, response.json("$._id"), username, email, password);
	}

	String taskJson(String name, String status) {
		return ApiClient.json("name", name, "description", "Finish reading", "priority", "low", "due",
				"2026-12-08T10:00:00Z", "status", status);
	}

	long avatarFileCount() {
		try (var files = Files.list(PUBLIC_DIR.resolve("images").resolve("users"))) {
			return files.filter(file -> file.getFileName().toString().startsWith("avatar-")).count();
		}
		catch (java.nio.file.NoSuchFileException error) {
			return 0;
		}
		catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}

	int countTasksOf(String ownerId) {
		TestDatabase database = TestDatabase.INSTANCE;
		try (Connection connection = DriverManager.getConnection(database.jdbcUrl, database.username,
				database.password);
				PreparedStatement statement = connection
						.prepareStatement("select count(*) from tasks where owner_id = ?")) {
			statement.setObject(1, UUID.fromString(ownerId));
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getInt(1);
			}
		}
		catch (SQLException error) {
			throw new IllegalStateException(error);
		}
	}

	private static Path createPublicDir() {
		try {
			return Files.createTempDirectory("taskly-public");
		}
		catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}
}
