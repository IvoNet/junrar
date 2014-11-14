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

import com.github.junrar.exception.RarException;
import com.github.junrar.unpack.decode.Compress;
import com.github.junrar.unpack.ppm.BlockTypes;
import com.github.junrar.unpack.ppm.ModelPPM;
import com.github.junrar.unpack.ppm.SubAllocator;
import com.github.junrar.unpack.vm.BitInput;
import com.github.junrar.unpack.vm.RarVM;
import com.github.junrar.unpack.vm.VMPreparedProgram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


public final class Unpack extends Unpack20 {

    private static final int[] DBitLengthCounts = {
            4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 14, 0, 12
    };
    private final ModelPPM ppm = new ModelPPM();
    private final RarVM rarVM = new RarVM();

    /* Filters code, one entry per filter */
    private final List<UnpackFilter> filters = new ArrayList<>();

    /* Filters stack, several entrances of same filter are possible */
    private final List<UnpackFilter> prgStack = new ArrayList<>();

    /*
     * lengths of preceding blocks, one length per filter. Used to reduce size
     * required to write block length if lengths are repeating
     */
    private final List<Integer> oldFilterLengths = new ArrayList<>();
    private final byte[] unpOldTable = new byte[Compress.HUFF_TABLE_SIZE];
    private int ppmEscChar;
    private int lastFilter;
    private boolean tablesRead;
    private BlockTypes unpBlockType;
    private long writtenFileSize;
    private boolean fileExtracted;
    private boolean ppmError;
    private int prevLowDist;
    private int lowDistRepCount;

    public Unpack(final ComprDataIO DataIO) {
        this.unpIO = DataIO;
        this.window = null;
        this.suspended = false;
        this.unpAllBuf = false;
        this.unpSomeRead = false;
    }

    public void init(final byte[] window) {
        //noinspection UnclearBinaryExpression
        this.window = window == null ? new byte[Compress.MAXWINSIZE] : window;
        this.inAddr = 0;
        unpInitData(false);
    }

    public void doUnpack(final int method, final boolean solid) throws IOException, RarException {
        if (this.unpIO.getSubHeader()
                      .getUnpMethod() == 0x30) {
            unstoreFile();
        }
        switch (method) {
            case 15: // rar 1.5 compression
                unpack15(solid);
                break;
            case 20: // rar 2.x compression
            case 26: // files larger than 2GB
                unpack20(solid);
                break;
            case 29: // rar 3.x compression
            case 36: // alternative hash
                unpack29(solid);
                break;
        }
    }

    private void unstoreFile() throws IOException, RarException {
        final byte[] buffer = new byte[0x10000];
        while (true) {
            int code = this.unpIO.unpRead(buffer, 0, (int) Math.min(buffer.length, this.destUnpSize));
            if (code == 0 || code == -1) {
                break;
            }
            code = code < this.destUnpSize ? code : (int) this.destUnpSize;
            this.unpIO.unpWrite(buffer, 0, code);
            if (this.destUnpSize >= 0) {
                this.destUnpSize -= code;
            }
        }

    }

