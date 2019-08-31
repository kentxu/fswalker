package com.labimo.fs.fswalker.concurrency;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.labimo.fs.fswalker.DefaultFSVisitor;
import com.labimo.fs.fswalker.FSDirEntry;
import com.labimo.fs.fswalker.FSEntry;
import com.labimo.fs.fswalker.FSVisitor;
import com.labimo.fs.fswalker.FSWriter;

public class ParallelFSVisitor extends DefaultFSVisitor {
	private static final Logger LOGGER = LogManager.getLogger(ParallelFSVisitor.class.getName());

	private ParallelFSVisitorContext ctx;
	private AtomicInteger subVisitorCount = new AtomicInteger(0);
	private FSEntry rootEntry;

	private ParallelFSVisitor() {
		super();
	}

	private ParallelFSVisitor(FSWriter writer) {
		super();
		this.writer = writer;
	}

	public ParallelFSVisitor(ParallelFSVisitorContext ctx) {
		super();
		this.ctx = ctx;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (getRootPath()!=null && ctx != null && !ctx.isTaskQueueFull()) {
			subVisitorCount.incrementAndGet();
			FSVisitor visitor = this.createVisitor();
			this.ctx.createFSWalker(file, attrs, visitor);
			return FileVisitResult.CONTINUE;
		}
		LOGGER.trace(file);
		if (getRootPath()==null) setRootPath(file);
		currentPath = file;
		// it may be a directory when max depth is reached.
		if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
			preVisitDirectory(file, attrs);
			postVisitDirectory(file, null);
			return FileVisitResult.CONTINUE;
		}
		addFileCount(1);
		addTotalSize(attrs.size());
		FSEntry entry = new FSEntry(file, attrs);
		
		
		furnishFSEntry(entry);

		this.writeFSEntry(entry);
		currentPath = null;
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return super.visitFileFailed(file, exc);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return super.postVisitDirectory(dir, exc);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		// do not start spawning walker on the first entry
		if (getDirCount() > 0 && ctx != null && !ctx.isTaskQueueFull()) {
			subVisitorCount.incrementAndGet();
			FSVisitor visitor = this.createVisitor();
			this.ctx.createFSWalker(dir, attrs, visitor);
			// not going into post visit directory
			return FileVisitResult.SKIP_SUBTREE;
		}

		return super.preVisitDirectory(dir, attrs);
	}

	@Override
	protected void writeFSEntry(FSEntry entry) throws UnsupportedEncodingException, IOException {
		if (entry.getPath().equals(this.getRootPath())) {
			// withhold root entry output until visitor completes
			this.rootEntry = entry;
			return;
		}
		super.writeFSEntry(entry);
	}

	private void flushRootFSEntry() {
		if (this.rootEntry == null)
			return;
		if (this.rootEntry instanceof FSDirEntry) {
			((FSDirEntry) this.rootEntry).setSize(this.getTotalSize());
		}
		try {
			super.writeFSEntry(this.rootEntry);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(this.rootEntry, e);
		} catch (IOException e) {
			LOGGER.error(this.rootEntry, e);
		} finally {
			this.rootEntry = null;
		}
	}

	protected void onChildComplete(FSVisitor visitor) {
		
		LOGGER.debug("onChildComplete" + this+" from "+visitor);
		int i = subVisitorCount.addAndGet(-1);
		if (i == 0) {
			// complete root entry
			flushRootFSEntry();
			FSVisitor p = getParent();
			if (p != null && p instanceof ParallelFSVisitor)
				getParent().onChildComplete(this);
		}
	}

	@Override
	public void setCompleted() {
		super.setCompleted();
		int i = subVisitorCount.get();
		if (i <= 0) {
			// complete root entry
			flushRootFSEntry();
			if (getParent() != null)
				getParent().onChildComplete(this);
		}
		
	}

	public boolean isCompleted() {
		return completed && subVisitorCount.get() < 0;
	}

	@Override
	public ParallelFSVisitor getParent() {
		return (ParallelFSVisitor) super.getParent();
	}

	private FSVisitor createVisitor() {
		ParallelFSVisitor fs = new ParallelFSVisitor(ctx);
		fs.setOptions(getOptions());
		fs.setParent(this);
		fs.setWriter(writer);
		return fs;
	}

}
