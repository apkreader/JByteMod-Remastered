package de.xbrowniecodez.jbytemod.utils.attach;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentServer {
    static final String PROTOCOL = "JByteMod-Agent/1";
    static final int COMMAND_LIST_CLASSES = 1;
    static final int COMMAND_REDEFINE_CLASSES = 2;
    static final int COMMAND_CLOSE = 3;
    static final int RESPONSE_OK = 0;
    static final int RESPONSE_ERROR = 1;
    static final int RESPONSE_PROGRESS = 2;

    private static volatile Socket connection;
    private static volatile Map<String, Class<?>> exposedClasses = Map.of();

    private AgentServer() {
    }

    public static synchronized void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        String[] options = agentArgs.split(";", 2);
        if (options.length != 2) {
            throw new IllegalArgumentException("Invalid JByteMod agent connection options");
        }

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(options[0])), 10000);
        socket.setTcpNoDelay(true);

        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        output.writeUTF(PROTOCOL);
        output.writeUTF(options[1]);
        output.flush();

        connection = socket;
        Thread server = new Thread(() -> serve(socket, instrumentation), "JByteMod agent bridge");
        server.setDaemon(true);
        server.setContextClassLoader(AgentServer.class.getClassLoader());
        server.start();
    }

    public static synchronized void shutdown() {
        Socket socket = connection;
        connection = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void serve(Socket socket, Instrumentation instrumentation) {
        try (socket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                int command = input.readUnsignedByte();
                if (command == COMMAND_CLOSE) return;

                try {
                    if (command == COMMAND_LIST_CLASSES) {
                        sendClasses(output, instrumentation);
                    } else if (command == COMMAND_REDEFINE_CLASSES) {
                        redefineClasses(input, output, instrumentation);
                    } else {
                        throw new IllegalArgumentException("Unknown agent command: " + command);
                    }
                } catch (Throwable failure) {
                    output.writeByte(RESPONSE_ERROR);
                    writeString(output, stackTrace(failure));
                    output.flush();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection == socket) connection = null;
        }
    }

    private static void sendClasses(DataOutputStream output, Instrumentation instrumentation) throws Exception {
        writeProgress(output, 0);
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, Class<?>> runtimeClasses = new LinkedHashMap<>();
        List<Class<?>> candidates = new ArrayList<>();
        Map<Class<?>, String> names = new IdentityHashMap<>();
        ClassLoader agentLoader = AgentServer.class.getClassLoader();
        for (Class<?> runtimeClass : instrumentation.getAllLoadedClasses()) {
            String name = runtimeClass.getName().replace('.', '/');
            if (!isReadable(runtimeClass, name, instrumentation, agentLoader)) continue;

            candidates.add(runtimeClass);
            names.put(runtimeClass, name);
        }
        writeProgress(output, 5);

        Map<Class<?>, byte[]> captured = captureClassBytes(candidates, instrumentation,
                progress -> writeProgress(output, 5 + progress * 70 / 100));
        for (int i = 0; i < candidates.size(); i++) {
            Class<?> runtimeClass = candidates.get(i);
            String name = names.get(runtimeClass);
            if (classes.containsKey(name)) continue;

            byte[] bytes = captured.get(runtimeClass);
            if (bytes == null) {
                try (InputStream stream = runtimeClass.getResourceAsStream("/" + name + ".class")) {
                    if (stream != null) bytes = readAllBytes(stream);
                } catch (Exception ignored) {
                }
            }
            if (bytes != null) {
                classes.put(name, bytes);
                runtimeClasses.put(name, runtimeClass);
            }
            if (i % 100 == 0 && !candidates.isEmpty()) {
                writeProgress(output, 75 + (i + 1) * 5 / candidates.size());
            }
        }
        exposedClasses = runtimeClasses;
        writeProgress(output, 80);

        output.writeByte(RESPONSE_OK);
        output.writeInt(classes.size());
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            writeString(output, entry.getKey());
            output.writeInt(entry.getValue().length);
            output.write(entry.getValue());
        }
        output.flush();
    }

    private static Map<Class<?>, byte[]> captureClassBytes(List<Class<?>> classes,
                                                            Instrumentation instrumentation,
                                                            ProgressWriter progress) throws Exception {
        if (classes.isEmpty() || !instrumentation.isRetransformClassesSupported()) return Map.of();

        Set<Class<?>> targets = new HashSet<>(classes);
        Map<Class<?>, byte[]> captured = new ConcurrentHashMap<>();
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (classBeingRedefined != null && targets.contains(classBeingRedefined)) {
                    captured.put(classBeingRedefined, classfileBuffer.clone());
                }
                return null;
            }
        };

        instrumentation.addTransformer(transformer, true);
        try {
            for (int start = 0; start < classes.size(); start += 100) {
                Class<?>[] batch = classes.subList(start, Math.min(start + 100, classes.size()))
                        .toArray(Class<?>[]::new);
                try {
                    instrumentation.retransformClasses(batch);
                } catch (Throwable ignored) {
                    for (Class<?> runtimeClass : batch) {
                        if (captured.containsKey(runtimeClass)) continue;
                        try {
                            instrumentation.retransformClasses(runtimeClass);
                        } catch (Throwable ignoredClass) {
                        }
                    }
                }
                progress.write(Math.min(100, (start + batch.length) * 100 / classes.size()));
            }
        } finally {
            instrumentation.removeTransformer(transformer);
        }
        return captured;
    }

    private static void writeProgress(DataOutputStream output, int progress) throws Exception {
        output.writeByte(RESPONSE_PROGRESS);
        output.writeByte(Math.max(0, Math.min(100, progress)));
        output.flush();
    }

    private static void redefineClasses(DataInputStream input, DataOutputStream output,
                                        Instrumentation instrumentation) throws Exception {
        int count = input.readInt();
        if (count < 0 || count > 100000) throw new IllegalArgumentException("Invalid class count: " + count);

        List<Map.Entry<String, ClassDefinition>> definitions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = readString(input);
            int length = input.readInt();
            if (length < 0 || length > 256 * 1024 * 1024) {
                throw new IllegalArgumentException("Invalid class size for " + name + ": " + length);
            }
            byte[] bytes = input.readNBytes(length);
            if (bytes.length != length) throw new IllegalStateException("Connection closed while reading " + name);

            Class<?> runtimeClass = exposedClasses.get(name);
            if (runtimeClass == null) throw new ClassNotFoundException(name.replace('/', '.'));
            if (!instrumentation.isModifiableClass(runtimeClass)) {
                throw new IllegalArgumentException(name + " is not modifiable");
            }
            definitions.add(Map.entry(name, new ClassDefinition(runtimeClass, bytes)));
        }

        try {
            instrumentation.redefineClasses(definitions.stream()
                    .map(Map.Entry::getValue)
                    .toArray(ClassDefinition[]::new));
        } catch (Throwable failure) {
            throw new IllegalStateException("Failed to redefine "
                    + definitions.stream().map(Map.Entry::getKey).toList(), failure);
        }
        output.writeByte(RESPONSE_OK);
        output.writeInt(definitions.size());
        output.flush();
    }

    private static boolean isReadable(Class<?> runtimeClass, String name, Instrumentation instrumentation,
                                      ClassLoader agentLoader) {
        return runtimeClass.getClassLoader() != agentLoader
                && !runtimeClass.isHidden()
                && !runtimeClass.isArray()
                && instrumentation.isModifiableClass(runtimeClass)
                && !name.startsWith("java/")
                && !name.startsWith("javax/")
                && !name.startsWith("sun/")
                && !name.startsWith("com/sun/")
                && !name.startsWith("jdk/");
    }

    private static byte[] readAllBytes(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
    }

    private static String stackTrace(Throwable failure) {
        StringWriter output = new StringWriter();
        failure.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    static void writeString(DataOutputStream output, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    static String readString(DataInputStream input) throws Exception {
        int length = input.readInt();
        if (length < 0 || length > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid string size: " + length);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new IllegalStateException("Connection closed while reading a string");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ProgressWriter {
        void write(int progress) throws Exception;
    }
}