    private void unpack29(final boolean solid) throws IOException, RarException {

        final int[] DDecode = new int[Compress.DC];
        final byte[] DBits = new byte[Compress.DC];

        int Bits;

        if (DDecode[1] == 0) {
            int Dist = 0;
            int BitLength = 0;
            int Slot = 0;
            for (int I = 0; I < DBitLengthCounts.length; I++, BitLength++) {
                final int count = DBitLengthCounts[I];
                for (int J = 0; J < count; J++, Slot++, Dist += (1 << BitLength)) {
                    DDecode[Slot] = Dist;
                    DBits[Slot] = (byte) BitLength;
                }
            }
        }

        this.fileExtracted = true;

        if (!this.suspended) {
            unpInitData(solid);
            if (!unpReadBuf()) {
                return;
            }
            if ((!solid || !this.tablesRead) && !readTables()) {
                return;
            }
        }

        if (this.ppmError) {
            return;
        }

        while (true) {
            this.unpPtr &= Compress.MAXWINMASK;

            if (this.inAddr > this.readBorder) {
                if (!unpReadBuf()) {
                    break;
                }
            }
            if ((((this.wrPtr - this.unpPtr) & Compress.MAXWINMASK) < 260) && (this.wrPtr != this.unpPtr)) {

                UnpWriteBuf();
                if (this.writtenFileSize > this.destUnpSize) {
                    return;
                }
                if (this.suspended) {
                    this.fileExtracted = false;
                    return;
                }
            }
            if (this.unpBlockType == BlockTypes.BLOCK_PPM) {
                final int Ch = this.ppm.decodeChar();
                if (Ch == -1) {
                    this.ppmError = true;
                    break;
                }
                if (Ch == this.ppmEscChar) {
                    final int NextCh = this.ppm.decodeChar();
                    if (NextCh == 0) {
                        if (!readTables()) {
                            break;
                        }
                        continue;
                    }
                    if ((NextCh == 2) || (NextCh == -1)) {
                        break;
                    }
                    if (NextCh == 3) {
                        if (!readVMCodePPM()) {
                            break;
                        }
                        continue;
                    }
                    if (NextCh == 4) {
                        int Distance = 0;
                        int Length = 0;
                        boolean failed = false;
                        for (int I = 0; (I < 4) && !failed; I++) {
                            final int ch = this.ppm.decodeChar();
                            if (ch == -1) {
                                failed = true;
                            } else {
                                if (I == 3) {
                                    // Bug fixed
                                    Length = ch & 0xff;
                                } else {
                                    // Bug fixed
                                    Distance = (Distance << 8) + (ch & 0xff);
                                }
                            }
                        }
                        if (failed) {
                            break;
                        }
                        copyString(Length + 32, Distance + 2);
                        continue;
                    }
                    if (NextCh == 5) {
                        final int Length = this.ppm.decodeChar();
                        if (Length == -1) {
                            break;
                        }
                        copyString(Length + 4, 1);
                        continue;
                    }
                }
                this.window[this.unpPtr++] = (byte) Ch;
                continue;
            }

            int Number = decodeNumber(this.LD);
            if (Number < 256) {
                this.window[this.unpPtr++] = (byte) Number;
                continue;
            }
            if (Number >= 271) {
                int Length = LDecode[Number -= 271] + 3;
                if ((Bits = LBits[Number]) > 0) {
                    Length += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }

                final int DistNumber = decodeNumber(this.DD);
                int Distance = DDecode[DistNumber] + 1;
                if ((Bits = DBits[DistNumber]) > 0) {
                    if (DistNumber > 9) {
                        if (Bits > 4) {
                            Distance += ((getbits() >>> (20 - Bits)) << 4);
                            addbits(Bits - 4);
                        }
                        if (this.lowDistRepCount > 0) {
                            this.lowDistRepCount--;
                            Distance += this.prevLowDist;
                        } else {
                            final int LowDist = decodeNumber(this.LDD);
                            if (LowDist == 16) {
                                this.lowDistRepCount = Compress.LOW_DIST_REP_COUNT - 1;
                                Distance += this.prevLowDist;
                            } else {
                                Distance += LowDist;
                                this.prevLowDist = LowDist;
                            }
                        }
                    } else {
                        Distance += getbits() >>> (16 - Bits);
                        addbits(Bits);
                    }
                }

                if (Distance >= 0x2000) {
                    Length++;
                    if (Distance >= 0x40000L) {
                        Length++;
                    }
                }

                insertOldDist(Distance);
                insertLastMatch(Length, Distance);

                copyString(Length, Distance);
                continue;
            }
            if (Number == 256) {
                if (!readEndOfBlock()) {
                    break;
                }
                continue;
            }
            if (Number == 257) {
                if (!readVMCode()) {
                    break;
                }
                continue;
            }
            if (Number == 258) {
                if (this.lastLength != 0) {
                    copyString(this.lastLength, this.lastDist);
                }
                continue;
            }
            if (Number < 263) {
                final int DistNum = Number - 259;
                final int Distance = this.oldDist[DistNum];
                System.arraycopy(this.oldDist, 0, this.oldDist, 1, DistNum);
                this.oldDist[0] = Distance;

                final int LengthNumber = decodeNumber(this.RD);
                int Length = LDecode[LengthNumber] + 2;
                if ((Bits = LBits[LengthNumber]) > 0) {
                    Length += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }
                insertLastMatch(Length, Distance);
                copyString(Length, Distance);
                continue;
            }
            if (Number < 272) {
                int Distance = SDDecode[Number -= 263] + 1;
                if ((Bits = SDBits[Number]) > 0) {
                    Distance += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }
                insertOldDist(Distance);
                insertLastMatch(2, Distance);
                copyString(2, Distance);
            }
        }
        UnpWriteBuf();

    }

