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

package com.github.junrar;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.io.ReadOnlyAccess;
import com.github.junrar.io.ReadOnlyAccessFile;
import com.github.junrar.rarfile.AVHeader;
import com.github.junrar.rarfile.BaseBlock;
import com.github.junrar.rarfile.BlockHeader;
import com.github.junrar.rarfile.CommentHeader;
import com.github.junrar.rarfile.EAHeader;
import com.github.junrar.rarfile.EndArcHeader;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.rarfile.MacInfoHeader;
import com.github.junrar.rarfile.MainHeader;
import com.github.junrar.rarfile.MarkHeader;
import com.github.junrar.rarfile.ProtectHeader;
import com.github.junrar.rarfile.SignHeader;
import com.github.junrar.rarfile.SubBlockHeader;
import com.github.junrar.rarfile.UnixOwnersHeader;
import com.github.junrar.rarfile.UnrarHeadertype;
import com.github.junrar.unpack.ComprDataIO;
import com.github.junrar.unpack.Unpack;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Archive implements Closeable {

    private static final Logger logger = Logger.getLogger(Archive.class.getName());
    private final UnrarCallback unrarCallback;
    private final ComprDataIO dataIO;
    private final List<BaseBlock> headers = new ArrayList<>();
    /**
     * Archive data CRC.
     */
    private final long arcDataCRC = 0xffffffff;
    private File file;
    private ReadOnlyAccess rof;
    private MarkHeader markHead;
    private MainHeader newMhd;
    private Unpack unpack;
    private int currentHeaderIndex;

    /**
     * Size of packed data in current file.
     */
    private long totalPackedSize;

    /**
     * Number of bytes of compressed data read from current file.
     */
    private long totalPackedRead;

    public Archive(final File file) throws RarException, IOException {
        this(file, null);
    }

    /**
     * create a new archive object using the given file
     *
     * @param file the file to extract
     */
    private Archive(final File file, final UnrarCallback unrarCallback) throws IOException {
        setFile(file);
        this.unrarCallback = unrarCallback;
        this.dataIO = new ComprDataIO(this);
    }

    public File getFile() {
        return this.file;
    }

    public void setFile(final File file) throws IOException {
        this.file = file;
        this.totalPackedSize = 0L;
        this.totalPackedRead = 0L;
        close();
        this.rof = new ReadOnlyAccessFile(file);
        try {
            readHeaders();
        } catch (final Exception e) {
            logger.log(Level.WARNING, "exception in archive constructor maybe file is encrypted " + "or currupt", e);
            // ignore exceptions to allow exraction of working files in
            // corrupt archive
        }
        // Calculate size of packed data
        this.headers.stream()
                    .filter(block -> block.getHeaderType() == UnrarHeadertype.FILE_HEADER)
                    .forEach(block -> {
                        this.totalPackedSize += ((FileHeader) block).getFullPackSize();
                    });
        if (this.unrarCallback != null) {
            this.unrarCallback.volumeProgressChanged(this.totalPackedRead, this.totalPackedSize);
        }
    }

    public void bytesReadRead(final int count) {
        if (count > 0) {
            this.totalPackedRead += count;
            if (this.unrarCallback != null) {
                this.unrarCallback.volumeProgressChanged(this.totalPackedRead, this.totalPackedSize);
            }
        }
    }

    public ReadOnlyAccess getRof() {
        return this.rof;
    }

    /**
     * @return returns all file headers of the archive
     */
    public List<FileHeader> getFileHeaders() {
        return this.headers.stream()
                           .filter(block -> block.getHeaderType() == UnrarHeadertype.FILE_HEADER)
                           .map(block -> (FileHeader) block)
                           .collect(Collectors.toList());
    }

    public FileHeader nextFileHeader() {
        final int n = this.headers.size();
        while (this.currentHeaderIndex < n) {
            final BaseBlock block = this.headers.get(this.currentHeaderIndex++);
            if (block.getHeaderType() == UnrarHeadertype.FILE_HEADER) {
                return (FileHeader) block;
            }
        }
        return null;
    }

    public UnrarCallback getUnrarCallback() {
        return this.unrarCallback;
    }

    /**
     * @return whether the archive is encrypted
     */
    public boolean isEncrypted() {
        if (this.newMhd != null) {
            return this.newMhd.isEncrypted();
        } else {
            throw new NullPointerException("mainheader is null");
        }
    }

    /**
     * Read the headers of the archive
     */
    private void readHeaders() throws IOException, RarException {
        this.markHead = null;
        this.newMhd = null;
        this.headers.clear();
        this.currentHeaderIndex = 0;
        int toRead;

        final long fileLength = this.file.length();

        while (true) {
            final int size;
            final long newpos;
            final byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

            final long position = this.rof.getPosition();

            // Weird, but is trying to read beyond the end of the file
            if (position >= fileLength) {
                break;
            }

            // logger.info("\n--------reading header--------");
            size = this.rof.readFully(baseBlockBuffer, BaseBlock.BaseBlockSize);
            if (size == 0) {
                break;
            }
            final BaseBlock block = new BaseBlock(baseBlockBuffer);

            block.setPositionInFile(position);

            switch (block.getHeaderType()) {

                case MARK_HEADER:
                    this.markHead = new MarkHeader(block);
                    if (!this.markHead.isSignature()) {
                        throw new RarException(RarExceptionType.BAD_RAR_ARCHIVE);
                    }
                    this.headers.add(this.markHead);
                    // markHead.print();
                    break;

                case MAIN_HEADER:
                    int mainHeaderSize = 0;
                    toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc : MainHeader.mainHeaderSize;
                    final byte[] mainbuff = new byte[toRead];
                    mainHeaderSize = this.rof.readFully(mainbuff, toRead);
                    final MainHeader mainhead = new MainHeader(block, mainbuff);
                    this.headers.add(mainhead);
                    this.newMhd = mainhead;
                    if (this.newMhd.isEncrypted()) {
                        throw new RarException(RarExceptionType.RAR_ENCRYPTED_EXCEPTION);
                    }
                    // mainhead.print();
                    break;

                case SIGN_HEADER:
                    int signHeaderSize = 0;
                    toRead = SignHeader.signHeaderSize;
                    final byte[] signBuff = new byte[toRead];
                    signHeaderSize = this.rof.readFully(signBuff, toRead);
                    final SignHeader signHead = new SignHeader(block, signBuff);
                    this.headers.add(signHead);
                    // logger.info("HeaderType: SIGN_HEADER");

                    break;

                case AV_HEADER:
                    int avHeaderSize = 0;
                    toRead = AVHeader.avHeaderSize;
                    final byte[] avBuff = new byte[toRead];
                    avHeaderSize = this.rof.readFully(avBuff, toRead);
                    final AVHeader avHead = new AVHeader(block, avBuff);
                    this.headers.add(avHead);
                    // logger.info("headertype: AVHeader");
                    break;

                case COMM_HEADER:
                    int commHeaderSize = 0;
                    toRead = CommentHeader.commentHeaderSize;
                    final byte[] commBuff = new byte[toRead];
                    commHeaderSize = this.rof.readFully(commBuff, toRead);
                    final CommentHeader commHead = new CommentHeader(block, commBuff);
                    this.headers.add(commHead);
                    newpos = commHead.getPositionInFile() + commHead.getHeaderSize();
                    this.rof.setPosition(newpos);

                    break;
                case END_ARC_HEADER:

                    toRead = 0;
                    if (block.hasArchiveDataCRC()) {
                        toRead += EndArcHeader.endArcArchiveDataCrcSize;
                    }
                    if (block.hasVolumeNumber()) {
                        toRead += EndArcHeader.endArcVolumeNumberSize;
                    }
                    final EndArcHeader endArcHead;
                    if (toRead > 0) {
                        int endArcHeaderSize = 0;
                        final byte[] endArchBuff = new byte[toRead];
                        endArcHeaderSize = this.rof.readFully(endArchBuff, toRead);
                        endArcHead = new EndArcHeader(block, endArchBuff);
                    } else {
                        endArcHead = new EndArcHeader(block, null);
                    }
                    this.headers.add(endArcHead);
                    return;

                default:
                    final byte[] blockHeaderBuffer = new byte[BlockHeader.blockHeaderSize];
                    final int bhsize = this.rof.readFully(blockHeaderBuffer, BlockHeader.blockHeaderSize);
                    final BlockHeader blockHead = new BlockHeader(block, blockHeaderBuffer);

                    switch (blockHead.getHeaderType()) {
                        case NEW_SUB_HEADER:
                        case FILE_HEADER:
                            toRead =
                                    blockHead.getHeaderSize() - BlockHeader.BaseBlockSize - BlockHeader.blockHeaderSize;
                            final byte[] fileHeaderBuffer = new byte[toRead];
                            final int fhsize = this.rof.readFully(fileHeaderBuffer, toRead);

                            final FileHeader fh = new FileHeader(blockHead, fileHeaderBuffer);
                            this.headers.add(fh);
                            newpos = fh.getPositionInFile() + fh.getHeaderSize() + fh.getFullPackSize();
                            this.rof.setPosition(newpos);
                            break;

                        case PROTECT_HEADER:
                            toRead =
                                    blockHead.getHeaderSize() - BlockHeader.BaseBlockSize - BlockHeader.blockHeaderSize;
                            final byte[] protectHeaderBuffer = new byte[toRead];
                            final int phsize = this.rof.readFully(protectHeaderBuffer, toRead);
                            final ProtectHeader ph = new ProtectHeader(blockHead, protectHeaderBuffer);

                            newpos = ph.getPositionInFile() + ph.getHeaderSize();
                            this.rof.setPosition(newpos);
                            break;

                        case SUB_HEADER:
                            final byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                            final int subheadersize = this.rof.readFully(subHeadbuffer,
                                                                         SubBlockHeader.SubBlockHeaderSize);
                            final SubBlockHeader subHead = new SubBlockHeader(blockHead, subHeadbuffer);
                            subHead.print();
                            switch (subHead.getSubType()) {
                                case MAC_HEAD:
                                    final byte[] macHeaderbuffer = new byte[MacInfoHeader.MacInfoHeaderSize];
                                    final int macheadersize = this.rof.readFully(macHeaderbuffer,
                                                                                 MacInfoHeader.MacInfoHeaderSize);
                                    final MacInfoHeader macHeader = new MacInfoHeader(subHead, macHeaderbuffer);
                                    macHeader.print();
                                    this.headers.add(macHeader);

                                    break;
                                // TODO implement other subheaders
                                case BEEA_HEAD:
                                    break;
                                case EA_HEAD:
                                    final byte[] eaHeaderBuffer = new byte[EAHeader.EAHeaderSize];
                                    final int eaheadersize = this.rof.readFully(eaHeaderBuffer, EAHeader.EAHeaderSize);
                                    final EAHeader eaHeader = new EAHeader(subHead, eaHeaderBuffer);
                                    eaHeader.print();
                                    this.headers.add(eaHeader);

                                    break;
                                case NTACL_HEAD:
                                    break;
                                case STREAM_HEAD:
                                    break;
                                case UO_HEAD:
                                    toRead = subHead.getHeaderSize();
                                    toRead -= BaseBlock.BaseBlockSize;
                                    toRead -= BlockHeader.blockHeaderSize;
                                    toRead -= SubBlockHeader.SubBlockHeaderSize;
                                    final byte[] uoHeaderBuffer = new byte[toRead];
                                    final int uoHeaderSize = this.rof.readFully(uoHeaderBuffer, toRead);
                                    final UnixOwnersHeader uoHeader = new UnixOwnersHeader(subHead, uoHeaderBuffer);
                                    uoHeader.print();
                                    this.headers.add(uoHeader);
                                    break;
                                default:
                                    break;
                            }

                            break;
                        default:
                            logger.warning("Unknown Header");
                            throw new RarException(RarExceptionType.NOT_RAR_ARCHIVE);

                    }
            }
        }
    }

    /**
     * Extract the file specified by the given header and write it to the supplied output stream
     */
    public void extractFile(final FileHeader fileHeader, final OutputStream os) throws RarException {
        if (!this.headers.contains(fileHeader)) {
            throw new RarException(RarExceptionType.HEADER_NOT_IN_ARCHIVE);
        }
        try {
            doExtractFile(fileHeader, os);
        } catch (final Exception e) {
            if (e instanceof RarException) {
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    private void doExtractFile(final FileHeader fileHeader, final OutputStream os) throws RarException, IOException {
        FileHeader hd1 = fileHeader;
        this.dataIO.init(os);
        this.dataIO.init(hd1);
        this.dataIO.setUnpFileCRC(this.isOldFormat() ? 0 : 0xffFFffFF);
        if (this.unpack == null) {
            this.unpack = new Unpack(this.dataIO);
        }
        if (!hd1.isSolid()) {
            this.unpack.init(null);
        }
        this.unpack.setDestSize(hd1.getFullUnpackSize());
        try {
            this.unpack.doUnpack(hd1.getUnpVersion(), hd1.isSolid());
            // Verify file CRC
            hd1 = this.dataIO.getSubHeader();
            final long actualCRC = hd1.isSplitAfter() ? ~this.dataIO.getPackedCRC() : ~this.dataIO.getUnpFileCRC();
            final int expectedCRC = hd1.getFileCRC();
            if (actualCRC != expectedCRC) {
                throw new RarException(RarExceptionType.CRC_ERROR);
            }
        } catch (final RarException e) {
            this.unpack.cleanUp();
            throw e;
        }
    }

    /**
     * @return returns the main header of this archive
     */
    public MainHeader getMainHeader() {
        return this.newMhd;
    }

    /**
     * @return whether the archive is old format
     */
    public boolean isOldFormat() {
        return this.markHead.isOldFormat();
    }

    /**
     * Close the underlying compressed file.
     */
    @Override
    public void close() throws IOException {
        if (this.rof != null) {
            this.rof.close();
            this.rof = null;
        }
        if (this.unpack != null) {
            this.unpack.cleanUp();
        }
    }
}
