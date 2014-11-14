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

package com.github.junrar.unpack.ppm;

import java.util.Arrays;


@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
public class SubAllocator {
    private static final int N1 = 4;
    private static final int N2 = 4;
    private static final int N3 = 4;
    private static final int N4 = ((128 + 3) - N1 - (2 * N2) - (3 * N3)) / 4;

    private static final int N_INDEXES = N1 + N2 + N3 + N4;

    private static final int UNIT_SIZE = Math.max(PPMContext.size, RarMemBlock.size);

    private static final int FIXED_UNIT_SIZE = 12;
    private final int[] indx2Units = new int[N_INDEXES];
    private final int[] units2Indx = new int[128];
    private final RarNode[] freeList = new RarNode[N_INDEXES];
    private int subAllocatorSize;
    private int glueCount;
    private int heapStart;
    private int loUnit;
    private int hiUnit;
    private int pText;
    private int unitsStart;
    private int heapEnd;
    private int fakeUnitsStart;
    private byte[] heap;
    private int freeListPos;
    private int tempMemBlockPos;
    // Temp fields
    private RarNode tempRarNode;
    private RarMemBlock tempRarMemBlock1;
    private RarMemBlock tempRarMemBlock2;
    private RarMemBlock tempRarMemBlock3;

    public SubAllocator() {
        clean();
    }

    void clean() {
        this.subAllocatorSize = 0;
    }

    private void insertNode(final int p/* rarnode ptr */, final int indx) {
        final RarNode temp = this.tempRarNode;
        temp.setAddress(p);
        temp.setNext(this.freeList[indx].getNext());
        this.freeList[indx].setNext(temp);
    }

    public void incPText() {
        this.pText++;
    }

    private int removeNode(final int indx) {
        final int retVal = this.freeList[indx].getNext();
        final RarNode temp = this.tempRarNode;
        temp.setAddress(retVal);
        this.freeList[indx].setNext(temp.getNext());
        return retVal;
    }

    private int U2B(final int NU) {
        return /* 8*NU+4*NU */UNIT_SIZE * NU;
    }

    /* memblockptr */
    private int MBPtr(final int BasePtr, final int Items) {
        return (BasePtr + U2B(Items));
    }

    private void splitBlock(final int pv/* ptr */, final int oldIndx, final int newIndx) {
        int i;
        int uDiff = this.indx2Units[oldIndx] - this.indx2Units[newIndx];
        int p = pv + U2B(this.indx2Units[newIndx]);
        if (this.indx2Units[i = this.units2Indx[uDiff - 1]] != uDiff) {
            insertNode(p, --i);
            p += U2B(i = this.indx2Units[i]);
            uDiff -= i;
        }
        insertNode(p, this.units2Indx[uDiff - 1]);
    }

    public void stopSubAllocator() {
        if (this.subAllocatorSize != 0) {
            this.subAllocatorSize = 0;
            this.heap = null;
            this.heapStart = 1;
            this.tempRarNode = null;
            this.tempRarMemBlock1 = null;
            this.tempRarMemBlock2 = null;
            this.tempRarMemBlock3 = null;
        }
    }

    public int GetAllocatedMemory() {
        return this.subAllocatorSize;
    }

    public boolean startSubAllocator(final int SASize) {
        final int t = SASize << 20;
        if (this.subAllocatorSize == t) {
            return true;
        }
        stopSubAllocator();
        final int allocSize = ((t / FIXED_UNIT_SIZE) * UNIT_SIZE) + UNIT_SIZE;

        // adding space for freelist (needed for poiters)
        // 1+ for null pointer
        int realAllocSize = 1 + allocSize + (4 * N_INDEXES);
        // adding space for an additional memblock
        this.tempMemBlockPos = realAllocSize;
        realAllocSize += RarMemBlock.size;

        this.heap = new byte[realAllocSize];
        this.heapStart = 1;
        this.heapEnd = (this.heapStart + allocSize) - UNIT_SIZE;
        this.subAllocatorSize = t;
        // Bug fixed
        this.freeListPos = this.heapStart + allocSize;
        assert ((realAllocSize - this.tempMemBlockPos) == RarMemBlock.size) :
                realAllocSize + " " + this.tempMemBlockPos + " " + RarMemBlock.size;

        // Init freeList
        for (int i = 0, pos = this.freeListPos; i < this.freeList.length; i++, pos += RarNode.size) {
            this.freeList[i] = new RarNode(this.heap);
            this.freeList[i].setAddress(pos);
        }

        // Init temp fields
        this.tempRarNode = new RarNode(this.heap);
        this.tempRarMemBlock1 = new RarMemBlock(this.heap);
        this.tempRarMemBlock2 = new RarMemBlock(this.heap);
        this.tempRarMemBlock3 = new RarMemBlock(this.heap);

        return true;
    }