    private void UnpWriteBuf() throws IOException {
        int WrittenBorder = this.wrPtr;
        int WriteSize = (this.unpPtr - WrittenBorder) & Compress.MAXWINMASK;
        for (int I = 0; I < this.prgStack.size(); I++) {
            final UnpackFilter flt = this.prgStack.get(I);
            if (flt == null) {
                continue;
            }
            if (flt.isNextWindow()) {
                flt.setNextWindow(false);// ->NextWindow=false;
                continue;
            }
            final int BlockStart = flt.getBlockStart();// ->BlockStart;
            final int BlockLength = flt.getBlockLength();// ->BlockLength;
            if (((BlockStart - WrittenBorder) & Compress.MAXWINMASK) < WriteSize) {
                if (WrittenBorder != BlockStart) {
                    UnpWriteArea(WrittenBorder, BlockStart);
                    WrittenBorder = BlockStart;
                    WriteSize = (this.unpPtr - WrittenBorder) & Compress.MAXWINMASK;
                }
                if (BlockLength <= WriteSize) {
                    final int BlockEnd = (BlockStart + BlockLength) & Compress.MAXWINMASK;
                    if ((BlockStart < BlockEnd) || (BlockEnd == 0)) {
                        // VM.SetMemory(0,Window+BlockStart,BlockLength);
                        this.rarVM.setMemory(0, this.window, BlockStart, BlockLength);
                    } else {
                        final int FirstPartLength = Compress.MAXWINSIZE - BlockStart;
                        // VM.SetMemory(0,Window+BlockStart,FirstPartLength);
                        this.rarVM.setMemory(0, this.window, BlockStart, FirstPartLength);
                        // VM.SetMemory(FirstPartLength,Window,BlockEnd);
                        this.rarVM.setMemory(FirstPartLength, this.window, 0, BlockEnd);

                    }

                    final VMPreparedProgram ParentPrg = this.filters.get(flt.getParentFilter())
                                                                    .getPrg();
                    final VMPreparedProgram Prg = flt.getPrg();

                    if (ParentPrg.getGlobalData()
                                 .size() > RarVM.VM_FIXEDGLOBALSIZE) {
                        Prg.getGlobalData()
                           .setSize(ParentPrg.getGlobalData()
                                             .size());
                        for (int i = 0; i < (ParentPrg.getGlobalData()
                                                      .size() - RarVM.VM_FIXEDGLOBALSIZE); i++) {
                            Prg.getGlobalData()
                               .set(RarVM.VM_FIXEDGLOBALSIZE + i, ParentPrg.getGlobalData()
                                                                           .get(RarVM.VM_FIXEDGLOBALSIZE + i));
                        }
                    }

                    ExecuteCode(Prg);

                    if (Prg.getGlobalData()
                           .size() > RarVM.VM_FIXEDGLOBALSIZE) {
                        // save global data for next script execution
                        if (ParentPrg.getGlobalData()
                                     .size() < Prg.getGlobalData()
                                                  .size()) {
                            ParentPrg.getGlobalData()
                                     .setSize(Prg.getGlobalData()
                                                 .size());// ->GlobalData.Alloc(Prg->GlobalData.Size());
                        }
                        for (int i = 0; i < (Prg.getGlobalData()
                                                .size() - RarVM.VM_FIXEDGLOBALSIZE); i++) {
                            ParentPrg.getGlobalData()
                                     .set(RarVM.VM_FIXEDGLOBALSIZE + i, Prg.getGlobalData()
                                                                           .get(RarVM.VM_FIXEDGLOBALSIZE + i));
                        }
                    } else {
                        ParentPrg.getGlobalData()
                                 .clear();
                    }

                    int FilteredDataOffset = Prg.getFilteredDataOffset();
                    int FilteredDataSize = Prg.getFilteredDataSize();
                    byte[] FilteredData = new byte[FilteredDataSize];

                    System.arraycopy(this.rarVM.getMem(), FilteredDataOffset, FilteredData, 0, FilteredDataSize);

                    this.prgStack.set(I, null);
                    while ((I + 1) < this.prgStack.size()) {
                        final UnpackFilter NextFilter = this.prgStack.get(I + 1);
                        if (NextFilter == null || NextFilter.getBlockStart() != BlockStart
                            || NextFilter.getBlockLength() != FilteredDataSize || NextFilter.isNextWindow()) {
                            break;
                        }
                        // apply several filters to same data block

                        this.rarVM.setMemory(0, FilteredData, 0,
                                             FilteredDataSize);// .SetMemory(0,FilteredData,FilteredDataSize);

                        final VMPreparedProgram pPrg = this.filters.get(NextFilter.getParentFilter())
                                                                   .getPrg();
                        final VMPreparedProgram NextPrg = NextFilter.getPrg();

                        if (pPrg.getGlobalData()
                                .size() > RarVM.VM_FIXEDGLOBALSIZE) {
                            NextPrg.getGlobalData()
                                   .setSize(pPrg.getGlobalData()
                                                .size());
                            for (int i = 0; i < (pPrg.getGlobalData()
                                                     .size() - RarVM.VM_FIXEDGLOBALSIZE); i++) {
                                NextPrg.getGlobalData()
                                       .set(RarVM.VM_FIXEDGLOBALSIZE + i, pPrg.getGlobalData()
                                                                              .get(RarVM.VM_FIXEDGLOBALSIZE + i));
                            }
                        }

                        ExecuteCode(NextPrg);

                        if (NextPrg.getGlobalData()
                                   .size() > RarVM.VM_FIXEDGLOBALSIZE) {
                            // save global data for next script execution
                            if (pPrg.getGlobalData()
                                    .size() < NextPrg.getGlobalData()
                                                     .size()) {
                                pPrg.getGlobalData()
                                    .setSize(NextPrg.getGlobalData()
                                                    .size());
                            }
                            for (int i = 0; i < (NextPrg.getGlobalData()
                                                        .size() - RarVM.VM_FIXEDGLOBALSIZE); i++) {
                                pPrg.getGlobalData()
                                    .set(RarVM.VM_FIXEDGLOBALSIZE + i, NextPrg.getGlobalData()
                                                                              .get(RarVM.VM_FIXEDGLOBALSIZE + i));
                            }
                        } else {
                            pPrg.getGlobalData()
                                .clear();
                        }
                        FilteredDataOffset = NextPrg.getFilteredDataOffset();
                        FilteredDataSize = NextPrg.getFilteredDataSize();

                        FilteredData = new byte[FilteredDataSize];
                        for (int i = 0; i < FilteredDataSize; i++) {
                            FilteredData[i] = NextPrg.getGlobalData()
                                                     .get(FilteredDataOffset + i);
                        }

                        I++;
                        this.prgStack.set(I, null);
                    }
                    this.unpIO.unpWrite(FilteredData, 0, FilteredDataSize);
                    this.unpSomeRead = true;
                    this.writtenFileSize += FilteredDataSize;
                    WrittenBorder = BlockEnd;
                    WriteSize = (this.unpPtr - WrittenBorder) & Compress.MAXWINMASK;
                } else {
                    for (int J = I; J < this.prgStack.size(); J++) {
                        final UnpackFilter filt = this.prgStack.get(J);
                        if (filt != null && filt.isNextWindow()) {
                            filt.setNextWindow(false);
                        }
                    }
                    this.wrPtr = WrittenBorder;
                    return;
                }
            }
        }

        UnpWriteArea(WrittenBorder, this.unpPtr);
        this.wrPtr = this.unpPtr;

    }

