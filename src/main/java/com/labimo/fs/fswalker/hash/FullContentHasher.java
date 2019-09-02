package com.labimo.fs.fswalker.hash;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.codec.digest.DigestUtils;

import com.labimo.fs.fswalker.FSEntry;
import com.labimo.fs.fswalker.FSEntryHasher;
import com.labimo.fs.fswalker.FSVisitorOption;

public class FullContentHasher implements FSEntryHasher {


	public FullContentHasher() {
	}


	@Override
	public String hashFSEntry(FSEntry entry, FSVisitorOption option) throws IOException {
		
		try (InputStream fis = Files.newInputStream(entry.getPath(), StandardOpenOption.READ)) {
			BufferedInputStream bis = new BufferedInputStream(fis);
			String sha256hex = DigestUtils.sha256Hex(bis);
			return sha256hex;
		}
	}

}
