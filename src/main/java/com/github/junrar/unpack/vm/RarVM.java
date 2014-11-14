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

package com.github.junrar.unpack.vm;

import com.github.junrar.crc.RarCRC;
import com.github.junrar.io.Raw;

import java.util.List;
import java.util.Vector;


public class RarVM extends BitInput {
    public static final int VM_GLOBALMEMADDR = 0x3C000;
    public static final int VM_GLOBALMEMSIZE = 0x2000;
    public static final int VM_FIXEDGLOBALSIZE = 64;
    private static final byte VMCF_OP0 = 0;
    private static final byte VMCF_OP1 = 1;
    private static final byte VMCF_OP2 = 2;
    private static final byte VMCF_OPMASK = 3;
    private static final byte VMCF_BYTEMODE = 4;
    private static final byte VMCF_JUMP = 8;
    private static final byte VMCF_PROC = 16;
    private static final byte VMCF_USEFLAGS = 32;
    private static final byte VMCF_CHFLAGS = 64;
    private static final byte[] VM_CmdFlags = {
      /* VM_MOV   */ VMCF_OP2 | VMCF_BYTEMODE,
      /* VM_CMP   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
      /* VM_ADD   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
      /* VM_SUB   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
      /* VM_JZ    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
      /* VM_JNZ   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
      /* VM_INC   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS,
      /* VM_DEC   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS,
      /* VM_JMP   */ VMCF_OP1 | VMCF_JUMP,
	  /* VM_XOR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_AND   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_OR    */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_TEST  */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_JS    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_JNS   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_JB    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_JBE   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_JA    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_JAE   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS,
	  /* VM_PUSH  */ VMCF_OP1,
	  /* VM_POP   */ VMCF_OP1,
	  /* VM_CALL  */ VMCF_OP1 | VMCF_PROC,
	  /* VM_RET   */ VMCF_PROC,
	  /* VM_NOT   */ VMCF_OP1 | VMCF_BYTEMODE,
	  /* VM_SHL   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_SHR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_SAR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_NEG   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS,
	  /* VM_PUSHA */ VMCF_OP0,
	  /* VM_POPA  */ VMCF_OP0,
	  /* VM_PUSHF */ VMCF_USEFLAGS,
	  /* VM_POPF  */ VMCF_CHFLAGS,
	  /* VM_MOVZX */ VMCF_OP2,
	  /* VM_MOVSX */ VMCF_OP2,
	  /* VM_XCHG  */ VMCF_OP2 | VMCF_BYTEMODE,
	  /* VM_MUL   */ VMCF_OP2 | VMCF_BYTEMODE,
	  /* VM_DIV   */ VMCF_OP2 | VMCF_BYTEMODE,
	  /* VM_ADC   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_USEFLAGS | VMCF_CHFLAGS,
	  /* VM_SBB   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_USEFLAGS | VMCF_CHFLAGS,
	  /* VM_PRINT */ VMCF_OP0
    };
    private static final int VM_MEMSIZE = 0x40000;
    private static final int VM_MEMMASK = (VM_MEMSIZE - 1);
    private static final int regCount = 8;
    private final RarCRC rarCRC;
    private final int[] R = new int[regCount];
    private byte[] mem;
    private int flags;

    private int maxOpCount = 25000000;

    private int codeSize;

    private int IP;

    public RarVM() {
        this.mem = null;
        this.rarCRC = new RarCRC();
    }

    public void init() {
        if (this.mem == null) {
            this.mem = new byte[VM_MEMSIZE + 4];
        }
    }

    private boolean isVMMem(final byte[] mem) {
        return this.mem == mem;
    }

    private int getValue(final boolean byteMode, final byte[] mem, final int offset) {
        if (byteMode) {
            return isVMMem(mem) ? mem[offset] : (mem[offset] & 0xff);
        } else {
            return isVMMem(mem) ? Raw.readIntLittleEndian(mem, offset) : Raw.readIntBigEndian(mem, offset);
        }
    }

    private void setValue(final boolean byteMode, final byte[] mem, final int offset, final int value) {
        if (byteMode) {
            mem[offset] = isVMMem(mem) ? (byte) value : (byte) ((byte) (value & 0xff));
        } else {
            if (isVMMem(mem)) {
                Raw.writeIntLittleEndian(mem, offset, value);
            } else {
                Raw.writeIntBigEndian(mem, offset, value);
            }

        }
    }

    public void setLowEndianValue(final byte[] mem, final int offset, final int value) {
        Raw.writeIntLittleEndian(mem, offset, value);
    }

    public void setLowEndianValue(final Vector<Byte> mem, final int offset, final int value) {
        mem.set(offset, (byte) (value & 0xff));
        mem.set(offset + 1, (byte) ((value >>> 8) & 0xff));
        mem.set(offset + 2, (byte) ((value >>> 16) & 0xff));
        mem.set(offset + 3, (byte) ((value >>> 24) & 0xff));
    }

    private int getOperand(final VMPreparedOperand cmdOp) {
        final int ret;
        if (cmdOp.getType() == VMOpType.VM_OPREGMEM) {
            final int pos = (cmdOp.getOffset() + cmdOp.getBase()) & VM_MEMMASK;
            ret = Raw.readIntLittleEndian(this.mem, pos);
        } else {
            final int pos = cmdOp.getOffset();
            ret = Raw.readIntLittleEndian(this.mem, pos);
        }
        return ret;
    }

