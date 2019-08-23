package com.labimo.fs.fswalker;

import java.nio.ByteBuffer;

public class Utils {

	public static byte[] longToBytes2(long l) {
		byte[] result = new byte[Long.BYTES];
		for (int i = Long.BYTES - 1; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	public static byte[] longToBytes(long x) {
		ByteBuffer longToBytesBuff = ByteBuffer.allocate(Long.BYTES);
		longToBytesBuff.putLong(x);
		return longToBytesBuff.array();
	}
}
