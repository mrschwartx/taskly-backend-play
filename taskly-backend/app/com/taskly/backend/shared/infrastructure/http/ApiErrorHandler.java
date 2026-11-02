package com.taskly.backend.shared.infrastructure.http;

import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.domain.DomainException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.http.HttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

@Singleton
public class ApiErrorHandler implements HttpErrorHandler {

	private static final String UPLOAD_PREFIX = "/api/v1/users/upload/";

	private final long avatarMaxMegabytes;

	@Inject
	public ApiErrorHandler(TasklyConfig config) {
		this.avatarMaxMegabytes = config.storage().avatarMaxMegabytes();
	}

	@Override
	public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
		if (statusCode == 404) {
			return CompletableFuture.completedFuture(ApiErrors.notFound());
		}
		if (statusCode == 413 && request.path().startsWith(UPLOAD_PREFIX)) {
			return CompletableFuture
					.completedFuture(ApiErrors.reject(400, "File size exceeds " + avatarMaxMegabytes + "MB"));
		}
		String detail = message == null || message.isBlank() ? "Request failed" : message;
		return CompletableFuture.completedFuture(ApiErrors.reject(statusCode, detail));
	}

	@Override
	public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
		DomainException domain = ApiErrors.findDomainException(exception);
		Result result = domain != null ? ApiErrors.fromDomain(domain) : ApiErrors.unexpected(exception);
		return CompletableFuture.completedFuture(result);
	}
}
