package com.taskly.backend.shared.infrastructure.http;

import java.util.List;
import java.util.Map;

public final class QueryParameters {

	private QueryParameters() {
	}

	public static Object value(Map<String, String[]> parameters, String key) {
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		return values.length == 1 ? values[0] : List.of(values);
	}
}
