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

package com.github.junrar.rarfile;

import com.github.junrar.io.Raw;

/**
 * Comment header
 */
public class CommentHeader extends BaseBlock {

    public static final short commentHeaderSize = 6;

    private final short unpSize;
    private final short commCRC;
    private byte unpVersion;
    private byte unpMethod;


    public CommentHeader(final BaseBlock bb, final byte[] commentHeader) {
        super(bb);

        int pos = 0;
        this.unpSize = Raw.readShortLittleEndian(commentHeader, pos);
        pos += 2;
        this.unpVersion |= commentHeader[pos] & 0xff;
        pos++;

        this.unpMethod |= commentHeader[pos] & 0xff;
        pos++;
        this.commCRC = Raw.readShortLittleEndian(commentHeader, pos);

    }

    public short getCommCRC() {
        return this.commCRC;
    }

    public byte getUnpMethod() {
        return this.unpMethod;
    }

    public short getUnpSize() {
        return this.unpSize;
    }

    public byte getUnpVersion() {
        return this.unpVersion;
    }
}
