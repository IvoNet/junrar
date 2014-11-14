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


public class SEE2Context {
    private static final int size = 4;

    // ushort Summ;
    private int summ;

    // byte Shift;
    private int shift;

    // byte Count;
    private int count;

    public void init(final int initVal) {
        this.shift = (ModelPPM.PERIOD_BITS - 4) & 0xff;
        this.summ = (initVal << this.shift) & 0xffff;
        this.count = 4;
    }

    public int getMean() {
        final int retVal = this.summ >>> this.shift;
        this.summ -= retVal;
        return retVal + ((retVal == 0) ? 1 : 0);
    }

    public void update() {
        if ((this.shift < ModelPPM.PERIOD_BITS) && (--this.count == 0)) {
            this.summ += this.summ;
            this.count = (3 << this.shift++);
        }
        this.summ &= 0xffff;
        this.count &= 0xff;
        this.shift &= 0xff;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(final int count) {
        this.count = count & 0xff;
    }

    public int getShift() {
        return this.shift;
    }

    public void setShift(final int shift) {
        this.shift = shift & 0xff;
    }

    int getSumm() {
        return this.summ;
    }

    void setSumm(final int summ) {
        this.summ = summ & 0xffff;
    }

    public void incSumm(final int dSumm) {
        setSumm(getSumm() + dSumm);
    }

    public String toString() {
        return "SEE2Context[" + "\n  size=" + size + "\n  summ=" + this.summ + "\n  shift=" + this.shift + "\n  count="
               + this.count + "\n]";
    }
}
