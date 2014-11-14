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

package com.github.junrar.unpack.vm;


public class VMPreparedOperand {
    private VMOpType Type;
    private int Data;
    private int Base;
    private int offset;


    public int getBase() {
        return this.Base;
    }

    public void setBase(final int base) {
        this.Base = base;
    }

    public int getData() {
        return this.Data;
    }

    public void setData(final int data) {
        this.Data = data;
    }

    public VMOpType getType() {
        return this.Type;
    }

    public void setType(final VMOpType type) {
        this.Type = type;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

}
