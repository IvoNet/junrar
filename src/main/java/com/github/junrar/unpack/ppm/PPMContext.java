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

import com.github.junrar.io.Raw;


@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
public class PPMContext extends Pointer {

    private static final int[] ExpEscape = {25, 14, 9, 7, 5, 5, 4, 4, 4, 3, 3, 3, 2, 2, 2, 2};
    private static final int unionSize = Math.max(FreqData.size, State.size);
    public static final int size = 2 + unionSize + 4; // 12

    private final FreqData freqData; // -\

    // |-> union
    private final State oneState; // -/
    // Temp fields
    private final State tempState1 = new State(null);
    private final State tempState2 = new State(null);
    private final State tempState3 = new State(null);
    private final State tempState4 = new State(null);
    private final State tempState5 = new State(null);
    private final int[] ps = new int[256];
    // ushort NumStats;
    private int numStats; // determines if feqData or onstate is used
    private int suffix; // pointer ppmcontext
    private PPMContext tempPPMContext;

    public PPMContext(final byte[] mem) {
        super(mem);
        this.oneState = new State(mem);
        this.freqData = new FreqData(mem);
    }

    public PPMContext init(final byte[] mem) {
        this.mem = mem;
        this.pos = 0;
        this.oneState.init(mem);
        this.freqData.init(mem);
        return this;
    }

    public FreqData getFreqData() {
        return this.freqData;
    }

    public void setFreqData(final FreqData freqData) {
        this.freqData.setSummFreq(freqData.getSummFreq());
        this.freqData.setStats(freqData.getStats());
    }

    public final int getNumStats() {
        if (this.mem != null) {
            this.numStats = Raw.readShortLittleEndian(this.mem, this.pos) & 0xffff;
        }
        return this.numStats;
    }

    public final void setNumStats(final int numStats) {
        this.numStats = numStats & 0xffff;
        if (this.mem != null) {
            Raw.writeShortLittleEndian(this.mem, this.pos, (short) numStats);
        }
    }

    public State getOneState() {
        return this.oneState;
    }

    void setOneState(final StateRef oneState) {
        this.oneState.setValues(oneState);
    }

    public int getSuffix() {
        if (this.mem != null) {
            this.suffix = Raw.readIntLittleEndian(this.mem, this.pos + 8);
        }
        return this.suffix;
    }

    public void setSuffix(final int suffix) {
        this.suffix = suffix;
        if (this.mem != null) {
            Raw.writeIntLittleEndian(this.mem, this.pos + 8, suffix);
        }
    }

    void setSuffix(final PPMContext suffix) {
        setSuffix(suffix.getAddress());
    }

    @Override
    public void setAddress(final int pos) {
        super.setAddress(pos);
        this.oneState.setAddress(pos + 2);
        this.freqData.setAddress(pos + 2);
    }

    private PPMContext getTempPPMContext(final byte[] mem) {
        if (this.tempPPMContext == null) {
            this.tempPPMContext = new PPMContext(null);
        }
        return this.tempPPMContext.init(mem);
    }

    public int createChild(final ModelPPM model, final State pStats/* ptr */, final StateRef firstState /* ref */) {
        final PPMContext pc = getTempPPMContext(model.getSubAlloc()
                                                     .getHeap());
        pc.setAddress(model.getSubAlloc()
                           .allocContext());
        if (pc != null) {
            pc.setNumStats(1);
            pc.setOneState(firstState);
            pc.setSuffix(this);
            pStats.setSuccessor(pc);
        }
        return pc.getAddress();
    }

