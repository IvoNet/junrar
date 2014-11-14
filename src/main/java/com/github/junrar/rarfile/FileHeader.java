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

package com.github.junrar.rarfile;

import com.github.junrar.io.Raw;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.Date;

public class FileHeader extends BlockHeader {

    private static final byte SALT_SIZE = 8;
    private static final byte NEWLHD_SIZE = 32;
    private final Log logger = LogFactory.getLog(FileHeader.class.getName());
    private final HostSystem hostOS;
    private final int fileCRC;
    private final int fileTime;
    private final int highPackSize;
    private final byte[] fileNameBytes;
    private final byte[] salt = new byte[SALT_SIZE];
    private long unpSize;
    private byte unpVersion;
    private byte unpMethod;
    private short nameSize;
    private int highUnpackSize;
    private String fileName;
    private String fileNameW;
    private byte[] subData;
    private Date mTime;

    private Date cTime;

    private Date aTime;

    private Date arcTime;

    private long fullPackSize;

    private long fullUnpackSize;

    private int fileAttr;

    private int subFlags; // same as fileAttr (in header)

    private int recoverySectors = -1;

    public FileHeader(final BlockHeader bh, final byte[] fileHeader) {
        super(bh);

        int position = 0;
        this.unpSize = Raw.readIntLittleEndian(fileHeader, position);
        position += 4;
        this.hostOS = HostSystem.findHostSystem(fileHeader[4]);
        position++;

        this.fileCRC = Raw.readIntLittleEndian(fileHeader, position);
        position += 4;

        this.fileTime = Raw.readIntLittleEndian(fileHeader, position);
        position += 4;

        this.unpVersion |= fileHeader[13] & 0xff;
        position++;
        this.unpMethod |= fileHeader[14] & 0xff;
        position++;
        this.nameSize = Raw.readShortLittleEndian(fileHeader, position);
        position += 2;

        this.fileAttr = Raw.readIntLittleEndian(fileHeader, position);
        position += 4;
        if (isLargeBlock()) {
            this.highPackSize = Raw.readIntLittleEndian(fileHeader, position);
            position += 4;

            this.highUnpackSize = Raw.readIntLittleEndian(fileHeader, position);
            position += 4;
        } else {
            this.highPackSize = 0;
            this.highUnpackSize = 0;
            if (this.unpSize == 0xffffffff) {

                this.unpSize = 0xffffffff;
                this.highUnpackSize = Integer.MAX_VALUE;
            }

        }
        this.fullPackSize |= this.highPackSize;
        this.fullPackSize <<= 32;
        this.fullPackSize |= getPackSize();

        this.fullUnpackSize |= this.highUnpackSize;
        this.fullUnpackSize <<= 32;
        this.fullUnpackSize += this.unpSize;

        this.nameSize = (this.nameSize > (4 * 1024)) ? (4 * 1024) : this.nameSize;

        this.fileNameBytes = new byte[this.nameSize];
        for (int i = 0; i < this.nameSize; i++) {
            this.fileNameBytes[i] = fileHeader[position];
            position++;
        }

        if (isFileHeader()) {
            if (isUnicode()) {
                int length = 0;
                this.fileName = "";
                this.fileNameW = "";
                while ((length < this.fileNameBytes.length) && (this.fileNameBytes[length] != 0)) {
                    length++;
                }
                final byte[] name = new byte[length];
                System.arraycopy(this.fileNameBytes, 0, name, 0, name.length);
                this.fileName = new String(name);
                if (length != this.nameSize) {
                    length++;
                    this.fileNameW = FileNameDecoder.decode(this.fileNameBytes, length);
                }
            } else {
                this.fileName = new String(this.fileNameBytes);
                this.fileNameW = "";
            }
        }

        if (UnrarHeadertype.NEW_SUB_HEADER.equals(this.headerType)) {
            int datasize = this.headerSize - NEWLHD_SIZE - this.nameSize;
            if (hasSalt()) {
                datasize -= SALT_SIZE;
            }
            if (datasize > 0) {
                this.subData = new byte[datasize];
                for (int i = 0; i < datasize; i++) {
                    this.subData[i] = (fileHeader[position]);
                    position++;
                }
            }

            if (NewSubHeaderType.SUBHEAD_TYPE_RR.byteEquals(this.fileNameBytes)) {
                this.recoverySectors =
                        this.subData[8] + (this.subData[9] << 8) + (this.subData[10] << 16) + (this.subData[11] << 24);
            }
        }

        if (hasSalt()) {
            for (int i = 0; i < SALT_SIZE; i++) {
                this.salt[i] = fileHeader[position];
                position++;
            }
        }
        this.mTime = getDateDos(this.fileTime);
        // TODO rartime -> extended

    }

