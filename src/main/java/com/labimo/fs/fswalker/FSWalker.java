package com.labimo.fs.fswalker;

import java.nio.file.Path;
public interface FSWalker {

	void walk(Path path, int depth);

	long getFileCount();

	void setFSVisitor(FSVisitor visitor);

}
