package de.xbrowniecodez.jbytemod.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

public final class CustomClassWriter extends ClassVisitor {
    private final ClassWriter writer;

    public CustomClassWriter(int flags) {
        this(new ClassWriter(flags));
    }

    private CustomClassWriter(ClassWriter writer) {
        super(Opcodes.ASM9, writer);
        this.writer = writer;
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return super.visitModule(name, access, version);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    public byte[] toByteArray() {
        return writer.toByteArray();
    }
}
