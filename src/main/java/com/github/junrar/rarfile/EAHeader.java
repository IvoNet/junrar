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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * extended archive CRC header
 */
public class EAHeader extends SubBlockHeader {
    public static final short EAHeaderSize = 10;
    private final Log logger = LogFactory.getLog(getClass());
    private final int unpSize;
    private final int EACRC;
    private byte unpVer;
    private byte method;

    public EAHeader(final SubBlockHeader sb, final byte[] eahead) {
        super(sb);
        int pos = 0;
        this.unpSize = Raw.readIntLittleEndian(eahead, pos);
        pos += 4;
        this.unpVer |= eahead[pos] & 0xff;
        pos++;
        this.method |= eahead[pos] & 0xff;
        pos++;
        this.EACRC = Raw.readIntLittleEndian(eahead, pos);
    }

    /**
     * @return the eACRC
     */
    public int getEACRC() {
        return this.EACRC;
    }

    /**
     * @return the method
     */
    public byte getMethod() {
        return this.method;
    }

    /**
     * @return the unpSize
     */
    public int getUnpSize() {
        return this.unpSize;
    }

    /**
     * @return the unpVer
     */
    public byte getUnpVer() {
        return this.unpVer;
    }

    @Override
    public void print() {
        super.print();
        this.logger.info("unpSize: " + this.unpSize);
        this.logger.info("unpVersion: " + this.unpVer);
        this.logger.info("method: " + this.method);
        this.logger.info("EACRC:" + this.EACRC);
    }
}

