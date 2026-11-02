package com.taskly.backend.config;

import com.taskly.backend.shared.application.AuthenticatedUser;
import com.taskly.backend.users.application.VerifyAccessToken;
import com.taskly.backend.users.presentation.AuthCookie;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.typedmap.TypedKey;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public class AuthenticatedAction extends Action<Authenticated> {

	public static final TypedKey<AuthenticatedUser> USER = TypedKey.create("authenticatedUser");

	private final VerifyAccessToken verifyAccessToken;

	private final AuthCookie authCookie;

	@Inject
	public AuthenticatedAction(VerifyAccessToken verifyAccessToken, AuthCookie authCookie) {
		this.verifyAccessToken = verifyAccessToken;
		this.authCookie = authCookie;
	}

	@Override
	public CompletionStage<Result> call(Http.Request request) {
		String token = request.getCookie(authCookie.name()).map(Http.Cookie::value).orElse(null);
		AuthenticatedUser user = verifyAccessToken.execute(token);
		return delegate.call(request.addAttr(USER, user));
	}

	public static AuthenticatedUser requester(Http.Request request) {
		return request.attrs().get(USER);
	}
}
