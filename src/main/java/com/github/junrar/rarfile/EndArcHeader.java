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
 * the optional End header
 */
public class EndArcHeader extends BaseBlock {

    public static final short endArcArchiveDataCrcSize = 4;
    public static final short endArcVolumeNumberSize = 2;
    private static final short EARC_NEXT_VOLUME = 0x0001;
    private static final short EARC_DATACRC = 0x0002;
    private static final short EARC_REVSPACE = 0x0004;
    private static final short EARC_VOLNUMBER = 0x0008;
    private static final short endArcHeaderSize = 6;
    private int archiveDataCRC;
    private short volumeNumber;


    public EndArcHeader(final BaseBlock bb, final byte[] endArcHeader) {
        super(bb);

        int pos = 0;
        if (hasArchiveDataCRC()) {
            this.archiveDataCRC = Raw.readIntLittleEndian(endArcHeader, pos);
            pos += 4;
        }
        if (hasVolumeNumber()) {
            this.volumeNumber = Raw.readShortLittleEndian(endArcHeader, pos);
        }
    }

    public int getArchiveDataCRC() {
        return this.archiveDataCRC;
    }

    public short getVolumeNumber() {
        return this.volumeNumber;
    }
}
