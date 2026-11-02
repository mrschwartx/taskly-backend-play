package com.taskly.backend.users.presentation;

import java.time.Duration;
import play.mvc.Http;

public class AuthCookie {

	private final String name;

	private final Duration maxAge;

	private final String domain;

	private final boolean production;

	public AuthCookie(String name, Duration maxAge, String domain, boolean production) {
		this.name = name;
		this.maxAge = maxAge;
		this.domain = domain;
		this.production = production;
	}

	public String name() {
		return name;
	}

	public Http.Cookie issue(String token) {
		return build(token, maxAge);
	}

	public Http.Cookie clear() {
		return build("", Duration.ZERO);
	}

	private Http.Cookie build(String value, Duration age) {
		Http.CookieBuilder builder = Http.Cookie.builder(name, value)
				.withHttpOnly(true)
				.withPath("/")
				.withMaxAge(age);
		if (production) {
			builder.withSameSite(Http.Cookie.SameSite.NONE).withSecure(true);
			if (domain != null && !domain.isBlank()) {
				builder.withDomain(domain);
			}
		}
		else {
			builder.withSameSite(Http.Cookie.SameSite.LAX).withSecure(false);
		}
		return builder.build();
	}
}
