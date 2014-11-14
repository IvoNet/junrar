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

public class SubBlockHeader extends BlockHeader {
    public static final short SubBlockHeaderSize = 3;
    private final Log logger = LogFactory.getLog(getClass());
    private final short subType;
    private byte level;

    SubBlockHeader(final SubBlockHeader sb) {
        super(sb);
        this.subType = sb.getSubType()
                         .getSubblocktype();
        this.level = sb.getLevel();
    }

    public SubBlockHeader(final BlockHeader bh, final byte[] subblock) {
        super(bh);
        int position = 0;
        this.subType = Raw.readShortLittleEndian(subblock, position);
        position += 2;
        this.level |= subblock[position] & 0xff;
    }

    byte getLevel() {
        return this.level;
    }

    public SubBlockHeaderType getSubType() {
        return SubBlockHeaderType.findSubblockHeaderType(this.subType);
    }

    @Override
    public void print() {
        super.print();
        this.logger.info("subtype: " + getSubType());
        this.logger.info("level: " + this.level);
    }
}
