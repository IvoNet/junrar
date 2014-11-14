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


class StateRef {

    private int symbol;

    private int freq;

    private int successor; // pointer ppmcontext

    public int getSymbol() {
        return this.symbol;
    }

    public void setSymbol(final int symbol) {
        this.symbol = symbol & 0xff;
    }

    public int getFreq() {
        return this.freq;
    }

    public void setFreq(final int freq) {
        this.freq = freq & 0xff;
    }

    public void incFreq(final int dFreq) {
        this.freq = (this.freq + dFreq) & 0xff;
    }

    public void decFreq(final int dFreq) {
        this.freq = (this.freq - dFreq) & 0xff;
    }

    public void setValues(final State statePtr) {
        setFreq(statePtr.getFreq());
        setSuccessor(statePtr.getSuccessor());
        setSymbol(statePtr.getSymbol());
    }

    public int getSuccessor() {
        return this.successor;
    }

    public void setSuccessor(final PPMContext successor) {
        setSuccessor(successor.getAddress());
    }

    public void setSuccessor(final int successor) {
        this.successor = successor;
    }

    public String toString() {
        return "State[" + "\n  symbol=" + getSymbol() + "\n  freq=" + getFreq() + "\n  successor=" + getSuccessor()
               + "\n]";
    }
}
