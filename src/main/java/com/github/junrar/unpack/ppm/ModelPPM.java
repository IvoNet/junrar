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

import com.github.junrar.exception.RarException;
import com.github.junrar.unpack.Unpack;

import java.io.IOException;
import java.util.Arrays;


@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
public class ModelPPM {
    public static final int PERIOD_BITS = 7;
    public static final int MAX_FREQ = 124;
    public static final int INT_BITS = 7;
    public static final int TOT_BITS = INT_BITS + PERIOD_BITS;
    public static final int BIN_SCALE = 1 << TOT_BITS;
    public static final int INTERVAL = 1 << INT_BITS;
    private static final int MAX_O = 64; /* maximum allowed model order */
    private static final int[] InitBinEsc = {
            0x3CDD, 0x1F3F, 0x59BF, 0x48F3, 0x64A1, 0x5ABC, 0x6632, 0x6051
    };
    private final SEE2Context[][] SEE2Cont = new SEE2Context[25][16];
    private final int[] charMask = new int[256];
    private final int[] NS2Indx = new int[256];
    private final int[] NS2BSIndx = new int[256];
    private final int[] HB2Flag = new int[256];
    private final int[][] binSumm = new int[128][64]; // binary SEE-contexts
    private final RangeCoder coder = new RangeCoder();
    private final SubAllocator subAlloc = new SubAllocator();
    // Temp fields
    private final State tempState1 = new State(null);
    private final State tempState2 = new State(null);
    private final State tempState3 = new State(null);
    private final State tempState4 = new State(null);
    private final StateRef tempStateRef1 = new StateRef();
    private final StateRef tempStateRef2 = new StateRef();
    private final PPMContext tempPPMContext1 = new PPMContext(null);
    private final PPMContext tempPPMContext2 = new PPMContext(null);
    private final PPMContext tempPPMContext3 = new PPMContext(null);
    private final PPMContext tempPPMContext4 = new PPMContext(null);
    private final int[] ps = new int[MAX_O];
    private SEE2Context dummySEE2Cont;
    private PPMContext minContext;
    private PPMContext maxContext;
    private State foundState; // found next state transition
    private int numMasked;
    private int initEsc;
    private int orderFall;
    private int maxOrder;
    private int runLength;
    private int initRL;
    // byte EscCount, PrevSuccess, HiBitsFlag;
    private int escCount;
    private int prevSuccess;
    private int hiBitsFlag;

    public ModelPPM() {
        this.minContext = null;
        this.maxContext = null;
    }

    public SubAllocator getSubAlloc() {
        return this.subAlloc;
    }

    private void restartModelRare() {
        Arrays.fill(this.charMask, 0);
        this.subAlloc.initSubAllocator();
        this.initRL = -((this.maxOrder < 12) ? this.maxOrder : 12) - 1;
        int addr = this.subAlloc.allocContext();
        this.minContext.setAddress(addr);
        this.maxContext.setAddress(addr);
        this.minContext.setSuffix(0);
        this.orderFall = this.maxOrder;
        this.minContext.setNumStats(256);
        this.minContext.getFreqData()
                       .setSummFreq(this.minContext.getNumStats() + 1);

        addr = this.subAlloc.allocUnits(256 / 2);
        this.foundState.setAddress(addr);
        this.minContext.getFreqData()
                       .setStats(addr);

        final State state = new State(this.subAlloc.getHeap());
        addr = this.minContext.getFreqData()
                              .getStats();
        this.runLength = this.initRL;
        this.prevSuccess = 0;
        for (int i = 0; i < 256; i++) {
            state.setAddress(addr + (i * State.size));
            state.setSymbol(i);
            state.setFreq(1);
            state.setSuccessor(0);
        }

        for (int i = 0; i < 128; i++) {
            for (int k = 0; k < 8; k++) {
                for (int m = 0; m < 64; m += 8) {
                    this.binSumm[i][k + m] = BIN_SCALE - (InitBinEsc[k] / (i + 2));
                }
            }
        }
        for (int i = 0; i < 25; i++) {
            for (int k = 0; k < 16; k++) {
                this.SEE2Cont[i][k].init((5 * i) + 10);
            }
        }
    }

