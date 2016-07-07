package io.pivotal.mt103;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

class CountingReader extends Reader {

    private final PushbackReader in;
    private int count;

    public CountingReader(PushbackReader in) {
        this.in = in;
    }

    public CountingReader(Reader in) {
        this(new PushbackReader(in));
    }

    public int getCount() {
        return count;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = in.read(cbuf, off, len);
        count += read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skip = in.skip(n);
        count += skip;
        return skip;
    }

    public void unread(int c) throws IOException {
        in.unread(c);
        --count;
    }

    public int peek() throws IOException {
        int c = read();
        if (c != -1) unread(c);
        return c;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
