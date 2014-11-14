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
 * The main header of an rar archive. holds information concerning the whole archive (solid, encrypted etc).
 */
public class MainHeader extends BaseBlock {
    public static final short mainHeaderSizeWithEnc = 7;
    public static final short mainHeaderSize = 6;
    private final Log logger = LogFactory.getLog(MainHeader.class.getName());
    private final short highPosAv;
    private final int posAv;
    private byte encryptVersion;

    public MainHeader(final BaseBlock bb, final byte[] mainHeader) {
        super(bb);
        int pos = 0;
        this.highPosAv = Raw.readShortLittleEndian(mainHeader, pos);
        pos += 2;
        this.posAv = Raw.readIntLittleEndian(mainHeader, pos);
        pos += 4;

        if (hasEncryptVersion()) {
            this.encryptVersion |= mainHeader[pos] & 0xff;
        }
    }

    /**
     * old cmt block is present
     *
     * @return true if has cmt block
     */
    boolean hasArchCmt() {
        return (this.flags & MHD_COMMENT) != 0;
    }

    /**
     * the version the the encryption
     */
    byte getEncryptVersion() {
        return this.encryptVersion;
    }

    short getHighPosAv() {
        return this.highPosAv;
    }

    int getPosAv() {
        return this.posAv;
    }

    /**
     * returns whether the archive is encrypted
     */
    public boolean isEncrypted() {
        return (this.flags & MHD_PASSWORD) != 0;
    }

    /**
     * return whether the archive is a multivolume archive
     */
    boolean isMultiVolume() {
        return (this.flags & MHD_VOLUME) != 0;
    }

    /**
     * if the archive is a multivolume archive this method returns whether this instance is the first part of the
     * multivolume archive
     */
    boolean isFirstVolume() {
        return (this.flags & MHD_FIRSTVOLUME) != 0;
    }

    @Override
    public void print() {
        super.print();
        this.logger.info(
                "posav: " + getPosAv() + "\nhighposav: " + getHighPosAv() + "\nhasencversion: " + hasEncryptVersion()
                + (hasEncryptVersion() ? getEncryptVersion() : "") + "\nhasarchcmt: " + hasArchCmt() + "\nisEncrypted: "
                + isEncrypted() + "\nisMultivolume: " + isMultiVolume() + "\nisFirstvolume: " + isFirstVolume()
                + "\nisSolid: " + isSolid() + "\nisLocked: " + isLocked() + "\nisProtected: " + isProtected()
                + "\nisAV: " + isAV());
    }

    /**
     * returns whether this archive is solid. in this case you can only extract all file at once
     */
    boolean isSolid() {
        return (this.flags & MHD_SOLID) != 0;
    }

    boolean isLocked() {
        return (this.flags & MHD_LOCK) != 0;
    }

    boolean isProtected() {
        return (this.flags & MHD_PROTECT) != 0;
    }

    boolean isAV() {
        return (this.flags & MHD_AV) != 0;
    }

    /**
     * the numbering format a multivolume archive
     */
    public boolean isNewNumbering() {
        return (this.flags & MHD_NEWNUMBERING) != 0;
    }
}
