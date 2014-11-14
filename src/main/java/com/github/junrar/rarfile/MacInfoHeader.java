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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mac File attribute header
 */
public class MacInfoHeader extends SubBlockHeader {
    public static final short MacInfoHeaderSize = 8;
    private final Log logger = LogFactory.getLog(getClass());
    private int fileType;
    private int fileCreator;

    public MacInfoHeader(final SubBlockHeader sb, final byte[] macHeader) {
        super(sb);
        int pos = 0;
        this.fileType = Raw.readIntLittleEndian(macHeader, pos);
        pos += 4;
        this.fileCreator = Raw.readIntLittleEndian(macHeader, pos);
    }

    /**
     * @return the fileCreator
     */
    public int getFileCreator() {
        return this.fileCreator;
    }

    /**
     * @param fileCreator the fileCreator to set
     */
    public void setFileCreator(final int fileCreator) {
        this.fileCreator = fileCreator;
    }

    /**
     * @return the fileType
     */
    public int getFileType() {
        return this.fileType;
    }

    /**
     * @param fileType the fileType to set
     */
    public void setFileType(final int fileType) {
        this.fileType = fileType;
    }

    @Override
    public void print() {
        super.print();
        this.logger.info("filetype: " + this.fileType);
        this.logger.info("creator :" + this.fileCreator);
    }

}
