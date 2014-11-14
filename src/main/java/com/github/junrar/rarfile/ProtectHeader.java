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

package com.github.junrar.rarfile;

import com.github.junrar.io.Raw;

/**
 * recovery header
 */
@SuppressWarnings("ClassTooDeepInInheritanceTree")
public class ProtectHeader extends BlockHeader {

    public static final int protectHeaderSize = 8;
    private final short recSectors;
    private final int totalBlocks;
    private byte version;
    private byte mark;


    public ProtectHeader(final BlockHeader bh, final byte[] protectHeader) {
        super(bh);

        int pos = 0;
        this.version |= protectHeader[pos] & 0xff;

        this.recSectors = Raw.readShortLittleEndian(protectHeader, pos);
        pos += 2;
        this.totalBlocks = Raw.readIntLittleEndian(protectHeader, pos);
        pos += 4;
        this.mark |= protectHeader[pos] & 0xff;
    }


    public byte getMark() {
        return this.mark;
    }

    public short getRecSectors() {
        return this.recSectors;
    }

    public int getTotalBlocks() {
        return this.totalBlocks;
    }

    public byte getVersion() {
        return this.version;
    }
}
