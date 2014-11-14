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


public enum UnrarHeadertype {
    MAIN_HEADER((byte) 0x73),
    MARK_HEADER((byte) 0x72),
    FILE_HEADER((byte) 0x74),
    COMM_HEADER((byte) 0x75),
    AV_HEADER((byte) 0x76),
    SUB_HEADER((byte) 0x77),
    PROTECT_HEADER((byte) 0x78),
    SIGN_HEADER((byte) 0x79),
    NEW_SUB_HEADER((byte) 0x7a),
    END_ARC_HEADER((byte) 0x7b);

    private final byte headerByte;


    UnrarHeadertype(final byte headerByte) {
        this.headerByte = headerByte;
    }

    /**
     * Return true if the given byte is equal to the enum's byte
     *
     * @return true if the given byte is equal to the enum's byte
     */
    public boolean equals(final byte header) {
        return this.headerByte == header;
    }

    /**
     * the header byte of this enum
     *
     * @return the header byte of this enum
     */
    public byte getHeaderByte() {
        return this.headerByte;
    }

    /**
     * Returns the enum according to the given byte or null
     *
     * @param headerType the headerbyte
     * @return the enum or null
     */
    public static UnrarHeadertype findType(final byte headerType) {
        for (final UnrarHeadertype unrarHeadertype : values()) {
            if (unrarHeadertype.headerByte == headerType) {
                return unrarHeadertype;
            }
        }
        return null;
    }


}
