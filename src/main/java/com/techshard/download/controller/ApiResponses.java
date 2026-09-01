package com.techshard.download.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Single place that shapes the JSON bodies returned by the API, so the
 * controllers and the exception handler stay consistent.
 */
final class ApiResponses {

	private ApiResponses() {
		// utility class
	}

	/**
	 * Builds an error body of the form {@code {"status": ..., "error": ...}}.
	 *
	 * @param status  the HTTP status to return
	 * @param message a human readable description of what went wrong
	 * @return the response entity to hand back to the client
	 */
	static ResponseEntity<Map<String, Object>> error(final HttpStatus status, final String message) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status.value());
		body.put("error", message);
		return new ResponseEntity<>(body, status);
	}

	/**
	 * Builds the per-file description returned after a successful upload.
	 *
	 * @param fileName the name the file was stored under
	 * @param size     the number of bytes written
	 * @return the description entry
	 */
	static Map<String, Object> describe(final String fileName, final long size) {
		final Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("fileName", fileName);
		entry.put("size", size);
		return entry;
	}

	/**
	 * Builds the body returned once an upload archive has been written.
	 *
	 * @param archiveName the name of the archive on disk
	 * @param archiveSize the compressed size of the archive in bytes
	 * @param files       the entries it holds, from {@link #describe}
	 * @return the response body
	 */
	static Map<String, Object> archive(final String archiveName, final long archiveSize,
			final List<Map<String, Object>> files) {

		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("archive", archiveName);
		body.put("archiveSize", archiveSize);
		body.put("count", files.size());
		body.put("files", files);
		return body;
	}

}
