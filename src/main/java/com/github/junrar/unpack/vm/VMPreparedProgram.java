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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class VMPreparedProgram {
    private List<VMPreparedCommand> Cmd = new ArrayList<>();
    private List<VMPreparedCommand> AltCmd = new ArrayList<>();
    private int CmdCount;


    private Vector<Byte> GlobalData = new Vector<>();
    private Vector<Byte> StaticData = new Vector<>(); // static data contained in DB operators
    private int[] InitR = new int[7];

    private int FilteredDataOffset;
    private int FilteredDataSize;

    public VMPreparedProgram() {
        this.AltCmd = null;
    }


    public List<VMPreparedCommand> getAltCmd() {
        return this.AltCmd;
    }


    public void setAltCmd(final List<VMPreparedCommand> altCmd) {
        this.AltCmd = altCmd;
    }


    public List<VMPreparedCommand> getCmd() {
        return this.Cmd;
    }

    public void setCmd(final List<VMPreparedCommand> cmd) {
        this.Cmd = cmd;
    }

    public int getCmdCount() {
        return this.CmdCount;
    }

    public void setCmdCount(final int cmdCount) {
        this.CmdCount = cmdCount;
    }


    public int getFilteredDataOffset() {
        return this.FilteredDataOffset;
    }


    public void setFilteredDataOffset(final int filteredDataOffset) {
        this.FilteredDataOffset = filteredDataOffset;
    }


    public int getFilteredDataSize() {
        return this.FilteredDataSize;
    }

    public void setFilteredDataSize(final int filteredDataSize) {
        this.FilteredDataSize = filteredDataSize;
    }

    public Vector<Byte> getGlobalData() {
        return this.GlobalData;
    }

    public void setGlobalData(final Vector<Byte> globalData) {
        this.GlobalData = globalData;
    }

    public int[] getInitR() {
        return this.InitR;
    }

    public void setInitR(final int[] initR) {
        this.InitR = initR;
    }

    public Vector<Byte> getStaticData() {
        return this.StaticData;
    }

    public void setStaticData(final Vector<Byte> staticData) {
        this.StaticData = staticData;
    }


}
