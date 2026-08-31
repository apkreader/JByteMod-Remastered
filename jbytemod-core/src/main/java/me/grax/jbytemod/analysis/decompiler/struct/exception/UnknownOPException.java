package me.grax.jbytemod.analysis.decompiler.struct.exception;

import de.xbrowniecodez.jbytemod.utils.OpcodeUtils;

public class UnknownOPException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnknownOPException(int opc) {
        super("Unresolved opcode: " + OpcodeUtils.getOpcodeText(opc) + " (" + opc + ")");
    }
}