    private void UnpWriteArea(final int startPtr, final int endPtr) throws IOException {
        if (endPtr != startPtr) {
            this.unpSomeRead = true;
        }
        if (endPtr < startPtr) {
            UnpWriteData(this.window, startPtr, -startPtr & Compress.MAXWINMASK);
            UnpWriteData(this.window, 0, endPtr);
            this.unpAllBuf = true;
        } else {
            UnpWriteData(this.window, startPtr, endPtr - startPtr);
        }
    }

    private void UnpWriteData(final byte[] data, final int offset, final int size) throws IOException {
        if (this.writtenFileSize >= this.destUnpSize) {
            return;
        }
        int writeSize = size;
        final long leftToWrite = this.destUnpSize - this.writtenFileSize;
        if (writeSize > leftToWrite) {
            writeSize = (int) leftToWrite;
        }
        this.unpIO.unpWrite(data, offset, writeSize);

        this.writtenFileSize += size;

    }

    private void insertOldDist(final int distance) {
        this.oldDist[3] = this.oldDist[2];
        this.oldDist[2] = this.oldDist[1];
        this.oldDist[1] = this.oldDist[0];
        this.oldDist[0] = distance;
    }

    private void insertLastMatch(final int length, final int distance) {
        this.lastDist = distance;
        this.lastLength = length;
    }

