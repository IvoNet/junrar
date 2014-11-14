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

package com.github.junrar.unpack.ppm;

/**
 * Simulates Pointers on a single mem block as a byte[]
 */
abstract class Pointer {
    byte[] mem;
    int pos;

    /**
     * Initialize the object with the array (may be null)
     *
     * @param mem the byte array
     */
    Pointer(final byte[] mem) {
        this.mem = mem;
    }

    /**
     * returns the position of this object in the byte[]
     *
     * @return the address of this object
     */
    public int getAddress() {
        assert (this.mem != null);
        return this.pos;
    }

    /**
     * needs to set the fields of this object to the values in the byte[] at the given position. be aware of the byte
     * order
     *
     * @param pos the position this object should point to
     */
    public void setAddress(final int pos) {
        assert (this.mem != null);
        assert (pos >= 0) && (pos < this.mem.length) : pos;
        this.pos = pos;
    }
}
