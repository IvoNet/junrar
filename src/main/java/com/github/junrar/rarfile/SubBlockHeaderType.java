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

public enum SubBlockHeaderType {
    EA_HEAD((short) 0x100),
    UO_HEAD((short) 0x101),
    MAC_HEAD((short) 0x102),
    BEEA_HEAD((short) 0x103),
    NTACL_HEAD((short) 0x104),
    STREAM_HEAD((short) 0x105);

    private final short subblocktype;

    SubBlockHeaderType(final short subblocktype) {
        this.subblocktype = subblocktype;
    }

    /**
     * @return the short representation of this enum
     */
    public short getSubblocktype() {
        return this.subblocktype;
    }

    /**
     * find the header type for the given short value
     *
     * @param subType the short value
     * @return the correspo nding enum or null
     */
    public static SubBlockHeaderType findSubblockHeaderType(final short subType) {
        for (final SubBlockHeaderType subBlockHeaderType : values()) {
            if (subBlockHeaderType.subblocktype == subType) {
                return subBlockHeaderType;
            }
        }
        return null;
    }
}
