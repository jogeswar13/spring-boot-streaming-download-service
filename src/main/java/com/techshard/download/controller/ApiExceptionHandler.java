package com.techshard.download.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Turns the failures an upload can hit into plain JSON instead of the default
 * error page, so scripted clients get something they can read.
 *
 * <p>
 * Deliberately global rather than scoped to {@link UploadController}: a
 * multipart request is parsed before the handler is resolved, so a
 * {@link MaxUploadSizeExceededException} reaches the resolver with no handler
 * method attached and a controller scoped advice would never match it.
 * </p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** Raised by {@code safeFileName} when the client sends a path, not a name. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidRequest(final IllegalArgumentException e) {
		logger.warn("Rejected upload: {} ", e.getMessage());
		return ApiResponses.error(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	/** Raised when the part exceeds {@code spring.servlet.multipart.max-file-size}. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> handleTooLarge(final MaxUploadSizeExceededException e) {
		logger.warn("Rejected upload, payload too large: {} ", e.getMessage());
		return ApiResponses.error(HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the configured maximum size");
	}

	/** The upload directory could not be created or is not writable. */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> handleStorageUnavailable(final IllegalStateException e) {
		logger.error("Upload storage unavailable ", e);
		return ApiResponses.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
	}

}
