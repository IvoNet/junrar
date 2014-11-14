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

package com.github.junrar;

import java.io.File;

/**
 * @author alban
 */
public interface UnrarCallback {

    /**
     * Return <tt>true</tt> if the next volume is ready to be processed, <tt>false</tt> otherwise.
     */
    boolean isNextVolumeReady(File nextVolume);

    /**
     * This method is invoked each time the progress of the current volume changes.
     */
    void volumeProgressChanged(long current, long total);
}