    private void startModelRare(final int MaxOrder) {
        int i;
        int k;
        int m;
        int Step;
        this.escCount = 1;
        this.maxOrder = MaxOrder;
        restartModelRare();
        // Bug Fixed
        this.NS2BSIndx[0] = 0;
        this.NS2BSIndx[1] = 2;
        for (int j = 0; j < 9; j++) {
            this.NS2BSIndx[2 + j] = 4;
        }
        for (int j = 0; j < (256 - 11); j++) {
            this.NS2BSIndx[11 + j] = 6;
        }
        for (i = 0; i < 3; i++) {
            this.NS2Indx[i] = i;
        }
        for (m = i, k = 1, Step = 1; i < 256; i++) {
            this.NS2Indx[i] = m;
            if ((--k) == 0) {
                k = ++Step;
                m++;
            }
        }
        for (int j = 0; j < 0x40; j++) {
            this.HB2Flag[j] = 0;
        }
        for (int j = 0; j < (0x100 - 0x40); j++) {
            this.HB2Flag[0x40 + j] = 0x08;
        }
        this.dummySEE2Cont.setShift(PERIOD_BITS);

    }

    private void clearMask() {
        this.escCount = 1;
        Arrays.fill(this.charMask, 0);
    }

    public boolean decodeInit(final Unpack unpackRead, int escChar /* ref */) throws IOException, RarException {

        int MaxOrder = unpackRead.getChar() & 0xff;
        final boolean reset = ((MaxOrder & 0x20) != 0);

        int MaxMB = 0;
        if (reset) {
            MaxMB = unpackRead.getChar();
        } else {
            if (this.subAlloc.GetAllocatedMemory() == 0) {
                return (false);
            }
        }
        if ((MaxOrder & 0x40) != 0) {
            escChar = unpackRead.getChar();
            unpackRead.setPpmEscChar(escChar);
        }
        this.coder.initDecoder(unpackRead);
        if (reset) {
            MaxOrder = (MaxOrder & 0x1f) + 1;
            if (MaxOrder > 16) {
                MaxOrder = 16 + ((MaxOrder - 16) * 3);
            }
            if (MaxOrder == 1) {
                this.subAlloc.stopSubAllocator();
                return (false);
            }
            this.subAlloc.startSubAllocator(MaxMB + 1);
            this.minContext = new PPMContext(getHeap());
            this.maxContext = new PPMContext(getHeap());
            this.foundState = new State(getHeap());
            this.dummySEE2Cont = new SEE2Context();
            for (int i = 0; i < 25; i++) {
                for (int j = 0; j < 16; j++) {
                    this.SEE2Cont[i][j] = new SEE2Context();
                }
            }
            startModelRare(MaxOrder);
        }
        return (this.minContext.getAddress() != 0);
    }

    public int decodeChar() throws IOException, RarException {
        if ((this.minContext.getAddress() <= this.subAlloc.getPText()) || (this.minContext.getAddress()
                                                                           > this.subAlloc.getHeapEnd())) {
            return (-1);
        }

        if (this.minContext.getNumStats() == 1) {
            this.minContext.decodeBinSymbol(this);
        } else {
            if ((this.minContext.getFreqData()
                                .getStats() <= this.subAlloc.getPText()) || (this.minContext.getFreqData()
                                                                                            .getStats()
                                                                             > this.subAlloc.getHeapEnd())) {
                return (-1);
            }
            if (!this.minContext.decodeSymbol1(this)) {
                return (-1);
            }
        }
        this.coder.decode();
        while (this.foundState.getAddress() == 0) {
            this.coder.ariDecNormalize();
            do {
                this.orderFall++;
                this.minContext.setAddress(this.minContext.getSuffix());// =MinContext->Suffix;
                if ((this.minContext.getAddress() <= this.subAlloc.getPText()) || (this.minContext.getAddress()
                                                                                   > this.subAlloc.getHeapEnd())) {
                    return (-1);
                }
            } while (this.minContext.getNumStats() == this.numMasked);
            if (!this.minContext.decodeSymbol2(this)) {
                return (-1);
            }
            this.coder.decode();
        }
        final int Symbol = this.foundState.getSymbol();
        if ((this.orderFall == 0) && (this.foundState.getSuccessor() > this.subAlloc.getPText())) {
            final int addr = this.foundState.getSuccessor();
            this.minContext.setAddress(addr);
            this.maxContext.setAddress(addr);
        } else {
            updateModel();
            if (this.escCount == 0) {
                clearMask();
            }
        }
        this.coder.ariDecNormalize();// ARI_DEC_NORMALIZE(Coder.code,Coder.low,Coder.range,Coder.UnpackRead);
        return (Symbol);
    }

