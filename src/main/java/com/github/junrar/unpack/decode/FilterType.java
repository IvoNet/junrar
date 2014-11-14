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


public enum FilterType {
    FILTER_NONE, FILTER_PPM /*dummy*/, FILTER_E8, FILTER_E8E9,
    FILTER_UPCASETOLOW, FILTER_AUDIO, FILTER_RGB, FILTER_DELTA,
    FILTER_ITANIUM, FILTER_E8E9V2
}
