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


class VMStandardFilterSignature {
    private final int CRC;
    private final VMStandardFilters type;
    private int length;

    public VMStandardFilterSignature(final int length, final int crc, final VMStandardFilters type) {
        this.length = length;
        this.CRC = crc;
        this.type = type;
    }

    public int getCRC() {
        return this.CRC;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    public VMStandardFilters getType() {
        return this.type;
    }


}