    public SEE2Context[][] getSEE2Cont() {
        return this.SEE2Cont;
    }

    public SEE2Context getDummySEE2Cont() {
        return this.dummySEE2Cont;
    }

    public int getInitRL() {
        return this.initRL;
    }

    public int getEscCount() {
        return this.escCount;
    }

    void setEscCount(final int escCount) {
        this.escCount = escCount & 0xff;
    }

    public void incEscCount(final int dEscCount) {
        setEscCount(getEscCount() + dEscCount);
    }

    public int[] getCharMask() {
        return this.charMask;
    }

    public int getNumMasked() {
        return this.numMasked;
    }

    public void setNumMasked(final int numMasked) {
        this.numMasked = numMasked;
    }

    public int getInitEsc() {
        return this.initEsc;
    }

    public void setInitEsc(final int initEsc) {
        this.initEsc = initEsc;
    }

    public int getRunLength() {
        return this.runLength;
    }

    public void setRunLength(final int runLength) {
        this.runLength = runLength;
    }

    public void incRunLength(final int dRunLength) {
        setRunLength(getRunLength() + dRunLength);
    }

    public int getPrevSuccess() {
        return this.prevSuccess;
    }

    public void setPrevSuccess(final int prevSuccess) {
        this.prevSuccess = prevSuccess & 0xff;
    }

    public int getHiBitsFlag() {
        return this.hiBitsFlag;
    }

    public void setHiBitsFlag(final int hiBitsFlag) {
        this.hiBitsFlag = hiBitsFlag & 0xff;
    }

    public int[][] getBinSumm() {
        return this.binSumm;
    }

    public RangeCoder getCoder() {
        return this.coder;
    }

    public int[] getHB2Flag() {
        return this.HB2Flag;
    }

    public int[] getNS2BSIndx() {
        return this.NS2BSIndx;
    }

    public int[] getNS2Indx() {
        return this.NS2Indx;
    }

    public State getFoundState() {
        return this.foundState;
    }

    public byte[] getHeap() {
        return this.subAlloc.getHeap();
    }

    public int getOrderFall() {
        return this.orderFall;
    }

