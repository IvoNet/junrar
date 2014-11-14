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

package com.github.junrar.unpack;

import com.github.junrar.exception.RarException;
import com.github.junrar.unpack.decode.Compress;
import com.github.junrar.unpack.vm.BitInput;

import java.io.IOException;
import java.util.Arrays;


abstract class Unpack15 extends BitInput {

    private static final int[] ShortLen1 = {1, 3, 4, 4, 5, 6, 7, 8, 8, 4, 4, 5, 6, 6, 4, 0};
    private static final int[] ShortXor1 = {
            0, 0xa0, 0xd0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe, 0xff, 0xc0, 0x80, 0x90, 0x98, 0x9c, 0xb0
    };
    private static final int[] ShortLen2 = {2, 3, 3, 3, 4, 4, 5, 6, 6, 4, 4, 5, 6, 6, 4, 0};
    private static final int[] ShortXor2 = {
            0, 0x40, 0x60, 0xa0, 0xd0, 0xe0, 0xf0, 0xf8, 0xfc, 0xc0, 0x80, 0x90, 0x98, 0x9c, 0xb0
    };
    private static final int STARTL1 = 2;
    private static final int[] DecL1 = {
            0x8000, 0xa000, 0xc000, 0xd000, 0xe000, 0xea00, 0xee00, 0xf000, 0xf200, 0xf200, 0xffff
    };
    private static final int[] PosL1 = {0, 0, 0, 2, 3, 5, 7, 11, 16, 20, 24, 32, 32};
    private static final int STARTL2 = 3;
    private static final int[] DecL2 = {
            0xa000, 0xc000, 0xd000, 0xe000, 0xea00, 0xee00, 0xf000, 0xf200, 0xf240, 0xffff
    };
    private static final int[] PosL2 = {0, 0, 0, 0, 5, 7, 9, 13, 18, 22, 26, 34, 36};
    private static final int STARTHF0 = 4;
    private static final int[] DecHf0 = {
            0x8000, 0xc000, 0xe000, 0xf200, 0xf200, 0xf200, 0xf200, 0xf200, 0xffff
    };
    private static final int[] PosHf0 = {
            0, 0, 0, 0, 0, 8, 16, 24, 33, 33, 33, 33, 33
    };
    private static final int STARTHF1 = 5;
    private static final int[] DecHf1 = {
            0x2000, 0xc000, 0xe000, 0xf000, 0xf200, 0xf200, 0xf7e0, 0xffff
    };
    private static final int[] PosHf1 = {
            0, 0, 0, 0, 0, 0, 4, 44, 60, 76, 80, 80, 127
    };
    private static final int STARTHF2 = 5;
    private static final int[] DecHf2 = {
            0x1000, 0x2400, 0x8000, 0xc000, 0xfa00, 0xffff, 0xffff, 0xffff
    };
    private static final int[] PosHf2 = {0, 0, 0, 0, 0, 0, 2, 7, 53, 117, 233, 0, 0};
    private static final int STARTHF3 = 6;
    private static final int[] DecHf3 = {
            0x800, 0x2400, 0xee00, 0xfe80, 0xffff, 0xffff, 0xffff
    };
    private static final int[] PosHf3 = {0, 0, 0, 0, 0, 0, 0, 2, 16, 218, 251, 0, 0};
    private static final int STARTHF4 = 8;
    private static final int[] DecHf4 = {
            0xff00, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff
    };
    private static final int[] PosHf4 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 255, 0, 0, 0};
    final int[] oldDist = new int[4];
    private final int[] ChSet = new int[256];
    private final int[] ChSetA = new int[256];
    private final int[] ChSetB = new int[256];
    private final int[] ChSetC = new int[256];
    private final int[] Place = new int[256];
    private final int[] PlaceA = new int[256];
    private final int[] PlaceB = new int[256];
    private final int[] PlaceC = new int[256];
    private final int[] NToPl = new int[256];
    private final int[] NToPlB = new int[256];
    private final int[] NToPlC = new int[256];
    int readBorder;
    boolean suspended;
    boolean unpAllBuf;
    ComprDataIO unpIO;
    boolean unpSomeRead;
    int readTop;
    long destUnpSize;
    byte[] window;
    int unpPtr;
    int wrPtr;
    int oldDistPtr;
    int lastDist;
    int lastLength;
    private int FlagBuf;
    private int AvrPlc;
    private int AvrPlcB;
    private int AvrLn1;
    private int AvrLn2;
    private int AvrLn3;
    private int Buf60;
    private int NumHuf;
    private int StMode;
    private int LCount;
    private int FlagsCnt;
    private int Nhfb;
    private int Nlzb;
    private int MaxDist3;

    protected abstract void unpInitData(boolean solid);

    void unpack15(final boolean solid) throws IOException, RarException {
        if (this.suspended) {
            this.unpPtr = this.wrPtr;
        } else {
            unpInitData(solid);
            oldUnpInitData(solid);
            unpReadBuf();
            if (solid) {
                this.unpPtr = this.wrPtr;
            } else {
                initHuff();
                this.unpPtr = 0;
            }
            --this.destUnpSize;
        }
        if (this.destUnpSize >= 0) {
            getFlagsBuf();
            this.FlagsCnt = 8;
        }

        while (this.destUnpSize >= 0) {
            this.unpPtr &= Compress.MAXWINMASK;

            if ((this.inAddr > (this.readTop - 30)) && !unpReadBuf()) {
                break;
            }
            if ((((this.wrPtr - this.unpPtr) & Compress.MAXWINMASK) < 270) && (this.wrPtr != this.unpPtr)) {
                oldUnpWriteBuf();
                if (this.suspended) {
                    return;
                }
            }
            if (this.StMode != 0) {
                huffDecode();
                continue;
            }

            if (--this.FlagsCnt < 0) {
                getFlagsBuf();
                this.FlagsCnt = 7;
            }

            if ((this.FlagBuf & 0x80) == 0) {
                this.FlagBuf <<= 1;
                if (--this.FlagsCnt < 0) {
                    getFlagsBuf();
                    this.FlagsCnt = 7;
                }
                if ((this.FlagBuf & 0x80) == 0) {
                    this.FlagBuf <<= 1;
                    shortLZ();
                } else {
                    this.FlagBuf <<= 1;
                    if (this.Nlzb > this.Nhfb) {
                        huffDecode();
                    } else {
                        longLZ();
                    }
                }
            } else {
                this.FlagBuf <<= 1;
                if (this.Nlzb > this.Nhfb) {
                    longLZ();
                } else {
                    huffDecode();
                }
            }
        }
        oldUnpWriteBuf();
    }


    boolean unpReadBuf() throws IOException, RarException {
        int dataSize = this.readTop - this.inAddr;
        if (dataSize < 0) {
            return (false);
        }
        if (this.inAddr > (BitInput.MAX_SIZE / 2)) {
            if (dataSize > 0) {
                System.arraycopy(this.inBuf, this.inAddr, this.inBuf, 0, dataSize);
            }
            this.inAddr = 0;
            this.readTop = dataSize;
        } else {
            dataSize = this.readTop;
        }
        //int readCode=UnpIO->UnpRead(InBuf+DataSize,(BitInput::MAX_SIZE-DataSize)&~0xf);
        final int readCode = this.unpIO.unpRead(this.inBuf, dataSize, (BitInput.MAX_SIZE - dataSize) & ~0xf);
        if (readCode > 0) {
            this.readTop += readCode;
        }
        this.readBorder = this.readTop - 30;
        return (readCode != -1);
    }

    private int getShortLen1(final int pos) {
        return (pos == 1) ? (this.Buf60 + 3) : ShortLen1[pos];
    }

    private int getShortLen2(final int pos) {
        return (pos == 3) ? (this.Buf60 + 3) : ShortLen2[pos];
    }

    void shortLZ() {
        int Length;
        final int SaveLength;
        final int LastDistance;
        int Distance;
        int DistancePlace;
        this.NumHuf = 0;

        int BitField = fgetbits();
        if (this.LCount == 2) {
            faddbits(1);
            if (BitField >= 0x8000) {
                oldCopyString(this.lastDist, this.lastLength);
                return;
            }
            BitField <<= 1;
            this.LCount = 0;
        }
        BitField >>>= 8;
        if (this.AvrLn1 < 37) {
            Length = 0;
            while (((BitField ^ ShortXor1[Length]) & (~(0xff >>> getShortLen1(Length)))) != 0) {
                Length++;
            }
            faddbits(getShortLen1(Length));
        } else {
            Length = 0;
            while (((BitField ^ ShortXor2[Length]) & (~(0xff >> getShortLen2(Length)))) != 0) {
                Length++;
            }
            faddbits(getShortLen2(Length));
        }

        if (Length >= 9) {
            if (Length == 9) {
                this.LCount++;
                oldCopyString(this.lastDist, this.lastLength);
                return;
            }
            if (Length == 14) {
                this.LCount = 0;
                Length = decodeNum(fgetbits(), STARTL2, DecL2, PosL2) + 5;
                Distance = (fgetbits() >> 1) | 0x8000;
                faddbits(15);
                this.lastLength = Length;
                this.lastDist = Distance;
                oldCopyString(Distance, Length);
                return;
            }

            this.LCount = 0;
            SaveLength = Length;
            Distance = this.oldDist[(this.oldDistPtr - (Length - 9)) & 3];
            Length = decodeNum(fgetbits(), STARTL1, DecL1, PosL1) + 2;
            if ((Length == 0x101) && (SaveLength == 10)) {
                this.Buf60 ^= 1;
                return;
            }
            if (Distance > 256) {
                Length++;
            }
            if (Distance >= this.MaxDist3) {
                Length++;
            }

            this.oldDist[this.oldDistPtr++] = Distance;
            this.oldDistPtr &= 3;
            this.lastLength = Length;
            this.lastDist = Distance;
            oldCopyString(Distance, Length);
            return;
        }

        this.LCount = 0;
        this.AvrLn1 += Length;
        this.AvrLn1 -= this.AvrLn1 >> 4;

        DistancePlace = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2) & 0xff;
        Distance = this.ChSetA[DistancePlace];
        if (--DistancePlace != -1) {
            this.PlaceA[Distance]--;
            LastDistance = this.ChSetA[DistancePlace];
            this.PlaceA[LastDistance]++;
            this.ChSetA[DistancePlace + 1] = LastDistance;
            this.ChSetA[DistancePlace] = Distance;
        }
        Length += 2;
        this.oldDist[this.oldDistPtr++] = ++Distance;
        this.oldDistPtr &= 3;
        this.lastLength = Length;
        this.lastDist = Distance;
        oldCopyString(Distance, Length);
    }

    void longLZ() {
        int Length;
        int Distance;
        final int DistancePlace;
        int NewDistancePlace;
        final int OldAvr2;
        final int OldAvr3;

        this.NumHuf = 0;
        this.Nlzb += 16;
        if (this.Nlzb > 0xff) {
            this.Nlzb = 0x90;
            this.Nhfb >>>= 1;
        }
        OldAvr2 = this.AvrLn2;

        int BitField = fgetbits();
        if (this.AvrLn2 >= 122) {
            Length = decodeNum(BitField, STARTL2, DecL2, PosL2);
        } else {
            if (this.AvrLn2 >= 64) {
                Length = decodeNum(BitField, STARTL1, DecL1, PosL1);
            } else {
                if (BitField < 0x100) {
                    Length = BitField;
                    faddbits(16);
                } else {
                    Length = 0;
                    while (((BitField << Length) & 0x8000) == 0) {
                        Length++;
                    }
                    faddbits(Length + 1);
                }
            }
        }
        this.AvrLn2 += Length;
        this.AvrLn2 -= this.AvrLn2 >>> 5;

        BitField = fgetbits();
        if (this.AvrPlcB > 0x28ff) {
            DistancePlace = decodeNum(BitField, STARTHF2, DecHf2, PosHf2);
        } else {
            DistancePlace = (this.AvrPlcB > 0x6ff) ? decodeNum(BitField, STARTHF1, DecHf1, PosHf1) : decodeNum(BitField,
                                                                                                               STARTHF0,
                                                                                                               DecHf0,
                                                                                                               PosHf0);
        }
        this.AvrPlcB += DistancePlace;
        this.AvrPlcB -= this.AvrPlcB >> 8;
        while (true) {
            Distance = this.ChSetB[DistancePlace & 0xff];
            NewDistancePlace = this.NToPlB[Distance++ & 0xff]++;
            if ((Distance & 0xff) == 0) {
                corrHuff(this.ChSetB, this.NToPlB);
            } else {
                break;
            }
        }

        this.ChSetB[DistancePlace] = this.ChSetB[NewDistancePlace];
        this.ChSetB[NewDistancePlace] = Distance;

        Distance = ((Distance & 0xff00) | (fgetbits() >>> 8)) >>> 1;
        faddbits(7);

        OldAvr3 = this.AvrLn3;
        if ((Length != 1) && (Length != 4)) {
            if ((Length == 0) && (Distance <= this.MaxDist3)) {
                this.AvrLn3++;
                this.AvrLn3 -= this.AvrLn3 >> 8;
            } else {
                if (this.AvrLn3 > 0) {
                    this.AvrLn3--;
                }
            }
        }
        Length += 3;
        if (Distance >= this.MaxDist3) {
            Length++;
        }
        if (Distance <= 256) {
            Length += 8;
        }
        this.MaxDist3 = ((OldAvr3 > 0xb0) || ((this.AvrPlc >= 0x2a00) && (OldAvr2 < 0x40))) ? 0x7f00 : 0x2001;
        this.oldDist[this.oldDistPtr++] = Distance;
        this.oldDistPtr &= 3;
        this.lastLength = Length;
        this.lastDist = Distance;
        oldCopyString(Distance, Length);
    }

    void huffDecode() {
        int CurByte;
        int NewBytePlace;
        final int Length;
        int Distance;
        int BytePlace;

        int BitField = fgetbits();

        if (this.AvrPlc > 0x75ff) {
            BytePlace = decodeNum(BitField, STARTHF4, DecHf4, PosHf4);
        } else {
            if (this.AvrPlc > 0x5dff) {
                BytePlace = decodeNum(BitField, STARTHF3, DecHf3, PosHf3);
            } else {
                if (this.AvrPlc > 0x35ff) {
                    BytePlace = decodeNum(BitField, STARTHF2, DecHf2, PosHf2);
                } else {
                    BytePlace = (this.AvrPlc > 0x0dff) ? decodeNum(BitField, STARTHF1, DecHf1, PosHf1) : decodeNum(
                            BitField, STARTHF0, DecHf0, PosHf0);
                }
            }
        }
        BytePlace &= 0xff;
        if (this.StMode == 0) {
            if ((this.NumHuf++ >= 16) && (this.FlagsCnt == 0)) {
                this.StMode = 1;
            }
        } else {
            if ((BytePlace == 0) && (BitField > 0xfff)) {
                BytePlace = 0x100;
            }
            if (--BytePlace == -1) {
                BitField = fgetbits();
                faddbits(1);
                if ((BitField & 0x8000) == 0) {
                    Length = ((BitField & 0x4000) != 0) ? 4 : 3;
                    faddbits(1);
                    Distance = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2);
                    Distance = (Distance << 5) | (fgetbits() >>> 11);
                    faddbits(5);
                    oldCopyString(Distance, Length);
                    return;
                } else {
                    this.NumHuf = 0;
                    this.StMode = 0;
                    return;
                }
            }
        }
        this.AvrPlc += BytePlace;
        this.AvrPlc -= this.AvrPlc >>> 8;
        this.Nhfb += 16;
        if (this.Nhfb > 0xff) {
            this.Nhfb = 0x90;
            this.Nlzb >>>= 1;
        }

        this.window[this.unpPtr++] = (byte) (this.ChSet[BytePlace] >>> 8);
        --this.destUnpSize;

        while (true) {
            CurByte = this.ChSet[BytePlace];
            NewBytePlace = this.NToPl[CurByte++ & 0xff]++;
            if ((CurByte & 0xff) > 0xa1) {
                corrHuff(this.ChSet, this.NToPl);
            } else {
                break;
            }
        }

        this.ChSet[BytePlace] = this.ChSet[NewBytePlace];
        this.ChSet[NewBytePlace] = CurByte;
    }

    void getFlagsBuf() {
        int Flags;
        int NewFlagsPlace;
        final int FlagsPlace = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2);

        while (true) {
            Flags = this.ChSetC[FlagsPlace];
            this.FlagBuf = Flags >>> 8;
            NewFlagsPlace = this.NToPlC[Flags++ & 0xff]++;
            if ((Flags & 0xff) != 0) {
                break;
            }
            corrHuff(this.ChSetC, this.NToPlC);
        }

        this.ChSetC[FlagsPlace] = this.ChSetC[NewFlagsPlace];
        this.ChSetC[NewFlagsPlace] = Flags;
    }

    void oldUnpInitData(final boolean Solid) {
        if (!Solid) {
            this.AvrPlcB = 0;
            this.AvrLn1 = 0;
            this.AvrLn2 = 0;
            this.AvrLn3 = 0;
            this.NumHuf = 0;
            this.Buf60 = 0;
            this.AvrPlc = 0x3500;
            this.MaxDist3 = 0x2001;
            this.Nhfb = 0x80;
            this.Nlzb = 0x80;
        }
        this.FlagsCnt = 0;
        this.FlagBuf = 0;
        this.StMode = 0;
        this.LCount = 0;
        this.readTop = 0;
    }

    void initHuff() {
        for (int I = 0; I < 256; I++) {
            this.Place[I] = I;
            this.PlaceA[I] = I;
            this.PlaceB[I] = I;
            this.PlaceC[I] = (~I + 1) & 0xff;
            this.ChSet[I] = I << 8;
            this.ChSetB[I] = I << 8;
            this.ChSetA[I] = I;
            this.ChSetC[I] = ((~I + 1) & 0xff) << 8;
        }

        Arrays.fill(this.NToPl, 0);// memset(NToPl,0,sizeof(NToPl));
        Arrays.fill(this.NToPlB, 0); // memset(NToPlB,0,sizeof(NToPlB));
        Arrays.fill(this.NToPlC, 0); // memset(NToPlC,0,sizeof(NToPlC));
        corrHuff(this.ChSetB, this.NToPlB);
    }

    void corrHuff(final int[] CharSet, final int[] NumToPlace) {
        int I;
        int J;
        int pos = 0;
        for (I = 7; I >= 0; I--) {
            for (J = 0; J < 32; J++, pos++) {
                CharSet[pos] = ((CharSet[pos] & ~0xff) | I);// *CharSet=(*CharSet
                // & ~0xff) | I;
            }
        }
        Arrays.fill(NumToPlace, 0);// memset(NumToPlace,0,sizeof(NToPl));
        for (I = 6; I >= 0; I--) {
            NumToPlace[I] = (7 - I) * 32;
        }
    }

    void oldCopyString(final int Distance, int Length) {
        this.destUnpSize -= Length;
        while ((Length--) != 0) {
            this.window[this.unpPtr] = this.window[(this.unpPtr - Distance) & Compress.MAXWINMASK];
            this.unpPtr = (this.unpPtr + 1) & Compress.MAXWINMASK;
        }
    }

    int decodeNum(int Num, int StartPos, final int[] DecTab, final int[] PosTab) {
        int I;
        for (Num &= 0xfff0, I = 0; DecTab[I] <= Num; I++) {
            StartPos++;
        }
        faddbits(StartPos);
        return (((Num - ((I != 0) ? DecTab[I - 1] : 0)) >>> (16 - StartPos)) + PosTab[StartPos]);
    }

    void oldUnpWriteBuf() throws IOException {
        if (this.unpPtr != this.wrPtr) {
            this.unpSomeRead = true;
        }
        if (this.unpPtr < this.wrPtr) {
            this.unpIO.unpWrite(this.window, this.wrPtr, -this.wrPtr & Compress.MAXWINMASK);
            this.unpIO.unpWrite(this.window, 0, this.unpPtr);
            this.unpAllBuf = true;
        } else {
            this.unpIO.unpWrite(this.window, this.wrPtr, this.unpPtr - this.wrPtr);
        }
        this.wrPtr = this.unpPtr;
    }


}
