package com.techshard.download.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
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

	/**
	 * A constants for buffer size used to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;

	@GetMapping(value = "/download", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StreamingResponseBody> download(final HttpServletResponse response) {

		String fileName = "sample_" + System.currentTimeMillis() + ".zip";
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
	 * Compresses files represented in an array of paths
	 * 
	 * @param files       a String array containing file paths
	 * @param destZipFile The path of the destination zip file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void zip(String[] files, String destZipFile) throws FileNotFoundException, IOException {
		List<File> listFiles = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {
			listFiles.add(new File(files[i]));
		}
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
			zipOut.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
			while ((read = bis.read(bytesIn)) != -1) {
				zipOut.write(bytesIn, 0, read);
			}
			bis.close();
			zipOut.closeEntry();
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
		zipOut.putNextEntry(new ZipEntry(file.getName()));
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = bis.read(bytesIn)) != -1) {
			zipOut.write(bytesIn, 0, read);
		}
		bis.close();
		zipOut.closeEntry();
	}

	@SuppressWarnings("unused")
	private void zipSingleFile(File file, ZipOutputStream zipOut) throws IOException {
		final InputStream inputStream = new FileInputStream(file);
		final ZipEntry zipEntry = new ZipEntry(file.getName());
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = inputStream.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		inputStream.close();
	}

}
