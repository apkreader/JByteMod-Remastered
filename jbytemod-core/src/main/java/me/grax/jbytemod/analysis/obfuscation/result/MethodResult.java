package me.grax.jbytemod.analysis.obfuscation.result;

import me.grax.jbytemod.analysis.obfuscation.enums.MethodObfType;

import java.util.ArrayList;

public class MethodResult {
    public ArrayList<MethodObfType> mobf;

    public MethodResult(ArrayList<MethodObfType> mobf) {
        super();
        this.mobf = mobf;
    }

}
