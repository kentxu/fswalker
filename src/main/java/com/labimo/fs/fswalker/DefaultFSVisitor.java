package com.labimo.fs.fswalker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.labimo.fs.fswalker.hash.FullContentHasher;
import com.labimo.fs.fswalker.hash.NKEHasher;
import com.labimo.fs.fswalker.hash.NKHasher;

public class DefaultFSVisitor extends SimpleFileVisitor<Path> implements FSVisitor {

	private static final Logger LOGGER = LogManager.getLogger(DefaultFSVisitor.class.getName());

	protected FSWriter writer;
	/**
	 * count every file visited, regardless whether the visit is successful
	 */
	private volatile long fileCount = 0;
	private volatile long fileErrorCount = 0;
	private volatile long dirErrorCount = 0;

	private volatile long totalSize = 0;

	private volatile long dirCount = 0;

	private HashMap<Path, FSDirEntry> visitingPaths = new HashMap<Path, FSDirEntry>();

	protected volatile Path currentPath = null;

	private Set<FSVisitorOption> options = EnumSet.noneOf(FSVisitorOption.class);

	static int SAMPLING_CONTENTTRESH = 1024 * 20;
	private byte[] contentBytes = null;
	
	private Path rootPath=null;
	private long depthLimit=-1;
	
	private FSVisitor parent=null;

	protected volatile boolean completed;

	private FSEntryHasher hasher;
	

	public DefaultFSVisitor() {
		super();
	}

	public DefaultFSVisitor(FSWriter writer) {
		super();
		this.writer = writer;
	}

	protected Set<FSVisitorOption> getOptions() {
		return EnumSet.copyOf(options);
	}

	public void setOptions(Set<FSVisitorOption> options) {
		this.options = options;
		if (this.options.contains(FSVisitorOption.HASH_SIZE20K)) {
			this.hasher=new NKHasher();
		}else if (this.options.contains(FSVisitorOption.HASH_SIZE20KE)) {
			this.hasher=new NKEHasher();
		}else if (this.options.contains(FSVisitorOption.HASH_FULL)) {
			this.hasher=new FullContentHasher();
		}else {
			this.hasher=null;
		}
	}

	public FSWriter getWriter() {
		return writer;
	}

	public void setWriter(FSWriter writer) {
		this.writer = writer;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		LOGGER.trace(file);
		if (this.rootPath==null) this.rootPath=file;
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
	
	protected void furnishFSEntry(FSEntry entry) {
		
		if (entry instanceof FSDirEntry) {
			
		}else if (this.hasher!=null) {
			try {
				String h=this.hasher.hashFSEntry(entry,null);
				entry.setHash(h);
			} catch (IOException e1) {
				LOGGER.trace("content hash failure ",e1);
				entry.setError(e1);
			} catch (Exception e) {
				LOGGER.trace("content hash failure ",e);
			}
		}
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		LOGGER.warn("failed file visit " + file, exc);
		currentPath = file;
		addFileErrorCount(1);
		FSEntry entry = new FSEntry(file, true);
		this.writeFSEntry(entry);
		currentPath = null;
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {


		LOGGER.trace(dir);
		if (this.rootPath==null) this.rootPath=dir;
		currentPath = dir;
		addDirCount(1);
		FSDirEntry currentDirEntry = new FSDirEntry(dir, attrs);
		synchronized (visitingPaths) {
			visitingPaths.put(dir, currentDirEntry);
		}

		currentDirEntry.setSize(this.totalSize);
		return super.preVisitDirectory(dir, attrs);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		LOGGER.trace(dir);

		if (exc != null) {
			addDirErrorCount(1);
			LOGGER.warn("failed diretory visit " + dir, exc);
		}
		FSDirEntry currentDirEntry = null;
		synchronized (visitingPaths) {

			currentDirEntry = visitingPaths.get(dir);
			if (currentDirEntry == null) {
				// not supposed to happen
				LOGGER.error("exiting a directory not in visiting stack " + dir);
				return FileVisitResult.CONTINUE;
			}
			visitingPaths.remove(dir);
		}
		if (exc != null) {
			currentDirEntry.setSize(0);
			currentDirEntry.setError(exc);
		} else {
			currentDirEntry.setSize(this.totalSize - currentDirEntry.getSize());
		}
		furnishFSEntry(currentDirEntry);
		this.writeFSEntry(currentDirEntry);
		currentPath = null;
		return FileVisitResult.CONTINUE;
	}

	protected void writeFSEntry(FSEntry entry) throws UnsupportedEncodingException, IOException {
		if (this.writer != null) {
			writer.write(entry);
		}
	}



	/**
	 * @return disk space
	 */
	@Override
	public long getSize() {
		return totalSize;
	}


	@Override
	public String toString() {
		return "DefaultFSVisitor [rootPath=" + rootPath + "]";
	}

	public long getFileCount() {
		return fileCount;
	}
	


	public long getFileErrorCount() {
		return fileErrorCount;
	}
	

	public long getDirErrorCount() {
		return dirErrorCount;
	}


	
	public long getTotalSize() {
		return totalSize;
	}
	

	public long getDirCount() {
		return dirCount;
	}
	

	@Override
	public Path getProcessingPath() {
		return currentPath;
	}

	@Override
	public FSVisitor getParent() {
		return parent;
	}
	
	public void setParent(FSVisitor p) {
		this.parent=p;
	}

	@Override
	public void setCompleted() {
		this.completed=true;
	}

	public Path getRootPath() {
		return rootPath;
	}
	
	@Override
	public synchronized void addFileCount(long i) {
		this.fileCount+=i;
		if (getParent()!=null) {
			getParent().addFileCount(i);
		}

	}

	@Override
	public synchronized  void addFileErrorCount(long i) {
		this.fileErrorCount+=i;
		if (getParent()!=null) {
			getParent().addFileErrorCount(i);
		}
	}

	@Override
	public synchronized void addDirErrorCount(long i) {
		this.dirErrorCount+=i;
		if (getParent()!=null) {
			getParent().addDirErrorCount(i);
		}
	}

	@Override
	public synchronized void addTotalSize(long i) {
		this.totalSize+=i;
		if (getParent()!=null) {
			getParent().addTotalSize(i);
		}
	}

	@Override
	public synchronized void addDirCount(long i) {
		this.dirCount+=i;
		if (getParent()!=null) {
			getParent().addDirCount(i);
		}
	}

	protected void setRootPath(Path rootPath) {
		this.rootPath = rootPath;
	}

}
