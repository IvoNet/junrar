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

package nl.ivonet.test.util;

import java.io.File;

/**
 * Utility class for getting resources from the maven test/resources folder.
 *
 * @author Ivo Woltring
 */
public class ResourceProvider {
    /**
     * Get a filname from the recourse folder.
     *
     * @param fileName the filename to get in src/test/resources
     * @return the absolute path to the filename
     */
    public static String getFileResource(final String fileName) {
        String abspath = new File(".").getAbsolutePath();
        abspath = abspath.substring(0, abspath.length() - 1);
        return new File(abspath + "src/test/resources/" + fileName).getAbsolutePath();
    }

    public static String getTargetLocation() {
        String abspath = new File(".").getAbsolutePath();
        abspath = abspath.substring(0, abspath.length() - 1);
        return new File(abspath + "target/").getAbsolutePath();
    }

}
