package com.labimo.fs.fswalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.openhft.hashing.LongHashFunction;

public class FSVisitor extends SimpleFileVisitor<Path> {

	private static final Logger LOGGER = LogManager.getLogger(FSVisitor.class.getName());

	FSEntryWriter writer;
	/**
	 * count every file visited, regardless whether the visit is successful
	 */
	private long fileCount = 0;
	private long fileErrorCount = 0;

	private long totalSize = 0;

	private long dirCount = 0;

	private ArrayList<FSDirEntry> visitingPaths = new ArrayList<FSDirEntry>();
	private FSDirEntry currentDirEntry = null;

	private Set<FSVisitorOption> options = EnumSet.noneOf(FSVisitorOption.class);

	static int SAMPLING_CONTENTTRESH = 1024 * 20;
	private byte[] contentBytes = null;

	public FSVisitor() {
		super();
	}

	public FSVisitor(FSEntryWriter writer) {
		super();
		this.writer = writer;
	}

	private Set<FSVisitorOption> getOptions() {
		return options;
	}

	public void setOptions(Set<FSVisitorOption> options) {
		this.options = options;
	}

	public FSEntryWriter getWriter() {
		return writer;
	}

	public void setWriter(FSEntryWriter writer) {
		this.writer = writer;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		// it may be a directory when max depth is reached.
		if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
			preVisitDirectory(file, attrs);
			postVisitDirectory(file, null);
			return FileVisitResult.CONTINUE;
		}
		fileCount++;
		FSEntry entry = new FSEntry(file, attrs);
		if (this.options.contains(FSVisitorOption.HASH_SIZE20K)) {
			try (InputStream fis = Files.newInputStream(file, StandardOpenOption.READ)) {

				if (contentBytes == null) {
					contentBytes = new byte[SAMPLING_CONTENTTRESH + Long.BYTES];
				}
				byte[] bytes = Utils.longToBytes(attrs.size());
				System.arraycopy(bytes, 0, contentBytes, 0, bytes.length);
				int len = fis.read(contentBytes, 0, SAMPLING_CONTENTTRESH);
				if (len > 0) {
					long hash = LongHashFunction.xx().hashBytes(contentBytes);
					entry.setHash(Long.toString(hash));
				}
			}
		}

		this.writeFSEntry(entry);
		totalSize += attrs.size();
		return super.visitFile(file, attrs);
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		fileErrorCount = getFileErrorCount() + 1;
		FSEntry entry = new FSEntry(file, true);
		this.writeFSEntry(entry);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//		if (dir.getFileName().toString().endsWith(".app")) {
//			return FileVisitResult.SKIP_SUBTREE;
//		}
		dirCount++;
//		FSEntry entry = new FSDirEntry(dir, attrs);
		currentDirEntry = new FSDirEntry(dir, attrs);
		visitingPaths.add(currentDirEntry);
//		if (!this.options.contains(FSVisitorOption.COMPUTE_DIRSIZE)) {
//			this.writeFSEntry(currentDirEntry);
//		}


		currentDirEntry.setSize(this.totalSize);
		return super.preVisitDirectory(dir, attrs);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

		if (visitingPaths.size() > 0 && currentDirEntry.getPath().equals(dir)) {
			currentDirEntry = visitingPaths.remove(visitingPaths.size() - 1);
			if (exc != null) {
				currentDirEntry.setSize(0);
				currentDirEntry.setError(exc);
			} else {
				currentDirEntry.setSize(this.totalSize - currentDirEntry.getSize());
			}
		}

		if (currentDirEntry != null /* && this.options.contains(FSVisitorOption.COMPUTE_DIRSIZE) */) {
			this.writeFSEntry(currentDirEntry);
		}


		return FileVisitResult.CONTINUE;
//		return super.postVisitDirectory(dir, exc);
	}

	private void writeFSEntry(FSEntry entry) throws UnsupportedEncodingException, IOException {
		if (this.writer != null) {
			writer.write(entry);
		}
	}

	/**
	 * 
	 * @return visited file count
	 */
	public long getFileCount() {
		return fileCount;
	}

	/**
	 * @return disk space
	 */
	public long getSize() {
		return totalSize;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("files: " + this.fileCount + "\n");
		buff.append("directories: " + this.dirCount + "\n");
		buff.append("size: " + this.totalSize + "\n");
		return buff.toString();
	}

	/**
	 * 
	 * @return file visit error count
	 */
	public long getFileErrorCount() {
		return fileErrorCount;
	}

	public long getDirCount() {
		return dirCount;
	}

}
