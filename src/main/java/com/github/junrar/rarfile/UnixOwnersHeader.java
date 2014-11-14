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

package com.github.junrar.rarfile;

import com.github.junrar.io.Raw;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UnixOwnersHeader extends SubBlockHeader {
    private final Log logger = LogFactory.getLog(UnixOwnersHeader.class);
    private int ownerNameSize;
    private int groupNameSize;
    private String owner;
    private String group;

    public UnixOwnersHeader(final SubBlockHeader sb, final byte[] uoHeader) {
        super(sb);
        int pos = 0;
        this.ownerNameSize = Raw.readShortLittleEndian(uoHeader, pos) & 0xFFFF;
        pos += 2;
        this.groupNameSize = Raw.readShortLittleEndian(uoHeader, pos) & 0xFFFF;
        pos += 2;
        if ((pos + this.ownerNameSize) < uoHeader.length) {
            final byte[] ownerBuffer = new byte[this.ownerNameSize];
            System.arraycopy(uoHeader, pos, ownerBuffer, 0, this.ownerNameSize);
            this.owner = new String(ownerBuffer);
        }
        pos += this.ownerNameSize;
        if ((pos + this.groupNameSize) < uoHeader.length) {
            final byte[] groupBuffer = new byte[this.groupNameSize];
            System.arraycopy(uoHeader, pos, groupBuffer, 0, this.groupNameSize);
            this.group = new String(groupBuffer);
        }
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(final String group) {
        this.group = group;
    }

    /**
     * @return the groupNameSize
     */
    public int getGroupNameSize() {
        return this.groupNameSize;
    }

    /**
     * @param groupNameSize the groupNameSize to set
     */
    public void setGroupNameSize(final int groupNameSize) {
        this.groupNameSize = groupNameSize;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    /**
     * @return the ownerNameSize
     */
    public int getOwnerNameSize() {
        return this.ownerNameSize;
    }

    /**
     * @param ownerNameSize the ownerNameSize to set
     */
    public void setOwnerNameSize(final int ownerNameSize) {
        this.ownerNameSize = ownerNameSize;
    }

    @Override
    public void print() {
        super.print();
        this.logger.info("ownerNameSize: " + this.ownerNameSize);
        this.logger.info("owner: " + this.owner);
        this.logger.info("groupNameSize: " + this.groupNameSize);
        this.logger.info("group: " + this.group);
    }
}
