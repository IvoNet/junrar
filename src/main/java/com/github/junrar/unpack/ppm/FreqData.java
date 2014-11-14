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

public class FreqData extends Pointer {

    public static final int size = 6;


    public FreqData(final byte[] mem) {
        super(mem);
    }

    public void init(final byte[] mem) {
        this.mem = mem;
        this.pos = 0;
    }

    public int getSummFreq() {
        return Raw.readShortLittleEndian(this.mem, this.pos) & 0xffff;
    }

    public void setSummFreq(final int summFreq) {
        Raw.writeShortLittleEndian(this.mem, this.pos, (short) summFreq);
    }

    public void incSummFreq(final int dSummFreq) {
        Raw.incShortLittleEndian(this.mem, this.pos, dSummFreq);
    }

    public int getStats() {
        return Raw.readIntLittleEndian(this.mem, this.pos + 2);
    }

    public void setStats(final int state) {
        Raw.writeIntLittleEndian(this.mem, this.pos + 2, state);
    }

    public void setStats(final State state) {
        setStats(state.getAddress());
    }

    public String toString() {
        return "FreqData[" + "\n  pos=" + this.pos + "\n  size=" + size + "\n  summFreq=" + getSummFreq() + "\n  stats="
               + getStats() + "\n]";
    }

}