    private void copyString(int length, final int distance) {
        // System.out.println("copyString(" + length + ", " + distance + ")");

        int destPtr = this.unpPtr - distance;
        // System.out.println(unpPtr+":"+distance);
        if (destPtr >= 0 && destPtr < Compress.MAXWINSIZE - 260 && this.unpPtr < Compress.MAXWINSIZE - 260) {

            this.window[this.unpPtr++] = this.window[destPtr++];

            while (--length > 0)

            {
                this.window[this.unpPtr++] = this.window[destPtr++];
            }
        } else {
            while (length-- != 0) {
                this.window[this.unpPtr] = this.window[destPtr++ & Compress.MAXWINMASK];
                this.unpPtr = (this.unpPtr + 1) & Compress.MAXWINMASK;
            }
        }
    }

    @Override
    protected void unpInitData(final boolean solid) {
        if (!solid) {
            this.tablesRead = false;
            Arrays.fill(this.oldDist, 0); // memset(oldDist,0,sizeof(OldDist));

            this.oldDistPtr = 0;
            this.lastDist = 0;
            this.lastLength = 0;

            Arrays.fill(this.unpOldTable, (byte) 0);// memset(UnpOldTable,0,sizeof(UnpOldTable));

            this.unpPtr = 0;
            this.wrPtr = 0;
            this.ppmEscChar = 2;

            initFilters();
        }
        InitBitInput();
        this.ppmError = false;
        this.writtenFileSize = 0;
        this.readTop = 0;
        this.readBorder = 0;
        unpInitData20(solid);
    }

    private void initFilters() {
        this.oldFilterLengths.clear();
        this.lastFilter = 0;

        this.filters.clear();

        this.prgStack.clear();
    }

    private boolean readEndOfBlock() throws IOException, RarException {
        final int BitField = getbits();
        final boolean NewTable;
        boolean NewFile = false;
        if ((BitField & 0x8000) == 0) {
            NewFile = true;
            NewTable = ((BitField & 0x4000) != 0);
            addbits(2);
        } else {
            NewTable = true;
            addbits(1);
        }
        this.tablesRead = !NewTable;
        return !(NewFile || !readTables());
    }

