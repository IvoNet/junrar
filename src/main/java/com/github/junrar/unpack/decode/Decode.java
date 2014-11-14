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

package com.github.junrar.unpack.decode;

/**
 * Used to store information for lz decoding
 */
public class Decode {
    private final int[] decodeLen = new int[16];
    private final int[] decodePos = new int[16];
    int[] decodeNum = new int[2];
    private int maxNum;

    /**
     * returns the decode Length array
     *
     * @return decodeLength
     */
    public int[] getDecodeLen() {
        return this.decodeLen;
    }

    /**
     * returns the decode num array
     *
     * @return decodeNum
     */
    public int[] getDecodeNum() {
        return this.decodeNum;
    }

    /**
     * returns the decodePos array
     *
     * @return decodePos
     */
    public int[] getDecodePos() {
        return this.decodePos;
    }

    /**
     * returns the max num
     *
     * @return maxNum
     */
    public int getMaxNum() {
        return this.maxNum;
    }

    /**
     * sets the max num
     *
     * @param maxNum to be set to maxNum
     */
    public void setMaxNum(final int maxNum) {
        this.maxNum = maxNum;
    }

}
