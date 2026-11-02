package com.taskly.backend.config;

import com.typesafe.config.Config;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.Database;

@Singleton
public class DatabaseMigrator {

	private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

	@Inject
	public DatabaseMigrator(Database database, Config config) {
		Flyway flyway = Flyway.configure()
				.dataSource(database.getDataSource())
				.locations("classpath:db/migration")
				.baselineOnMigrate(config.getBoolean("taskly.flyway.baseline-on-migrate"))
				.baselineVersion(config.getString("taskly.flyway.baseline-version"))
				.load();
		int applied = flyway.migrate().migrationsExecuted;
		log.info("Database migrations applied={}", applied);
	}
}
