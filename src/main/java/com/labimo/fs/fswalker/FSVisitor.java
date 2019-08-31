package com.labimo.fs.fswalker;

import java.nio.file.FileVisitor;
import java.nio.file.Path;

public interface FSVisitor extends FileVisitor<Path> {

	long getDirCount();

	long getFileErrorCount();

	long getSize();

	long getFileCount();
	
	Path getProcessingPath();
	
	/**
	 * null if no parent is set
	 * @return
	 */
	FSVisitor getParent();

	void addDirCount(long i);

	void addTotalSize(long i);

	void addDirErrorCount(long i);

	void addFileErrorCount(long i);

	void addFileCount(long i);
	
	void setCompleted();

}