    @Override
    public void print() {
        super.print();
        this.logger.info("unpSize: " + getUnpSize() + "\nHostOS: " + this.hostOS.name() + "\nMDate: " + this.mTime
                         + "\nFileName: " + getFileNameString() + "\nunpMethod: " + Integer.toHexString(getUnpMethod())
                         + "\nunpVersion: " + Integer.toHexString(getUnpVersion()) + "\nfullpackedsize: "
                         + getFullPackSize() + "\nfullunpackedsize: " + getFullUnpackSize() + "\nisEncrypted: "
                         + isEncrypted() + "\nisfileHeader: " + isFileHeader() + "\nisSolid: " + isSolid()
                         + "\nisSplitafter: " + isSplitAfter() + "\nisSplitBefore:" + isSplitBefore() + "\nunpSize: "
                         + getUnpSize() + "\ndataSize: " + getDataSize() + "\nisUnicode: " + isUnicode()
                         + "\nhasVolumeNumber: " + hasVolumeNumber() + "\nhasArchiveDataCRC: " + hasArchiveDataCRC()
                         + "\nhasSalt: " + hasSalt() + "\nhasEncryptVersions: " + hasEncryptVersion() + "\nisSubBlock: "
                         + isSubBlock());
    }

    private Date getDateDos(final int time) {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, (time >>> 25) + 1980);
        cal.set(Calendar.MONTH, ((time >>> 21) & 0x0f) - 1);
        cal.set(Calendar.DAY_OF_MONTH, (time >>> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (time >>> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (time >>> 5) & 0x3f);
        cal.set(Calendar.SECOND, (time & 0x1f) * 2);
        return cal.getTime();
    }

    public Date getArcTime() {
        return this.arcTime;
    }

    public void setArcTime(final Date arcTime) {
        this.arcTime = arcTime;
    }

    public Date getATime() {
        return this.aTime;
    }

    public void setATime(final Date time) {
        this.aTime = time;
    }

    public Date getCTime() {
        return this.cTime;
    }

    public void setCTime(final Date time) {
        this.cTime = time;
    }

    public int getFileAttr() {
        return this.fileAttr;
    }

    public void setFileAttr(final int fileAttr) {
        this.fileAttr = fileAttr;
    }

    public int getFileCRC() {
        return this.fileCRC;
    }

    public byte[] getFileNameByteArray() {
        return this.fileNameBytes;
    }

    public String getFileNameString() {
        return this.fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getFileNameW() {
        return this.fileNameW;
    }

    public void setFileNameW(final String fileNameW) {
        this.fileNameW = fileNameW;
    }

    public int getHighPackSize() {
        return this.highPackSize;
    }

    public int getHighUnpackSize() {
        return this.highUnpackSize;
    }

    public HostSystem getHostOS() {
        return this.hostOS;
    }

    public Date getMTime() {
        return this.mTime;
    }

    public void setMTime(final Date time) {
        this.mTime = time;
    }

    public short getNameSize() {
        return this.nameSize;
    }

    public int getRecoverySectors() {
        return this.recoverySectors;
    }

    public byte[] getSalt() {
        return this.salt;
    }

    public byte[] getSubData() {
        return this.subData;
    }

    public int getSubFlags() {
        return this.subFlags;
    }

    public byte getUnpMethod() {
        return this.unpMethod;
    }

    long getUnpSize() {
        return this.unpSize;
    }

    public byte getUnpVersion() {
        return this.unpVersion;
    }

    public long getFullPackSize() {
        return this.fullPackSize;
    }

    public long getFullUnpackSize() {
        return this.fullUnpackSize;
    }


    /**
     * the file will be continued in the next archive part
     */
    public boolean isSplitAfter() {
        return (this.flags & LHD_SPLIT_AFTER) != 0;
    }

    /**
     * the file is continued in this archive
     */
    boolean isSplitBefore() {
        return (this.flags & LHD_SPLIT_BEFORE) != 0;
    }

    /**
     * this file is compressed as solid (all files handeled as one)
     */
    public boolean isSolid() {
        return (this.flags & LHD_SOLID) != 0;
    }

    /**
     * the file is encrypted
     */
    public boolean isEncrypted() {
        return (this.flags & LHD_PASSWORD) != 0;
    }

    /**
     * the filename is also present in unicode
     */
    public boolean isUnicode() {
        return (this.flags & LHD_UNICODE) != 0;
    }

    public boolean isFileHeader() {
        return UnrarHeadertype.FILE_HEADER.equals(this.headerType);
    }

    boolean hasSalt() {
        return (this.flags & LHD_SALT) != 0;
    }

    boolean isLargeBlock() {
        return (this.flags & LHD_LARGE) != 0;
    }

    /**
     * whether this fileheader represents a directory
     */
    public boolean isDirectory() {
        return (this.flags & LHD_WINDOWMASK) == LHD_DIRECTORY;
    }
}
