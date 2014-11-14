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

import java.util.Arrays;

/**
 * subheaders new version of the info headers
 */
public class NewSubHeaderType {

    /**
     * comment subheader
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_CMT = new NewSubHeaderType(new byte[]{'C', 'M', 'T'});
    public static final NewSubHeaderType SUBHEAD_TYPE_ACL = new NewSubHeaderType(new byte[]{'A', 'C', 'L'});
    public static final NewSubHeaderType SUBHEAD_TYPE_STREAM = new NewSubHeaderType(new byte[]{'S', 'T', 'M'});
    public static final NewSubHeaderType SUBHEAD_TYPE_UOWNER = new NewSubHeaderType(new byte[]{'U', 'O', 'W'});
    public static final NewSubHeaderType SUBHEAD_TYPE_AV = new NewSubHeaderType(new byte[]{'A', 'V'});
    /**
     * recovery record subheader
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_RR = new NewSubHeaderType(new byte[]{'R', 'R'});
    public static final NewSubHeaderType SUBHEAD_TYPE_OS2EA = new NewSubHeaderType(new byte[]{'E', 'A', '2'});
    public static final NewSubHeaderType SUBHEAD_TYPE_BEOSEA = new NewSubHeaderType(new byte[]{'E', 'A', 'B', 'E'});

    private final byte[] headerTypes;

    private NewSubHeaderType(final byte[] headerTypes) {
        this.headerTypes = headerTypes;
    }

    /**
     * @return Returns true if the given byte array matches to the internal byte array of this header.
     */
    public boolean byteEquals(final byte[] toCompare) {
        return Arrays.equals(this.headerTypes, toCompare);
    }

    @Override
    public String toString() {
        return new String(this.headerTypes);
    }
}
