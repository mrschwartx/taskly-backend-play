package com.taskly.backend.shared.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.taskly.backend.shared.domain.ValidationException;
import java.util.Collections;
import java.util.Map;
import play.libs.Json;
import play.mvc.Http;

public final class RequestBodies {

	private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {
	};

	private RequestBodies() {
	}

	public static Map<String, Object> json(Http.Request request) {
		String text = request.body().asText();
		if (text == null || text.isBlank()) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> body = Json.mapper().readValue(text, OBJECT);
			return body != null ? body : Collections.emptyMap();
		}
		catch (JsonProcessingException error) {
			throw new ValidationException("Malformed JSON body");
		}
	}
}