    private int /* ppmcontext ptr */createSuccessors(final boolean Skip, final State p1 /* state ptr */) {
        //State upState = tempState1.init(null);
        final StateRef upState = this.tempStateRef2;
        final State tempState = this.tempState1.init(getHeap());

        // PPM_CONTEXT* pc=MinContext, * UpBranch=FoundState->Successor;
        final PPMContext pc = this.tempPPMContext1.init(getHeap());
        pc.setAddress(this.minContext.getAddress());
        final PPMContext upBranch = this.tempPPMContext2.init(getHeap());
        upBranch.setAddress(this.foundState.getSuccessor());

        // STATE * p, * ps[MAX_O], ** pps=ps;
        final State p = this.tempState2.init(getHeap());
        int pps = 0;

        boolean noLoop = false;

        if (!Skip) {
            this.ps[pps++] = this.foundState.getAddress();// *pps++ = FoundState;
            if (pc.getSuffix() == 0) {
                noLoop = true;
            }
        }
        if (!noLoop) {
            boolean loopEntry = false;
            if (p1.getAddress() != 0) {
                p.setAddress(p1.getAddress());
                pc.setAddress(pc.getSuffix());// =pc->Suffix;
                loopEntry = true;
            }
            do {
                if (!loopEntry) {
                    pc.setAddress(pc.getSuffix());// pc=pc->Suffix;
                    if (pc.getNumStats() == 1) {
                        p.setAddress(pc.getOneState()
                                       .getAddress());// p=&(pc->OneState);
                    } else {
                        p.setAddress(pc.getFreqData()
                                       .getStats());// p=pc->U.Stats
                        if (p.getSymbol() != this.foundState.getSymbol()) {
                            do {
                                p.incAddress();
                            } while (p.getSymbol() != this.foundState.getSymbol());
                        }
                    }
                }// LOOP_ENTRY:
                loopEntry = false;
                if (p.getSuccessor() != upBranch.getAddress()) {
                    pc.setAddress(p.getSuccessor());// =p->Successor;
                    break;
                }
                this.ps[pps++] = p.getAddress();
            } while (pc.getSuffix() != 0);

        } // NO_LOOP:
        if (pps == 0) {
            return pc.getAddress();
        }
        upState.setSymbol(getHeap()[upBranch.getAddress()]);// UpState.Symbol=*(byte*)
        // UpBranch;
        // UpState.Successor=(PPM_CONTEXT*) (((byte*) UpBranch)+1);
        upState.setSuccessor(upBranch.getAddress() + 1); //TODO check if +1 necessary
        if (pc.getNumStats() == 1) {
            upState.setFreq(pc.getOneState()
                              .getFreq());// UpState.Freq=pc->OneState.Freq;
        } else {
            if (pc.getAddress() <= this.subAlloc.getPText()) {
                return (0);
            }
            p.setAddress(pc.getFreqData()
                           .getStats());
            if (p.getSymbol() != upState.getSymbol()) {
                do {
                    p.incAddress();
                } while (p.getSymbol() != upState.getSymbol());
            }
            final int cf = p.getFreq() - 1;
            final int s0 = pc.getFreqData()
                             .getSummFreq() - pc.getNumStats() - cf;
            // UpState.Freq=1+((2*cf <= s0)?(5*cf > s0):((2*cf+3*s0-1)/(2*s0)));
            if ((5 * cf) > s0) {
                upState.setFreq(1 + (((2 * cf) <= s0) ? 1 : ((((2 * cf) + (3 * s0)) - 1) / (2 * s0))));
            } else {
                upState.setFreq(1 + (((2 * cf) <= s0) ? 0 : ((((2 * cf) + (3 * s0)) - 1) / (2 * s0))));
            }
        }
        do {
            // pc = pc->createChild(this,*--pps,UpState);
            tempState.setAddress(this.ps[--pps]);
            pc.setAddress(pc.createChild(this, tempState, upState));
            if (pc.getAddress() == 0) {
                return 0;
            }
        } while (pps != 0);
        return pc.getAddress();
    }

    private void updateModelRestart() {
        restartModelRare();
        this.escCount = 0;
    }