    private boolean readTables() throws IOException, RarException {
        final byte[] bitLength = new byte[Compress.BC];

        final byte[] table = new byte[Compress.HUFF_TABLE_SIZE];
        if (this.inAddr > (this.readTop - 25)) {
            if (!unpReadBuf()) {
                return (false);
            }
        }
        faddbits((8 - this.inBit) & 7);
        final long bitField = fgetbits();
        if ((bitField & 0x8000) != 0) {
            this.unpBlockType = BlockTypes.BLOCK_PPM;
            return (this.ppm.decodeInit(this, this.ppmEscChar));
        }
        this.unpBlockType = BlockTypes.BLOCK_LZ;

        this.prevLowDist = 0;
        this.lowDistRepCount = 0;

        if ((bitField & 0x4000) == 0) {
            Arrays.fill(this.unpOldTable, (byte) 0);// memset(UnpOldTable,0,sizeof(UnpOldTable));
        }
        faddbits(2);

        for (int i = 0; i < Compress.BC; i++) {
            final int length = (fgetbits() >>> 12) & 0xFF;
            faddbits(4);
            if (length == 15) {
                int zeroCount = (fgetbits() >>> 12) & 0xFF;
                faddbits(4);
                if (zeroCount == 0) {
                    bitLength[i] = 15;
                } else {
                    zeroCount += 2;
                    while ((zeroCount-- > 0) && (i < bitLength.length)) {
                        bitLength[i++] = 0;
                    }
                    i--;
                }
            } else {
                bitLength[i] = (byte) length;
            }
        }

        makeDecodeTables(bitLength, 0, this.BD, Compress.BC);

        final int TableSize = Compress.HUFF_TABLE_SIZE;

        int i = 0;
        while (i < TableSize) {
            if (this.inAddr > (this.readTop - 5)) {
                if (!unpReadBuf()) {
                    return (false);
                }
            }
            final int Number = decodeNumber(this.BD);
            if (Number < 16) {
                table[i] = (byte) ((Number + this.unpOldTable[i]) & 0xf);
                i++;
            } else if (Number < 18) {
                int N;
                if (Number == 16) {
                    N = (fgetbits() >>> 13) + 3;
                    faddbits(3);
                } else {
                    N = (fgetbits() >>> 9) + 11;
                    faddbits(7);
                }
                while ((N-- > 0) && (i < TableSize)) {
                    table[i] = table[i - 1];
                    i++;
                }
            } else {
                int N;
                if (Number == 18) {
                    N = (fgetbits() >>> 13) + 3;
                    faddbits(3);
                } else {
                    N = (fgetbits() >>> 9) + 11;
                    faddbits(7);
                }
                while ((N-- > 0) && (i < TableSize)) {
                    table[i++] = 0;
                }
            }
        }
        this.tablesRead = true;
        if (this.inAddr > this.readTop) {
            return (false);
        }
        makeDecodeTables(table, 0, this.LD, Compress.NC);
        makeDecodeTables(table, Compress.NC, this.DD, Compress.DC);
        makeDecodeTables(table, Compress.NC + Compress.DC, this.LDD, Compress.LDC);
        makeDecodeTables(table, Compress.NC + Compress.DC + Compress.LDC, this.RD, Compress.RC);

        System.arraycopy(table, 0, this.unpOldTable, 0, this.unpOldTable.length);
        return (true);

    }

    private boolean readVMCode() throws IOException, RarException {
        final int FirstByte = getbits() >> 8;
        addbits(8);
        int Length = (FirstByte & 7) + 1;
        if (Length == 7) {
            Length = (getbits() >> 8) + 7;
            addbits(8);
        } else if (Length == 8) {
            Length = getbits();
            addbits(16);
        }
        final List<Byte> vmCode = new ArrayList<>();
        for (int I = 0; I < Length; I++) {
            if ((this.inAddr >= (this.readTop - 1)) && !unpReadBuf() && (I < (Length - 1))) {
                return false;
            }
            vmCode.add((byte) (getbits() >> 8));
            addbits(8);
        }
        return (addVMCode(FirstByte, vmCode));
    }

    private boolean readVMCodePPM() throws IOException, RarException {
        final int FirstByte = this.ppm.decodeChar();
        if (FirstByte == -1) {
            return (false);
        }
        int Length = (FirstByte & 7) + 1;
        if (Length == 7) {
            final int B1 = this.ppm.decodeChar();
            if (B1 == -1) {
                return (false);
            }
            Length = B1 + 7;
        } else if (Length == 8) {
            final int B1 = this.ppm.decodeChar();
            if (B1 == -1) {
                return (false);
            }
            final int B2 = this.ppm.decodeChar();
            if (B2 == -1) {
                return (false);
            }
            Length = B1 * 256 + B2;
        }
        final List<Byte> vmCode = new ArrayList<>();
        for (int I = 0; I < Length; I++) {
            final int Ch = this.ppm.decodeChar();
            if (Ch == -1) {
                return (false);
            }
            vmCode.add((byte) Ch);// VMCode[I]=Ch;
        }
        return (addVMCode(FirstByte, vmCode));
    }