    void rescale(final ModelPPM model) {
        final int OldNS = getNumStats();
        int i = getNumStats() - 1;
        final int Adder;
        int EscFreq;
        // STATE* p1, * p;
        final State p1 = new State(model.getHeap());
        final State p = new State(model.getHeap());
        final State temp = new State(model.getHeap());

        for (p.setAddress(model.getFoundState()
                               .getAddress()); p.getAddress() != this.freqData.getStats(); p.decAddress()) {
            temp.setAddress(p.getAddress() - State.size);
            State.ppmdSwap(p, temp);
        }
        temp.setAddress(this.freqData.getStats());
        temp.incFreq(4);
        this.freqData.incSummFreq(4);
        EscFreq = this.freqData.getSummFreq() - p.getFreq();
        Adder = (model.getOrderFall() != 0) ? 1 : 0;
        p.setFreq((p.getFreq() + Adder) >>> 1);
        this.freqData.setSummFreq(p.getFreq());
        do {
            p.incAddress();
            EscFreq -= p.getFreq();
            p.setFreq((p.getFreq() + Adder) >>> 1);
            this.freqData.incSummFreq(p.getFreq());
            temp.setAddress(p.getAddress() - State.size);
            if (p.getFreq() > temp.getFreq()) {
                p1.setAddress(p.getAddress());
                final StateRef tmp = new StateRef();
                tmp.setValues(p1);
                final State temp2 = new State(model.getHeap());
                final State temp3 = new State(model.getHeap());
                do {
                    // p1[0]=p1[-1];
                    temp2.setAddress(p1.getAddress() - State.size);
                    p1.setValues(temp2);
                    p1.decAddress();
                    temp3.setAddress(p1.getAddress() - State.size);
                } while ((p1.getAddress() != this.freqData.getStats()) && (tmp.getFreq() > temp3.getFreq()));
                p1.setValues(tmp);
            }
        } while (--i != 0);
        if (p.getFreq() == 0) {
            do {
                i++;
                p.decAddress();
            } while (p.getFreq() == 0);
            EscFreq += i;
            setNumStats(getNumStats() - i);
            if (getNumStats() == 1) {
                final StateRef tmp = new StateRef();
                temp.setAddress(this.freqData.getStats());
                tmp.setValues(temp);
                // STATE tmp=*U.Stats;
                do {
                    // tmp.Freq-=(tmp.Freq >> 1)
                    tmp.decFreq(tmp.getFreq() >>> 1);
                    EscFreq >>>= 1;
                } while (EscFreq > 1);
                model.getSubAlloc()
                     .freeUnits(this.freqData.getStats(), (OldNS + 1) >>> 1);
                this.oneState.setValues(tmp);
                model.getFoundState()
                     .setAddress(this.oneState.getAddress());
                return;
            }
        }
        EscFreq -= EscFreq >>> 1;
        this.freqData.incSummFreq(EscFreq);
        final int n0 = (OldNS + 1) >>> 1;
        final int n1 = (getNumStats() + 1) >>> 1;
        if (n0 != n1) {
            this.freqData.setStats(model.getSubAlloc()
                                        .shrinkUnits(this.freqData.getStats(), n0, n1));
        }
        model.getFoundState()
             .setAddress(this.freqData.getStats());
    }

    private int getArrayIndex(final ModelPPM Model, final State rs) {
        final PPMContext tempSuffix = getTempPPMContext(Model.getSubAlloc()
                                                             .getHeap());
        tempSuffix.setAddress(getSuffix());
        int ret = 0;
        ret += Model.getPrevSuccess();
        ret += Model.getNS2BSIndx()[tempSuffix.getNumStats() - 1];
        ret += Model.getHiBitsFlag() + (2 * Model.getHB2Flag()[rs.getSymbol()]);
        ret += ((Model.getRunLength() >>> 26) & 0x20);
        return ret;
    }

    int getMean(final int summ, final int shift, final int round) {
        return ((summ + (1 << (shift - round))) >>> (shift));
    }

