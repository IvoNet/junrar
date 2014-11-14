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

package com.github.junrar.unpack.ppm;

import com.github.junrar.io.Raw;


class RarNode extends Pointer {
    public static final int size = 4;
    private int next; //rarnode pointer

    public RarNode(final byte[] mem) {
        super(mem);
    }

    public int getNext() {
        if (this.mem != null) {
            this.next = Raw.readIntLittleEndian(this.mem, this.pos);
        }
        return this.next;
    }

    public void setNext(final int next) {
        this.next = next;
        if (this.mem != null) {
            Raw.writeIntLittleEndian(this.mem, this.pos, next);
        }
    }

    public void setNext(final RarNode next) {
        setNext(next.getAddress());
    }

    public String toString() {
        return "State[" + "\n  pos=" + this.pos + "\n  size=" + size + "\n  next=" + getNext() + "\n]";
    }
}