    private void glueFreeBlocks() {
        final RarMemBlock s0 = this.tempRarMemBlock1;
        s0.setAddress(this.tempMemBlockPos);
        final RarMemBlock p = this.tempRarMemBlock2;
        final RarMemBlock p1 = this.tempRarMemBlock3;
        int i;
        int k;
        int sz;
        if (this.loUnit != this.hiUnit) {
            this.heap[this.loUnit] = 0;
        }
        for (i = 0, s0.setPrev(s0), s0.setNext(s0); i < N_INDEXES; i++) {
            while (this.freeList[i].getNext() != 0) {
                p.setAddress(removeNode(i));// =(RAR_MEM_BLK*)RemoveNode(i);
                p.insertAt(s0);// p->insertAt(&s0);
                p.setStamp(0xFFFF);// p->Stamp=0xFFFF;
                p.setNU(this.indx2Units[i]);// p->NU=Indx2Units[i];
            }
        }
        for (p.setAddress(s0.getNext()); p.getAddress() != s0.getAddress(); p.setAddress(p.getNext())) {
            // while ((p1=MBPtr(p,p->NU))->Stamp == 0xFFFF && int(p->NU)+p1->NU
            // < 0x10000)
            // Bug fixed
            p1.setAddress(MBPtr(p.getAddress(), p.getNU()));
            while ((p1.getStamp() == 0xFFFF) && ((p.getNU() + p1.getNU()) < 0x10000)) {
                p1.remove();
                p.setNU(p.getNU() + p1.getNU());// ->NU += p1->NU;
                p1.setAddress(MBPtr(p.getAddress(), p.getNU()));
            }
        }
        // while ((p=s0.next) != &s0)
        // Bug fixed
        p.setAddress(s0.getNext());
        while (p.getAddress() != s0.getAddress()) {
            for (p.remove(), sz = p.getNU(); sz > 128; sz -= 128, p.setAddress(MBPtr(p.getAddress(), 128))) {
                insertNode(p.getAddress(), N_INDEXES - 1);
            }
            if (this.indx2Units[i = this.units2Indx[sz - 1]] != sz) {
                k = sz - this.indx2Units[--i];
                insertNode(MBPtr(p.getAddress(), sz - k), k - 1);
            }
            insertNode(p.getAddress(), i);
            p.setAddress(s0.getNext());
        }
    }

    private int allocUnitsRare(final int indx) {
        if (this.glueCount == 0) {
            this.glueCount = 255;
            glueFreeBlocks();
            if (this.freeList[indx].getNext() != 0) {
                return removeNode(indx);
            }
        }
        int i = indx;
        do {
            if (++i == N_INDEXES) {
                this.glueCount--;
                i = U2B(this.indx2Units[indx]);
                final int j = FIXED_UNIT_SIZE * this.indx2Units[indx];
                if ((this.fakeUnitsStart - this.pText) > j) {
                    this.fakeUnitsStart -= j;
                    this.unitsStart -= i;
                    return this.unitsStart;
                }
                return (0);
            }
        } while (this.freeList[i].getNext() == 0);
        final int retVal = removeNode(i);
        splitBlock(retVal, i, indx);
        return retVal;
    }

    public int allocUnits(final int NU) {
        final int indx = this.units2Indx[NU - 1];
        if (this.freeList[indx].getNext() != 0) {
            return removeNode(indx);
        }
        final int retVal = this.loUnit;
        this.loUnit += U2B(this.indx2Units[indx]);
        if (this.loUnit <= this.hiUnit) {
            return retVal;
        }
        this.loUnit -= U2B(this.indx2Units[indx]);
        return allocUnitsRare(indx);
    }

    public int allocContext() {
        if (this.hiUnit != this.loUnit) {
            return (this.hiUnit -= UNIT_SIZE);
        }
        if (this.freeList[0].getNext() != 0) {
            return removeNode(0);
        }
        return allocUnitsRare(0);
    }

