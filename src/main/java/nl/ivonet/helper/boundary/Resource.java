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

package nl.ivonet.helper.boundary;

import com.sun.istack.internal.NotNull;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author Ivo Woltring
 */
public class Resource {

    private final String filename;
    private String path;
    private byte[] data;

    public Resource(@NotNull final String filename) {
        this.filename = filename;
        this.path = "";
    }

    public Resource(@NotNull final String path, @NotNull final String filename) {
        this.path = path;
        this.filename = filename;
    }

    public Resource(@NotNull final String path, @NotNull final String filename, @NotNull final byte[] data) {
        this.path = path;
        this.filename = filename;
        this.data = Arrays.copyOf(data, data.length);
    }


    public String getPath() {
        return this.path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getFilename() {
        return this.filename;
    }


    public byte[] getData() {
        return this.data;
    }

    public void setData(final byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    public String fullPath() {
        return Paths.get(this.path, this.filename)
                    .toString();
    }
}
