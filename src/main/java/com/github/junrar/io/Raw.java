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

package com.github.junrar.io;

/**
 * Read / write numbers to a byte[] regarding the endianness of the array
 */
public final class Raw {
    private Raw() {
    }

    /**
     * Read a short value from the byte array at the given position (Big Endian)
     *
     * @param array the array to read from
     * @param pos   the position
     * @return the value
     */
    public static short readShortBigEndian(final byte[] array, final int pos) {
        short temp = 0;
        temp |= array[pos] & 0xff;
        temp <<= 8;
        temp |= array[pos + 1] & 0xff;
        return temp;
    }

    /**
     * Read a int value from the byte array at the given position (Big Endian)
     *
     * @param array the array to read from
     * @param pos   the offset
     * @return the value
     */
    public static int readIntBigEndian(final byte[] array, final int pos) {
        int temp = 0;
        temp |= array[pos] & 0xff;
        temp <<= 8;
        temp |= array[pos + 1] & 0xff;
        temp <<= 8;
        temp |= array[pos + 2] & 0xff;
        temp <<= 8;
        temp |= array[pos + 3] & 0xff;
        return temp;
    }

    /**
     * Read a long value from the byte array at the given position (Big Endian)
     *
     * @param array the array to read from
     * @param pos   the offset
     * @return the value
     */
    public static long readLongBigEndian(final byte[] array, final int pos) {
        int temp = 0;
        temp |= array[pos] & 0xff;
        temp <<= 8;
        temp |= array[pos + 1] & 0xff;
        temp <<= 8;
        temp |= array[pos + 2] & 0xff;
        temp <<= 8;
        temp |= array[pos + 3] & 0xff;
        temp <<= 8;
        temp |= array[pos + 4] & 0xff;
        temp <<= 8;
        temp |= array[pos + 5] & 0xff;
        temp <<= 8;
        temp |= array[pos + 6] & 0xff;
        temp <<= 8;
        temp |= array[pos + 7] & 0xff;
        return temp;
    }

    /**
     * Read a short value from the byte array at the given position (little Endian)
     *
     * @param array the array to read from
     * @param pos   the offset
     * @return the value
     */
    public static short readShortLittleEndian(final byte[] array, final int pos) {
        short result = 0;
        result += array[pos + 1] & 0xff;
        result <<= 8;
        result += array[pos] & 0xff;
        return result;
    }

    /**
     * Read an int value from the byte array at the given position (little Endian)
     *
     * @param array the array to read from
     * @param pos   the offset
     * @return the value
     */
    public static int readIntLittleEndian(final byte[] array, final int pos) {
        return ((array[pos + 3] & 0xff) << 24) | ((array[pos + 2] & 0xff) << 16) | ((array[pos + 1] & 0xff) << 8)
               | ((array[pos] & 0xff));
    }

    /**
     * Read an long value(unsigned int) from the byte array at the given position (little Endian)
     */
    public static long readIntLittleEndianAsLong(final byte[] array, final int pos) {
        return (((long) array[pos + 3] & 0xff) << 24) | (((long) array[pos + 2] & 0xff) << 16) | (
                ((long) array[pos + 1] & 0xff) << 8) | (((long) array[pos] & 0xff));
    }

    /**
     * Read a long value from the byte array at the given position (little Endian)
     *
     * @param array the array to read from
     * @param pos   the offset
     * @return the value
     */
    public static long readLongLittleEndian(final byte[] array, final int pos) {
        int temp = 0;
        temp |= array[pos + 7] & 0xff;
        temp <<= 8;
        temp |= array[pos + 6] & 0xff;
        temp <<= 8;
        temp |= array[pos + 5] & 0xff;
        temp <<= 8;
        temp |= array[pos + 4] & 0xff;
        temp <<= 8;
        temp |= array[pos + 3] & 0xff;
        temp <<= 8;
        temp |= array[pos + 2] & 0xff;
        temp <<= 8;
        temp |= array[pos + 1] & 0xff;
        temp <<= 8;
        temp |= array[pos];
        return temp;
    }

    /**
     * Write a short value into the byte array at the given position (Big endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeShortBigEndian(final byte[] array, final int pos, final short value) {
        array[pos] = (byte) (value >>> 8);
        array[pos + 1] = (byte) (value & 0xFF);

    }

    /**
     * Write an int value into the byte array at the given position (Big endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeIntBigEndian(final byte[] array, final int pos, final int value) {
        array[pos] = (byte) ((value >>> 24) & 0xff);
        array[pos + 1] = (byte) ((value >>> 16) & 0xff);
        array[pos + 2] = (byte) ((value >>> 8) & 0xff);
        array[pos + 3] = (byte) ((value) & 0xff);

    }

    /**
     * Write a long value into the byte array at the given position (Big endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeLongBigEndian(final byte[] array, final int pos, final long value) {
        array[pos] = (byte) (value >>> 56);
        array[pos + 1] = (byte) (value >>> 48);
        array[pos + 2] = (byte) (value >>> 40);
        array[pos + 3] = (byte) (value >>> 32);
        array[pos + 4] = (byte) (value >>> 24);
        array[pos + 5] = (byte) (value >>> 16);
        array[pos + 6] = (byte) (value >>> 8);
        array[pos + 7] = (byte) (value & 0xFF);

    }

    /**
     * Write a short value into the byte array at the given position (little endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeShortLittleEndian(final byte[] array, final int pos, final short value) {
        array[pos + 1] = (byte) (value >>> 8);
        array[pos] = (byte) (value & 0xFF);

    }

    /**
     * Increment a short value at the specified position by the specified amount (little endian).
     */
    public static void incShortLittleEndian(final byte[] array, final int pos, final int dv) {
        final int c = ((array[pos] & 0xff) + (dv & 0xff)) >>> 8;
        array[pos] += dv & 0xff;
        if ((c > 0) || ((dv & 0xff00) != 0)) {
            array[pos + 1] += ((dv >>> 8) & 0xff) + c;
        }
    }

    /**
     * Write an int value into the byte array at the given position (little endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeIntLittleEndian(final byte[] array, final int pos, final int value) {
        array[pos + 3] = (byte) (value >>> 24);
        array[pos + 2] = (byte) (value >>> 16);
        array[pos + 1] = (byte) (value >>> 8);
        array[pos] = (byte) (value & 0xFF);

    }

    /**
     * Write a long value into the byte array at the given position (little endian)
     *
     * @param array the array
     * @param pos   the offset
     * @param value the value to write
     */
    public static void writeLongLittleEndian(final byte[] array, final int pos, final long value) {
        array[pos + 7] = (byte) (value >>> 56);
        array[pos + 6] = (byte) (value >>> 48);
        array[pos + 5] = (byte) (value >>> 40);
        array[pos + 4] = (byte) (value >>> 32);
        array[pos + 3] = (byte) (value >>> 24);
        array[pos + 2] = (byte) (value >>> 16);
        array[pos + 1] = (byte) (value >>> 8);
        array[pos] = (byte) (value & 0xFF);

    }
}
