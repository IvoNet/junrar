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

import com.github.junrar.io.Raw;


class RarMemBlock extends Pointer {

    public static final int size = 12;

    private int stamp;
    private int NU;

    private int next;
    private int prev; // Pointer RarMemBlock

    public RarMemBlock(final byte[] mem) {
        super(mem);
    }

    public void insertAt(final RarMemBlock p) {
        final RarMemBlock temp = new RarMemBlock(this.mem);
        setPrev(p.getAddress());
        temp.setAddress(getPrev());
        setNext(temp.getNext());// prev.getNext();
        temp.setNext(this);// prev.setNext(this);
        temp.setAddress(getNext());
        temp.setPrev(this);// next.setPrev(this);
    }

    public void remove() {
        final RarMemBlock temp = new RarMemBlock(this.mem);
        temp.setAddress(getPrev());
        temp.setNext(getNext());// prev.setNext(next);
        temp.setAddress(getNext());
        temp.setPrev(getPrev());// next.setPrev(prev);
//		next = -1;
//		prev = -1;
    }

    public int getNext() {
        if (this.mem != null) {
            this.next = Raw.readIntLittleEndian(this.mem, this.pos + 4);
        }
        return this.next;
    }

    public void setNext(final RarMemBlock next) {
        setNext(next.getAddress());
    }

    void setNext(final int next) {
        this.next = next;
        if (this.mem != null) {
            Raw.writeIntLittleEndian(this.mem, this.pos + 4, next);
        }
    }

    public int getNU() {
        if (this.mem != null) {
            this.NU = Raw.readShortLittleEndian(this.mem, this.pos + 2) & 0xffff;
        }
        return this.NU;
    }

    public void setNU(final int nu) {
        this.NU = nu & 0xffff;
        if (this.mem != null) {
            Raw.writeShortLittleEndian(this.mem, this.pos + 2, (short) nu);
        }
    }

    int getPrev() {
        if (this.mem != null) {
            this.prev = Raw.readIntLittleEndian(this.mem, this.pos + 8);
        }
        return this.prev;
    }

    public void setPrev(final RarMemBlock prev) {
        setPrev(prev.getAddress());
    }

    void setPrev(final int prev) {
        this.prev = prev;
        if (this.mem != null) {
            Raw.writeIntLittleEndian(this.mem, this.pos + 8, prev);
        }
    }

    public int getStamp() {
        if (this.mem != null) {
            this.stamp = Raw.readShortLittleEndian(this.mem, this.pos) & 0xffff;
        }
        return this.stamp;
    }

    public void setStamp(final int stamp) {
        this.stamp = stamp;
        if (this.mem != null) {
            Raw.writeShortLittleEndian(this.mem, this.pos, (short) stamp);
        }
    }
}
