package com.labimo.fs.fswalker;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSWalker implements FSWalkerAPI, Runnable {

	private static final Logger LOGGER = LogManager.getLogger(FSWalker.class.getName());
	public final static int INTERVAL = 1000;
	private FSVisitor visitor = null;
	Path path = null;

	public FSWalker() {
		super();
	}

	public FSWalker(Path path) {
		super();
		this.path = path;
	}

	public FSWalker(FSVisitor visitor) {
		super();
		this.visitor = visitor;
	}

	@Override
	public void walk(Path path, int depth) {
		LOGGER.debug("start walking " + this.path);
		Path dir = path;
		try {
			if (visitor == null) {
				visitor = new FSVisitor();
			}
			if (dir == null)
				return;
			EnumSet<FileVisitOption> walkOptions = EnumSet.noneOf(FileVisitOption.class);
			Files.walkFileTree(dir, walkOptions, depth, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public long getFileCount() {
		return visitor == null ? 0 : visitor.getFileCount();
	}

	@Override
	public void setFSVisitor(FSVisitor visitor) {
		this.visitor = visitor;
	}

	public FSVisitor getVisitor() {
		return visitor;
	}

	@Override
	public void run() {
		walk(this.path, Integer.MAX_VALUE);
	}

}
