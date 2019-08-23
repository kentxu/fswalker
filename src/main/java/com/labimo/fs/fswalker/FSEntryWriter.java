package com.labimo.fs.fswalker;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;

public interface FSEntryWriter extends AutoCloseable {

	enum OPTION {
		INCLUDE_DIR, 
		INCLUDE_FAILED, 
		SHOW_SIZE, 
		SHOW_DATE, 
		SHOW_ERROR,
		SHOW_TYPE,
		SHOW_HASH,
		HUMAN_SHORTFORMAT, //OPTIONAL
		
	}

	public void write(FSEntry entry) throws UnsupportedEncodingException, IOException;

	void close();

	void setOptions(EnumSet<OPTION> options);

	EnumSet<OPTION> getOptions();
}
