package com.taskly.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;

public final class SeedMain {

	private static final Logger log = LoggerFactory.getLogger(SeedMain.class);

	private SeedMain() {
	}

	public static void main(String[] args) {
		int exitCode;
		try {
			Application application = new GuiceApplicationBuilder().in(Mode.PROD).build();
			exitCode = application.injector().instanceOf(SeedRunner.class).run();
		}
		catch (RuntimeException error) {
			log.error("Seeding failed: {}", error.getMessage(), error);
			exitCode = 1;
		}
		System.exit(exitCode);
	}
}
