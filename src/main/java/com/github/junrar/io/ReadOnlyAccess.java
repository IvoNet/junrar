/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 *
 * Copyright (c) 2014 IvoNet.nl. All rights reserved
 * Refactoring and upgrading of original code: Ivo Woltring
 * Author of all nl.ivonet packaged code: Ivo Woltring
 *
 * The original unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 */

package com.github.junrar.io;

import java.io.IOException;


public interface ReadOnlyAccess {

    /**
     * @return the current position in the file
     */
    long getPosition() throws IOException;

    /**
     * @param pos the position in the file
     */
    void setPosition(long pos) throws IOException;

    /**
     * Read a single byte of data.
     */
    int read() throws IOException;

    /**
     * Read up to <tt>count</tt> bytes to the specified buffer.
     */
    int read(byte[] buffer, int off, int count) throws IOException;

    /**
     * Read exactly <tt>count</tt> bytes to the specified buffer.
     *
     * @param buffer where to store the read data
     * @param count  how many bytes to read
     * @return bytes read || -1 if  IO problem
     */
    int readFully(byte[] buffer, int count) throws IOException;

    /**
     * Close this file.
     */
    void close() throws IOException;
}
