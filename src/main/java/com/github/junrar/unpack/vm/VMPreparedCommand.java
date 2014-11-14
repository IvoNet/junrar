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


class VMPreparedCommand {
    private final VMPreparedOperand Op1 = new VMPreparedOperand();
    private final VMPreparedOperand Op2 = new VMPreparedOperand();
    private VMCommands OpCode;
    private boolean ByteMode;

    public boolean isByteMode() {
        return this.ByteMode;
    }

    public void setByteMode(final boolean byteMode) {
        this.ByteMode = byteMode;
    }

    public VMPreparedOperand getOp1() {
        return this.Op1;
    }


    public VMPreparedOperand getOp2() {
        return this.Op2;
    }


    public VMCommands getOpCode() {
        return this.OpCode;
    }

    public void setOpCode(final VMCommands opCode) {
        this.OpCode = opCode;
    }

}
