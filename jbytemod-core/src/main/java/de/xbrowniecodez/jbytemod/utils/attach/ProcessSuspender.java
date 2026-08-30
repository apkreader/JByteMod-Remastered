package de.xbrowniecodez.jbytemod.utils.attach;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ProcessSuspender {
    private static final int PROCESS_SUSPEND_RESUME = 0x0800;

    private ProcessSuspender() {
    }

    static void setSuspended(long processId, boolean suspended) throws IOException {
        if (processId == ProcessHandle.current().pid()) {
            throw new IOException("JByteMod cannot suspend its own process");
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            setWindowsSuspended(processId, suspended);
        } else if (os.contains("linux") || os.contains("mac") || os.contains("bsd") || os.contains("unix")) {
            sendUnixSignal(processId, suspended ? "-STOP" : "-CONT");
        } else {
            throw new IOException("Freezing processes is not supported on " + System.getProperty("os.name"));
        }
    }

    private static void setWindowsSuspended(long processId, boolean suspended) throws IOException {
        if (processId > Integer.MAX_VALUE) throw new IOException("Invalid Windows process ID: " + processId);

        Pointer process = Kernel32.INSTANCE.OpenProcess(PROCESS_SUSPEND_RESUME, false, (int) processId);
        if (process == null || Pointer.nativeValue(process) == 0) {
            throw new IOException("Could not open process " + processId + " (Windows error "
                    + Native.getLastError() + ")");
        }
        try {
            int status = suspended
                    ? NtDll.INSTANCE.NtSuspendProcess(process)
                    : NtDll.INSTANCE.NtResumeProcess(process);
            if (status != 0) {
                throw new IOException((suspended ? "NtSuspendProcess" : "NtResumeProcess")
                        + " failed with status 0x" + Integer.toHexString(status));
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(process);
        }
    }

    private static void sendUnixSignal(long processId, String signal) throws IOException {
        Process process = new ProcessBuilder("kill", signal, Long.toString(processId))
                .redirectErrorStream(true)
                .start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                throw new IOException("Failed to " + ("-STOP".equals(signal) ? "freeze" : "resume")
                        + " process " + processId + (output.isEmpty() ? "" : ": " + output));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while changing process state", exception);
        }
    }

    private interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer OpenProcess(int desiredAccess, boolean inheritHandle, int processId);

        boolean CloseHandle(Pointer handle);
    }

    private interface NtDll extends Library {
        NtDll INSTANCE = Native.load("ntdll", NtDll.class);

        int NtSuspendProcess(Pointer processHandle);

        int NtResumeProcess(Pointer processHandle);
    }
}
