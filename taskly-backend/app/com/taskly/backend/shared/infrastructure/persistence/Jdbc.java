package com.taskly.backend.shared.infrastructure.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import play.db.Database;

public final class Jdbc {

	@FunctionalInterface
	public interface Work<T> {

		T apply(Connection connection) throws SQLException;
	}

	private Jdbc() {
	}

	public static <T> T execute(Database database, Work<T> work) {
		try (Connection connection = database.getConnection()) {
			return work.apply(connection);
		}
		catch (SQLException error) {
			throw new DataAccessException(error);
		}
	}

	public static OffsetDateTime timestamp(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	public static Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getObject(column, OffsetDateTime.class).toInstant();
	}

	public static UUID uuid(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getObject(column, UUID.class);
	}
}
