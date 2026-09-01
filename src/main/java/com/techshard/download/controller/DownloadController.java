package com.techshard.download.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api")
public class DownloadController {

	private final Logger logger = LoggerFactory.getLogger(DownloadController.class);

	@Value("${file.directory:default}")
	String fileDirectory;

	@Value("${file.zip-prefix:sample_}")
	String zipPrefix;

	@GetMapping(value = "/download", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StreamingResponseBody> download(final HttpServletResponse response) {

		String fileName = ZipArchives.newArchiveName(zipPrefix);
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		StreamingResponseBody stream = out -> {

			final String home = fileDirectory;
			final File directory = new File(home);
			final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());

			if (directory.exists() && directory.isDirectory()) {
				try {
					for (final File file : directory.listFiles()) {

						if (file.isDirectory()) {
							zipDirectory(file, file.getName(), zipOut);
						} else {
							zipFile(file, zipOut);
						}
					}
					zipOut.flush();
					zipOut.close();
				} catch (final IOException e) {
					logger.error("Exception while reading and streaming data {} ", e);
				}
			}
		};
		logger.info("steaming response {} ", stream);
		return new ResponseEntity<>(stream, HttpStatus.OK);
	}

	/**
	 * Adds a directory to the current zip output stream
	 *
	 * @param folder       the directory to be added
	 * @param parentFolder the path of parent directory
	 * @param zipOut       the current zip output stream
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void zipDirectory(File folder, String parentFolder, ZipOutputStream zipOut)
			throws FileNotFoundException, IOException {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				zipDirectory(file, parentFolder + "/" + file.getName(), zipOut);
				continue;
			}
			addFileEntry(file, parentFolder + "/" + file.getName(), zipOut);
		}
	}

	/**
	 * Adds a file to the current zip output stream
	 *
	 * @param file   the file to be added
	 * @param zipOut the current zip output stream
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void zipFile(File file, ZipOutputStream zipOut) throws FileNotFoundException, IOException {
		addFileEntry(file, file.getName(), zipOut);
	}

	/**
	 * Reads a file off disk into one entry of the archive being streamed.
	 *
	 * @param file      the file to be added
	 * @param entryName the name the entry is stored under
	 * @param zipOut    the current zip output stream
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void addFileEntry(File file, String entryName, ZipOutputStream zipOut)
			throws FileNotFoundException, IOException {
		try (final InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			ZipArchives.addEntry(zipOut, entryName, in);
		}
	}

}
