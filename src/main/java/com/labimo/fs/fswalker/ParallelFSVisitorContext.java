package com.labimo.fs.fswalker;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ParallelFSVisitorContext {
	private int MAX_WALKER=4;
	private int MAX_WALKER_QUEUE=2;
	ThreadPoolExecutor walkerPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_WALKER);
	
	public boolean canAddWalker() {
		return walkerPoolExecutor.getQueue().size()<MAX_WALKER_QUEUE;
	}

}
