package com.taskly.backend.shared.infrastructure.http;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.pekko.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Filter;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

@Singleton
public class RequestLoggingFilter extends Filter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Inject
	public RequestLoggingFilter(Materializer materializer) {
		super(materializer);
	}

	@Override
	public CompletionStage<Result> apply(Function<RequestHeader, CompletionStage<Result>> next,
			RequestHeader request) {
		long startedAt = System.nanoTime();
		return next.apply(request).whenComplete((result, error) -> {
			double durationMs = Math.round((System.nanoTime() - startedAt) / 10_000.0) / 100.0;
			int status = result != null ? result.status() : 500;
			log.info("HTTP request method={} path={} status={} durationMs={}", request.method(), request.path(),
					status, durationMs);
		});
	}
}
