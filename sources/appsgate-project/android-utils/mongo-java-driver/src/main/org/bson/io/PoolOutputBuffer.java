// PoolOutputBuffer.java

/**
 *      Copyright (C) 2008 10gen Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.bson.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated This class is NOT a part of public API and will be dropped in 3.x
 *             versions.
 */
@Deprecated
public class PoolOutputBuffer extends OutputBuffer {

    static class Position {
	int x; // which buffer -1 == _mine

	int y; // position in buffer

	Position() {
	    reset();
	}

	int getAndInc() {
	    return y++;
	}

	void inc(int amt) {
	    y += amt;
	    if (y > BUF_SIZE) {
		throw new IllegalArgumentException("something is wrong");
	    }
	}

	int len(int which) {
	    if (which < x) {
		return BUF_SIZE;
	    }
	    return y;
	}

	void nextBuffer() {
	    if (y != BUF_SIZE) {
		throw new IllegalArgumentException("broken");
	    }
	    x++;
	    y = 0;
	}

	int pos() {
	    return ((x + 1) * BUF_SIZE) + y;
	}

	void reset() {
	    x = -1;
	    y = 0;
	}

	void reset(int pos) {
	    x = (pos / BUF_SIZE) - 1;
	    y = pos % BUF_SIZE;
	}

	void reset(Position other) {
	    x = other.x;
	    y = other.y;
	}

	@Override
	public String toString() {
	    return x + "," + y;
	}
    }

    public static final int BUF_SIZE = 1024 * 16;

    final byte[] _mine = new byte[BUF_SIZE];

    final char[] _chars = new char[BUF_SIZE];

    final List<byte[]> _fromPool = new ArrayList<byte[]>();

    final UTF8Encoding _encoding = new UTF8Encoding();

    private static final String DEFAULT_ENCODING_1 = "UTF-8";

    private static final String DEFAULT_ENCODING_2 = "UTF8";

    private final Position _cur = new Position();

    private final Position _end = new Position();

    private static org.bson.util.SimplePool<byte[]> _extra = new org.bson.util.SimplePool<byte[]>(
	    (1024 * 1024 * 10) / BUF_SIZE) {

	@Override
	protected byte[] createNew() {
	    return new byte[BUF_SIZE];
	}

    };

    public PoolOutputBuffer() {
	reset();
    }

    void _afterWrite() {

	if (_cur.pos() < _end.pos()) {
	    // we're in the middle of the total space
	    // just need to make sure we're not at the end of a buffer
	    if (_cur.y == BUF_SIZE) {
		_cur.nextBuffer();
	    }
	    return;
	}

	_end.reset(_cur);

	if (_end.y < BUF_SIZE) {
	    return;
	}

	_fromPool.add(_extra.get());
	_end.nextBuffer();
	_cur.reset(_end);
    }

    byte[] _cur() {
	return _get(_cur.x);
    }

    byte[] _get(int z) {
	if (z < 0) {
	    return _mine;
	}
	return _fromPool.get(z);
    }

    public String asAscii() {
	if (_fromPool.size() > 0) {
	    return super.asString();
	}

	final int m = size();
	final char c[] = m < _chars.length ? _chars : new char[m];

	for (int i = 0; i < m; i++) {
	    c[i] = (char) _mine[i];
	}

	return new String(c, 0, m);
    }

    /**
     * @deprecated This method is NOT a part of public API and will be dropped
     *             in 3.x versions.
     */
    @Override
    @Deprecated
    public String asString(String encoding) throws UnsupportedEncodingException {

	if (_fromPool.size() > 0) {
	    return super.asString(encoding);
	}

	if (encoding.equals(DEFAULT_ENCODING_1)
		|| encoding.equals(DEFAULT_ENCODING_2)) {
	    try {
		return _encoding.decode(_mine, 0, size());
	    } catch (IOException ioe) {
		// we failed, fall back
	    }
	}
	return new String(_mine, 0, size(), encoding);
    }

    @Override
    public int getPosition() {
	return _cur.pos();
    }

    @Override
    public int pipe(final OutputStream out) throws IOException {

	if (out == null) {
	    throw new NullPointerException("out is null");
	}

	int total = 0;

	for (int i = -1; i < _fromPool.size(); i++) {
	    final byte[] b = _get(i);
	    final int amt = _end.len(i);
	    out.write(b, 0, amt);
	    total += amt;
	}

	return total;
    }

    public void reset() {
	_cur.reset();
	_end.reset();

	for (int i = 0; i < _fromPool.size(); i++) {
	    _extra.done(_fromPool.get(i));
	}
	_fromPool.clear();
    }

    /**
     * @deprecated This method is NOT a part of public API and will be dropped
     *             in 3.x versions.
     */
    @Override
    @Deprecated
    public void seekEnd() {
	_cur.reset(_end);
    }

    /**
     * @deprecated This method is NOT a part of public API and will be dropped
     *             in 3.x versions.
     */
    @Override
    @Deprecated
    public void seekStart() {
	_cur.reset();
    }

    /**
     * @deprecated This method is NOT a part of public API and will be dropped
     *             in 3.x versions.
     */
    @Override
    @Deprecated
    public void setPosition(int position) {
	_cur.reset(position);
    }

    @Override
    public int size() {
	return _end.pos();
    }

    @Override
    public void write(byte[] b) {
	write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
	while (len > 0) {
	    byte[] bs = _cur();
	    int space = Math.min(bs.length - _cur.y, len);
	    System.arraycopy(b, off, bs, _cur.y, space);
	    _cur.inc(space);
	    len -= space;
	    off += space;
	    _afterWrite();
	}
    }

    @Override
    public void write(int b) {
	byte[] bs = _cur();
	bs[_cur.getAndInc()] = (byte) (b & 0xFF);
	_afterWrite();
    }
}