    public void execute(final VMPreparedProgram prg) {
        System.arraycopy(prg.getInitR(), 0, this.R, 0, prg.getInitR().length);

        final long globalSize = Math.min(prg.getGlobalData()
                                            .size(), VM_GLOBALMEMSIZE);
        if (globalSize != 0) {
            for (int i = 0; i < globalSize; i++) // memcpy(Mem+VM_GLOBALMEMADDR,&Prg->GlobalData[0],GlobalSize);
            {
                this.mem[VM_GLOBALMEMADDR + i] = prg.getGlobalData()
                                                    .get(i);
            }

        }
        final long staticSize = Math.min(prg.getStaticData()
                                            .size(), VM_GLOBALMEMSIZE - globalSize);
        if (staticSize != 0) {
            for (int i = 0; i < staticSize;
                 i++) // memcpy(Mem+VM_GLOBALMEMADDR+GlobalSize,&Prg->StaticData[0],StaticSize);
            {
                this.mem[VM_GLOBALMEMADDR + (int) globalSize + i] = prg.getStaticData()
                                                                       .get(i);
            }

        }
        this.R[7] = VM_MEMSIZE;
        this.flags = 0;

        final List<VMPreparedCommand> preparedCode = !prg.getAltCmd()
                                                         .isEmpty() ? prg.getAltCmd() : prg.getCmd();

        if (!ExecuteCode(preparedCode, prg.getCmdCount())) {
            preparedCode.get(0)
                        .setOpCode(VMCommands.VM_RET);
        }
        int newBlockPos = getValue(false, this.mem, VM_GLOBALMEMADDR + 0x20) & VM_MEMMASK;
        int newBlockSize = getValue(false, this.mem, VM_GLOBALMEMADDR + 0x1c) & VM_MEMMASK;
        if ((newBlockPos + newBlockSize) >= VM_MEMSIZE) {
            newBlockPos = 0;
            newBlockSize = 0;
        }

        prg.setFilteredDataOffset(newBlockPos);
        prg.setFilteredDataSize(newBlockSize);

        prg.getGlobalData()
           .clear();

        final int dataSize = Math.min(getValue(false, this.mem, VM_GLOBALMEMADDR + 0x30),
                                      VM_GLOBALMEMSIZE - VM_FIXEDGLOBALSIZE);
        if (dataSize != 0) {
            prg.getGlobalData()
               .setSize(dataSize + VM_FIXEDGLOBALSIZE);
            // ->GlobalData.Add(dataSize+VM_FIXEDGLOBALSIZE);

            for (int i = 0; i < (dataSize + VM_FIXEDGLOBALSIZE);
                 i++) // memcpy(&Prg->GlobalData[0],&Mem[VM_GLOBALMEMADDR],DataSize+VM_FIXEDGLOBALSIZE);
            {
                prg.getGlobalData()
                   .set(i, this.mem[VM_GLOBALMEMADDR + i]);
            }
        }
    }

    public byte[] getMem() {
        return this.mem;
    }

    private boolean setIP(final int ip) {
        if ((ip) >= this.codeSize) {
            return (true);
        }

        if (--this.maxOpCount <= 0) {
            return (false);
        }

        this.IP = ip;
        return true;
    }

