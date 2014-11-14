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

package nl.ivonet.helper;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import nl.ivonet.helper.boundary.Memory;
import nl.ivonet.helper.boundary.Resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Ivo Woltring
 */
public class ArchiveToMemory {


    public Memory extract(final String rarFilename) {
        return this.extract(new File(rarFilename));
    }

    /**
     * Read the comic into a {@link Memory}.
     *
     * @param file the comic to read
     * @return {@link Memory}
     */
    public Memory extract(final File file) {
        final Memory memory = new Memory(file.getPath());
        try {
            final Archive archive = new Archive(file);
            final List<FileHeader> fileHeaders = archive.getFileHeaders();
            for (final FileHeader fileHeader : fileHeaders) {
                if (!fileHeader.isDirectory()) {
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    archive.extractFile(fileHeader, stream);
                    stream.close();
                    final Path path = Paths.get(fileHeader.getUnifiedFilename());
                    final String folder = (path.getParent() == null) ? "" : path.getParent()
                                                                                .toString();
                    final String filename = path.getFileName()
                                                .toString();
                    memory.add(new Resource(folder, filename, stream.toByteArray()));
                }
            }
        } catch (RarException | IOException e) {
            throw new RuntimeException(e);
        }
        return memory;
    }

}
