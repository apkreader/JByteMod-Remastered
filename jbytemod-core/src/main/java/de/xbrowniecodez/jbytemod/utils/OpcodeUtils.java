package de.xbrowniecodez.jbytemod.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.util.Printer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class OpcodeUtils {
    private static final Map<String, Integer> OPCODES_BY_NAME = createOpcodeMap();
    private static final Map<AbstractInsnNode, Integer> LABEL_INDICES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private OpcodeUtils() {
    }

    private static Map<String, Integer> createOpcodeMap() {
        Map<String, Integer> opcodes = new HashMap<>();
        for (int opcode = 0; opcode < Printer.OPCODES.length; opcode++) {
            String name = Printer.OPCODES[opcode];
            if (name != null) {
                opcodes.put(name, opcode);
            }
        }
        return Map.copyOf(opcodes);
    }

    public static int getOpcodeIndex(String opcode) {
        Integer value = OPCODES_BY_NAME.get(opcode.toUpperCase());
        if (value == null) {
            throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        return value;
    }

    public static String getOpcodeText(int opcode) {
        if (opcode >= 0 && opcode < Printer.OPCODES.length && Printer.OPCODES[opcode] != null) {
            return Printer.OPCODES[opcode];
        }
        return "INVALID OPCODE";
    }

    public static int getIntValue(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        if (opcode == Opcodes.ICONST_M1) {
            return -1;
        }
        if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) {
            return opcode - Opcodes.ICONST_0;
        }
        return ((IntInsnNode) instruction).operand;
    }

    public static String getFrameType(int type) {
        return switch (type) {
            case Opcodes.F_NEW -> "F_NEW";
            case Opcodes.F_FULL -> "F_FULL";
            case Opcodes.F_APPEND -> "F_APPEND";
            case Opcodes.F_CHOP -> "F_CHOP";
            case Opcodes.F_SAME -> "F_SAME";
            case Opcodes.F_SAME1 -> "F_SAME1";
            default -> "FRAME";
        };
    }

    public static int getLabelIndex(LabelNode label) {
        return LABEL_INDICES.computeIfAbsent(label, OpcodeUtils::calculateLabelIndex);
    }

    private static int calculateLabelIndex(AbstractInsnNode instruction) {
        int index = 0;
        for (AbstractInsnNode current = instruction.getPrevious(); current != null; current = current.getPrevious()) {
            if (current instanceof LabelNode) {
                index++;
            }
        }
        return index;
    }

    public static void clearLabelCache() {
        LABEL_INDICES.clear();
    }
}