    private boolean addVMCode(final int firstByte, final List<Byte> vmCode) {
        final BitInput Inp = new BitInput();
        Inp.InitBitInput();
        // memcpy(Inp.InBuf,Code,Min(BitInput::MAX_SIZE,CodeSize));
        for (int i = 0; i < Math.min(BitInput.MAX_SIZE, vmCode.size()); i++) {
            Inp.getInBuf()[i] = vmCode.get(i);
        }
        this.rarVM.init();

        int FiltPos;
        if ((firstByte & 0x80) == 0) {
            FiltPos = this.lastFilter; // use the same filter as last time
        } else {
            FiltPos = RarVM.ReadData(Inp);
            if (FiltPos == 0) {
                initFilters();
            } else {
                FiltPos--;
            }
        }

        if ((FiltPos > this.filters.size()) || (FiltPos > this.oldFilterLengths.size())) {
            return (false);
        }
        this.lastFilter = FiltPos;
        final boolean NewFilter = (FiltPos == this.filters.size());

        final UnpackFilter StackFilter = new UnpackFilter(); // new filter for
        // PrgStack

        final UnpackFilter Filter;
        if (NewFilter) // new filter code, never used before since VM reset
        {
            if (FiltPos > 1024) {
                return (false);
            }

            Filter = new UnpackFilter();
            this.filters.add(Filter);
            StackFilter.setParentFilter(this.filters.size() - 1);
            this.oldFilterLengths.add(0);
            Filter.setExecCount(0);
        } else // filter was used in the past
        {
            Filter = this.filters.get(FiltPos);
            StackFilter.setParentFilter(FiltPos);
            Filter.setExecCount(Filter.getExecCount() + 1);// ->ExecCount++;
        }

        this.prgStack.add(StackFilter);
        StackFilter.setExecCount(Filter.getExecCount());// ->ExecCount;

        int BlockStart = RarVM.ReadData(Inp);
        if ((firstByte & 0x40) != 0) {
            BlockStart += 258;
        }
        StackFilter.setBlockStart((BlockStart + this.unpPtr) & Compress.MAXWINMASK);
        if ((firstByte & 0x20) == 0) {
            StackFilter.setBlockLength((FiltPos < this.oldFilterLengths.size()) ? this.oldFilterLengths.get(FiltPos) :
                                       0);
        } else {
            StackFilter.setBlockLength(RarVM.ReadData(Inp));
        }
        StackFilter.setNextWindow(
                (this.wrPtr != this.unpPtr) && (((this.wrPtr - this.unpPtr) & Compress.MAXWINMASK) <= BlockStart));

        this.oldFilterLengths.set(FiltPos, StackFilter.getBlockLength());

        Arrays.fill(StackFilter.getPrg()
                               .getInitR(), 0);
        StackFilter.getPrg()
                   .getInitR()[3] = RarVM.VM_GLOBALMEMADDR;// StackFilter->Prg.InitR[3]=VM_GLOBALMEMADDR;
        StackFilter.getPrg()
                   .getInitR()[4] = StackFilter.getBlockLength();// StackFilter->Prg.InitR[4]=StackFilter->BlockLength;
        StackFilter.getPrg()
                   .getInitR()[5] = StackFilter.getExecCount();// StackFilter->Prg.InitR[5]=StackFilter->ExecCount;

        if ((firstByte & 0x10) != 0) // set registers to optional parameters
        // if any
        {
            final int InitMask = Inp.fgetbits() >>> 9;
            Inp.faddbits(7);
            for (int I = 0; I < 7; I++) {
                if ((InitMask & (1 << I)) != 0) {
                    // StackFilter->Prg.InitR[I]=RarVM::ReadData(Inp);
                    StackFilter.getPrg()
                               .getInitR()[I] = RarVM.ReadData(Inp);
                }
            }
        }

        if (NewFilter) {
            final int VMCodeSize = RarVM.ReadData(Inp);
            if ((VMCodeSize >= 0x10000) || (VMCodeSize == 0)) {
                return (false);
            }
            final byte[] VMCode = new byte[VMCodeSize];
            for (int I = 0; I < VMCodeSize; I++) {
                if (Inp.Overflow(3)) {
                    return (false);
                }
                VMCode[I] = (byte) (Inp.fgetbits() >> 8);
                Inp.faddbits(8);
            }
            // VM.Prepare(&VMCode[0],VMCodeSize,&Filter->Prg);
            this.rarVM.prepare(VMCode, VMCodeSize, Filter.getPrg());
        }
        StackFilter.getPrg()
                   .setAltCmd(Filter.getPrg()
                                    .getCmd());// StackFilter->Prg.AltCmd=&Filter->Prg.Cmd[0];
        StackFilter.getPrg()
                   .setCmdCount(Filter.getPrg()
                                      .getCmdCount());// StackFilter->Prg.CmdCount=Filter->Prg.CmdCount;

        final int StaticDataSize = Filter.getPrg()
                                         .getStaticData()
                                         .size();
        if ((StaticDataSize > 0) && (StaticDataSize < RarVM.VM_GLOBALMEMSIZE)) {
            StackFilter.getPrg()
                       .setStaticData(Filter.getPrg()
                                            .getStaticData());
        }

        if (StackFilter.getPrg()
                       .getGlobalData()
                       .size() < RarVM.VM_FIXEDGLOBALSIZE) {
            StackFilter.getPrg()
                       .getGlobalData()
                       .clear();
            StackFilter.getPrg()
                       .getGlobalData()
                       .setSize(RarVM.VM_FIXEDGLOBALSIZE);
        }

        // byte *GlobalData=&StackFilter->Prg.GlobalData[0];
        Vector<Byte> globalData = StackFilter.getPrg()
                                             .getGlobalData();
        for (int I = 0; I < 7; I++) {
            this.rarVM.setLowEndianValue(globalData, I * 4, StackFilter.getPrg()
                                                                       .getInitR()[I]);
        }

        this.rarVM.setLowEndianValue(globalData, 0x1c, StackFilter.getBlockLength());
        this.rarVM.setLowEndianValue(globalData, 0x20, 0);
        this.rarVM.setLowEndianValue(globalData, 0x24, 0);
        this.rarVM.setLowEndianValue(globalData, 0x28, 0);

        this.rarVM.setLowEndianValue(globalData, 0x2c, StackFilter.getExecCount());
        for (int i = 0; i < 16; i++) {
            globalData.set(0x30 + i, (byte) (0));
        }
        if ((firstByte & 8) != 0) // put data block passed as parameter if any
        {
            if (Inp.Overflow(3)) {
                return (false);
            }
            final int DataSize = RarVM.ReadData(Inp);
            if (DataSize > (RarVM.VM_GLOBALMEMSIZE - RarVM.VM_FIXEDGLOBALSIZE)) {
                return (false);
            }
            final int CurSize = StackFilter.getPrg()
                                           .getGlobalData()
                                           .size();
            if (CurSize < (DataSize + RarVM.VM_FIXEDGLOBALSIZE)) {
                StackFilter.getPrg()
                           .getGlobalData()
                           .setSize((DataSize + RarVM.VM_FIXEDGLOBALSIZE) - CurSize);
            }
            final int offset = RarVM.VM_FIXEDGLOBALSIZE;
            globalData = StackFilter.getPrg()
                                    .getGlobalData();
            for (int I = 0; I < DataSize; I++) {
                if (Inp.Overflow(3)) {
                    return (false);
                }
                globalData.set(offset + I, (byte) (Inp.fgetbits() >>> 8));
                Inp.faddbits(8);
            }
        }
        return (true);
    }

