package com.labimo.fs.fswalker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;

import com.opencsv.CSVWriter;

public class SimpleFSEntryWriter implements FSEntryWriter {
	private static final Logger LOGGER = LogManager.getLogger(SimpleFSEntryWriter.class.getName());

	private OutputStream os = null;
	private Writer osw = null;
	private EnumSet<OPTION> options = EnumSet.noneOf(OPTION.class);
	CSVWriter csvwriter = null;

	public SimpleFSEntryWriter(OutputStream os) {
		CloseShieldOutputStream csos = new CloseShieldOutputStream(os);
		this.os = csos;
		try {
			osw = new BufferedWriter(new OutputStreamWriter(this.os, "UTF-8"));
			csvwriter = new CSVWriter(osw, '\t', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER,
					CSVWriter.DEFAULT_LINE_END);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public SimpleFSEntryWriter(File file) throws FileNotFoundException {
		this(new FileOutputStream(file));
	}

	public SimpleFSEntryWriter(Path outputPath) throws IOException {
		this(Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
	}

	@Override
	public void write(FSEntry entry) throws UnsupportedEncodingException, IOException {
		Path p = entry.getPath();

		if (entry.getError() != null && !options.contains(OPTION.INCLUDE_FAILED)) {
			return;
		}

		if (entry instanceof FSDirEntry && !options.contains(OPTION.INCLUDE_DIR)) {
			return;
		}

		String pathString = entry.getPath().toString();

		ArrayList<String> entries = new ArrayList<String>();

		if (options.contains(OPTION.SHOW_TYPE)) {
			if (entry instanceof FSDirEntry) {
				entries.add("D");
			} else {
				entries.add("F");
			}
		}

		// size
		if (options.contains(OPTION.SHOW_SIZE)) {
			if (options.contains(OPTION.HUMAN_SHORTFORMAT)) {
				entries.add(FileUtils.byteCountToDisplaySize(entry.getSize()) + "");
			} else {
				entries.add(entry.getSize() + "");
			}
		}

		// date
		if (options.contains(OPTION.SHOW_DATE)) {
			if (entry.getDate() == null) {
				entries.add("?");
				LOGGER.debug("no creation date info for " + entry.getPath());
			} else {

			}
			entries.add(entry.getDate() == null ? "?" : entry.getDate().toString());
		}

		// error
		if (options.contains(OPTION.SHOW_ERROR)) {
			entries.add(entry.getError() == null ? "[OK]" : "[ERROR]");
		}

		// path
		entries.add(pathString);

		// hash
		if (options.contains(OPTION.SHOW_HASH)) {
			entries.add(entry.getHash() == null ? "?" : entry.getHash());
		}

		csvwriter.writeNext(entries.toArray(new String[entries.size()]));
	}

	@Override
	public void close() {
		try {
			if (csvwriter != null) {
				csvwriter.close();
			}
		} catch (Exception e) {

		}

		try (Writer o = osw) {
			o.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public EnumSet<OPTION> getOptions() {
		return options;
	}

	@Override
	public void setOptions(EnumSet<OPTION> options) {
		this.options = options;
	}

}
