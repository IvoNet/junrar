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

package com.github.junrar.unpack.vm;


public enum VMFlags {
    VM_FC(1),
    VM_FZ(2),
    VM_FS(0x80000000);

    private final int flag;

    VMFlags(final int flag) {
        this.flag = flag;
    }

    /**
     * @return the flag as int
     */
    public int flag() {
        return this.flag;
    }

}
