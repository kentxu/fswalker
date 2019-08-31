package com.labimo.fs.fswalker;

public interface FSWalkerListener {

	enum EVT {
		START,END
	}
	
	/**
	 * return true to be removed from the listener list
	 * @param evt
	 * @param walker
	 * @return
	 */
	public boolean onEvent(EVT evt, FSWalker walker);
	
}
