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

package com.github.junrar.unpack;

import com.github.junrar.Archive;
import com.github.junrar.UnrarCallback;
import com.github.junrar.crc.RarCRC;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.io.ReadOnlyAccessInputStream;
import com.github.junrar.rarfile.FileHeader;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ComprDataIO {
    private final RarCRC rarCRC;
    private final Archive archive;
    private long unpPackedSize;
    private boolean testMode;
    private boolean skipUnpCRC;
    private InputStream inputStream;
    private OutputStream outputStream;
    private FileHeader subHead;
    private long unpFileCRC;
    private long packedCRC;

    public ComprDataIO(final Archive arc) {
        this.archive = arc;
        this.rarCRC = new RarCRC();
    }

    private static boolean isDigit(final char c) {
        return (c >= '0') && (c <= '9');
    }

    private static boolean mergeArchive(final Archive archive, final ComprDataIO dataIO) throws IOException {
        FileHeader hd = dataIO.getSubHeader();
        if ((hd.getUnpVersion() >= 20) &&
            (hd.getFileCRC() != 0xffffffff) &&
            (dataIO.getPackedCRC() != ~hd.getFileCRC())) {
            throw new RuntimeException(new RarException(RarExceptionType.CRC_ERROR));
        }

        final boolean oldNumbering = !archive.getMainHeader()
                                             .isNewNumbering() || archive.isOldFormat();
        final String nextName = nextVolumeName(archive.getFile()
                                                      .getAbsolutePath(), oldNumbering);
        final File nextVolume = new File(nextName);
        final UnrarCallback callback = archive.getUnrarCallback();
        if ((callback != null) && !callback.isNextVolumeReady(nextVolume)) {
            return false;
        }
        if (!nextVolume.exists()) {
            return false;
        }
        archive.setFile(nextVolume);
        hd = archive.nextFileHeader();
        if (hd == null) {
            return false;
        }
        dataIO.init(hd);
        return true;
    }

    private static String nextVolumeName(final String arcName, final boolean oldNumbering) {
        if (oldNumbering) {
            // .rar, .r00, .r01, ...
            final int len = arcName.length();
            if ((len <= 4) || (arcName.charAt(len - 4) != '.')) {
                return null;
            }
            final StringBuilder buffer = new StringBuilder();
            final int off = len - 3;
            buffer.append(arcName, 0, off);
            if (!isDigit(arcName.charAt(off + 1)) || !isDigit(arcName.charAt(off + 2))) {
                buffer.append("r00");
            } else {
                final char[] ext = new char[3];
                arcName.getChars(off, len, ext, 0);
                int i = ext.length - 1;
                while ((++ext[i]) == ('9' + 1)) {
                    ext[i] = '0';
                    i--;
                }
                buffer.append(ext);
            }
            return buffer.toString();
        } else {
            // part1.rar, part2.rar, ...
            final int len = arcName.length();
            int indexR = len - 1;
            while ((indexR >= 0) && !isDigit(arcName.charAt(indexR))) {
                indexR--;
            }
            final int index = indexR + 1;
            int indexL = indexR - 1;
            while ((indexL >= 0) && isDigit(arcName.charAt(indexL))) {
                indexL--;
            }
            if (indexL < 0) {
                return null;
            }
            indexL++;
            final StringBuilder buffer = new StringBuilder(len);
            buffer.append(arcName, 0, indexL);
            final char[] digits = new char[(indexR - indexL) + 1];
            arcName.getChars(indexL, indexR + 1, digits, 0);
            indexR = digits.length - 1;
            while ((indexR >= 0) && ((++digits[indexR]) == ('9' + 1))) {
                digits[indexR] = '0';
                indexR--;
            }
            if (indexR < 0) {
                buffer.append('1');
            }
            buffer.append(digits);
            buffer.append(arcName, index, len);
            return buffer.toString();
        }
    }

    public void init(final OutputStream outputStream) {
        this.outputStream = outputStream;
        this.unpPackedSize = 0;
        this.testMode = false;
        this.skipUnpCRC = false;
        this.unpFileCRC = 0xffffffff;
        this.packedCRC = 0xffffffff;
        this.subHead = null;
    }

    public void init(final FileHeader hd) throws IOException {
        final long startPos = hd.getPositionInFile() + hd.getHeaderSize();
        this.unpPackedSize = hd.getFullPackSize();
        this.inputStream = new ReadOnlyAccessInputStream(this.archive.getRof(), startPos,
                                                         startPos + this.unpPackedSize);
        this.subHead = hd;
        this.packedCRC = 0xFFffFFff;
    }

    public int unpRead(final byte[] addr, final int offset, final int count) throws IOException, RarException {
        int theCount = count;
        int theOffset = offset;
        int retCode = 0;
        int totalRead = 0;
        while (theCount > 0) {
            final int readSize = (theCount > this.unpPackedSize) ? (int) this.unpPackedSize : theCount;
            retCode = this.inputStream.read(addr, theOffset, readSize);
            if (retCode < 0) {
                throw new EOFException();
            }
            if (this.subHead.isSplitAfter()) {
                this.packedCRC = this.rarCRC.checkCrc((int) this.packedCRC, addr, theOffset, retCode);
            }

            totalRead += retCode;
            theOffset += retCode;
            theCount -= retCode;
            this.unpPackedSize -= retCode;
            this.archive.bytesReadRead(retCode);
            if ((this.unpPackedSize == 0) && this.subHead.isSplitAfter()) {
                if (!mergeArchive(this.archive, this)) {
                    return -1;
                }
            } else {
                break;
            }
        }

        if (retCode != -1) {
            retCode = totalRead;
        }
        return retCode;
    }

    public void unpWrite(final byte[] address, final int offset, final int count) throws IOException {
        if (!this.testMode) {
            this.outputStream.write(address, offset, count);
        }

        if (!this.skipUnpCRC) {
            this.unpFileCRC = this.archive.isOldFormat() ? this.rarCRC.checkOldCrc((short) this.unpFileCRC, address,
                                                                                   count) : this.rarCRC.checkCrc(
                    (int) this.unpFileCRC, address, offset, count);
        }
    }

    public long getPackedCRC() {
        return this.packedCRC;
    }

    public long getUnpFileCRC() {
        return this.unpFileCRC;
    }

    public void setUnpFileCRC(final long unpFileCRC) {
        this.unpFileCRC = unpFileCRC;
    }

    public FileHeader getSubHeader() {
        return this.subHead;
    }
}
