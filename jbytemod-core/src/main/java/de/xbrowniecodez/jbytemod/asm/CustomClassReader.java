package de.xbrowniecodez.jbytemod.asm;

import java.lang.reflect.Field;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class CustomClassReader extends ClassReader {

    public CustomClassReader(byte[] classFile) {
        super(classFile);
    }

    private byte[] getClassFileBuffer() {
        try {
            Field field = ClassReader.class.getDeclaredField("classFileBuffer");
            field.setAccessible(true);
            return (byte[]) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    private int[] getCpInfoOffsets() {
        try {
            Field field = ClassReader.class.getDeclaredField("cpInfoOffsets");
            field.setAccessible(true);
            return (int[]) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new int[] {};
    }

    @Override
    public short readShort(int offset) {
        byte[] classBuffer = this.getClassFileBuffer();
        if (offset >= classBuffer.length)
            return Opcodes.V1_7;
        return (short) ((classBuffer[offset] & 255) << 8 | classBuffer[offset + 1] & 255);
    }


    @Override
    public String readUTF8(int offset, char[] charBuffer) {
        try {
            return super.readUTF8(offset, charBuffer);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String readModule(int offset, char[] charBuffer) {
        try {
            return super.readModule(offset, charBuffer);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int readUnsignedShort(int offset) {
        try {
            return super.readUnsignedShort(offset);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String readClass(int offset, char[] charBuffer) {
        try {
            return super.readClass(offset, charBuffer);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public Object readConst(int constantPoolEntryIndex, char[] charBuffer) {
        try {
            if (this.getCpInfoOffsets().length > constantPoolEntryIndex && charBuffer != null)
                return super.readConst(constantPoolEntryIndex, charBuffer);
            else
                return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
