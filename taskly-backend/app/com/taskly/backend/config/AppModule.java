package com.taskly.backend.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.infrastructure.events.InMemoryEventBus;
import com.taskly.backend.tasks.domain.TaskRepository;
import com.taskly.backend.tasks.infrastructure.JdbcTaskRepository;
import com.taskly.backend.tasks.infrastructure.UserDeletedSubscriber;
import com.taskly.backend.users.application.AvatarStorage;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.TokenService;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.infrastructure.BcryptPasswordHasher;
import com.taskly.backend.users.infrastructure.JdbcUserRepository;
import com.taskly.backend.users.infrastructure.JwtTokenService;
import com.taskly.backend.users.infrastructure.LocalAvatarStorage;
import com.taskly.backend.users.presentation.AuthCookie;
import javax.inject.Singleton;

public class AppModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DatabaseMigrator.class).asEagerSingleton();
		bind(EventBus.class).to(InMemoryEventBus.class);
		bind(UserRepository.class).to(JdbcUserRepository.class);
		bind(TaskRepository.class).to(JdbcTaskRepository.class);
		bind(UserDeletedSubscriber.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	PasswordHasher passwordHasher(TasklyConfig config) {
		return new BcryptPasswordHasher(config.auth().bcryptRounds());
	}

	@Provides
	@Singleton
	TokenService tokenService(TasklyConfig config) {
		return new JwtTokenService(config.auth().secret(), config.auth().tokenTtl());
	}

	@Provides
	@Singleton
	AvatarStorage avatarStorage(TasklyConfig config) {
		return new LocalAvatarStorage(config.storage().publicPath());
	}

	@Provides
	@Singleton
	AuthCookie authCookie(TasklyConfig config) {
		return new AuthCookie(config.auth().cookieName(), config.auth().tokenTtl(), config.auth().cookieDomain(),
				config.app().production());
	}
}
