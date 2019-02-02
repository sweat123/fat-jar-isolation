package com.laomei.fatjar.classloader.boot.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * {@link RandomAccessData} implementation backed by a byte array.
 *
 * @author Phillip Webb
 */
public class ByteArrayRandomAccessData implements RandomAccessData {

	private final byte[] bytes;

	private final long offset;

	private final long length;

	public ByteArrayRandomAccessData(byte[] bytes) {
		this(bytes, 0, (bytes != null ? bytes.length : 0));
	}

	public ByteArrayRandomAccessData(byte[] bytes, long offset, long length) {
		this.bytes = (bytes != null ? bytes : new byte[0]);
		this.offset = offset;
		this.length = length;
	}

	@Override
	public InputStream getInputStream(ResourceAccess access) {
		return new ByteArrayInputStream(this.bytes, (int) this.offset, (int) this.length);
	}

	@Override
	public RandomAccessData getSubsection(long offset, long length) {
		return new ByteArrayRandomAccessData(this.bytes, this.offset + offset, length);
	}

	@Override
	public long getSize() {
		return this.length;
	}

}
