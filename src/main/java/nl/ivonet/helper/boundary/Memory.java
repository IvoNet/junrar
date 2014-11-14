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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Ivo Woltring
 */
public class Memory {
    private final Map<String, Resource> resources;

    public Memory() {
        this.resources = new TreeMap<>();
    }

    public void add(final Resource resource) {
        this.resources.put(resource.fullPath(), resource);
    }

    public Resource get(final String key) {
        return this.resources.get(key);
    }

    public Set<String> keys() {
        return this.resources.keySet();
    }

    public Collection<Resource> values() {
        return this.resources.values();
    }

}
