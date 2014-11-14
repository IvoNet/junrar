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
 * the header to recognize a file to be a rar archive
 */
public class MarkHeader extends BaseBlock {

    private final Log logger = LogFactory.getLog(MarkHeader.class.getName());
    private boolean oldFormat;

    public MarkHeader(final BaseBlock bb) {
        super(bb);
    }

    boolean isValid() {
        if (getHeadCRC() == 0x6152) {
            if (getHeaderType() == UnrarHeadertype.MARK_HEADER) {
                if (getFlags() == 0x1a21) {
                    if (getHeaderSize() == BaseBlockSize) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSignature() {
        boolean valid = false;
        final byte[] d = new byte[BaseBlockSize];
        Raw.writeShortLittleEndian(d, 0, this.headCRC);
        d[2] = this.headerType;
        Raw.writeShortLittleEndian(d, 3, this.flags);
        Raw.writeShortLittleEndian(d, 5, this.headerSize);

        if (d[0] == 0x52) {
            if ((d[1] == 0x45) && (d[2] == 0x7e) && (d[3] == 0x5e)) {
                this.oldFormat = true;
                valid = true;
            } else if ((d[1] == 0x61) && (d[2] == 0x72) && (d[3] == 0x21) && (d[4] == 0x1a) &&
                       (d[5] == 0x07) && (d[6] == 0x00)) {
                this.oldFormat = false;
                valid = true;
            }
        }
        return valid;
    }

    public boolean isOldFormat() {
        return this.oldFormat;
    }

    @Override
    public void print() {
        super.print();
        this.logger.info("valid: " + isValid());
    }
}
