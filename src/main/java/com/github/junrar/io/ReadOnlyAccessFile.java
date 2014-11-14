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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


public class ReadOnlyAccessFile extends RandomAccessFile implements ReadOnlyAccess {

    /**
     * @param file the file
     */
    public ReadOnlyAccessFile(final File file) throws FileNotFoundException {
        super(file, "r");
    }

    @Override
    public int readFully(final byte[] buffer, final int count) throws IOException {
        assert (count > 0) : count;
        this.readFully(buffer, 0, count);
        return count;
    }

    @Override
    public long getPosition() throws IOException {
        return this.getFilePointer();
    }

    @Override
    public void setPosition(final long pos) throws IOException {
        this.seek(pos);
    }
}
