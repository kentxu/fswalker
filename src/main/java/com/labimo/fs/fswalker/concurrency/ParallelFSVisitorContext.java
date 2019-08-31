package com.labimo.fs.fswalker.concurrency;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.labimo.fs.fswalker.FSVisitor;
import com.labimo.fs.fswalker.FSWalker;
import com.labimo.fs.fswalker.FSWalkerFactory;
import com.labimo.fs.fswalker.FSWalkerListener;
import com.labimo.fs.fswalker.FSWriter;
import com.labimo.fs.fswalker.FSWalkerListener.EVT;

public class ParallelFSVisitorContext  implements FSWalkerListener {
	private int MAX_WALKER=4;
	private int MAX_WALKER_QUEUE=2;
	ThreadPoolExecutor walkerPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_WALKER);
	private FSWriter writer;
	
	
	public ParallelFSVisitorContext() {
	}
	
	
	public ParallelFSVisitorContext(FSWriter writer) {
		this.writer=writer;
	}
	
	
	public ParallelFSVisitorContext(int workermax, int queuemax, FSWriter writer) {
		this.writer=writer;
		MAX_WALKER=workermax;
		MAX_WALKER_QUEUE=queuemax;
	}
	
	
	/*
	 * determine if the pending execution queue is filled up to the threshold.
	 * it is only serving as a load gauge. The queue size is not constraint and returning true does not prevent new task being added to the queue.
	 */
	public boolean isTaskQueueFull() {
		return walkerPoolExecutor.getQueue().size()>=MAX_WALKER_QUEUE;
	}

	private void addTask(Runnable cmd) {
		walkerPoolExecutor.execute(cmd);
	}
	
	public FSWalker createFSWalker(Path path,BasicFileAttributes attrs,FSVisitor visitor ) {
		FSWalker walker = FSWalkerFactory.getInstance().getFSWalker(path, attrs, visitor);
		walker.addWalkerListener(this);
		this.addTask(walker);
		
		return walker;
	}
	

	

	@Override
	public boolean onEvent(EVT evt, FSWalker walker) {
		if (evt.equals(EVT.END)) {
//			walker.getVisitor().setCompleted();
		}
		return true;
	}
	
	public void waitForScanToComplete() throws InterruptedException {
		
		while ( walkerPoolExecutor.getActiveCount()!=0 || !walkerPoolExecutor.getQueue().isEmpty() ) {
			Thread.sleep(100);
		}
		walkerPoolExecutor.shutdown();
		walkerPoolExecutor.awaitTermination(20, TimeUnit.SECONDS);
		
	}
	
	
}
