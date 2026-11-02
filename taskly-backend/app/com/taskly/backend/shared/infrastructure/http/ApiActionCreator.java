package com.taskly.backend.shared.infrastructure.http;

import com.taskly.backend.shared.domain.DomainException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Singleton;
import play.http.ActionCreator;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;

@Singleton
public class ApiActionCreator implements ActionCreator {

	@Override
	public Action createAction(Request request, Method actionMethod) {
		return new Action.Simple() {
			@Override
			public CompletionStage<Result> call(Request req) {
				CompletionStage<Result> stage;
				try {
					stage = delegate.call(req);
				}
				catch (RuntimeException error) {
					return CompletableFuture.completedFuture(map(error));
				}
				return stage.handle((result, error) -> {
					if (error == null) {
						return CompletableFuture.completedFuture(result);
					}
					DomainException domain = ApiErrors.findDomainException(error);
					if (domain == null) {
						return CompletableFuture.<Result>failedFuture(error);
					}
					return CompletableFuture.completedFuture(ApiErrors.fromDomain(domain));
				}).thenCompose(Function.identity());
			}

			private Result map(RuntimeException error) {
				DomainException domain = ApiErrors.findDomainException(error);
				if (domain == null) {
					throw error;
				}
				return ApiErrors.fromDomain(domain);
			}
		};
	}
}
