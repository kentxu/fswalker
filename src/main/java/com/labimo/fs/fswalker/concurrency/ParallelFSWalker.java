package com.labimo.fs.fswalker.concurrency;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.labimo.fs.fswalker.DefaultFSVisitor;
import com.labimo.fs.fswalker.FSVisitor;
import com.labimo.fs.fswalker.FSWalker;
import com.labimo.fs.fswalker.FSWalkerListener;
import com.labimo.fs.fswalker.FSWalkerListener.EVT;

public class ParallelFSWalker implements FSWalker {

	private static final Logger LOGGER = LogManager.getLogger(ParallelFSWalker.class.getName());
	public final static int INTERVAL = 1000;
	private FSVisitor visitor = null;
	Path path = null;
	List<FSWalkerListener> listeners = Collections.synchronizedList(new ArrayList<FSWalkerListener>(1));
	ParallelFSVisitorContext ctx = new ParallelFSVisitorContext();

	public ParallelFSWalker(ParallelFSVisitorContext ctx) {
		this();
		this.ctx=ctx;
	}
	
	private ParallelFSWalker() {
		super();
	}
//
//	public ParallelFSWalker(Path path) {
//		super();
//		this.path = path;
//	}
//
//	public ParallelFSWalker(FSVisitor visitor) {
//		super();
//		this.visitor = visitor;
//	}

	@Override
	public void walk(Path path, int depth) {
		this.path=path;
		LOGGER.trace("start parallel walking " + this.path);
		Path dir = path;
		fireEvent(FSWalkerListener.EVT.START);

		try {
			if (visitor == null) {
				visitor = new DefaultFSVisitor();
			}
			if (dir == null)
				return;
			EnumSet<FileVisitOption> walkOptions = EnumSet.noneOf(FileVisitOption.class);
			Files.walkFileTree(dir, walkOptions, depth, visitor);
			ctx.waitForScanToComplete();
			visitor.setCompleted();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			LOGGER.trace("end walking " + this.path);
			fireEvent(FSWalkerListener.EVT.END);
		}

	}

	private void fireEvent(EVT evt) {
		synchronized (this.listeners) {
			ArrayList<FSWalkerListener> removeList = new ArrayList<FSWalkerListener>();
			for (FSWalkerListener l : listeners) {
				boolean b = l.onEvent(evt, this);
				if (b == false) {
					removeList.add(l);
				}
			}
			if (removeList.size() > 0)
				listeners.removeAll(removeList);
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

	@Override
	public FSVisitor getVisitor() {
		return visitor;
	}

	@Override
	public void run() {
		walk(this.path, Integer.MAX_VALUE);
	}

	@Override
	public void addWalkerListener(FSWalkerListener listener) {
		synchronized (this.listeners) {
			if (!this.listeners.contains(listener))
				this.listeners.add(listener);
		}
	}

	@Override
	public void removeWalkerListener(FSWalkerListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

}
