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

package com.github.junrar.unpack.vm;


public class BitInput {
    /**
     * the max size of the input
     */
    protected static final int MAX_SIZE = 0x8000;
    protected final byte[] inBuf;
    protected int inAddr;
    protected int inBit;

    public BitInput() {
        this.inBuf = new byte[MAX_SIZE];
    }

    public void InitBitInput() {
        this.inAddr = 0;
        this.inBit = 0;
    }

    public void addbits(int Bits) {  //FIXME don't assign to the parameter
        Bits += this.inBit;
        this.inAddr += Bits >> 3;
        this.inBit = Bits & 7;
    }

    public int getbits() {
        return (((((this.inBuf[this.inAddr] & 0xff) << 16) +
                  ((this.inBuf[this.inAddr + 1] & 0xff) << 8) +
                  ((this.inBuf[this.inAddr + 2] & 0xff))) >>> (8 - this.inBit)) & 0xffff);
    }

    /**
     * @param Bits add the bits
     */
    public void faddbits(final int Bits) {
        addbits(Bits);
    }


    /**
     * @return get the bits
     */
    public int fgetbits() {
        return (getbits());
    }

    /**
     * Indicates an Overfow
     *
     * @param IncPtr how many bytes to inc
     * @return true if an Oververflow would occur
     */
    public boolean Overflow(final int IncPtr) {
        return ((this.inAddr + IncPtr) >= MAX_SIZE);
    }

    public byte[] getInBuf() {
        return this.inBuf;
    }


}
