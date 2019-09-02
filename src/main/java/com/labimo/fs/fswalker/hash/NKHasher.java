package com.labimo.fs.fswalker.hash;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.labimo.fs.fswalker.FSEntry;
import com.labimo.fs.fswalker.FSEntryHasher;
import com.labimo.fs.fswalker.FSVisitorOption;
import com.labimo.fs.fswalker.Utils;

import net.openhft.hashing.LongHashFunction;

public class NKHasher implements FSEntryHasher {


	int SAMPLING_CONTENTTRESH = Long.BYTES + 1024 * 20;

	public NKHasher() {
	}

	public NKHasher(int blockCount) {
		super();
		SAMPLING_CONTENTTRESH = Long.BYTES + 1024 * blockCount;
	}

	@Override
	public String hashFSEntry(FSEntry entry, FSVisitorOption option) throws IOException {
		long hash = 0;

		byte[] contentBytes = new byte[SAMPLING_CONTENTTRESH];
		try (InputStream fis = Files.newInputStream(entry.getPath(), StandardOpenOption.READ)) {
			BufferedInputStream bis = new BufferedInputStream(fis);
			byte[] bytes = Utils.longToBytes(entry.getSize());
			System.arraycopy(bytes, 0, contentBytes, 0, bytes.length);
			int len = bis.read(contentBytes, bytes.length, SAMPLING_CONTENTTRESH-Long.BYTES);
			if (len > 0) {
				hash = LongHashFunction.xx().hashBytes(contentBytes);
			}
		}
		return Long.toString(hash);
	}

}
