package com.techshard.download.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Counterpart of {@link DownloadController}. Whatever is uploaded is written
 * into the configured directory as a timestamped archive — the same
 * {@code sample_<millis>.zip} shape the download produces — holding the
 * original files as entries.
 *
 * <p>
 * Two flavours are exposed:
 * </p>
 * <ul>
 * <li><b>POST /api/upload</b> — classic {@code multipart/form-data}, one or many
 * files in a single request, all landing in one archive (browser / form
 * friendly).</li>
 * <li><b>PUT /api/upload/stream</b> — the raw request body is compressed
 * straight into the archive, nothing is buffered in memory. Use this for very
 * large files.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class UploadController {

	private final Logger logger = LoggerFactory.getLogger(UploadController.class);

	@Value("${file.directory:default}")
	String fileDirectory;

	@Value("${file.zip-prefix:sample_}")
	String zipPrefix;

	/**
	 * Uploads one or more files as {@code multipart/form-data}, zipped together
	 * into a single archive.
	 *
	 * @param files the parts posted under the field name {@code files}
	 * @return the archive name plus the entries it holds and their sizes
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> upload(@RequestPart("files") final MultipartFile[] files) {

		final List<MultipartFile> filled = new ArrayList<>();
		for (final MultipartFile file : files) {
			if (file.isEmpty()) {
				logger.warn("Skipping empty part {} ", file.getOriginalFilename());
			} else {
				filled.add(file);
			}
		}

		if (filled.isEmpty()) {
			return ApiResponses.error(HttpStatus.BAD_REQUEST, "No file content received");
		}

		final Path archive = prepareDirectory().resolve(ZipArchives.newArchiveName(zipPrefix));
		final Set<String> entryNames = new LinkedHashSet<>();
		final List<Map<String, Object>> entries = new ArrayList<>();

		try (final OutputStream out = Files.newOutputStream(archive);
				final ZipOutputStream zipOut = new ZipOutputStream(out)) {

			for (final MultipartFile file : filled) {

				final String entryName = ZipArchives.uniqueEntryName(safeFileName(file.getOriginalFilename()),
						entryNames);

				long size;
				try (final InputStream in = file.getInputStream()) {
					size = ZipArchives.addEntry(zipOut, entryName, in);
				}

				entryNames.add(entryName);
				entries.add(ApiResponses.describe(entryName, size));
			}
			zipOut.finish();

		} catch (final IOException e) {
			logger.error("Exception while writing archive {} ", archive, e);
			discard(archive);
			return ApiResponses.error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not write archive");
		}

		return archiveCreated(archive, entries);
	}

	/**
	 * Compresses the raw request body straight into an archive without buffering
	 * it in memory — the upload mirror of the {@code StreamingResponseBody}
	 * download.
	 *
	 * @param name    the name the file is stored under inside the archive
	 * @param request the current request, whose input stream is consumed
	 * @return the archive name plus the single entry it holds
	 */
	@PutMapping(value = "/upload/stream", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> uploadStream(@RequestParam("name") final String name,
			final HttpServletRequest request) {

		final String entryName = safeFileName(name);
		final Path archive = prepareDirectory().resolve(ZipArchives.newArchiveName(zipPrefix));

		final List<Map<String, Object>> entries = new ArrayList<>();

		try (final OutputStream out = Files.newOutputStream(archive);
				final ZipOutputStream zipOut = new ZipOutputStream(out);
				final InputStream in = request.getInputStream()) {

			entries.add(ApiResponses.describe(entryName, ZipArchives.addEntry(zipOut, entryName, in)));
			zipOut.finish();

		} catch (final IOException e) {
			logger.error("Exception while streaming {} into archive {} ", entryName, archive, e);
			discard(archive);
			return ApiResponses.error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not write archive");
		}

		return archiveCreated(archive, entries);
	}

	/**
	 * Logs the finished archive and builds the 201 response.
	 *
	 * @param archive the archive just written
	 * @param entries the entries it holds
	 * @return the response to hand back to the client
	 */
	private ResponseEntity<Map<String, Object>> archiveCreated(final Path archive,
			final List<Map<String, Object>> entries) {

		long archiveSize = 0;
		try {
			archiveSize = Files.size(archive);
		} catch (final IOException e) {
			logger.warn("Could not read the size of {} ", archive, e);
		}

		logger.info("Wrote archive {} holding {} file(s), {} bytes ", archive, entries.size(), archiveSize);
		return new ResponseEntity<>(ApiResponses.archive(archive.getFileName().toString(), archiveSize, entries),
				HttpStatus.CREATED);
	}

	/**
	 * Removes a half written archive so a failed upload leaves nothing behind for
	 * the download endpoint to pick up.
	 *
	 * @param archive the archive to remove
	 */
	private void discard(final Path archive) {
		try {
			Files.deleteIfExists(archive);
		} catch (final IOException e) {
			logger.warn("Could not remove the incomplete archive {} ", archive, e);
		}
	}

	/**
	 * Resolves the configured directory, creating it when it does not exist yet.
	 *
	 * @return the directory archives are written into
	 */
	private Path prepareDirectory() {
		final Path directory = Paths.get(fileDirectory).toAbsolutePath().normalize();
		try {
			Files.createDirectories(directory);
		} catch (final IOException e) {
			throw new IllegalStateException("Could not create upload directory " + directory, e);
		}
		return directory;
	}

	/**
	 * Strips any path information from the supplied name, so a crafted name can
	 * neither escape the upload directory nor plant a path inside the archive.
	 *
	 * @param originalFileName the client supplied file name
	 * @return a bare file name, safe to use as an entry name
	 */
	private String safeFileName(final String originalFileName) {
		final String cleaned = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
		final String fileName = new File(cleaned).getName();

		if (fileName.isEmpty() || fileName.contains("..")) {
			throw new IllegalArgumentException("Invalid file name: " + originalFileName);
		}
		return fileName;
	}

}