    public void decodeBinSymbol(final ModelPPM model) {
        final State rs = this.tempState1.init(model.getHeap());
        rs.setAddress(this.oneState.getAddress());// State&
        model.setHiBitsFlag(model.getHB2Flag()[model.getFoundState()
                                                    .getSymbol()]);
        final int off1 = rs.getFreq() - 1;
        final int off2 = getArrayIndex(model, rs);
        int bs = model.getBinSumm()[off1][off2];
        if (model.getCoder()
                 .getCurrentShiftCount(ModelPPM.TOT_BITS) < bs) {
            model.getFoundState()
                 .setAddress(rs.getAddress());
            rs.incFreq((rs.getFreq() < 128) ? 1 : 0);
            model.getCoder()
                 .getSubRange()
                 .setLowCount(0);
            model.getCoder()
                 .getSubRange()
                 .setHighCount(bs);
            bs = (((bs + ModelPPM.INTERVAL) - getMean(bs, ModelPPM.PERIOD_BITS, 2)) & 0xffff);
            model.getBinSumm()[off1][off2] = bs;
            model.setPrevSuccess(1);
            model.incRunLength(1);
        } else {
            model.getCoder()
                 .getSubRange()
                 .setLowCount(bs);
            bs = (bs - getMean(bs, ModelPPM.PERIOD_BITS, 2)) & 0xFFFF;
            model.getBinSumm()[off1][off2] = bs;
            model.getCoder()
                 .getSubRange()
                 .setHighCount(ModelPPM.BIN_SCALE);
            model.setInitEsc(ExpEscape[bs >>> 10]);
            model.setNumMasked(1);
            model.getCharMask()[rs.getSymbol()] = model.getEscCount();
            model.setPrevSuccess(0);
            model.getFoundState()
                 .setAddress(0);
        }
    }

    void update1(final ModelPPM model, final int p/* ptr */) {
        model.getFoundState()
             .setAddress(p);
        model.getFoundState()
             .incFreq(4);
        this.freqData.incSummFreq(4);
        final State p0 = this.tempState3.init(model.getHeap());
        final State p1 = this.tempState4.init(model.getHeap());
        p0.setAddress(p);
        p1.setAddress(p - State.size);
        if (p0.getFreq() > p1.getFreq()) {
            State.ppmdSwap(p0, p1);
            model.getFoundState()
                 .setAddress(p1.getAddress());
            if (p1.getFreq() > ModelPPM.MAX_FREQ) {
                rescale(model);
            }
        }
    }

    public boolean decodeSymbol2(final ModelPPM model) {
        final long count;
        int hiCnt;
        int i = getNumStats() - model.getNumMasked();
        final SEE2Context psee2c = makeEscFreq2(model, i);
        final RangeCoder coder = model.getCoder();
        // STATE* ps[256], ** pps=ps, * p=U.Stats-1;
        final State p = this.tempState1.init(model.getHeap());
        final State temp = this.tempState2.init(model.getHeap());
        p.setAddress(this.freqData.getStats() - State.size);
        int pps = 0;
        hiCnt = 0;

        do {
            do {
                p.incAddress();// p++;
            } while (model.getCharMask()[p.getSymbol()] == model.getEscCount());
            hiCnt += p.getFreq();
            this.ps[pps++] = p.getAddress();
        } while (--i != 0);
        coder.getSubRange()
             .incScale(hiCnt);
        count = coder.getCurrentCount();
        if (count >= coder.getSubRange()
                          .getScale()) {
            return false;
        }
        pps = 0;
        p.setAddress(this.ps[pps]);
        if (count < hiCnt) {
            hiCnt = 0;
            while ((hiCnt += p.getFreq()) <= count) {
                p.setAddress(this.ps[++pps]);// p=*++pps;
            }
            coder.getSubRange()
                 .setHighCount(hiCnt);
            coder.getSubRange()
                 .setLowCount(hiCnt - p.getFreq());
            psee2c.update();
            update2(model, p.getAddress());
        } else {
            coder.getSubRange()
                 .setLowCount(hiCnt);
            coder.getSubRange()
                 .setHighCount(coder.getSubRange()
                                    .getScale());
            i = getNumStats() - model.getNumMasked();// ->NumMasked;
            pps--;
            do {
                temp.setAddress(this.ps[++pps]);// (*++pps)
                model.getCharMask()[temp.getSymbol()] = model.getEscCount();
            } while (--i != 0);
            psee2c.incSumm((int) coder.getSubRange()
                                      .getScale());
            model.setNumMasked(getNumStats());
        }
        return (true);
    }

