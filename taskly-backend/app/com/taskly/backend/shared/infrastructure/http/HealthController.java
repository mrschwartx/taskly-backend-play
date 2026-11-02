package com.taskly.backend.shared.infrastructure.http;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.Database;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class HealthController extends Controller {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final Database database;

	@Inject
	public HealthController(Database database) {
		this.database = database;
	}

	public Result health() {
		try (Connection connection = database.getConnection()) {
			if (connection.isValid(2)) {
				return ok(Json.toJson(Map.of("status", "UP")));
			}
		}
		catch (SQLException error) {
			log.warn("Health check failed: {}", error.getMessage());
		}
		return status(503, Json.toJson(Map.of("status", "DOWN")));
	}
}
