package com.taskly.backend.users.presentation;

import com.taskly.backend.config.Authenticated;
import com.taskly.backend.config.AuthenticatedAction;
import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.shared.infrastructure.http.QueryParameters;
import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.users.application.AvatarUpload;
import com.taskly.backend.users.application.ChangeUserAvatar;
import com.taskly.backend.users.application.DeleteUser;
import com.taskly.backend.users.application.GetUser;
import com.taskly.backend.users.application.ListUsers;
import com.taskly.backend.users.application.UpdateUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import javax.inject.Inject;
import play.libs.Files.TemporaryFile;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Authenticated
public class UsersController extends Controller {

	private final GetUser getUser;

	private final ListUsers listUsers;

	private final UpdateUser updateUser;

	private final DeleteUser deleteUser;

	private final ChangeUserAvatar changeUserAvatar;

	private final AuthCookie authCookie;

	private final long avatarMaxBytes;

	private final long avatarMaxMegabytes;

	@Inject
	public UsersController(GetUser getUser, ListUsers listUsers, UpdateUser updateUser, DeleteUser deleteUser,
			ChangeUserAvatar changeUserAvatar, AuthCookie authCookie, TasklyConfig config) {
		this.getUser = getUser;
		this.listUsers = listUsers;
		this.updateUser = updateUser;
		this.deleteUser = deleteUser;
		this.changeUserAvatar = changeUserAvatar;
		this.authCookie = authCookie;
		this.avatarMaxBytes = config.storage().avatarMaxBytes();
		this.avatarMaxMegabytes = config.storage().avatarMaxMegabytes();
	}

	public Result list(Http.Request request) {
		Map<String, String[]> parameters = request.queryString();
		return ok(Json.toJson(listUsers.execute(QueryParameters.value(parameters, "username"),
				QueryParameters.value(parameters, "email"))));
	}

	public Result get(String id) {
		return ok(Json.toJson(getUser.execute(id)));
	}

	@BodyParser.Of(BodyParser.TolerantText.class)
	public Result update(Http.Request request, String id) {
		return ok(Json.toJson(updateUser.execute(AuthenticatedAction.requester(request).userId(), id,
				RequestBodies.json(request))));
	}

	public Result uploadAvatar(Http.Request request, String id) {
		AvatarUpload upload = readUpload(request);
		return ok(Json.toJson(
				changeUserAvatar.execute(AuthenticatedAction.requester(request).userId(), id, upload)));
	}

	public Result delete(Http.Request request, String id) {
		deleteUser.execute(AuthenticatedAction.requester(request).userId(), id);
		return ok(Json.toJson(Map.of("message", "User has been deleted"))).withCookies(authCookie.clear());
	}

	private AvatarUpload readUpload(Http.Request request) {
		Http.MultipartFormData<TemporaryFile> form = request.body().asMultipartFormData();
		if (form == null) {
			return null;
		}
		Http.MultipartFormData.FilePart<TemporaryFile> part = form.getFile("avatar");
		if (part == null) {
			return null;
		}
		try {
			byte[] content = Files.readAllBytes(part.getRef().path());
			if (content.length > avatarMaxBytes) {
				throw new ValidationException("File size exceeds " + avatarMaxMegabytes + "MB");
			}
			return new AvatarUpload(part.getContentType(), content);
		}
		catch (IOException error) {
			throw new UncheckedIOException("Failed to read uploaded avatar", error);
		}
	}
}
