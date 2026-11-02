package com.taskly.backend.users.presentation;

import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.users.application.AuthResult;
import com.taskly.backend.users.application.AuthenticateUser;
import com.taskly.backend.users.application.RegisterUser;
import java.util.Map;
import javax.inject.Inject;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class AuthController extends Controller {

	private final RegisterUser registerUser;

	private final AuthenticateUser authenticateUser;

	private final AuthCookie authCookie;

	@Inject
	public AuthController(RegisterUser registerUser, AuthenticateUser authenticateUser, AuthCookie authCookie) {
		this.registerUser = registerUser;
		this.authenticateUser = authenticateUser;
		this.authCookie = authCookie;
	}

	@BodyParser.Of(BodyParser.TolerantText.class)
	public Result signUp(Http.Request request) {
		return withSession(registerUser.execute(RequestBodies.json(request)));
	}

	@BodyParser.Of(BodyParser.TolerantText.class)
	public Result signIn(Http.Request request) {
		return withSession(authenticateUser.execute(RequestBodies.json(request)));
	}

	public Result signOut() {
		return ok(Json.toJson(Map.of("message", "Sign out successful"))).withCookies(authCookie.clear());
	}

	private Result withSession(AuthResult result) {
		return ok(Json.toJson(result.user())).withCookies(authCookie.issue(result.token()));
	}
}
