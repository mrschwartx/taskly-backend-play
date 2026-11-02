package com.taskly.backend.config;

import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.Timestamps;
import com.taskly.backend.tasks.application.CreateTask;
import com.taskly.backend.users.application.AuthResult;
import com.taskly.backend.users.application.RegisterUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SeedRunner {

	private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

	private record SeedEntry(Map<String, Object> user, List<Map<String, Object>> tasks) {
	}

	private final TasklyConfig config;

	private final RegisterUser registerUser;

	private final CreateTask createTask;

	@Inject
	public SeedRunner(TasklyConfig config, RegisterUser registerUser, CreateTask createTask) {
		this.config = config;
		this.registerUser = registerUser;
		this.createTask = createTask;
	}

	public int run() {
		if (config.app().production() && !config.seed().allowProduction()) {
			log.error("Refusing to seed a production database");
			return 1;
		}
		try {
			for (SeedEntry entry : entries()) {
				seedEntry(entry);
			}
			log.info("Seeding completed");
			return 0;
		}
		catch (RuntimeException error) {
			log.error("Seeding failed: {}", error.getMessage(), error);
			return 1;
		}
	}

	private void seedEntry(SeedEntry entry) {
		String username = String.valueOf(entry.user().get("username"));
		try {
			AuthResult result = registerUser.execute(entry.user());
			UUID ownerId = UUID.fromString(result.user().id());
			for (Map<String, Object> task : entry.tasks()) {
				createTask.execute(ownerId, task);
			}
			log.info("Seeded user {}", username);
		}
		catch (AlreadyExistsException error) {
			log.warn("Skipped existing user {}", username);
		}
	}

	private static List<SeedEntry> entries() {
		String now = Timestamps.format(Timestamps.now());
		return List.of(
				new SeedEntry(
						Map.of("username", "johndoe123", "email", "johndoe123@mail.com", "password", "password123"),
						List.of(Map.of(
								"name", "Read Atomic Habits",
								"description", "Finish reading Atomic Habits by James Clear",
								"priority", "low",
								"due", now,
								"status", "todo"))),
				new SeedEntry(
						Map.of("username", "jane78", "email", "jane@mail.com", "password", "securepassword456"),
						List.of(Map.of(
								"name", "Learn MERN Stack",
								"description", "Learn the MERN stack and build a full-stack application with it",
								"priority", "high",
								"due", now,
								"status", "todo"))));
	}
}
