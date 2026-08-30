package de.xbrowniecodez.android.asm;

import com.googlecode.d2j.dex.ClassVisitorFactory;
import com.googlecode.d2j.dex.LambadaNameSafeClassAdapter;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

@Getter
public class Dex2ASMVisitorFactory implements ClassVisitorFactory {
    private final ClassNode classNode;
    public Dex2ASMVisitorFactory(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public ClassVisitor create(final String name) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final LambadaNameSafeClassAdapter lambadaNameSafeClassAdapter = new LambadaNameSafeClassAdapter(classWriter);
        return new ClassVisitor(Opcodes.ASM9, lambadaNameSafeClassAdapter) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                byte[] data = classWriter.toByteArray();
                ClassReader classReader = new ClassReader(data);
                classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        };
    }
}