    private void ExecuteCode(final VMPreparedProgram Prg) {
        if (!Prg.getGlobalData()
                .isEmpty()) {
            Prg.getInitR()[6] = (int) (this.writtenFileSize);
            this.rarVM.setLowEndianValue(Prg.getGlobalData(), 0x24, (int) this.writtenFileSize);
            this.rarVM.setLowEndianValue(Prg.getGlobalData(), 0x28, (int) (this.writtenFileSize >>> 32));
            this.rarVM.execute(Prg);
        }
    }

    public boolean isFileExtracted() {
        return this.fileExtracted;
    }

    public void setDestSize(final long destSize) {
        this.destUnpSize = destSize;
        this.fileExtracted = false;
    }

    public void setSuspended(final boolean suspended) {
        this.suspended = suspended;
    }

    public int getChar() throws IOException, RarException {
        if (this.inAddr > (BitInput.MAX_SIZE - 30)) {
            unpReadBuf();
        }
        return (this.inBuf[this.inAddr++] & 0xff);
    }

    public int getPpmEscChar() {
        return this.ppmEscChar;
    }

    public void setPpmEscChar(final int ppmEscChar) {
        this.ppmEscChar = ppmEscChar;
    }

    public void cleanUp() {
        if (this.ppm != null) {
            final SubAllocator allocator = this.ppm.getSubAlloc();
            if (allocator != null) {
                allocator.stopSubAllocator();
            }
        }
    }
}
