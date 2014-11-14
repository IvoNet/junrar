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
 * Base class of all rar headers
 */
public class BaseBlock {

    public static final short BaseBlockSize = 7;
    public static final short MHD_PACK_COMMENT = 0x0010;
    public static final short LHD_COMMENT = 0x0008;
    public static final short LHD_WINDOW64 = 0x0000;
    public static final short LHD_WINDOW128 = 0x0020;
    public static final short LHD_WINDOW256 = 0x0040;
    public static final short LHD_WINDOW512 = 0x0060;
    public static final short LHD_WINDOW1024 = 0x0080;
    public static final short LHD_WINDOW2048 = 0x00a0;
    public static final short LHD_WINDOW4096 = 0x00c0;
    public static final short LHD_VERSION = 0x0800;
    public static final short LHD_EXTTIME = 0x1000;
    public static final short LHD_EXTFLAGS = 0x2000;
    public static final short SKIP_IF_UNKNOWN = 0x4000;
    public static final short LONG_BLOCK = -0x8000;
    public static final short EARC_NEXT_VOLUME = 0x0001;
    public static final short EARC_REVSPACE = 0x0004;
    static final short MHD_VOLUME = 0x0001;
    //TODO move somewhere else
    static final short MHD_COMMENT = 0x0002;
    static final short MHD_LOCK = 0x0004;
    static final short MHD_SOLID = 0x0008;
    static final short MHD_NEWNUMBERING = 0x0010;
    static final short MHD_AV = 0x0020;
    static final short MHD_PROTECT = 0x0040;
    static final short MHD_PASSWORD = 0x0080;
    static final short MHD_FIRSTVOLUME = 0x0100;
    static final short LHD_SPLIT_BEFORE = 0x0001;
    static final short LHD_SPLIT_AFTER = 0x0002;
    static final short LHD_PASSWORD = 0x0004;
    static final short LHD_SOLID = 0x0010;
    static final short LHD_WINDOWMASK = 0x00e0;
    static final short LHD_DIRECTORY = 0x00e0;
    static final short LHD_LARGE = 0x0100;
    static final short LHD_UNICODE = 0x0200;
    static final short LHD_SALT = 0x0400;
    private static final short MHD_ENCRYPTVER = 0x0200;
    private static final short EARC_DATACRC = 0x0002;
    private static final short EARC_VOLNUMBER = 0x0008;
    private final Log logger = LogFactory.getLog(BaseBlock.class.getName());
    long positionInFile;
    short headCRC;
    byte headerType;
    short flags;
    short headerSize;

    BaseBlock() {
    }

    BaseBlock(final BaseBlock bb) {
        this.flags = bb.getFlags();
        this.headCRC = bb.getHeadCRC();
        this.headerType = bb.getHeaderType()
                            .getHeaderByte();
        this.headerSize = bb.getHeaderSize();
        this.positionInFile = bb.getPositionInFile();
    }

    public BaseBlock(final byte[] baseBlockHeader) {

        int pos = 0;
        this.headCRC = Raw.readShortLittleEndian(baseBlockHeader, pos);
        pos += 2;
        this.headerType |= baseBlockHeader[pos] & 0xff;
        pos++;
        this.flags = Raw.readShortLittleEndian(baseBlockHeader, pos);
        pos += 2;
        this.headerSize = Raw.readShortLittleEndian(baseBlockHeader, pos);
    }


    public boolean hasArchiveDataCRC() {
        return (this.flags & EARC_DATACRC) != 0;
    }

    public boolean hasVolumeNumber() {
        return (this.flags & EARC_VOLNUMBER) != 0;
    }

    public boolean hasEncryptVersion() {
        return (this.flags & MHD_ENCRYPTVER) != 0;
    }

    boolean isSubBlock() {
        return UnrarHeadertype.SUB_HEADER.equals(this.headerType) || (UnrarHeadertype.NEW_SUB_HEADER.equals(
                this.headerType) && ((this.flags & LHD_SOLID) != 0));

    }

    public long getPositionInFile() {
        return this.positionInFile;
    }

    public void setPositionInFile(final long positionInFile) {
        this.positionInFile = positionInFile;
    }

    short getFlags() {
        return this.flags;
    }

    short getHeadCRC() {
        return this.headCRC;
    }

    public short getHeaderSize() {
        return this.headerSize;
    }

    public UnrarHeadertype getHeaderType() {
        return UnrarHeadertype.findType(this.headerType);
    }

    void print() {
        this.logger.info("HeaderType: " + getHeaderType() + "\nHeadCRC: " + Integer.toHexString(getHeadCRC())
                         + "\nFlags: " + Integer.toHexString(getFlags()) + "\nHeaderSize: " + getHeaderSize()
                         + "\nPosition in file: " + getPositionInFile());
    }
}
