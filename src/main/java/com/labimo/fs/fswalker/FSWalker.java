package com.labimo.fs.fswalker;

import java.nio.file.Path;
public interface FSWalker extends Runnable {

	void walk(Path path, int depth);

	long getFileCount();

	void setFSVisitor(FSVisitor visitor);
	
	void addWalkerListener(FSWalkerListener listener);
	
	void removeWalkerListener(FSWalkerListener listener);
	
	FSVisitor getVisitor();
}
