package com.taskly.backend.tasks.infrastructure;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.infrastructure.persistence.Jdbc;
import com.taskly.backend.tasks.domain.DueDate;
import com.taskly.backend.tasks.domain.Task;
import com.taskly.backend.tasks.domain.TaskDescription;
import com.taskly.backend.tasks.domain.TaskName;
import com.taskly.backend.tasks.domain.TaskPage;
import com.taskly.backend.tasks.domain.TaskPriority;
import com.taskly.backend.tasks.domain.TaskRepository;
import com.taskly.backend.tasks.domain.TaskStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.db.Database;

@Singleton
public class JdbcTaskRepository implements TaskRepository {

	private static final String COLUMNS = "id, owner_id, name, description, priority, due, status, created_at, updated_at";

	private static final Map<String, String> SORT_COLUMNS = Map.of(
			"name", "name",
			"description", "description",
			"priority", "priority",
			"due", "due",
			"status", "status",
			"createdAt", "created_at",
			"updatedAt", "updated_at");

	private final Database database;

	@Inject
	public JdbcTaskRepository(Database database) {
		this.database = database;
	}

	@Override
	public UUID nextId() {
		return EntityIds.generate();
	}

	@Override
	public void add(Task task) {
		Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection.prepareStatement("INSERT INTO tasks (" + COLUMNS
					+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
				statement.setObject(1, task.getId());
				statement.setObject(2, task.getOwnerId());
				statement.setString(3, task.getName().value());
				statement.setString(4, task.getDescription().value());
				statement.setString(5, task.getPriority().value());
				statement.setObject(6, Jdbc.timestamp(task.getDue().value()));
				statement.setString(7, task.getStatus().value());
				statement.setObject(8, Jdbc.timestamp(task.getCreatedAt()));
				statement.setObject(9, Jdbc.timestamp(task.getUpdatedAt()));
				statement.executeUpdate();
			}
			return null;
		});
	}

	@Override
	public boolean update(Task task) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection.prepareStatement(
					"UPDATE tasks SET name = ?, description = ?, priority = ?, due = ?, status = ?, updated_at = ? "
							+ "WHERE id = ?")) {
				statement.setString(1, task.getName().value());
				statement.setString(2, task.getDescription().value());
				statement.setString(3, task.getPriority().value());
				statement.setObject(4, Jdbc.timestamp(task.getDue().value()));
				statement.setString(5, task.getStatus().value());
				statement.setObject(6, Jdbc.timestamp(task.getUpdatedAt()));
				statement.setObject(7, task.getId());
				return statement.executeUpdate() == 1;
			}
		});
	}

	@Override
	public Optional<Task> findById(UUID id) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT " + COLUMNS + " FROM tasks WHERE id = ?")) {
				statement.setObject(1, id);
				try (ResultSet resultSet = statement.executeQuery()) {
					return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
				}
			}
		});
	}

	@Override
	public TaskPage findByOwner(UUID ownerId, String status, String orderBy, int page, int pageSize) {
		String filter = status != null ? " AND status = ?" : "";
		String countSql = "SELECT count(*) FROM tasks WHERE owner_id = ?" + filter;
		String listSql = "SELECT " + COLUMNS + " FROM tasks WHERE owner_id = ?" + filter + " ORDER BY "
				+ orderClause(orderBy) + " LIMIT ? OFFSET ?";
		long offset = (long) (page - 1) * pageSize;
		return Jdbc.execute(database, connection -> {
			long total;
			try (PreparedStatement statement = connection.prepareStatement(countSql)) {
				bindOwner(statement, ownerId, status);
				try (ResultSet resultSet = statement.executeQuery()) {
					resultSet.next();
					total = resultSet.getLong(1);
				}
			}
			List<Task> tasks = new ArrayList<>();
			try (PreparedStatement statement = connection.prepareStatement(listSql)) {
				int next = bindOwner(statement, ownerId, status);
				statement.setInt(next, pageSize);
				statement.setLong(next + 1, offset);
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						tasks.add(map(resultSet));
					}
				}
			}
			return new TaskPage(tasks, total);
		});
	}

	@Override
	public boolean deleteById(UUID id) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
				statement.setObject(1, id);
				return statement.executeUpdate() == 1;
			}
		});
	}

	@Override
	public int deleteByOwner(UUID ownerId) {
		return Jdbc.execute(database, connection -> {
			try (PreparedStatement statement = connection
					.prepareStatement("DELETE FROM tasks WHERE owner_id = ?")) {
				statement.setObject(1, ownerId);
				return statement.executeUpdate();
			}
		});
	}

	private static int bindOwner(PreparedStatement statement, UUID ownerId, String status) throws SQLException {
		statement.setObject(1, ownerId);
		if (status != null) {
			statement.setString(2, status);
			return 3;
		}
		return 2;
	}

	private static String orderClause(String orderBy) {
		Set<String> columns = new LinkedHashSet<>();
		if (orderBy != null) {
			String column = SORT_COLUMNS.get(orderBy);
			if (column == null) {
				throw new IllegalArgumentException("Unsupported sort field: " + orderBy);
			}
			columns.add(column);
		}
		columns.add("created_at");
		columns.add("id");
		return String.join(" ASC, ", columns) + " ASC";
	}

	private static Task map(ResultSet resultSet) throws SQLException {
		return Task.restore(
				Jdbc.uuid(resultSet, "id"),
				Jdbc.uuid(resultSet, "owner_id"),
				TaskName.restore(resultSet.getString("name")),
				TaskDescription.restore(resultSet.getString("description")),
				TaskPriority.restore(resultSet.getString("priority")),
				DueDate.restore(Jdbc.instant(resultSet, "due")),
				TaskStatus.restore(resultSet.getString("status")),
				Jdbc.instant(resultSet, "created_at"),
				Jdbc.instant(resultSet, "updated_at"));
	}
}
