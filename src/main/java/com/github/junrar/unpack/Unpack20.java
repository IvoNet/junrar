
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
import com.github.junrar.unpack.decode.AudioVariables;
import com.github.junrar.unpack.decode.BitDecode;
import com.github.junrar.unpack.decode.Compress;
import com.github.junrar.unpack.decode.Decode;
import com.github.junrar.unpack.decode.DistDecode;
import com.github.junrar.unpack.decode.LitDecode;
import com.github.junrar.unpack.decode.LowDistDecode;
import com.github.junrar.unpack.decode.MultDecode;
import com.github.junrar.unpack.decode.RepDecode;

import java.io.IOException;
import java.util.Arrays;


abstract class Unpack20 extends Unpack15 {

    static final int[] LDecode = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224
    };
    static final byte[] LBits = {
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5
    };
    static final int[] SDDecode = {0, 4, 8, 16, 32, 64, 128, 192};
    static final int[] SDBits = {2, 2, 3, 4, 5, 6, 6, 6};
    private static final int[] DDecode = {
            0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096,
            6144, 8192, 12288, 16384, 24576, 32768, 49152, 65536, 98304, 131072, 196608, 262144, 327680, 393216, 458752,
            524288, 589824, 655360, 720896, 786432, 851968, 917504, 983040
    };
    private static final int[] DBits = {
            0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14,
            15, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16
    };
    final LitDecode LD = new LitDecode();
    final DistDecode DD = new DistDecode();
    final LowDistDecode LDD = new LowDistDecode();
    final RepDecode RD = new RepDecode();
    final BitDecode BD = new BitDecode();
    private final MultDecode[] MD = new MultDecode[4];
    private final byte[] UnpOldTable20 = new byte[Compress.MC20 * 4];
    private final AudioVariables[] AudV = new AudioVariables[4];
    private int UnpAudioBlock;
    private int UnpChannels;
    private int UnpCurChannel;
    private int UnpChannelDelta;

    void unpack20(final boolean solid) throws IOException, RarException {

        int Bits;

        if (this.suspended) {
            this.unpPtr = this.wrPtr;
        } else {
            unpInitData(solid);
            if (!unpReadBuf()) {
                return;
            }
            if (!solid) {
                if (!ReadTables20()) {
                    return;
                }
            }
            --this.destUnpSize;
        }

        while (this.destUnpSize >= 0) {
            this.unpPtr &= Compress.MAXWINMASK;

            if (this.inAddr > (this.readTop - 30)) {
                if (!unpReadBuf()) {
                    break;
                }
            }
            if ((((this.wrPtr - this.unpPtr) & Compress.MAXWINMASK) < 270) && (this.wrPtr != this.unpPtr)) {
                oldUnpWriteBuf();
                if (this.suspended) {
                    return;
                }
            }
            if (this.UnpAudioBlock != 0) {
                final int AudioNumber = decodeNumber(this.MD[this.UnpCurChannel]);

                if (AudioNumber == 256) {
                    if (!ReadTables20()) {
                        break;
                    }
                    continue;
                }
                this.window[this.unpPtr++] = DecodeAudio(AudioNumber);
                if (++this.UnpCurChannel == this.UnpChannels) {
                    this.UnpCurChannel = 0;
                }
                --this.destUnpSize;
                continue;
            }

            int Number = decodeNumber(this.LD);
            if (Number < 256) {
                this.window[this.unpPtr++] = (byte) Number;
                --this.destUnpSize;
                continue;
            }
            if (Number > 269) {
                int Length = LDecode[Number -= 270] + 3;
                if ((Bits = LBits[Number]) > 0) {
                    Length += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }

                final int DistNumber = decodeNumber(this.DD);
                int Distance = DDecode[DistNumber] + 1;
                if ((Bits = DBits[DistNumber]) > 0) {
                    Distance += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }

                if (Distance >= 0x2000) {
                    Length++;
                    if (Distance >= 0x40000L) {
                        Length++;
                    }
                }

                CopyString20(Length, Distance);
                continue;
            }
            if (Number == 269) {
                if (!ReadTables20()) {
                    break;
                }
                continue;
            }
            if (Number == 256) {
                CopyString20(this.lastLength, this.lastDist);
                continue;
            }
            if (Number < 261) {
                final int Distance = this.oldDist[(this.oldDistPtr - (Number - 256)) & 3];
                final int LengthNumber = decodeNumber(this.RD);
                int Length = LDecode[LengthNumber] + 2;
                if ((Bits = LBits[LengthNumber]) > 0) {
                    Length += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }
                if (Distance >= 0x101) {
                    Length++;
                    if (Distance >= 0x2000) {
                        Length++;
                        if (Distance >= 0x40000) {
                            Length++;
                        }
                    }
                }
                CopyString20(Length, Distance);
                continue;
            }
            if (Number < 270) {
                int Distance = SDDecode[Number -= 261] + 1;
                if ((Bits = SDBits[Number]) > 0) {
                    Distance += getbits() >>> (16 - Bits);
                    addbits(Bits);
                }
                CopyString20(2, Distance);
            }
        }
        ReadLastTables();
        oldUnpWriteBuf();

    }

    void CopyString20(int length, final int distance) {
        this.lastDist = distance;
        this.oldDist[this.oldDistPtr++ & 3] = distance;
        this.lastLength = length;
        this.destUnpSize -= length;

        int destPtr = this.unpPtr - distance;
        if ((destPtr < (Compress.MAXWINSIZE - 300)) && (this.unpPtr < (Compress.MAXWINSIZE - 300))) {
            this.window[this.unpPtr++] = this.window[destPtr++];
            this.window[this.unpPtr++] = this.window[destPtr++];
            while (length > 2) {
                length--;
                this.window[this.unpPtr++] = this.window[destPtr++];
            }
        } else {
            while ((length--) != 0) {
                this.window[this.unpPtr] = this.window[destPtr++ & Compress.MAXWINMASK];
                this.unpPtr = (this.unpPtr + 1) & Compress.MAXWINMASK;
            }
        }
    }

    void makeDecodeTables(final byte[] lenTab, final int offset, final Decode dec, final int size) {
        final int[] lenCount = new int[16];
        final int[] tmpPos = new int[16];
        int i;
        long M;
        long N;

        Arrays.fill(lenCount, 0);// memset(LenCount,0,sizeof(LenCount));

        Arrays.fill(dec.getDecodeNum(), 0);// memset(Dec->DecodeNum,0,Size*sizeof(*Dec->DecodeNum));

        for (i = 0; i < size; i++) {
            lenCount[(lenTab[offset + i] & 0xF)]++;
        }
        lenCount[0] = 0;
        for (tmpPos[0] = 0, dec.getDecodePos()[0] = 0, dec.getDecodeLen()[0] = 0, N = 0, i = 1; i < 16; i++) {
            N = 2 * (N + lenCount[i]);
            M = N << (15 - i);
            if (M > 0xFFFF) {
                M = 0xFFFF;
            }
            dec.getDecodeLen()[i] = (int) M;
            tmpPos[i] = dec.getDecodePos()[i - 1] + lenCount[i - 1];
            dec.getDecodePos()[i] = tmpPos[i];
        }

        for (i = 0; i < size; i++) {
            if (lenTab[offset + i] != 0) {
                dec.getDecodeNum()[tmpPos[lenTab[offset + i] & 0xF]++] = i;
            }
        }
        dec.setMaxNum(size);
    }

    int decodeNumber(final Decode dec) {
        final int bits;
        final long bitField = getbits() & 0xfffe;

        final int[] decodeLen = dec.getDecodeLen();
        if (bitField < decodeLen[8]) {
            if (bitField < decodeLen[4]) {
                if (bitField < decodeLen[2]) {
                    bits = (bitField < decodeLen[1]) ? 1 : 2;
                } else {
                    bits = (bitField < decodeLen[3]) ? 3 : 4;
                }
            } else {
                if (bitField < decodeLen[6]) {
                    bits = (bitField < decodeLen[5]) ? 5 : 6;
                } else {
                    bits = (bitField < decodeLen[7]) ? 7 : 8;
                }
            }
        } else {
            if (bitField < decodeLen[12]) {
                if (bitField < decodeLen[10]) {
                    bits = (bitField < decodeLen[9]) ? 9 : 10;
                } else {
                    bits = (bitField < decodeLen[11]) ? 11 : 12;
                }
            } else {
                if (bitField < decodeLen[14]) {
                    bits = (bitField < decodeLen[13]) ? 13 : 14;
                } else {
                    bits = 15;
                }
            }
        }
        addbits(bits);
        int N = dec.getDecodePos()[bits] + (((int) bitField - decodeLen[bits - 1]) >>> (16 - bits));
        if (N >= dec.getMaxNum()) {
            N = 0;
        }
        return (dec.getDecodeNum()[N]);
    }

    boolean ReadTables20() throws IOException, RarException {
        final byte[] BitLength = new byte[Compress.BC20];
        final byte[] Table = new byte[Compress.MC20 * 4];
        final int TableSize;
        int N;
        int I;
        if (this.inAddr > (this.readTop - 25)) {
            if (!unpReadBuf()) {
                return (false);
            }
        }
        final int BitField = getbits();
        this.UnpAudioBlock = (BitField & 0x8000);

        if ((BitField & 0x4000) == 0) {
            Arrays.fill(this.UnpOldTable20, (byte) 0);
        }
        addbits(2);

        if (this.UnpAudioBlock == 0) {
            TableSize = Compress.NC20 + Compress.DC20 + Compress.RC20;
        } else {
            this.UnpChannels = ((BitField >>> 12) & 3) + 1;
            if (this.UnpCurChannel >= this.UnpChannels) {
                this.UnpCurChannel = 0;
            }
            addbits(2);
            TableSize = Compress.MC20 * this.UnpChannels;
        }
        for (I = 0; I < Compress.BC20; I++) {
            BitLength[I] = (byte) (getbits() >>> 12);
            addbits(4);
        }
        makeDecodeTables(BitLength, 0, this.BD, Compress.BC20);
        I = 0;
        while (I < TableSize) {
            if (this.inAddr > (this.readTop - 5)) {
                if (!unpReadBuf()) {
                    return (false);
                }
            }
            final int Number = decodeNumber(this.BD);
            if (Number < 16) {
                Table[I] = (byte) ((Number + this.UnpOldTable20[I]) & 0xf);
                I++;
            } else if (Number == 16) {
                N = (getbits() >>> 14) + 3;
                addbits(2);
                while ((N-- > 0) && (I < TableSize)) {
                    Table[I] = Table[I - 1];
                    I++;
                }
            } else {
                if (Number == 17) {
                    N = (getbits() >>> 13) + 3;
                    addbits(3);
                } else {
                    N = (getbits() >>> 9) + 11;
                    addbits(7);
                }
                while ((N-- > 0) && (I < TableSize)) {
                    Table[I++] = 0;
                }
            }
        }
        if (this.inAddr > this.readTop) {
            return (true);
        }
        if (this.UnpAudioBlock == 0) {
            makeDecodeTables(Table, 0, this.LD, Compress.NC20);
            makeDecodeTables(Table, Compress.NC20, this.DD, Compress.DC20);
            makeDecodeTables(Table, Compress.NC20 + Compress.DC20, this.RD, Compress.RC20);
        } else {
            for (I = 0; I < this.UnpChannels; I++) {
                makeDecodeTables(Table, I * Compress.MC20, this.MD[I], Compress.MC20);
            }
        }
        System.arraycopy(Table, 0, this.UnpOldTable20, 0, this.UnpOldTable20.length);
        return (true);
    }

    void unpInitData20(final boolean Solid) {
        if (!Solid) {
            this.UnpChannelDelta = 0;
            this.UnpCurChannel = 0;
            this.UnpChannels = 1;
            Arrays.fill(this.AudV, new AudioVariables());
            Arrays.fill(this.UnpOldTable20, (byte) 0);
        }
    }

    void ReadLastTables() throws IOException, RarException {
        if (this.readTop >= (this.inAddr + 5)) {
            if (this.UnpAudioBlock == 0) {
                if (decodeNumber(this.LD) == 269) {
                    ReadTables20();
                }
            } else {
                if (decodeNumber(this.MD[this.UnpCurChannel]) == 256) {
                    ReadTables20();
                }
            }
        }
    }

    byte DecodeAudio(final int Delta) {
        final AudioVariables v = this.AudV[this.UnpCurChannel];
        v.setByteCount(v.getByteCount() + 1);
        v.setD4(v.getD3());
        v.setD3(v.getD2());// ->D3=V->D2;
        v.setD2(v.getLastDelta() - v.getD1());// ->D2=V->LastDelta-V->D1;
        v.setD1(v.getLastDelta());// V->D1=V->LastDelta;
        // int PCh=8*V->LastChar+V->K1*V->D1 +V->K2*V->D2 +V->K3*V->D3
        // +V->K4*V->D4+ V->K5*UnpChannelDelta;
        int PCh = (8 * v.getLastChar()) + (v.getK1() * v.getD1());
        PCh += (v.getK2() * v.getD2()) + (v.getK3() * v.getD3());
        PCh += (v.getK4() * v.getD4()) + (v.getK5() * this.UnpChannelDelta);
        PCh = (PCh >>> 3) & 0xFF;

        final int Ch = PCh - Delta;

        final int D = ((byte) Delta) << 3;

        v.getDif()[0] += Math.abs(D);// V->Dif[0]+=abs(D);
        v.getDif()[1] += Math.abs(D - v.getD1());// V->Dif[1]+=abs(D-V->D1);
        v.getDif()[2] += Math.abs(D + v.getD1());// V->Dif[2]+=abs(D+V->D1);
        v.getDif()[3] += Math.abs(D - v.getD2());// V->Dif[3]+=abs(D-V->D2);
        v.getDif()[4] += Math.abs(D + v.getD2());// V->Dif[4]+=abs(D+V->D2);
        v.getDif()[5] += Math.abs(D - v.getD3());// V->Dif[5]+=abs(D-V->D3);
        v.getDif()[6] += Math.abs(D + v.getD3());// V->Dif[6]+=abs(D+V->D3);
        v.getDif()[7] += Math.abs(D - v.getD4());// V->Dif[7]+=abs(D-V->D4);
        v.getDif()[8] += Math.abs(D + v.getD4());// V->Dif[8]+=abs(D+V->D4);
        v.getDif()[9] += Math.abs(D - this.UnpChannelDelta);// V->Dif[9]+=abs(D-UnpChannelDelta);
        v.getDif()[10] += Math.abs(D + this.UnpChannelDelta);// V->Dif[10]+=abs(D+UnpChannelDelta);

        v.setLastDelta((byte) (Ch - v.getLastChar()));
        this.UnpChannelDelta = v.getLastDelta();
        v.setLastChar(Ch);// V->LastChar=Ch;

        if ((v.getByteCount() & 0x1F) == 0) {
            int MinDif = v.getDif()[0];
            int NumMinDif = 0;
            v.getDif()[0] = 0;// ->Dif[0]=0;
            for (int I = 1; I < v.getDif().length; I++) {
                if (v.getDif()[I] < MinDif) {
                    MinDif = v.getDif()[I];
                    NumMinDif = I;
                }
                v.getDif()[I] = 0;
            }
            switch (NumMinDif) {
                case 1:
                    if (v.getK1() >= -16) {
                        v.setK1(v.getK1() - 1);// V->K1--;
                    }
                    break;
                case 2:
                    if (v.getK1() < 16) {
                        v.setK1(v.getK1() + 1);// V->K1++;
                    }
                    break;
                case 3:
                    if (v.getK2() >= -16) {
                        v.setK2(v.getK2() - 1);// V->K2--;
                    }
                    break;
                case 4:
                    if (v.getK2() < 16) {
                        v.setK2(v.getK2() + 1);// V->K2++;
                    }
                    break;
                case 5:
                    if (v.getK3() >= -16) {
                        v.setK3(v.getK3() - 1);
                    }
                    break;
                case 6:
                    if (v.getK3() < 16) {
                        v.setK3(v.getK3() + 1);
                    }
                    break;
                case 7:
                    if (v.getK4() >= -16) {
                        v.setK4(v.getK4() - 1);
                    }
                    break;
                case 8:
                    if (v.getK4() < 16) {
                        v.setK4(v.getK4() + 1);
                    }
                    break;
                case 9:
                    if (v.getK5() >= -16) {
                        v.setK5(v.getK5() - 1);
                    }
                    break;
                case 10:
                    if (v.getK5() < 16) {
                        v.setK5(v.getK5() + 1);
                    }
                    break;
            }
        }
        return ((byte) Ch);
    }

}
