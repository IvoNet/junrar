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
import java.util.ArrayList;
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
                final Resource resource = extract(archive, fileHeader);
                memory.add(resource);
            }
        } catch (RarException | IOException e) {
            throw new RuntimeException(e);
        }
        return memory;
    }


    public Resource extract(final String file, final String filename) {
        return extract(new File(file), filename);
    }

    public Resource extract(final File file, final String filename) {
        try {
            final Archive archive = new Archive(file);
            final FileHeader fileHeader = contains(archive, filename);
            if (fileHeader != null) {
                return extract(archive, fileHeader);
            }
        } catch (RarException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Resource extract(final Archive archive, final FileHeader fileHeader) {
        if (!fileHeader.isDirectory()) {
            try {
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                archive.extractFile(fileHeader, stream);
                stream.close();
                final Path path = Paths.get(fileHeader.getUnifiedFilename());
                final String folder = (path.getParent() == null) ? "" : path.getParent()
                                                                            .toString();
                final String filename = path.getFileName()
                                            .toString();
                return new Resource(folder, filename, stream.toByteArray());
            } catch (RarException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("You should have given a correct fileheader to extract");
    }

    public FileHeader contains(final Archive archive, final String filename) {
        final List<FileHeader> fileHeaders = archive.getFileHeaders();
        for (final FileHeader fileHeader : fileHeaders) {
            if (!fileHeader.isDirectory()) {
                if (fileHeader.getUnifiedFilename()
                              .equals(filename)) {
                    return fileHeader;
                }
            }
        }
        return null;
    }

    public List<String> files(final String file) {
        return files(new File(file));
    }

    public List<String> files(final File file) {
        try {
            final Archive archive = new Archive(file);
            return files(archive);
        } catch (RarException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> files(final Archive archive) {
        final List<String> files = new ArrayList<>();
        for (final FileHeader fileHeader : archive.getFileHeaders()) {
            if (!fileHeader.isDirectory()) {
                files.add(fileHeader.getUnifiedFilename());
            }
        }
        return files;
    }


}