    private boolean ExecuteCode(final List<VMPreparedCommand> preparedCode, final int cmdCount) {

        this.maxOpCount = 25000000;
        this.codeSize = cmdCount;
        this.IP = 0;

        while (true) {
            final VMPreparedCommand cmd = preparedCode.get(this.IP);
            final int op1 = getOperand(cmd.getOp1());
            final int op2 = getOperand(cmd.getOp2());
            switch (cmd.getOpCode()) {
                case VM_MOV:
                    setValue(cmd.isByteMode(), this.mem, op1, getValue(cmd.isByteMode(), this.mem,
                                                                       op2)); // SET_VALUE(Cmd->ByteMode,Op1,
                    // GET_VALUE(Cmd->ByteMode,Op2));
                    break;
                case VM_MOVB:
                    setValue(true, this.mem, op1, getValue(true, this.mem, op2));
                    break;
                case VM_MOVD:
                    setValue(false, this.mem, op1, getValue(false, this.mem, op2));
                    break;

                case VM_CMP: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int result = value1 - getValue(cmd.isByteMode(), this.mem, op2);

                    if (result == 0) {
                        this.flags = VMFlags.VM_FZ.flag();
                    } else {
                        this.flags = (result > value1) ? 1 : (result & VMFlags.VM_FS.flag());
                    }
                }
                break;

                case VM_CMPB: {
                    final int value1 = getValue(true, this.mem, op1);
                    final int result = value1 - getValue(true, this.mem, op2);
                    if (result == 0) {
                        this.flags = VMFlags.VM_FZ.flag();
                    } else {
                        this.flags = (result > value1) ? 1 : (result & VMFlags.VM_FS.flag());
                    }
                }
                break;
                case VM_CMPD: {
                    final int value1 = getValue(false, this.mem, op1);
                    final int result = value1 - getValue(false, this.mem, op2);
                    if (result == 0) {
                        this.flags = VMFlags.VM_FZ.flag();
                    } else {
                        this.flags = (result > value1) ? 1 : (result & VMFlags.VM_FS.flag());
                    }
                }
                break;

                case VM_ADD: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    int result = (int) ((((long) value1 + (long) getValue(cmd.isByteMode(), this.mem, op2))));
                    if (cmd.isByteMode()) {
                        result &= 0xff;
                        if (result == 0) {
                            this.flags = (result < value1) ? 1 : VMFlags.VM_FZ.flag();
                        } else {
                            if ((result & 0x80) == 0) {
                                this.flags = (result < value1) ? 1 : 0;
                            } else {
                                this.flags = (result < value1) ? 1 : VMFlags.VM_FS.flag();
                            }
                        }
                        // Flags=(Result<Value1)|(Result==0 ? VM_FZ:((Result&0x80) ?
                        // VM_FS:0));
                    } else {
                        if (result == 0) {
                            this.flags = (result < value1) ? 1 : VMFlags.VM_FZ.flag();
                        } else {
                            this.flags = (result < value1) ? 1 : (result & VMFlags.VM_FS.flag());
                        }
                    }
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;

                case VM_ADDB:
                    setValue(true, this.mem, op1, (int) ((long) getValue(true, this.mem, op1) & (0xFFffFFff
                                                                                                 + (long) getValue(true,
                                                                                                                   this.mem,
                                                                                                                   op2))));
                    break;
                case VM_ADDD:
                    setValue(false, this.mem, op1, (int) ((long) getValue(false, this.mem, op1) & (0xFFffFFff
                                                                                                   + (long) getValue(
                            false, this.mem, op2))));
                    break;

                case VM_SUB: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int result = (int) ((long) value1 & (0xffFFffFF - (long) getValue(cmd.isByteMode(), this.mem,
                                                                                            op2)));
                    if (result > value1) {
                        this.flags = (result == 0) ? VMFlags.VM_FZ.flag() : 1;
                    } else {
                        this.flags = (result == 0) ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag());
                    }
                    setValue(cmd.isByteMode(), this.mem, op1, result);// (Cmd->ByteMode,Op1,Result);
                }
                break;

                case VM_SUBB:
                    setValue(true, this.mem, op1, (int) ((long) getValue(true, this.mem, op1) & (0xFFffFFff
                                                                                                 - (long) getValue(true,
                                                                                                                   this.mem,
                                                                                                                   op2))));
                    break;
                case VM_SUBD:
                    setValue(false, this.mem, op1, (int) ((long) getValue(false, this.mem, op1)
                                                          & 0xFFffFFff - (long) getValue(false, this.mem, op2)));
                    break;

