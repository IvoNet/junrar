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
import com.github.junrar.rarfile.FileHeader;
import nl.ivonet.test.util.ResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ArchiveExtractorTest {

    private ArchiveExtractor archiveExtractor;

    @Before
    public void setUp() throws Exception {
        this.archiveExtractor = new ArchiveExtractor();

    }

    @org.junit.Test
    public void testGetFile() throws Exception {
        final Archive archive = new Archive(new File(ResourceProvider.getFileResource("comicA.cbr")));

        final List<FileHeader> fileHeaders = archive.getFileHeaders();
        assertFalse(fileHeaders.isEmpty());
        assertThat(fileHeaders.size(), is(3));

        for (final FileHeader fileHeader : fileHeaders) {
            assertTrue(fileHeader.getFileNameString()
                                 .startsWith("img"));
            assertTrue(fileHeader.getFileNameString()
                                 .endsWith("jpg"));
        }

    }


    @Test
    public void testExtractArchive() throws Exception {
        this.archiveExtractor.extractArchive(ResourceProvider.getFileResource("comicA.cbr"),
                                         ResourceProvider.getTargetLocation());

        assertTrue(Files.exists(Paths.get(ResourceProvider.getTargetLocation(), "img001.jpg")));
        assertTrue(Files.exists(Paths.get(ResourceProvider.getTargetLocation(), "img002.jpg")));
        assertTrue(Files.exists(Paths.get(ResourceProvider.getTargetLocation(), "img003.jpg")));
    }
}