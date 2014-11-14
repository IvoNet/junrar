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

import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class MVTestMainClass {

    private MVTestMainClass() {
    }

    public static void main(final String[] args) {
        final String filename = "/home/Avenger/testdata/test2.part01.rar";
        final File f = new File(filename);
        Archive a = null;
        try {
            a = new Archive(f);
        } catch (final RarException | IOException e) {
            e.printStackTrace();
        }
        if (a != null) {
            a.getMainHeader()
             .print();
            FileHeader fh = a.nextFileHeader();
            while (fh != null) {
                try {
                    final File out = new File("/home/Avenger/testdata/" + fh.getFileNameString()
                                                                            .trim());
                    System.out.println(out.getAbsolutePath());
                    final FileOutputStream os = new FileOutputStream(out);
                    a.extractFile(fh, os);
                    os.close();
                } catch (final RarException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                fh = a.nextFileHeader();
            }
        }
    }
}



