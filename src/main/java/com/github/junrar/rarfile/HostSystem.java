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


public enum HostSystem {
    MSDOS((byte) 0),
    OS_2((byte) 1),
    WIN_32((byte) 2),
    UNIX((byte) 3),
    MACOS((byte) 4),
    BEOS((byte) 5);

    private final byte hostByte;

    HostSystem(final byte hostByte) {
        this.hostByte = hostByte;
    }

    public static HostSystem findHostSystem(final byte hostByte) {
        for (final HostSystem hostSystem : values()) {
            if (hostSystem.hostByte == hostByte) {
                return hostSystem;
            }
        }
        return null;
    }
}
