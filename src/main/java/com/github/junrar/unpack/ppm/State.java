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


public class State extends Pointer {

    public static final int size = 6;

    public State(final byte[] mem) {
        super(mem);
    }

    public State init(final byte[] mem) {
        this.mem = mem;
        this.pos = 0;
        return this;
    }

    public int getSymbol() {
        return this.mem[this.pos] & 0xff;
    }

    public void setSymbol(final int symbol) {
        this.mem[this.pos] = (byte) symbol;
    }

    public int getFreq() {
        return this.mem[this.pos + 1] & 0xff;
    }

    public void setFreq(final int freq) {
        this.mem[this.pos + 1] = (byte) freq;
    }

    public void incFreq(final int dFreq) {
        this.mem[this.pos + 1] += dFreq;
    }

    public int getSuccessor() {
        return Raw.readIntLittleEndian(this.mem, this.pos + 2);
    }

    public void setSuccessor(final PPMContext successor) {
        setSuccessor(successor.getAddress());
    }

    public void setSuccessor(final int successor) {
        Raw.writeIntLittleEndian(this.mem, this.pos + 2, successor);
    }

    public void setValues(final StateRef state) {
        setSymbol(state.getSymbol());
        setFreq(state.getFreq());
        setSuccessor(state.getSuccessor());
    }

    public void setValues(final State ptr) {
        System.arraycopy(ptr.mem, ptr.pos, this.mem, this.pos, size);
    }

    public State decAddress() {
        setAddress(this.pos - size);
        return this;
    }

    public State incAddress() {
        setAddress(this.pos + size);
        return this;
    }

    public String toString() {
        return "State[" + "\n  pos=" + this.pos + "\n  size=" + size + "\n  symbol=" + getSymbol() + "\n  freq="
               + getFreq() + "\n  successor=" + getSuccessor() + "\n]";
    }

    public static void ppmdSwap(final State ptr1, final State ptr2) {
        final byte[] mem1 = ptr1.mem;
        final byte[] mem2 = ptr2.mem;
        for (int i = 0, pos1 = ptr1.pos, pos2 = ptr2.pos; i < size; i++, pos1++, pos2++) {
            final byte temp = mem1[pos1];
            mem1[pos1] = mem2[pos2];
            mem2[pos2] = temp;
        }
    }
}