    private void updateModel() {
        //System.out.println("ModelPPM.updateModel()");
        // STATE fs = *FoundState, *p = NULL;
        final StateRef fs = this.tempStateRef1;
        fs.setValues(this.foundState);
        final State p = this.tempState3.init(getHeap());
        final State tempState = this.tempState4.init(getHeap());

        final PPMContext pc = this.tempPPMContext3.init(getHeap());
        final PPMContext successor = this.tempPPMContext4.init(getHeap());

        int ns1;
        final int ns;
        int cf;
        int sf;
        final int s0;
        pc.setAddress(this.minContext.getSuffix());
        if ((fs.getFreq() < (MAX_FREQ / 4)) && (pc.getAddress() != 0)) {
            if (pc.getNumStats() == 1) {
                p.setAddress(pc.getOneState()
                               .getAddress());
                if (p.getFreq() < 32) {
                    p.incFreq(1);
                }
            } else {
                p.setAddress(pc.getFreqData()
                               .getStats());
                if (p.getSymbol() != fs.getSymbol()) {
                    do {
                        p.incAddress();
                    } while (p.getSymbol() != fs.getSymbol());
                    tempState.setAddress(p.getAddress() - State.size);
                    if (p.getFreq() >= tempState.getFreq()) {
                        State.ppmdSwap(p, tempState);
                        p.decAddress();
                    }
                }
                if (p.getFreq() < (MAX_FREQ - 9)) {
                    p.incFreq(2);
                    pc.getFreqData()
                      .incSummFreq(2);
                }
            }
        }
        if (this.orderFall == 0) {
            this.foundState.setSuccessor(createSuccessors(true, p));
            this.minContext.setAddress(this.foundState.getSuccessor());
            this.maxContext.setAddress(this.foundState.getSuccessor());
            if (this.minContext.getAddress() == 0) {
                updateModelRestart();
                return;
            }
            return;
        }
        this.subAlloc.getHeap()[this.subAlloc.getPText()] = (byte) fs.getSymbol();
        this.subAlloc.incPText();
        successor.setAddress(this.subAlloc.getPText());
        if (this.subAlloc.getPText() >= this.subAlloc.getFakeUnitsStart()) {
            updateModelRestart();
            return;
        }
//        // Debug
//        subAlloc.dumpHeap();
        if (fs.getSuccessor() == 0) {
            this.foundState.setSuccessor(successor.getAddress());
            fs.setSuccessor(this.minContext);
        } else {
            if (fs.getSuccessor() <= this.subAlloc.getPText()) {
                fs.setSuccessor(createSuccessors(false, p));
                if (fs.getSuccessor() == 0) {
                    updateModelRestart();
                    return;
                }
            }
            if (--this.orderFall == 0) {
                successor.setAddress(fs.getSuccessor());
                if (this.maxContext.getAddress() != this.minContext.getAddress()) {
                    this.subAlloc.decPText(1);
                }
            }
        }
//        // Debug
//        subAlloc.dumpHeap();
        ns = this.minContext.getNumStats();
        s0 = this.minContext.getFreqData()
                            .getSummFreq() - (ns) - (fs.getFreq() - 1);
        for (pc.setAddress(this.maxContext.getAddress()); pc.getAddress() != this.minContext.getAddress();
             pc.setAddress(pc.getSuffix())) {
            if ((ns1 = pc.getNumStats()) == 1) {
                p.setAddress(this.subAlloc.allocUnits(1));
                if (p.getAddress() == 0) {
                    updateModelRestart();
                    return;
                }
                p.setValues(pc.getOneState());
                pc.getFreqData()
                  .setStats(p);
                if (p.getFreq() < ((MAX_FREQ / 4) - 1)) {
                    p.incFreq(p.getFreq());
                } else {
                    p.setFreq(MAX_FREQ - 4);
                }
                pc.getFreqData()
                  .setSummFreq((p.getFreq() + this.initEsc + ((ns > 3) ? 1 : 0)));
            } else {
                if ((ns1 & 1) == 0) {
                    //System.out.println(ns1);
                    pc.getFreqData()
                      .setStats(this.subAlloc.expandUnits(pc.getFreqData()
                                                            .getStats(), ns1 >>> 1));
                    if (pc.getFreqData()
                          .getStats() == 0) {
                        updateModelRestart();
                        return;
                    }
                }
                final int sum = (((2 * ns1) < ns) ? 1 : 0) + (2 * ((((4 * ns1) <= ns) ? 1 : 0) & ((pc.getFreqData()
                                                                                                     .getSummFreq() <= (
                                                                                                           8 * ns1)) ?
                                                                                                  1 : 0)));
                pc.getFreqData()
                  .incSummFreq(sum);
            }
            cf = 2 * fs.getFreq() * (pc.getFreqData()
                                       .getSummFreq() + 6);
            sf = s0 + pc.getFreqData()
                        .getSummFreq();
            if (cf < (6 * sf)) {
                cf = 1 + ((cf > sf) ? 1 : 0) + ((cf >= (4 * sf)) ? 1 : 0);
                pc.getFreqData()
                  .incSummFreq(3);
            } else {
                cf = 4 + ((cf >= (9 * sf)) ? 1 : 0) + ((cf >= (12 * sf)) ? 1 : 0) +
                     ((cf >= (15 * sf)) ? 1 : 0);
                pc.getFreqData()
                  .incSummFreq(cf);
            }
            p.setAddress(pc.getFreqData()
                           .getStats() + (ns1 * State.size));
            p.setSuccessor(successor);
            p.setSymbol(fs.getSymbol());
            p.setFreq(cf);
            pc.setNumStats(++ns1);
        }

        final int address = fs.getSuccessor();
        this.maxContext.setAddress(address);
        this.minContext.setAddress(address);
    }

    // Debug
    public String toString() {
        return "ModelPPM[" + "\n  numMasked=" + this.numMasked + "\n  initEsc=" + this.initEsc + "\n  orderFall="
               + this.orderFall + "\n  maxOrder=" + this.maxOrder + "\n  runLength=" + this.runLength + "\n  initRL="
               + this.initRL + "\n  escCount=" + this.escCount + "\n  prevSuccess=" + this.prevSuccess
               + "\n  foundState=" + this.foundState + "\n  coder=" + this.coder + "\n  subAlloc=" + this.subAlloc
               + "\n]";
    }
}
