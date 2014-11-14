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

package com.github.junrar.crc;

public class RarCRC {

    private static final int[] crcTab;

    static {
        crcTab = new int[256];
        for (int i = 0; i < 256; i++) {
            int c = i;
            for (int j = 0; j < 8; j++) {
                if ((c & 1) == 0) {
                    c >>>= 1;
                } else {
                    c >>>= 1;
                    c ^= 0xEDB88320;
                }
            }
            crcTab[i] = c;
        }
    }

    public int checkCrc(final int startCrc, final byte[] data, final int offset, final int count) {
        int crc = startCrc;
        final int size = Math.min(data.length - offset, count);
        int i = 0;
        while (i < size) {
            crc = (crcTab[(crc ^ (int) data[offset + i]) & 0xff] ^ (crc >>> 8));
            i++;
        }
        return (crc);
    }

    public short checkOldCrc(final short startCrc, final byte[] data, final int count) {
        short strtCrc = startCrc;
        final int n = Math.min(data.length, count);
        for (int i = 0; i < n; i++) {
            strtCrc += (short) (data[i] & 0x00ff);
            strtCrc = (short) (((strtCrc << 1) | (strtCrc >>> 15)));
        }
        return strtCrc;
    }


}
