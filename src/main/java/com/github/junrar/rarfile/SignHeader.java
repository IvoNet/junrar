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
 * sign header
 */
public class SignHeader extends BaseBlock {

    public static final short signHeaderSize = 8;

    private final int creationTime;
    private final short arcNameSize;
    private final short userNameSize;


    public SignHeader(final BaseBlock bb, final byte[] signHeader) {
        super(bb);

        int pos = 0;
        this.creationTime = Raw.readIntLittleEndian(signHeader, pos);
        pos += 4;
        this.arcNameSize = Raw.readShortLittleEndian(signHeader, pos);
        pos += 2;
        this.userNameSize = Raw.readShortLittleEndian(signHeader, pos);
    }

    public short getArcNameSize() {
        return this.arcNameSize;
    }

    public int getCreationTime() {
        return this.creationTime;
    }

    public short getUserNameSize() {
        return this.userNameSize;
    }
}
