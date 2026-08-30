package de.xbrowniecodez.jbytemod.utils.attach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class AgentBootstrap {
    private static URLClassLoader agentClassLoader;

    private AgentBootstrap() {
    }

    public static synchronized void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        String[] options = agentArgs == null ? new String[0] : agentArgs.split(";", 3);
        if (options.length != 3) throw new IllegalArgumentException("Invalid JByteMod agent options");

        String path = new String(Base64.getUrlDecoder().decode(options[0]), StandardCharsets.UTF_8);
        Path agentPath = Path.of(path);
        Path errorPath = Path.of(path + ".error.log");
        URL agentJar = agentPath.toUri().toURL();
        String connectionOptions = options[1] + ";" + options[2];

        try {
            releasePreviousAgent();
            agentClassLoader = new URLClassLoader(new URL[]{agentJar}, ClassLoader.getPlatformClassLoader());

            Thread thread = Thread.currentThread();
            ClassLoader previousClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(agentClassLoader);
            try {
                Class<?> entryPoint = Class.forName("de.xbrowniecodez.jbytemod.utils.attach.AgentServer", true, agentClassLoader);
                Method agentMain = entryPoint.getMethod("agentmain", String.class, Instrumentation.class);
                agentMain.invoke(null, connectionOptions, instrumentation);
            } finally {
                thread.setContextClassLoader(previousClassLoader);
            }
        } catch (Throwable failure) {
            Throwable cause = unwrapInvocationFailure(failure);
            writeFailure(errorPath, cause);
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        }
    }

    private static void releasePreviousAgent() throws Exception {
        if (agentClassLoader == null) return;

        try {
            Class<?> server = Class.forName("de.xbrowniecodez.jbytemod.utils.attach.AgentServer", false, agentClassLoader);
            server.getMethod("shutdown").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }

        agentClassLoader.close();
        agentClassLoader = null;
    }

    private static Throwable unwrapInvocationFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof InvocationTargetException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static void writeFailure(Path errorPath, Throwable failure) {
        if (errorPath == null) return;
        try {
            StringWriter output = new StringWriter();
            failure.printStackTrace(new PrintWriter(output));
            Files.writeString(errorPath, output.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
