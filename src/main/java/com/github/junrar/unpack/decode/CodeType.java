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

package com.github.junrar.unpack.decode;

/**
 * DOCUMENT ME the unrar licence applies to all junrar source and binary distributions you are not allowed to use this
 * source to re-create the RAR compression algorithm
 */
public enum CodeType {
    CODE_HUFFMAN, CODE_LZ, CODE_LZ2, CODE_REPEATLZ, CODE_CACHELZ,
    CODE_STARTFILE, CODE_ENDFILE, CODE_VM, CODE_VMDATA
}
