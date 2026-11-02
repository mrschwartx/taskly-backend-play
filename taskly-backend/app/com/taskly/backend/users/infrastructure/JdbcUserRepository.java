package com.taskly.backend.users.infrastructure;

import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.infrastructure.persistence.Jdbc;
import com.taskly.backend.shared.infrastructure.persistence.UniqueViolations;
import com.taskly.backend.users.domain.Email;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.domain.Username;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.db.Database;

@Singleton
public class JdbcUserRepository implements UserRepository {

	private static final String DUPLICATE_MESSAGE = "Email or Username is already registered.";

	private static final UUID NO_EXCLUSION = new UUID(0L, 0L);

	private static final String COLUMNS = "id, username, email, password_hash, avatar_file, created_at, updated_at";

	private final Database database;

	@Inject
	public JdbcUserRepository(Database database) {
		this.database = database;
	}

	@Override
	public UUID nextId() {
		return EntityIds.generate();
	}

	@Override
	public void add(User user) {
		try {
			Jdbc.execute(database, connection -> {
				try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users (" + COLUMNS
						+ ") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
					statement.setObject(1, user.getId());
					statement.setString(2, user.getUsername().value());
					statement.setString(3, user.getEmail().value());
					statement.setString(4, user.getPasswordHash());
					statement.setString(5, user.getAvatar());
					statement.setObject(6, Jdbc.timestamp(user.getCreatedAt()));
					statement.setObject(7, Jdbc.timestamp(user.getUpdatedAt()));
					statement.executeUpdate();
				}
				return null;
			});
		}
		catch (RuntimeException error) {
			throw translate(error);
		}
	}

	@Override
	public boolean update(User user) {
		try {
			return Jdbc.execute(database, connection -> {
				try (PreparedStatement statement = connection.prepareStatement(
						"UPDATE users SET username = ?, email = ?, password_hash = ?, avatar_file = ?, updated_at = ? "
								+ "WHERE id = ?")) {
					statement.setString(1, user.getUsername().value());
					statement.setString(2, user.getEmail().value());
					statement.setString(3, user.getPasswordHash());
					statement.setString(4, user.getAvatar());
					statement.setObject(5, Jdbc.timestamp(user.getUpdatedAt()));
					statement.setObject(6, user.getId());
					return statement.executeUpdate() == 1;
				}
			});
		}
		catch (RuntimeException error) {
			throw translate(error);
		}
	}

	@Override
	public Optional<User> findById(UUID id) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id = ?")) {
				statement.setObject(1, id);
				try (ResultSet resultSet = statement.executeQuery()) {
					return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
				}
			}
		});
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE lower(email) = lower(?)")) {
				statement.setString(1, email);
				try (ResultSet resultSet = statement.executeQuery()) {
					return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
				}
			}
		});
	}

	@Override
	public boolean existsByEmailOrUsername(String email, String username, UUID excludeId) {
		UUID excluded = excludeId != null ? excludeId : NO_EXCLUSION;
		return Jdbc.execute(database, connection -> {
			if (email != null && exists(connection.prepareStatement(
					"SELECT 1 FROM users WHERE lower(email) = lower(?) AND id <> ? LIMIT 1"), email, excluded)) {
				return true;
			}
			return username != null && exists(connection.prepareStatement(
					"SELECT 1 FROM users WHERE lower(username) = lower(?) AND id <> ? LIMIT 1"), username, excluded);
		});
	}

	@Override
	public List<User> search(String username, String email) {
		String usernamePattern = likePattern(username);
		String emailPattern = likePattern(email);
		StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM users WHERE TRUE");
		if (usernamePattern != null) {
			sql.append(" AND username ILIKE ?");
		}
		if (emailPattern != null) {
			sql.append(" AND email ILIKE ?");
		}
		sql.append(" ORDER BY created_at ASC, id ASC");
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
				int index = 1;
				if (usernamePattern != null) {
					statement.setString(index++, usernamePattern);
				}
				if (emailPattern != null) {
					statement.setString(index, emailPattern);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					List<User> users = new ArrayList<>();
					while (resultSet.next()) {
						users.add(map(resultSet));
					}
					return users;
				}
			}
		});
	}

	@Override
	public boolean deleteById(UUID id) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
				statement.setObject(1, id);
				return statement.executeUpdate() == 1;
			}
		});
	}

	private static boolean exists(PreparedStatement statement, String value, UUID excluded) throws SQLException {
		try (statement) {
			statement.setString(1, value);
			statement.setObject(2, excluded);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private static User map(ResultSet resultSet) throws SQLException {
		return User.restore(
				Jdbc.uuid(resultSet, "id"),
				Username.restore(resultSet.getString("username")),
				Email.restore(resultSet.getString("email")),
				resultSet.getString("password_hash"),
				resultSet.getString("avatar_file"),
				Jdbc.instant(resultSet, "created_at"),
				Jdbc.instant(resultSet, "updated_at"));
	}

	private static String likePattern(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		return "%" + value.replaceAll("[\\\\%_]", "\\\\$0") + "%";
	}

	private static RuntimeException translate(RuntimeException error) {
		if (UniqueViolations.isUniqueViolation(error)) {
			return new AlreadyExistsException(DUPLICATE_MESSAGE);
		}
		return error;
	}
}
