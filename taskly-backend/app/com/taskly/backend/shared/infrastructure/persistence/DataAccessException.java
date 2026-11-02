package com.taskly.backend.shared.infrastructure.persistence;

import java.sql.SQLException;

public class DataAccessException extends RuntimeException {

	public DataAccessException(SQLException cause) {
		super(cause.getMessage(), cause);
	}
}
