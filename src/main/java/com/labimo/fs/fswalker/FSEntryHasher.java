package com.labimo.fs.fswalker;

public interface FSEntryHasher {

	public String hashFSEntry(FSEntry entry,FSVisitorOption option) throws Exception;
}
