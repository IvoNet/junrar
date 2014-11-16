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

import nl.ivonet.helper.boundary.Memory;
import nl.ivonet.helper.boundary.Resource;
import nl.ivonet.test.util.ResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArchiveToMemoryTest {

    private ArchiveToMemory archiveToMemory;

    @Before
    public void setUp() throws Exception {
        this.archiveToMemory = new ArchiveToMemory();
    }

    @Test
    public void testExtractArchive() throws Exception {

        final Memory memory = this.archiveToMemory.extract(ResourceProvider.getFileResource("files.rar"));
//        System.out.println("memory.getFileName() = " + memory.getFileName());

        assertFalse(memory.keys()
                          .isEmpty());

        for (final Resource resource : memory.values()) {
            final Path path = Paths.get(ResourceProvider.getTargetLocation(), resource.getPath());
            Files.createDirectories(path);
            Files.write(Paths.get(path.toString(), resource.getFilename()), resource.getData());
        }

        final Path comic = Paths.get(ResourceProvider.getTargetLocation(), "src/test/resources/comicA.cbr");
        assertTrue(Files.exists(comic));

        final Memory comicMem = this.archiveToMemory.extract(comic.toFile());
//        System.out.println("comicMem.getFileName() = " + comicMem.getFileName());

        assertFalse(comicMem.keys()
                            .isEmpty());


    }

    @Test
    public void testFiles() throws Exception {
        final String filename = ResourceProvider.getFileResource("files.rar");
        final List<String> files = this.archiveToMemory.files(filename);
        for (final String file : files) {
            System.out.println("file = " + file);
            assertNotNull(this.archiveToMemory.extract(filename, file));
        }


    }
}