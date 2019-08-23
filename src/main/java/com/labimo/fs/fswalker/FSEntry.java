package com.labimo.fs.fswalker;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class FSEntry {

	final static IOException GENERIC_ERROR=new IOException();
	Path path;
	BasicFileAttributes attrs;
	String hash=null;
	IOException error;
	
	
	public FSEntry(Path path) {
		super();
		this.path = path;
	}
	
	public FSEntry(Path path,boolean failed) {
		this(path);
		if (failed) {
			this.error=GENERIC_ERROR;
		}
	}

	public FSEntry(Path path, BasicFileAttributes attrs) {
		super();
		this.path = path;
		this.attrs = attrs;
	}

	public String toString() {
		return path.toString();
	}

	public IOException getError() {
		return error;
	}

	public void setError(IOException error) {
		this.error = error;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}
	
	public long getSize() {
		return attrs==null? 0:attrs.size();
	}
	
	public FileTime getDate() {
		return attrs==null? null:attrs.creationTime();
	}
	
	public Object getFileKey() {
		return attrs==null? null:attrs.fileKey();
	}
	
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
	
	
}