                case VM_JZ:
                    if ((this.flags & VMFlags.VM_FZ.flag()) != 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JNZ:
                    if ((this.flags & VMFlags.VM_FZ.flag()) == 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_INC: {
                    int result = (int) (0L);
                    if (cmd.isByteMode()) {
                        result &= 0xff;
                    }

                    setValue(cmd.isByteMode(), this.mem, op1, result);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                }
                break;

                case VM_INCB:
                    setValue(true, this.mem, op1, (int) (0L));
                    break;
                case VM_INCD:
                    setValue(false, this.mem, op1, (int) (0L));
                    break;

                case VM_DEC: {
                    final int result = (int) ((long) getValue(cmd.isByteMode(), this.mem, op1) & 0xFFffFFff - 1);
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                }
                break;

                case VM_DECB:
                    setValue(true, this.mem, op1, (int) ((long) getValue(true, this.mem, op1) & 0xFFffFFff - 1));
                    break;
                case VM_DECD:
                    setValue(false, this.mem, op1, (int) ((long) getValue(false, this.mem, op1) & 0xFFffFFff - 1));
                    break;

                case VM_JMP:
                    setIP(getValue(false, this.mem, op1));
                    continue;
                case VM_XOR: {
                    final int result = getValue(cmd.isByteMode(), this.mem, op1) ^ getValue(cmd.isByteMode(), this.mem,
                                                                                            op2);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_AND: {
                    final int result = getValue(cmd.isByteMode(), this.mem, op1) & getValue(cmd.isByteMode(), this.mem,
                                                                                            op2);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_OR: {
                    final int result = getValue(cmd.isByteMode(), this.mem, op1) | getValue(cmd.isByteMode(), this.mem,
                                                                                            op2);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_TEST: {
                    final int result = getValue(cmd.isByteMode(), this.mem, op1) & getValue(cmd.isByteMode(), this.mem,
                                                                                            op2);
                    this.flags = result == 0 ? VMFlags.VM_FZ.flag() : result & VMFlags.VM_FS.flag();
                }
                break;
                case VM_JS:
                    if ((this.flags & VMFlags.VM_FS.flag()) != 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JNS:
                    if ((this.flags & VMFlags.VM_FS.flag()) == 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JB:
                    if ((this.flags & VMFlags.VM_FC.flag()) != 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JBE:
                    if ((this.flags & (VMFlags.VM_FC.flag() | VMFlags.VM_FZ.flag())) != 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JA:
                    if ((this.flags & (VMFlags.VM_FC.flag() | VMFlags.VM_FZ.flag())) == 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_JAE:
                    if ((this.flags & VMFlags.VM_FC.flag()) == 0) {
                        setIP(getValue(false, this.mem, op1));
                        continue;
                    }
                    break;
                case VM_PUSH:
                    this.R[7] -= 4;
                    setValue(false, this.mem, this.R[7] & VM_MEMMASK, getValue(false, this.mem, op1));
                    break;
                case VM_POP:
                    setValue(false, this.mem, op1, getValue(false, this.mem, this.R[7] & VM_MEMMASK));
                    this.R[7] += 4;
                    break;
                case VM_CALL:
                    this.R[7] -= 4;
                    setValue(false, this.mem, this.R[7] & VM_MEMMASK, this.IP + 1);
                    setIP(getValue(false, this.mem, op1));
                    continue;
                case VM_NOT:
                    setValue(cmd.isByteMode(), this.mem, op1, ~getValue(cmd.isByteMode(), this.mem, op1));
                    break;
                case VM_SHL: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int value2 = getValue(cmd.isByteMode(), this.mem, op2);
                    final int result = value1 << value2;
                    this.flags = (result == 0 ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag())) | (
                            ((value1 << (value2 - 1)) & 0x80000000) != 0 ? VMFlags.VM_FC.flag() : 0);
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_SHR: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int value2 = getValue(cmd.isByteMode(), this.mem, op2);
                    final int result = value1 >>> value2;
                    this.flags = (result == 0 ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag())) | (
                            (value1 >>> (value2 - 1)) & VMFlags.VM_FC.flag());
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_SAR: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int value2 = getValue(cmd.isByteMode(), this.mem, op2);
                    final int result = value1 >> value2;
                    this.flags = (result == 0 ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag())) | (
                            (value1 >> (value2 - 1)) & VMFlags.VM_FC.flag());
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_NEG: {
                    final int result = -getValue(cmd.isByteMode(), this.mem, op1);
                    this.flags =
                            result == 0 ? VMFlags.VM_FZ.flag() : VMFlags.VM_FC.flag() | (result & VMFlags.VM_FS.flag());
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;

                case VM_NEGB:
                    setValue(true, this.mem, op1, -getValue(true, this.mem, op1));
                    break;
                case VM_NEGD:
                    setValue(false, this.mem, op1, -getValue(false, this.mem, op1));
                    break;
                case VM_PUSHA:
                    for (int i = 0, SP = this.R[7] - 4; i < regCount; i++, SP -= 4) {
                        setValue(false, this.mem, SP & VM_MEMMASK, this.R[i]);
                    }
                    this.R[7] -= regCount * 4;
                    break;
                case VM_POPA:
                    for (int i = 0, SP = this.R[7]; i < regCount; i++, SP += 4) {
                        this.R[7 - i] = getValue(false, this.mem, SP & VM_MEMMASK);
                    }
                    break;
                case VM_PUSHF:
                    this.R[7] -= 4;
                    setValue(false, this.mem, this.R[7] & VM_MEMMASK, this.flags);
                    break;
                case VM_POPF:
                    this.flags = getValue(false, this.mem, this.R[7] & VM_MEMMASK);
                    this.R[7] += 4;
                    break;
                case VM_MOVZX:
                    setValue(false, this.mem, op1, getValue(true, this.mem, op2));
                    break;
                case VM_MOVSX:
                    setValue(false, this.mem, op1, (byte) getValue(true, this.mem, op2));
                    break;
                case VM_XCHG: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    setValue(cmd.isByteMode(), this.mem, op1, getValue(cmd.isByteMode(), this.mem, op2));
                    setValue(cmd.isByteMode(), this.mem, op2, value1);
                }
                break;
                case VM_MUL: {
                    final int result = (int) (((long) getValue(cmd.isByteMode(), this.mem, op1)
                                               & 0xFFffFFff * (long) getValue(cmd.isByteMode(), this.mem, op2)));
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_DIV:
                    final int divider = getValue(cmd.isByteMode(), this.mem, op2);
                    if (divider != 0) {
                        final int result = getValue(cmd.isByteMode(), this.mem, op1) / divider;
                        setValue(cmd.isByteMode(), this.mem, op1, result);
                    }
                    break;
                case VM_ADC: {
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int FC = (this.flags & VMFlags.VM_FC.flag());
                    int result = (int) ((long) value1 & 0xFFffFFff + (long) getValue(cmd.isByteMode(), this.mem, op2)
                                        & 0xFFffFFff + (long) FC);
                    if (cmd.isByteMode()) {
                        result &= 0xff;
                    }

                    this.flags = (result < value1 || result == value1 && FC != 0) ? 1 :
                                 (result == 0 ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag()));
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                }
                break;
                case VM_SBB:
                    final int value1 = getValue(cmd.isByteMode(), this.mem, op1);
                    final int FC = (this.flags & VMFlags.VM_FC.flag());
                    int result = (int) ((long) value1 & 0xFFffFFff - (long) getValue(cmd.isByteMode(), this.mem, op2)
                                        & 0xFFffFFff - (long) FC);
                    if (cmd.isByteMode()) {
                        result &= 0xff;
                    }
                    this.flags = (result > value1 || result == value1 && FC != 0) ? 1 :
                                 (result == 0 ? VMFlags.VM_FZ.flag() : (result & VMFlags.VM_FS.flag()));
                    setValue(cmd.isByteMode(), this.mem, op1, result);
                    break;

                case VM_RET:
                    if (this.R[7] >= VM_MEMSIZE) {
                        return (true);
                    }
                    setIP(getValue(false, this.mem, this.R[7] & VM_MEMMASK));
                    this.R[7] += 4;
                    continue;

                case VM_STANDARD:
                    ExecuteStandardFilter(VMStandardFilters.findFilter(cmd.getOp1()
                                                                          .getData()));
                    break;
                case VM_PRINT:
                    break;
            }
            this.IP++;
            --this.maxOpCount;
        }
    }

    public void prepare(final byte[] code, final int codeSize, final VMPreparedProgram prg) {
        int codeSize1 = codeSize;
        InitBitInput();
        final int cpLength = Math.min(MAX_SIZE, codeSize1);
        for (int i = 0; i < cpLength; i++) // memcpy(inBuf,Code,Min(CodeSize,BitInput::MAX_SIZE));
        {
            this.inBuf[i] |= code[i];
        }

        byte xorSum = 0;
        for (int i = 1; i < codeSize1; i++) {
            xorSum ^= code[i];
        }

        faddbits(8);

        prg.setCmdCount(0);
        if (xorSum == code[0]) {
            final VMStandardFilters filterType = IsStandardFilter(code);
            if (filterType != VMStandardFilters.VMSF_NONE) {

                final VMPreparedCommand curCmd = new VMPreparedCommand();
                curCmd.setOpCode(VMCommands.VM_STANDARD);
                curCmd.getOp1()
                      .setData(filterType.getFilter());
                curCmd.getOp1()
                      .setType(VMOpType.VM_OPNONE);
                curCmd.getOp2()
                      .setType(VMOpType.VM_OPNONE);
                codeSize1 = 0;
                prg.getCmd()
                   .add(curCmd);
                prg.setCmdCount(prg.getCmdCount() + 1);
                // TODO
                // curCmd->Op1.Data=FilterType;
                // >>>>>> CurCmd->Op1.Addr=&CurCmd->Op1.Data; <<<<<<<<<< not set
                // do i need to ?
                // >>>>>> CurCmd->Op2.Addr=&CurCmd->Op2.Data; <<<<<<<<<< "
                // CurCmd->Op1.Type=CurCmd->Op2.Type=VM_OPNONE;
                // CodeSize=0;
            }
            final int dataFlag = fgetbits();
            faddbits(1);

            // Read static data contained in DB operators. This data cannot be
            // changed,
            // it is a part of VM code, not a filter parameter.

            if ((dataFlag & 0x8000) != 0) {
                final long dataSize = 0L;
                for (int i = 0; this.inAddr < codeSize1 && i < dataSize; i++) {
                    prg.getStaticData()
                       .add((byte) (fgetbits() >> 8));
                    faddbits(8);
                }
            }

            while (this.inAddr < codeSize1) {
                final VMPreparedCommand curCmd = new VMPreparedCommand();
                final int data = fgetbits();
                if ((data & 0x8000) == 0) {
                    curCmd.setOpCode(VMCommands.findVMCommand((data >> 12)));
                    faddbits(4);
                } else {
                    curCmd.setOpCode(VMCommands.findVMCommand((data >> 10) - 24));
                    faddbits(6);
                }
                if ((VM_CmdFlags[curCmd.getOpCode()
                                       .getVMCommand()] & VMCF_BYTEMODE) == 0) {
                    curCmd.setByteMode(false);
                } else {
                    curCmd.setByteMode((fgetbits() >> 15) == 1);
                    faddbits(1);
                }
                curCmd.getOp1()
                      .setType(VMOpType.VM_OPNONE);
                curCmd.getOp2()
                      .setType(VMOpType.VM_OPNONE);

                final int opNum = (VM_CmdFlags[curCmd.getOpCode()
                                                     .getVMCommand()] & VMCF_OPMASK);
                // TODO >>> CurCmd->Op1.Addr=CurCmd->Op2.Addr=NULL; <<<???
                if (opNum > 0) {
                    decodeArg(curCmd.getOp1(), curCmd.isByteMode());
                    if (opNum == 2) {
                        decodeArg(curCmd.getOp2(), curCmd.isByteMode());
                    } else {
                        if (curCmd.getOp1()
                                  .getType() == VMOpType.VM_OPINT && (VM_CmdFlags[curCmd.getOpCode()
                                                                                        .getVMCommand()] & (VMCF_JUMP
                                                                                                            |
                                                                                                            VMCF_PROC))
                                                                     != 0) {
                            int distance = curCmd.getOp1()
                                                 .getData();
                            if (distance >= 256) {
                                distance -= 256;
                            } else {
                                if (distance >= 136) {
                                    distance -= 264;
                                } else {
                                    if (distance >= 16) {
                                        distance -= 8;
                                    } else {
                                        if (distance >= 8) {
                                            distance -= 16;
                                        }
                                    }
                                }
                                distance += prg.getCmdCount();
                            }
                            curCmd.getOp1()
                                  .setData(distance);
                        }
                    }
                }
                prg.setCmdCount(prg.getCmdCount() + 1);
                prg.getCmd()
                   .add(curCmd);
            }
        }
        final VMPreparedCommand curCmd = new VMPreparedCommand();
        curCmd.setOpCode(VMCommands.VM_RET);
        curCmd.getOp1()
              .setType(VMOpType.VM_OPNONE);
        curCmd.getOp2()
              .setType(VMOpType.VM_OPNONE);

        prg.getCmd()
           .add(curCmd);
        prg.setCmdCount(prg.getCmdCount() + 1);
        // #ifdef VM_OPTIMIZE
        if (codeSize1 != 0) {
            optimize(prg);
        }
    }

    private void decodeArg(final VMPreparedOperand op, final boolean byteMode) {
        final int data = fgetbits();
        if ((data & 0x8000) == 0) {
            if ((data & 0xc000) == 0) {
                op.setType(VMOpType.VM_OPINT);
                if (byteMode) {
                    op.setData((data >> 6) & 0xff);
                    faddbits(10);
                } else {
                    faddbits(2);
                    op.setData(ReadData(this));
                }
            } else {
                op.setType(VMOpType.VM_OPREGMEM);
                if ((data & 0x2000) == 0) {
                    op.setData((data >> 10) & 7);
                    op.setOffset(op.getData());
                    op.setBase(0);
                    faddbits(6);
                } else {
                    if ((data & 0x1000) == 0) {
                        op.setData((data >> 9) & 7);
                        op.setOffset(op.getData());
                        faddbits(7);
                    } else {
                        op.setData(0);
                        faddbits(4);
                    }
                    op.setBase(ReadData(this));
                }
            }
        } else {
            op.setType(VMOpType.VM_OPREG);
            op.setData((data >> 12) & 7);
            op.setOffset(op.getData());
            faddbits(4);
        }

    }

    private void optimize(final VMPreparedProgram prg) {
        final List<VMPreparedCommand> commands = prg.getCmd();

        for (final VMPreparedCommand cmd : commands) {
            switch (cmd.getOpCode()) {
                case VM_MOV:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_MOVB : VMCommands.VM_MOVD);
                    continue;
                case VM_CMP:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_CMPB : VMCommands.VM_CMPD);
                    continue;
            }
            if ((VM_CmdFlags[cmd.getOpCode()
                                .getVMCommand()] & VMCF_CHFLAGS) == 0) {
                continue;
            }
            boolean flagsRequired = false;

            for (int i = commands.indexOf(cmd) + 1; i < commands.size(); i++) {
                final int flags = VM_CmdFlags[commands.get(i)
                                                      .getOpCode()
                                                      .getVMCommand()];
                if ((flags & (VMCF_JUMP | VMCF_PROC | VMCF_USEFLAGS)) != 0) {
                    flagsRequired = true;
                    break;
                }
                if ((flags & VMCF_CHFLAGS) != 0) {
                    break;
                }
            }
            if (flagsRequired) {
                continue;
            }
            switch (cmd.getOpCode()) {
                case VM_ADD:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_ADDB : VMCommands.VM_ADDD);
                    continue;
                case VM_SUB:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_SUBB : VMCommands.VM_SUBD);
                    continue;
                case VM_INC:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_INCB : VMCommands.VM_INCD);
                    continue;
                case VM_DEC:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_DECB : VMCommands.VM_DECD);
                    continue;
                case VM_NEG:
                    cmd.setOpCode(cmd.isByteMode() ? VMCommands.VM_NEGB : VMCommands.VM_NEGD);
            }
        }

    }

    private VMStandardFilters IsStandardFilter(final byte[] code) {
        final VMStandardFilterSignature[] stdList = {
                new VMStandardFilterSignature(53, 0xad576887, VMStandardFilters.VMSF_E8), new VMStandardFilterSignature(

                57, 0x3cd7e57e, VMStandardFilters.VMSF_E8E9), new VMStandardFilterSignature(120, 0x3769893f,
                                                                                            VMStandardFilters
                                                                                                    .VMSF_ITANIUM),
                new VMStandardFilterSignature(29, 0x0e06077d, VMStandardFilters.VMSF_DELTA),
                new VMStandardFilterSignature(149, 0x1c2c5dc8, VMStandardFilters.VMSF_RGB),
                new VMStandardFilterSignature(216, 0xbc85e701, VMStandardFilters.VMSF_AUDIO),
                new VMStandardFilterSignature(40, 0x46b9c560, VMStandardFilters.VMSF_UPCASE)
        };
        final int CodeCRC = ~this.rarCRC.checkCrc(0xffffffff, code, 0, code.length);
        for (final VMStandardFilterSignature aStdList : stdList) {
            if ((aStdList.getCRC() == CodeCRC) && (aStdList.getLength() == code.length)) {
                return (aStdList.getType());
            }

        }
        return (VMStandardFilters.VMSF_NONE);
    }

    private void ExecuteStandardFilter(final VMStandardFilters filterType) {
        switch (filterType) {
            case VMSF_NONE:
                break;
            case VMSF_E8:
            case VMSF_E8E9: {
                final int dataSize = this.R[4];
                final long fileOffset = this.R[6];

                if (dataSize >= VM_GLOBALMEMADDR) {
                    break;
                }
                final int fileSize = 0x1000000;
                final byte cmpByte2 = (byte) ((filterType == VMStandardFilters.VMSF_E8E9) ? 0xe9 : 0xe8);
                int curPos = 0;
                while (curPos < (dataSize - 4)) {
                    final byte curByte = this.mem[curPos++];
                    if ((curByte == 0xe8) || (curByte == cmpByte2)) {
                        final long offset = curPos + fileOffset;
                        final long Addr = getValue(false, this.mem, curPos);
                        if ((Addr & 0x80000000) == 0) {
                            if (((Addr - fileSize) & 0x80000000) != 0) {
                                setValue(false, this.mem, curPos, (int) (Addr - offset));
                            }
                        } else {
                            if (((Addr + offset) & 0x80000000) == 0) {
                                setValue(false, this.mem, curPos, (int) Addr + fileSize);
                            }
                        }
                        curPos += 4;
                    }
                }
            }
            break;
            case VMSF_ITANIUM: {

                final int dataSize = this.R[4];
                long fileOffset = this.R[6];

                if (dataSize >= VM_GLOBALMEMADDR) {
                    break;
                }
                int curPos = 0;
                final byte[] Masks = {4, 4, 6, 6, 0, 0, 7, 7, 4, 4, 0, 0, 4, 4, 0, 0};
                fileOffset >>>= 4;

                while (curPos < (dataSize - 21)) {
                    final int Byte = (this.mem[curPos] & 0x1f) - 0x10;
                    if (Byte >= 0) {

                        final byte cmdMask = Masks[Byte];
                        if (cmdMask != 0) {
                            for (int i = 0; i <= 2; i++) {
                                if ((cmdMask & (1 << i)) != 0) {
                                    final int startPos = (i * 41) + 5;
                                    final int opType = filterItanium_GetBits(curPos, startPos + 37, 4);
                                    if (opType == 5) {
                                        final int offset = filterItanium_GetBits(curPos, startPos + 13, 20);
                                        filterItanium_SetBits(curPos, (int) (offset - fileOffset) & 0xfffff,
                                                              startPos + 13, 20);
                                    }
                                }
                            }
                        }
                    }
                    curPos += 16;
                    fileOffset++;
                }
            }
            break;
            case VMSF_DELTA: {
                final int dataSize = this.R[4];
                final int channels = this.R[0];
                int srcPos = 0;
                final int border = (dataSize * 2);
                setValue(false, this.mem, VM_GLOBALMEMADDR + 0x20, dataSize);
                if (dataSize >= (VM_GLOBALMEMADDR / 2)) {
                    break;
                }
//		 bytes from same channels are grouped to continual data blocks,
//		 so we need to place them back to their interleaving positions

                for (int curChannel = 0; curChannel < channels; curChannel++) {
                    byte PrevByte = 0;
                    for (int destPos = dataSize + curChannel; destPos < border; destPos += channels) {
                        this.mem[destPos] = (PrevByte -= this.mem[srcPos++]);
                    }

                }
            }
            break;
            case VMSF_RGB: {
                final int dataSize = this.R[4];
                final int width = this.R[0] - 3;
                final int posR = this.R[1];
                final int channels = 3;
                int srcPos = 0;
                setValue(false, this.mem, VM_GLOBALMEMADDR + 0x20, dataSize);
                if ((dataSize >= (VM_GLOBALMEMADDR / 2)) || (posR < 0)) {
                    break;
                }
                for (int curChannel = 0; curChannel < channels; curChannel++) {
                    long prevByte = 0;

                    for (int i = curChannel; i < dataSize; i += channels) {
                        long predicted;
                        final int upperPos = i - width;
                        if (upperPos >= 3) {
                            final int upperDataPos = dataSize + upperPos;
                            final int upperByte = this.mem[upperDataPos] & 0xff;
                            final int upperLeftByte = this.mem[upperDataPos - 3] & 0xff;
                            predicted = (prevByte + upperByte) - upperLeftByte;
                            final int pa = Math.abs((int) (predicted - prevByte));
                            final int pb = Math.abs((int) (predicted - upperByte));
                            final int pc = Math.abs((int) (predicted - upperLeftByte));
                            if ((pa <= pb) && (pa <= pc)) {
                                predicted = prevByte;
                            } else {
                                predicted = (pb <= pc) ? upperByte : upperLeftByte;
                            }
                        } else {
                            predicted = prevByte;
                        }

                        prevByte = ((predicted - this.mem[srcPos++]) & 0xff) & 0xff;
                        this.mem[dataSize + i] = (byte) (prevByte & 0xff);

                    }
                }
                for (int i = posR, border = dataSize - 2; i < border; i += 3) {
                    final byte G = this.mem[dataSize + i + 1];
                    this.mem[dataSize + i] += G;
                    this.mem[dataSize + i + 2] += G;
                }
            }
            break;
            case VMSF_AUDIO: {
                final int dataSize = this.R[4];
                final int channels = this.R[0];
                int srcPos = 0;
                setValue(false, this.mem, VM_GLOBALMEMADDR + 0x20, dataSize);
                if (dataSize >= (VM_GLOBALMEMADDR / 2)) {
                    break;
                }
                for (int curChannel = 0; curChannel < channels; curChannel++) {
                    long prevByte = 0;
                    long prevDelta = 0;
                    final long[] Dif = new long[7];
                    int D1 = 0;
                    int D2 = 0;
                    int D3;
                    int K1 = 0;
                    int K2 = 0;
                    int K3 = 0;

                    for (int i = curChannel, byteCount = 0; i < dataSize; i += channels, byteCount++) {
                        D3 = D2;
                        D2 = (int) prevDelta - D1;
                        D1 = (int) prevDelta;

                        long predicted = (8 * prevByte) + (K1 * D1) + (K2 * D2) + (K3 * D3);
                        predicted = (predicted >>> 3) & 0xff;

                        final long curByte = this.mem[srcPos++] & 0xff;

                        predicted -= curByte;
                        this.mem[dataSize + i] = (byte) predicted;
                        prevDelta = (byte) (predicted - prevByte);
                        prevByte = predicted;

                        final int D = ((byte) curByte) << 3;

                        Dif[0] += Math.abs(D);
                        Dif[1] += Math.abs(D - D1);
                        Dif[2] += Math.abs(D + D1);
                        Dif[3] += Math.abs(D - D2);
                        Dif[4] += Math.abs(D + D2);
                        Dif[5] += Math.abs(D - D3);
                        Dif[6] += Math.abs(D + D3);

                        if ((byteCount & 0x1f) == 0) {
                            long minDif = Dif[0];
                            long numMinDif = 0;
                            Dif[0] = 0;
                            for (int j = 1; j < Dif.length; j++) {
                                if (Dif[j] < minDif) {
                                    minDif = Dif[j];
                                    numMinDif = j;
                                }
                                Dif[j] = 0;
                            }
                            switch ((int) numMinDif) {
                                case 1:
                                    if (K1 >= -16) {
                                        K1--;
                                    }
                                    break;
                                case 2:
                                    if (K1 < 16) {
                                        K1++;
                                    }
                                    break;
                                case 3:
                                    if (K2 >= -16) {
                                        K2--;
                                    }
                                    break;
                                case 4:
                                    if (K2 < 16) {
                                        K2++;
                                    }
                                    break;
                                case 5:
                                    if (K3 >= -16) {
                                        K3--;
                                    }
                                    break;
                                case 6:
                                    if (K3 < 16) {
                                        K3++;
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
            break;
            case VMSF_UPCASE:
                final int dataSize = this.R[4];
                int srcPos = 0;
                int destPos = dataSize;
                if (dataSize >= (VM_GLOBALMEMADDR / 2)) {
                    break;
                }
                while (srcPos < dataSize) {
                    byte curByte = this.mem[srcPos++];
                    if ((curByte == 2) && (((curByte = this.mem[srcPos++])) != 2)) {
                        curByte -= 32;
                    }
                    this.mem[destPos++] = curByte;
                }
                setValue(false, this.mem, VM_GLOBALMEMADDR + 0x1c, destPos - dataSize);
                setValue(false, this.mem, VM_GLOBALMEMADDR + 0x20, dataSize);
                break;
        }

    }

    private void filterItanium_SetBits(final int curPos, int bitField, final int bitPos, final int bitCount) {
        final int inAddr = bitPos / 8;
        final int inBit = bitPos & 7;
        int andMask = 0xffffffff >>> (32 - bitCount);
        andMask = ~(andMask << inBit);

        bitField <<= inBit;

        for (int i = 0; i < 4; i++) {
            this.mem[curPos + inAddr + i] &= andMask;
            this.mem[curPos + inAddr + i] |= bitField;
            andMask = (andMask >>> 8) | 0xff000000;
            bitField >>>= 8;
        }

    }

    private int filterItanium_GetBits(final int curPos, final int bitPos, final int bitCount) {
        int inAddr = bitPos / 8;
        final int inBit = bitPos & 7;
        int bitField = this.mem[curPos + inAddr++] & 0xff;
        bitField |= (this.mem[curPos + inAddr++] & 0xff) << 8;
        bitField |= (this.mem[curPos + inAddr++] & 0xff) << 16;
        bitField |= (this.mem[curPos + inAddr] & 0xff) << 24;
        bitField >>>= inBit;
        return (bitField & (0xffffffff >>> (32 - bitCount)));
    }

    public void setMemory(final int pos, final byte[] data, final int offset, final int dataSize) {
        if (pos < VM_MEMSIZE) { //&& data!=Mem+Pos)
            //memmove(Mem+Pos,Data,Min(DataSize,VM_MEMSIZE-Pos));
            for (int i = 0; i < Math.min(data.length - offset, dataSize); i++) {
                if ((VM_MEMSIZE - pos) < i) {
                    break;
                }
                this.mem[pos + i] = data[offset + i];
            }
        }
    }

    public static int ReadData(final BitInput rarVM) {
        int data = rarVM.fgetbits();
        switch (data & 0xc000) {
            case 0:
                rarVM.faddbits(6);
                return ((data >> 10) & 0xf);
            case 0x4000:
                if ((data & 0x3c00) == 0) {
                    data = 0xffffff00 | ((data >> 2) & 0xff);
                    rarVM.faddbits(14);
                } else {
                    data = (data >> 6) & 0xff;
                    rarVM.faddbits(10);
                }
                return (data);
            case 0x8000:
                rarVM.faddbits(2);
                data = rarVM.fgetbits();
                rarVM.faddbits(16);
                return (data);
            default:
                rarVM.faddbits(2);
                data = (rarVM.fgetbits() << 16);
                rarVM.faddbits(16);
                data |= rarVM.fgetbits();
                rarVM.faddbits(16);
                return (data);
        }
    }


}

//