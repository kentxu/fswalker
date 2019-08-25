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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.openhft.hashing.LongHashFunction;

public class ParallelFSVisitor extends SimpleFileVisitor<Path> {
	
	private static final Logger LOGGER = LogManager.getLogger(ParallelFSVisitor.class.getName());

	FSEntryWriter writer;
	/**
	 * count every file visited, regardless whether the visit is successful
	 */
	private long fileCount = 0;
	private long fileErrorCount = 0;

	private long totalSize = 0;

	private long dirCount = 0;

	private HashMap<Path, FSDirEntry> visitingPaths = new HashMap<Path, FSDirEntry>();
//	private FSDirEntry currentDirEntry = null;

	private Set<FSVisitorOption> options = EnumSet.noneOf(FSVisitorOption.class);

	static int SAMPLING_CONTENTTRESH = 1024 * 20;
	private byte[] contentBytes = null;

	public ParallelFSVisitor() {
		super();
	}

	public ParallelFSVisitor(FSEntryWriter writer) {
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
		
		dirCount++;
//		FSEntry entry = new FSDirEntry(dir, attrs);
		FSDirEntry currentDirEntry = new FSDirEntry(dir, attrs);
		visitingPaths.put(dir, currentDirEntry);
//		if (!this.options.contains(FSVisitorOption.COMPUTE_DIRSIZE)) {
//			this.writeFSEntry(currentDirEntry);
//		}

		currentDirEntry.setSize(this.totalSize);
		return super.preVisitDirectory(dir, attrs);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

		FSDirEntry currentDirEntry = visitingPaths.get(dir);
		if (currentDirEntry == null) {
			// not supposed to happen
			LOGGER.error("exiting a directory not in visiting stack " + dir);
			return FileVisitResult.CONTINUE;
		}
		visitingPaths.remove(dir);

		if (exc != null) {
			currentDirEntry.setSize(0);
			currentDirEntry.setError(exc);
		} else {
			currentDirEntry.setSize(this.totalSize - currentDirEntry.getSize());
		}

		this.writeFSEntry(currentDirEntry);

		return FileVisitResult.CONTINUE;
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

	public ParallelFSVisitor fork() {
		return null;
	}

}
