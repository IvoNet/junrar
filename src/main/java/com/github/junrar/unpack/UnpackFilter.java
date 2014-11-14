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

package com.github.junrar.unpack;

import com.github.junrar.unpack.vm.VMPreparedProgram;


class UnpackFilter {

    private int BlockStart;

    private int BlockLength;

    private int ExecCount;

    private boolean NextWindow;

    // position of parent filter in Filters array used as prototype for filter
    // in PrgStack array. Not defined for filters in Filters array.
    private int ParentFilter;

    private VMPreparedProgram Prg = new VMPreparedProgram();

    public int getBlockLength() {
        return this.BlockLength;
    }

    public void setBlockLength(final int blockLength) {
        this.BlockLength = blockLength;
    }

    public int getBlockStart() {
        return this.BlockStart;
    }

    public void setBlockStart(final int blockStart) {
        this.BlockStart = blockStart;
    }

    public int getExecCount() {
        return this.ExecCount;
    }

    public void setExecCount(final int execCount) {
        this.ExecCount = execCount;
    }

    public boolean isNextWindow() {
        return this.NextWindow;
    }

    public void setNextWindow(final boolean nextWindow) {
        this.NextWindow = nextWindow;
    }

    public int getParentFilter() {
        return this.ParentFilter;
    }

    public void setParentFilter(final int parentFilter) {
        this.ParentFilter = parentFilter;
    }

    public VMPreparedProgram getPrg() {
        return this.Prg;
    }

    public void setPrg(final VMPreparedProgram prg) {
        this.Prg = prg;
    }


}
