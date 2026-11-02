package com.taskly.backend.shared.infrastructure.http;

import com.taskly.backend.shared.domain.DomainException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

public final class ApiErrors {

	public static final String DEFAULT_MESSAGE = "We're having technical issues. Please try again later.";

	private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

	private static final Map<String, Integer> STATUS_BY_CODE = Map.of(
			"VALIDATION_ERROR", 400,
			"NOT_FOUND", 404,
			"ALREADY_EXISTS", 422,
			"AUTHENTICATION_FAILED", 401,
			"FORBIDDEN", 403);

	private ApiErrors() {
	}

	public static DomainException findDomainException(Throwable error) {
		Throwable current = error;
		for (int depth = 0; current != null && depth < 16; depth++) {
			if (current instanceof DomainException domain) {
				return domain;
			}
			if (current.getCause() == current) {
				return null;
			}
			current = current.getCause();
		}
		return null;
	}

	public static Result fromDomain(DomainException error) {
		int status = STATUS_BY_CODE.getOrDefault(error.getCode(), 500);
		return reject(status, error.getMessage());
	}

	public static Result unexpected(Throwable error) {
		log.error("Unhandled error: {}", error.getMessage(), error);
		return Results.status(500, Json.toJson(new ApiError(500, DEFAULT_MESSAGE)));
	}

	public static Result reject(int status, String message) {
		log.warn("Request rejected status={} message={}", status, message);
		return Results.status(status, Json.toJson(new ApiError(status, message)));
	}

	public static Result notFound() {
		return Results.status(404, Json.toJson(Map.of("message", "Not found")));
	}
}
