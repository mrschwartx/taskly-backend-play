package com.taskly.backend.users.presentation;

import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.infrastructure.http.ApiErrors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public class AvatarFilesController extends Controller {

	private static final Map<String, String> CONTENT_TYPES = Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp",
			"gif", "image/gif");

	private final Path imagesDir;

	@Inject
	public AvatarFilesController(TasklyConfig config) {
		this.imagesDir = config.storage().publicPath().resolve("images").normalize();
	}

	public Result show(String file) {
		Path target = imagesDir.resolve(file).normalize();
		if (!target.startsWith(imagesDir) || !Files.isRegularFile(target)) {
			return ApiErrors.notFound();
		}
		String name = target.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
		String contentType = CONTENT_TYPES.get(extension);
		if (contentType == null) {
			return ApiErrors.notFound();
		}
		try {
			return ok(Files.readAllBytes(target)).as(contentType).withHeader("X-Content-Type-Options", "nosniff");
		}
		catch (IOException error) {
			return ApiErrors.notFound();
		}
	}
}
