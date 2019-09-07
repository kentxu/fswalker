package com.labimo.fs.fswalker;


import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FSDirEntry extends FSEntry {

	long size = 0;

	public FSDirEntry(Path path) {
		super(path);
	}

	public FSDirEntry(Path path, BasicFileAttributes attrs) {
		super(path, attrs);
	}

	public FSDirEntry(Path path, boolean failed) {
		super(path, failed);
	}

	@Override
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public String toString() {
		return "FSDirEntry [path=" + path + "]";
	}
	
	@Override
	public short getType() {
		return TYPE_DIR;
	}
	
}
