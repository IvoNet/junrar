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

public class AudioVariables {
    private final int[] dif = new int[11];
    private int k1;
    private int k2;
    private int k3;
    private int k4;
    private int k5;
    private int d1;
    private int d2;
    private int d3;
    private int d4;
    private int lastDelta;
    private int byteCount;

    private int lastChar;

    public int getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(final int byteCount) {
        this.byteCount = byteCount;
    }

    public int getD1() {
        return this.d1;
    }

    public void setD1(final int d1) {
        this.d1 = d1;
    }

    public int getD2() {
        return this.d2;
    }

    public void setD2(final int d2) {
        this.d2 = d2;
    }

    public int getD3() {
        return this.d3;
    }

    public void setD3(final int d3) {
        this.d3 = d3;
    }

    public int getD4() {
        return this.d4;
    }

    public void setD4(final int d4) {
        this.d4 = d4;
    }

    public int[] getDif() {
        return this.dif;
    }


    public int getK1() {
        return this.k1;
    }

    public void setK1(final int k1) {
        this.k1 = k1;
    }

    public int getK2() {
        return this.k2;
    }

    public void setK2(final int k2) {
        this.k2 = k2;
    }

    public int getK3() {
        return this.k3;
    }

    public void setK3(final int k3) {
        this.k3 = k3;
    }

    public int getK4() {
        return this.k4;
    }

    public void setK4(final int k4) {
        this.k4 = k4;
    }

    public int getK5() {
        return this.k5;
    }

    public void setK5(final int k5) {
        this.k5 = k5;
    }

    public int getLastChar() {
        return this.lastChar;
    }

    public void setLastChar(final int lastChar) {
        this.lastChar = lastChar;
    }

    public int getLastDelta() {
        return this.lastDelta;
    }

    public void setLastDelta(final int lastDelta) {
        this.lastDelta = lastDelta;
    }


}
