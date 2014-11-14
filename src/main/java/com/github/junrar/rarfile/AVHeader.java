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
 * extended version info header
 */
public class AVHeader extends BaseBlock {

    public static final int avHeaderSize = 7;
    private final int avInfoCRC;
    private byte unpackVersion;
    private byte method;
    private byte avVersion;

    public AVHeader(final BaseBlock bb, final byte[] avHeader) {
        super(bb);

        int pos = 0;
        this.unpackVersion |= avHeader[pos] & 0xff;
        pos++;
        this.method |= avHeader[pos] & 0xff;
        pos++;
        this.avVersion |= avHeader[pos] & 0xff;
        pos++;
        this.avInfoCRC = Raw.readIntLittleEndian(avHeader, pos);
    }

    public int getAvInfoCRC() {
        return this.avInfoCRC;
    }

    public byte getAvVersion() {
        return this.avVersion;
    }

    public byte getMethod() {
        return this.method;
    }

    public byte getUnpackVersion() {
        return this.unpackVersion;
    }
}
