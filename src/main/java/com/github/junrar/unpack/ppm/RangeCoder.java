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


public class RangeCoder {
    private static final int TOP = 1 << 24;

    private static final int BOT = 1 << 15;

    private static final long uintMask = 0xFFFFffffL;
    private final SubRange subRange = new SubRange();
    // uint low, code, range;
    private long low;
    private long code;
    private long range;
    private Unpack unpackRead;

    public SubRange getSubRange() {
        return this.subRange;
    }

    public void initDecoder(final Unpack unpackRead) throws IOException, RarException {
        this.unpackRead = unpackRead;

        this.low = 0L;
        this.code = 0L;
        this.range = 0xFFFFffffL;
        for (int i = 0; i < 4; i++) {
            this.code = ((this.code << 8) | getChar()) & uintMask;
        }
    }

    public int getCurrentCount() {
        this.range = (this.range / this.subRange.getScale()) & uintMask;
        return (int) ((this.code - this.low) / (this.range));
    }

    public long getCurrentShiftCount(final int SHIFT) {
        this.range >>>= SHIFT;
        return ((this.code - this.low) / (this.range)) & uintMask;
    }

    public void decode() {
        this.low = (this.low + (this.range * this.subRange.getLowCount())) & uintMask;
        this.range = (this.range * (this.subRange.getHighCount() - this.subRange.getLowCount())) & uintMask;
    }

    private int getChar() throws IOException, RarException {
        return (this.unpackRead.getChar());
    }

    public void ariDecNormalize() throws IOException, RarException {
        // Rewrote for clarity
        boolean c2 = false;
        while (((this.low ^ (this.low + this.range)) < TOP) || ((c2 = this.range < BOT))) {
            if (c2) {
                this.range = (-this.low & (BOT - 1)) & uintMask;
                c2 = false;
            }
            this.code = ((this.code << 8) | getChar()) & uintMask;
            this.range = (this.range << 8) & uintMask;
            this.low = (this.low << 8) & uintMask;
        }
    }

    // Debug
    public String toString() {
        return "RangeCoder[" + "\n  low=" + this.low + "\n  code=" + this.code + "\n  range=" + this.range
               + "\n  subrange=" + this.subRange + "]";
    }

    public static class SubRange {
        // uint LowCount, HighCount, scale;
        private long lowCount;
        private long highCount;
        private long scale;

        public long getHighCount() {
            return this.highCount;
        }

        public void setHighCount(final long highCount) {
            this.highCount = highCount & uintMask;
        }

        public long getLowCount() {
            return this.lowCount & uintMask;
        }

        public void setLowCount(final long lowCount) {
            this.lowCount = lowCount & uintMask;
        }

        public long getScale() {
            return this.scale;
        }

        public void setScale(final long scale) {
            this.scale = scale & uintMask;
        }

        public void incScale(final int dScale) {
            setScale(getScale() + dScale);
        }

        // Debug
        public String toString() {
            return "SubRange[" + "\n  lowCount=" + this.lowCount + "\n  highCount=" + this.highCount + "\n  scale="
                   + this.scale + "]";
        }
    }
}