    void update2(final ModelPPM model, final int p/* state ptr */) {
        final State temp = this.tempState5.init(model.getHeap());
        temp.setAddress(p);
        model.getFoundState()
             .setAddress(p);
        model.getFoundState()
             .incFreq(4);
        this.freqData.incSummFreq(4);
        if (temp.getFreq() > ModelPPM.MAX_FREQ) {
            rescale(model);
        }
        model.incEscCount(1);
        model.setRunLength(model.getInitRL());
    }

    private SEE2Context makeEscFreq2(final ModelPPM model, final int Diff) {
        final SEE2Context psee2c;
        final int numStats = getNumStats();
        if (numStats == 256) {
            psee2c = model.getDummySEE2Cont();
            model.getCoder()
                 .getSubRange()
                 .setScale(1);
        } else {
            final PPMContext suff = getTempPPMContext(model.getHeap());
            suff.setAddress(getSuffix());
            final int idx1 = model.getNS2Indx()[Diff - 1];
            int idx2 = 0;
            idx2 += (Diff < (suff.getNumStats() - numStats)) ? 1 : 0;
            idx2 += 2 * ((this.freqData.getSummFreq() < (11 * numStats)) ? 1 : 0);
            idx2 += 4 * ((model.getNumMasked() > Diff) ? 1 : 0);
            idx2 += model.getHiBitsFlag();
            psee2c = model.getSEE2Cont()[idx1][idx2];
            model.getCoder()
                 .getSubRange()
                 .setScale(psee2c.getMean());
        }
        return psee2c;
    }

    public boolean decodeSymbol1(final ModelPPM model) {

        final RangeCoder coder = model.getCoder();
        coder.getSubRange()
             .setScale(this.freqData.getSummFreq());
        final State p = new State(model.getHeap());
        p.setAddress(this.freqData.getStats());
        int i;
        int HiCnt;
        final long count = coder.getCurrentCount();
        if (count >= coder.getSubRange()
                          .getScale()) {
            return false;
        }
        if (count < (HiCnt = p.getFreq())) {
            coder.getSubRange()
                 .setHighCount(HiCnt);
            model.setPrevSuccess(((2 * HiCnt) > coder.getSubRange()
                                                     .getScale()) ? 1 : 0);
            model.incRunLength(model.getPrevSuccess());
            HiCnt += 4;
            model.getFoundState()
                 .setAddress(p.getAddress());
            model.getFoundState()
                 .setFreq(HiCnt);
            this.freqData.incSummFreq(4);
            if (HiCnt > ModelPPM.MAX_FREQ) {
                rescale(model);
            }
            coder.getSubRange()
                 .setLowCount(0);
            return true;
        }
        if (model.getFoundState()
                 .getAddress() == 0) {
            return (false);
        }
        model.setPrevSuccess(0);
        final int numStats = getNumStats();
        i = numStats - 1;
        while ((HiCnt += p.incAddress()
                          .getFreq()) <= count) {
            if (--i == 0) {
                model.setHiBitsFlag(model.getHB2Flag()[model.getFoundState()
                                                            .getSymbol()]);
                coder.getSubRange()
                     .setLowCount(HiCnt);
                model.getCharMask()[p.getSymbol()] = model.getEscCount();
                model.setNumMasked(numStats);
                i = numStats - 1;
                model.getFoundState()
                     .setAddress(0);
                do {
                    model.getCharMask()[p.decAddress()
                                         .getSymbol()] = model.getEscCount();
                } while (--i != 0);
                coder.getSubRange()
                     .setHighCount(coder.getSubRange()
                                        .getScale());
                return (true);
            }
        }
        coder.getSubRange()
             .setLowCount(HiCnt - p.getFreq());
        coder.getSubRange()
             .setHighCount(HiCnt);
        update1(model, p.getAddress());
        return (true);
    }

    public String toString() {
        return "PPMContext[" + "\n  pos=" + this.pos + "\n  size=" + size + "\n  numStats=" + getNumStats()
               + "\n  Suffix=" + getSuffix() + "\n  freqData=" + this.freqData + "\n  oneState=" + this.oneState
               + "\n]";
    }

}
