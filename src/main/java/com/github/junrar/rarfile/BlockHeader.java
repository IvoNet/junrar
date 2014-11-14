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
 * Base class of headers that contain data
 */
public class BlockHeader extends BaseBlock {
    public static final short blockHeaderSize = 4;

    private final Log logger = LogFactory.getLog(BlockHeader.class.getName());

    private int dataSize;
    private int packSize;

    BlockHeader() {

    }

    BlockHeader(final BlockHeader bh) {
        super(bh);
        this.packSize = bh.getDataSize();
        this.dataSize = this.packSize;
        this.positionInFile = bh.getPositionInFile();
    }

    public BlockHeader(final BaseBlock bb, final byte[] blockHeader) {
        super(bb);

        this.packSize = Raw.readIntLittleEndian(blockHeader, 0);
        this.dataSize = this.packSize;
    }

    int getDataSize() {
        return this.dataSize;
    }

    int getPackSize() {
        return this.packSize;
    }

    @Override
    public void print() {
        super.print();
        final String s = "DataSize: " + getDataSize() + " packSize: " + getPackSize();
        this.logger.info(s);
    }
}
