/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 *
 * Copyright (c) 2014 IvoNet.nl. All rights reserved.
 * Refactoring and upgrading of original code: Ivo Woltring
 * Author of all nl.ivonet packaged code: Ivo Woltring
 *
 * The original unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 */

package com.github.junrar.io;

import java.io.IOException;
import java.io.InputStream;


public class ReadOnlyAccessInputStream extends InputStream {

    private final ReadOnlyAccess file;
    private final long endPos;
    private long curPos;

    public ReadOnlyAccessInputStream(final ReadOnlyAccess file, final long startPos, final long endPos)
            throws IOException {
        this.file = file;
        this.curPos = startPos;
        this.endPos = endPos;
        file.setPosition(this.curPos);
    }

    @Override
    public int read() throws IOException {
        if (this.curPos == this.endPos) {
            return -1;
        } else {
            final int b = this.file.read();
            this.curPos++;
            return b;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (this.curPos == this.endPos) {
            return -1;
        }
        final int bytesRead = this.file.read(b, off, (int) Math.min(len, this.endPos - this.curPos));
        this.curPos += bytesRead;
        return bytesRead;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