    public int expandUnits(final int oldPtr, final int OldNU) {
        final int i0 = this.units2Indx[OldNU - 1];
        final int i1 = this.units2Indx[((OldNU - 1) + 1)];
        if (i0 == i1) {
            return oldPtr;
        }
        final int ptr = allocUnits(OldNU + 1);
        if (ptr != 0) {
            System.arraycopy(this.heap, oldPtr, this.heap, ptr, U2B(OldNU));
            insertNode(oldPtr, i0);
        }
        return ptr;
    }

    public int shrinkUnits(final int oldPtr, final int oldNU, final int newNU) {
        // System.out.println("SubAllocator.shrinkUnits(" + OldPtr + ", " +
        // OldNU + ", " + NewNU + ")");
        final int i0 = this.units2Indx[oldNU - 1];
        final int i1 = this.units2Indx[newNU - 1];
        if (i0 == i1) {
            return oldPtr;
        }
        if (this.freeList[i1].getNext() == 0) {
            splitBlock(oldPtr, i0, i1);
            return oldPtr;
        } else {
            final int ptr = removeNode(i1);
            System.arraycopy(this.heap, oldPtr, this.heap, ptr, U2B(newNU));
            insertNode(oldPtr, i0);
            return ptr;
        }
    }

    public void freeUnits(final int ptr, final int OldNU) {
        insertNode(ptr, this.units2Indx[OldNU - 1]);
    }

    public int getFakeUnitsStart() {
        return this.fakeUnitsStart;
    }

    public void setFakeUnitsStart(final int fakeUnitsStart) {
        this.fakeUnitsStart = fakeUnitsStart;
    }

    public int getHeapEnd() {
        return this.heapEnd;
    }

    public int getPText() {
        return this.pText;
    }

    void setPText(final int text) {
        this.pText = text;
    }

    public void decPText(final int dPText) {
        setPText(getPText() - dPText);
    }

    public int getUnitsStart() {
        return this.unitsStart;
    }

    public void setUnitsStart(final int unitsStart) {
        this.unitsStart = unitsStart;
    }

    public void initSubAllocator() {
        int i;
        int k;
        Arrays.fill(this.heap, this.freeListPos, this.freeListPos + sizeOfFreeList(), (byte) 0);

        this.pText = this.heapStart;

        final int size2 = FIXED_UNIT_SIZE * ((this.subAllocatorSize / 8 / FIXED_UNIT_SIZE) * 7);
        final int realSize2 = (size2 / FIXED_UNIT_SIZE) * UNIT_SIZE;
        final int size1 = this.subAllocatorSize - size2;
        final int realSize1 = ((size1 / FIXED_UNIT_SIZE) * UNIT_SIZE) + (size1 % FIXED_UNIT_SIZE);
        this.hiUnit = this.heapStart + this.subAllocatorSize;
        this.loUnit = this.heapStart + realSize1;
        this.unitsStart = this.heapStart + realSize1;
        this.fakeUnitsStart = this.heapStart + size1;
        this.hiUnit = this.loUnit + realSize2;

        for (i = 0, k = 1; i < N1; i++, k += 1) {
            this.indx2Units[i] = k & 0xff;
        }
        for (k++; i < (N1 + N2); i++, k += 2) {
            this.indx2Units[i] = k & 0xff;
        }
        for (k++; i < (N1 + N2 + N3); i++, k += 3) {
            this.indx2Units[i] = k & 0xff;
        }
        for (k++; i < (N1 + N2 + N3 + N4); i++, k += 4) {
            this.indx2Units[i] = k & 0xff;
        }

        for (this.glueCount = 0, k = 0, i = 0; k < 128; k++) {
            i += ((this.indx2Units[i] < (k + 1)) ? 1 : 0);
            this.units2Indx[k] = i & 0xff;
        }

    }

    private int sizeOfFreeList() {
        return this.freeList.length * RarNode.size;
    }

    public byte[] getHeap() {
        return this.heap;
    }

    // Debug
    public String toString() {
        return "SubAllocator[" + "\n  subAllocatorSize=" + this.subAllocatorSize + "\n  glueCount=" + this.glueCount
               + "\n  heapStart=" + this.heapStart + "\n  loUnit=" + this.loUnit + "\n  hiUnit=" + this.hiUnit
               + "\n  pText=" + this.pText + "\n  unitsStart=" + this.unitsStart + "\n]";
    }

}
