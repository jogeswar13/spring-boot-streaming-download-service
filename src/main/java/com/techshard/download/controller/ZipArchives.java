package com.techshard.download.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Shared zip plumbing: the archive naming both endpoints use, and the single
 * buffered copy routine that writes an entry.
 */
final class ZipArchives {

	/**
	 * A constants for buffer size used to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;

	private static final String ZIP_EXTENSION = ".zip";

	private ZipArchives() {
		// utility class
	}

	/**
	 * Builds the archive name shared by the download and upload endpoints, e.g.
	 * {@code sample_1788287531099.zip}.
	 *
	 * @param prefix the configured name prefix
	 * @return a name unique to this millisecond
	 */
	static String newArchiveName(final String prefix) {
		return prefix + System.currentTimeMillis() + ZIP_EXTENSION;
	}

	/**
	 * Streams one entry into the archive, copying in fixed size chunks so the
	 * payload is never held in memory.
	 *
	 * @param zipOut    the archive being written
	 * @param entryName the name the entry is stored under
	 * @param source    the data to copy; the caller closes it
	 * @return the number of bytes read from the source
	 * @throws IOException if reading or writing fails
	 */
	static long addEntry(final ZipOutputStream zipOut, final String entryName, final InputStream source)
			throws IOException {

		zipOut.putNextEntry(new ZipEntry(entryName));

		final byte[] buffer = new byte[BUFFER_SIZE];
		long copied = 0;
		int read;
		while ((read = source.read(buffer)) != -1) {
			zipOut.write(buffer, 0, read);
			copied += read;
		}

		zipOut.closeEntry();
		return copied;
	}

	/**
	 * A zip cannot hold two entries of the same name, and a single upload may
	 * well carry the same file name from two different folders. Suffixes the
	 * name with a counter until it is free.
	 *
	 * @param entryName the desired name
	 * @param taken     the names already written to this archive
	 * @return a name not present in {@code taken}
	 */
	static String uniqueEntryName(final String entryName, final Set<String> taken) {
		if (!taken.contains(entryName)) {
			return entryName;
		}

		final int dot = entryName.lastIndexOf('.');
		final String base = dot > 0 ? entryName.substring(0, dot) : entryName;
		final String extension = dot > 0 ? entryName.substring(dot) : "";

		int counter = 1;
		String candidate;
		do {
			candidate = base + "_" + counter + extension;
			counter++;
		} while (taken.contains(candidate));

		return candidate;
	}

}
