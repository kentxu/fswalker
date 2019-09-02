package com.labimo.fs.fswalker.hash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.labimo.fs.fswalker.FSEntry;
import com.labimo.fs.fswalker.FSEntryHasher;
import com.labimo.fs.fswalker.FSVisitorOption;
import com.labimo.fs.fswalker.Utils;

import net.openhft.hashing.LongHashFunction;

public class NKEHasher implements FSEntryHasher {

	int SAMPLING_CONTENTTRESH = Long.BYTES + 1024 * 20;

	public NKEHasher() {
	}

	public NKEHasher(int blockCount) {
		super();
		SAMPLING_CONTENTTRESH = Long.BYTES + 1024 * blockCount;
	}

	@Override
	public String hashFSEntry(FSEntry entry, FSVisitorOption option) throws IOException {
		long hash = 0;

		long contentSize = entry.getSize();
		int prefSize = (SAMPLING_CONTENTTRESH - Long.BYTES) / 2;

		try (SeekableByteChannel ch = Files.newByteChannel(entry.getPath(), StandardOpenOption.READ)) {
			ByteBuffer buff = ByteBuffer.allocate(SAMPLING_CONTENTTRESH);
			byte[] bytes = Utils.longToBytes(entry.getSize());
			buff.put(bytes);
			if (contentSize <= (SAMPLING_CONTENTTRESH - Long.BYTES)) {
				ch.read(buff);
			} else {
				ByteBuffer headerBuff = ByteBuffer.allocate(prefSize);
				ch.read(headerBuff);
				buff.put(headerBuff);

				ch.position(contentSize - prefSize);
				ch.read(buff);

			}
			hash = LongHashFunction.xx().hashBytes(buff);
		}

		return Long.toString(hash);
	}

}
