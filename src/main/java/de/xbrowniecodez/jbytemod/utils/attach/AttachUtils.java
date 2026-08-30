package de.xbrowniecodez.jbytemod.utils.attach;

import com.sun.tools.attach.VirtualMachine;

import javax.swing.*;

public class AttachUtils {
    public static VirtualMachine getVirtualMachine(int pid) {
        if (pid == ProcessHandle.current().pid()) {
            throw new IllegalArgumentException("Cannot attach JByteMod to itself");
        }
        try {
            return VirtualMachine.attach(String.valueOf(pid));
        } catch (Exception exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("process running under")) {
                JOptionPane.showMessageDialog(null, "Cannot attach to process run with different jvm!");
            }
            throw new RuntimeException(exception);
        }
    }

    public void loadAgent(String agentJar, int pid, String options) {
        VirtualMachine vm = getVirtualMachine(pid);
        if (vm == null) {
            throw new RuntimeException("Can\'t attach to this jvm. Add -javaagent:" + agentJar + " to the commandline");
        } else {
            try {
                try {
                    vm.loadAgent(agentJar, options);
                } finally {
                    vm.detach();
                }

            } catch (Exception var9) {
                throw new RuntimeException("Can\'t attach to this jvm. Add -javaagent:" + agentJar + " to the commandline", var9);
            }
        }
    }
}
