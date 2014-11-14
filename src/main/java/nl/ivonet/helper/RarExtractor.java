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

package nl.ivonet.helper;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author Ivo Woltring
 */
public class RarExtractor {
    private static final Log logger = LogFactory.getLog(RarExtractor.class.getName());

    public void extractArchive(final String archive, final String destination) {
        if ((archive == null) || (destination == null)) {
            throw new RuntimeException("archive and destination must me set");
        }
        final File arch = new File(archive);
        if (!arch.exists()) {
            throw new RuntimeException("the archive does not exit: " + archive);
        }
        final File destinationDirectory = new File(destination);
        if (!destinationDirectory.exists() || !destinationDirectory.isDirectory()) {
            throw new RuntimeException("the destination must exist and point to a directory: " + destination);
        }
        extractArchive(arch, destinationDirectory);
    }

    private void extractArchive(final File archive, final File destinationDirectory) {
        Archive arch = null;
        try {
            arch = new Archive(archive);
        } catch (final RarException | IOException e) {
            logger.error(e);
        }
        if (arch == null) {
            return;
        }
        if (arch.isEncrypted()) {
            logger.warn("archive is encrypted cannot extract");
            return;
        }
        FileHeader fh;
        while (true) {
            fh = arch.nextFileHeader();
            if (fh == null) {
                break;
            }
            if (fh.isEncrypted()) {
                logger.warn("file is encrypted cannot extract: " + fh.getFileNameString());
                continue;
            }
            logger.info("extracting: " + fh.getFileNameString());
            try {
                if (!fh.isDirectory()) {
                    final File f = createFile(fh, destinationDirectory);
                    final OutputStream stream = new FileOutputStream(f);
                    arch.extractFile(fh, stream);
                    stream.close();
                }
            } catch (final IOException | RarException e) {
                logger.error("error extracting the file", e);
            }
        }
    }


    private File createFile(final FileHeader fh, final File destinationDirectory) throws IOException {
        final String name = ((fh.isFileHeader() && fh.isUnicode()) ? fh.getFileNameW() :
                             fh.getFileNameString()).replace("\\", File.separator);
        final File createdFile = new File(destinationDirectory, name);
        Files.createDirectories(createdFile.toPath()
                                           .getParent());
        if (!createdFile.createNewFile()) {
            logger.info("Could not create file: " + createdFile.getAbsolutePath());
        }
        return createdFile;
    }


    public static void main(final String[] args) {
        final RarExtractor rarExtractor = new RarExtractor();
        if (args.length == 2) {
            rarExtractor.extractArchive(args[0], args[1]);
        } else {
            System.out.println("usage: java -jar extractArchive.jar <thearchive> <the destination directory>");
        }
    }

}
