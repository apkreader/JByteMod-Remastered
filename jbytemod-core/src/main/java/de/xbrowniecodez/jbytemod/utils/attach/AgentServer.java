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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentServer {
    public static final String PROTOCOL = "JByteMod-Agent/3";
    public static final String PORT_PROPERTY = "jbytemod.agent.port";
    public static final String TOKEN_PROPERTY = "jbytemod.agent.token";
    public static final String PROTOCOL_PROPERTY = "jbytemod.agent.protocol";
    static final int HANDSHAKE_OK = 0;
    static final int HANDSHAKE_REJECTED = 1;
    static final int HANDSHAKE_BUSY = 2;
    static final int COMMAND_LIST_CLASSES = 1;
    static final int COMMAND_REDEFINE_CLASSES = 2;
    static final int COMMAND_CLOSE = 3;
    static final int COMMAND_TERMINATE = 4;
    static final int COMMAND_RUNTIME_INFO = 5;
    static final int COMMAND_THREADS = 6;
    static final int COMMAND_CLASS_LOADERS = 7;
    static final int COMMAND_SYSTEM_PROPERTIES = 8;
    static final int COMMAND_AGENT_EXTENSION = 9;
    static final int RESPONSE_OK = 0;
    static final int RESPONSE_ERROR = 1;
    static final int RESPONSE_PROGRESS = 2;

    private static volatile Socket connection;
    private static volatile ServerSocket reusableServer;
    private static volatile Properties discoveryProperties;
    private static volatile Map<String, Class<?>> exposedClasses = Map.of();
    private static final Map<String, LoadedAgentExtension> agentExtensions = new ConcurrentHashMap<>();

    private AgentServer() {
    }

    public static synchronized void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        String[] options = agentArgs.split(";", 2);
        if (options.length != 2) {
            throw new IllegalArgumentException("Invalid JByteMod agent connection options");
        }

        startReusableServer(options[1], instrumentation);

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(options[0])), 10000);
        socket.setTcpNoDelay(true);

        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        output.writeUTF(PROTOCOL);
        output.writeUTF(options[1]);
        output.flush();

        if (!claimConnection(socket)) {
            socket.close();
            throw new IllegalStateException("A JByteMod client is already connected");
        }
        startConnectionThread(socket, instrumentation);
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
        ServerSocket server = reusableServer;
        reusableServer = null;
        if (server != null) {
            try {
                server.close();
            } catch (Exception ignored) {
            }
        }
        Properties properties = discoveryProperties;
        discoveryProperties = null;
        if (properties != null) {
            properties.remove(PORT_PROPERTY);
            properties.remove(TOKEN_PROPERTY);
            properties.remove(PROTOCOL_PROPERTY);
        }
        agentExtensions.clear();
    }

    private static synchronized void startReusableServer(String token, Instrumentation instrumentation)
            throws Exception {
        if (reusableServer != null && !reusableServer.isClosed()) return;

        ServerSocket server = new ServerSocket(0, 4, InetAddress.getLoopbackAddress());
        try {
            Properties properties = getAgentProperties(instrumentation);
            discoveryProperties = properties;
            properties.setProperty(PORT_PROPERTY, Integer.toString(server.getLocalPort()));
            properties.setProperty(TOKEN_PROPERTY, token);
            properties.setProperty(PROTOCOL_PROPERTY, PROTOCOL);
            reusableServer = server;
        } catch (Exception exception) {
            server.close();
            throw exception;
        }

        Thread listener = new Thread(() -> acceptReusableConnections(server, token, instrumentation),
                "JByteMod reusable agent");
        listener.setDaemon(true);
        listener.setContextClassLoader(AgentServer.class.getClassLoader());
        listener.start();
    }

    private static Properties getAgentProperties(Instrumentation instrumentation) throws Exception {
        openJavaBasePackage(instrumentation, "jdk.internal.vm");
        Class<?> vmSupport = Class.forName("jdk.internal.vm.VMSupport");
        Method getter = vmSupport.getMethod("getAgentProperties");
        return (Properties) getter.invoke(null);
    }

    private static void openJavaBasePackage(Instrumentation instrumentation, String packageName) {
        Module javaBase = Object.class.getModule();
        Module agentModule = AgentServer.class.getModule();
        instrumentation.redefineModule(javaBase, Set.of(),
                Map.of(packageName, Set.of(agentModule)),
                Map.of(packageName, Set.of(agentModule)),
                Set.of(), Map.of());
    }

    private static void acceptReusableConnections(ServerSocket server, String token,
                                                   Instrumentation instrumentation) {
        while (!server.isClosed()) {
            Socket socket = null;
            try {
                socket = server.accept();
                socket.setTcpNoDelay(true);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                if (!PROTOCOL.equals(input.readUTF()) || !token.equals(input.readUTF())) {
                    output.writeByte(HANDSHAKE_REJECTED);
                    output.flush();
                    socket.close();
                    continue;
                }
                if (!claimConnection(socket)) {
                    output.writeByte(HANDSHAKE_BUSY);
                    output.flush();
                    socket.close();
                    continue;
                }
                output.writeByte(HANDSHAKE_OK);
                output.flush();
                startConnectionThread(socket, instrumentation);
            } catch (Exception ignored) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception ignoredClose) {
                    }
                }
            }
        }
    }

    private static synchronized boolean claimConnection(Socket socket) {
        if (connection != null && !connection.isClosed()) return false;
        connection = socket;
        return true;
    }

    private static void startConnectionThread(Socket socket, Instrumentation instrumentation) {
        Thread thread = new Thread(() -> serve(socket, instrumentation), "JByteMod agent bridge");
        thread.setDaemon(true);
        thread.setContextClassLoader(AgentServer.class.getClassLoader());
        thread.start();
    }

    private static synchronized void releaseConnection(Socket socket) {
        if (connection == socket) connection = null;
    }

    private static void serve(Socket socket, Instrumentation instrumentation) {
        try (socket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                int command = input.readUnsignedByte();
                if (command == COMMAND_CLOSE) {
                    releaseConnection(socket);
                    output.writeByte(RESPONSE_OK);
                    output.flush();
                    return;
                }

                try {
                    if (command == COMMAND_LIST_CLASSES) {
                        sendClasses(output, instrumentation);
                    } else if (command == COMMAND_REDEFINE_CLASSES) {
                        redefineClasses(input, output, instrumentation);
                    } else if (command == COMMAND_TERMINATE) {
                        output.writeByte(RESPONSE_OK);
                        output.flush();
                        Runtime.getRuntime().halt(0);
                        return;
                    } else if (command == COMMAND_RUNTIME_INFO) {
                        sendRuntimeInfo(output);
                    } else if (command == COMMAND_THREADS) {
                        sendThreads(input, output);
                    } else if (command == COMMAND_CLASS_LOADERS) {
                        sendClassLoaders(output, instrumentation);
                    } else if (command == COMMAND_SYSTEM_PROPERTIES) {
                        sendSystemProperties(output);
                    } else if (command == COMMAND_AGENT_EXTENSION) {
                        invokeAgentExtension(input, output, instrumentation);
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
            releaseConnection(socket);
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
        ClassFileTransformer transformer = new CaptureTransformer(targets, captured);

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

    private static void sendRuntimeInfo(DataOutputStream output) throws Exception {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        output.writeByte(RESPONSE_OK);
        writeString(output, runtime.getName().split("@", 2)[0]);
        writeString(output, System.getProperty("java.vm.name", ""));
        writeString(output, System.getProperty("java.vm.vendor", ""));
        writeString(output, System.getProperty("java.vm.version", ""));
        writeString(output, System.getProperty("java.runtime.name", ""));
        writeString(output, System.getProperty("java.runtime.version", ""));
        output.writeLong(runtime.getStartTime());
        output.writeLong(runtime.getUptime());
        output.writeInt(Runtime.getRuntime().availableProcessors());
        writeMemoryUsage(output, heap);
        writeMemoryUsage(output, nonHeap);
        output.writeInt(classes.getLoadedClassCount());
        output.writeLong(classes.getTotalLoadedClassCount());
        output.writeLong(classes.getUnloadedClassCount());
        output.writeInt(threads.getThreadCount());
        output.writeInt(threads.getPeakThreadCount());
        output.writeInt(threads.getDaemonThreadCount());
        output.writeLong(threads.getTotalStartedThreadCount());
        output.flush();
    }

    private static void writeMemoryUsage(DataOutputStream output, MemoryUsage usage) throws Exception {
        output.writeLong(usage.getUsed());
        output.writeLong(usage.getCommitted());
        output.writeLong(usage.getMax());
    }

    private static void sendThreads(DataInputStream input, DataOutputStream output) throws Exception {
        int maxDepth = input.readInt();
        if (maxDepth < 0 || maxDepth > 256) {
            throw new IllegalArgumentException("Invalid maximum stack depth: " + maxDepth);
        }
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.getThreadInfo(bean.getAllThreadIds(), maxDepth);
        Map<Long, Thread> liveThreads = new LinkedHashMap<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            liveThreads.put(thread.threadId(), thread);
        }
        List<ThreadInfo> available = new ArrayList<>();
        for (ThreadInfo info : infos) {
            if (info != null) available.add(info);
        }
        available.sort((left, right) -> Long.compare(left.getThreadId(), right.getThreadId()));

        output.writeByte(RESPONSE_OK);
        output.writeInt(available.size());
        for (ThreadInfo info : available) {
            Thread thread = liveThreads.get(info.getThreadId());
            output.writeLong(info.getThreadId());
            writeString(output, info.getThreadName());
            writeString(output, info.getThreadState().name());
            output.writeBoolean(thread != null && thread.isDaemon());
            output.writeInt(thread == null ? Thread.NORM_PRIORITY : thread.getPriority());
            writeNullableString(output, info.getLockName());
            writeNullableString(output, info.getLockOwnerName());
            StackTraceElement[] stack = info.getStackTrace();
            output.writeInt(stack.length);
            for (StackTraceElement element : stack) writeString(output, element.toString());
        }
        output.flush();
    }

    private static void sendClassLoaders(DataOutputStream output, Instrumentation instrumentation) throws Exception {
        Map<ClassLoader, Integer> counts = new IdentityHashMap<>();
        int bootstrapCount = 0;
        for (Class<?> type : instrumentation.getAllLoadedClasses()) {
            ClassLoader loader = type.getClassLoader();
            if (loader == null) bootstrapCount++;
            else counts.merge(loader, 1, Integer::sum);
        }
        Set<ClassLoader> loaders = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ClassLoader loader : counts.keySet()) {
            for (ClassLoader current = loader; current != null; current = current.getParent()) {
                loaders.add(current);
            }
        }
        List<ClassLoader> sorted = new ArrayList<>(loaders);
        sorted.sort((left, right) -> loaderId(left).compareTo(loaderId(right)));

        output.writeByte(RESPONSE_OK);
        output.writeInt(sorted.size() + 1);
        writeString(output, "bootstrap");
        writeString(output, "Bootstrap ClassLoader");
        writeString(output, "<bootstrap>");
        writeNullableString(output, null);
        output.writeInt(bootstrapCount);
        output.writeBoolean(true);
        for (ClassLoader loader : sorted) {
            writeString(output, loaderId(loader));
            writeString(output, loader.getName() == null ? loader.toString() : loader.getName());
            writeString(output, loader.getClass().getName());
            writeNullableString(output, loader.getParent() == null ? "bootstrap" : loaderId(loader.getParent()));
            output.writeInt(counts.getOrDefault(loader, 0));
            output.writeBoolean(false);
        }
        output.flush();
    }

    private static String loaderId(ClassLoader loader) {
        return loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader));
    }

    private static void sendSystemProperties(DataOutputStream output) throws Exception {
        Properties properties = System.getProperties();
        Map<String, String> values = new TreeMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name, ""));
        }
        output.writeByte(RESPONSE_OK);
        output.writeInt(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            writeString(output, entry.getKey());
            writeString(output, entry.getValue());
        }
        output.flush();
    }

    private static void invokeAgentExtension(DataInputStream input, DataOutputStream output,
                                             Instrumentation instrumentation) throws Exception {
        String extensionId = readString(input);
        String revision = readString(input);
        String entryClassName = readString(input);
        if (extensionId.isBlank() || revision.isBlank() || entryClassName.isBlank()) {
            throw new IllegalArgumentException("Invalid agent extension identity");
        }

        int classCount = input.readInt();
        if (classCount < 1 || classCount > 256) {
            throw new IllegalArgumentException("Invalid agent extension class count: " + classCount);
        }
        Map<String, byte[]> classFiles = new LinkedHashMap<>();
        long totalClassSize = 0;
        for (int index = 0; index < classCount; index++) {
            String className = readString(input);
            byte[] classFile = readBytes(input, 8 * 1024 * 1024, "extension class " + className);
            totalClassSize += classFile.length;
            if (totalClassSize > 32L * 1024 * 1024) {
                throw new IllegalArgumentException("Agent extension classes exceed 32 MiB");
            }
            if (className.isBlank() || classFiles.putIfAbsent(className, classFile) != null) {
                throw new IllegalArgumentException("Invalid or duplicate extension class: " + className);
            }
        }
        if (!classFiles.containsKey(entryClassName)) {
            throw new IllegalArgumentException("Agent extension does not contain its entry class");
        }
        byte[] request = readBytes(input, 32 * 1024 * 1024, "extension request");

        LoadedAgentExtension extension = agentExtensions.get(extensionId);
        if (extension == null || !extension.revision().equals(revision)) {
            if (extension == null && agentExtensions.size() >= 256) {
                throw new IllegalStateException("Too many agent extensions are loaded");
            }
            extension = loadAgentExtension(revision, entryClassName, classFiles);
            agentExtensions.put(extensionId, extension);
        }

        byte[] response = extension.invoke(request, instrumentation);
        if (response == null || response.length > 256 * 1024 * 1024) {
            throw new IllegalArgumentException("Agent extension response exceeds 256 MiB");
        }
        output.writeByte(RESPONSE_OK);
        output.writeInt(response.length);
        output.write(response);
        output.flush();
    }

    private static LoadedAgentExtension loadAgentExtension(String revision, String entryClassName,
                                                            Map<String, byte[]> classFiles) throws Exception {
        ClassLoader loader = new AgentExtensionClassLoader(classFiles, AgentServer.class.getClassLoader());
        Class<?> entryClass = Class.forName(entryClassName, true, loader);
        if (entryClass.getClassLoader() != loader) {
            throw new IllegalArgumentException("Agent extension entry class resolved outside its extension");
        }
        Method method = entryClass.getMethod("invoke", byte[].class, Instrumentation.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != byte[].class) {
            throw new IllegalArgumentException(
                    "Agent extension entry point must be public static byte[] invoke(byte[], Instrumentation)");
        }
        return new LoadedAgentExtension(revision, method);
    }

    private static byte[] readBytes(DataInputStream input, int maximum, String description) throws Exception {
        int length = input.readInt();
        if (length < 0 || length > maximum) {
            throw new IllegalArgumentException("Invalid " + description + " size: " + length);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new IllegalStateException("Connection closed while reading " + description);
        return bytes;
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

    private static void writeNullableString(DataOutputStream output, String value) throws Exception {
        output.writeBoolean(value != null);
        if (value != null) writeString(output, value);
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

    private static final class CaptureTransformer implements ClassFileTransformer {
        private final Set<Class<?>> targets;
        private final Map<Class<?>, byte[]> captured;

        private CaptureTransformer(Set<Class<?>> targets, Map<Class<?>, byte[]> captured) {
            this.targets = targets;
            this.captured = captured;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (classBeingRedefined != null && targets.contains(classBeingRedefined)) {
                captured.put(classBeingRedefined, classfileBuffer.clone());
            }
            return null;
        }
    }

    private record LoadedAgentExtension(String revision, Method method) {
        private byte[] invoke(byte[] request, Instrumentation instrumentation) throws Exception {
            try {
                return (byte[]) method.invoke(null, request, instrumentation);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception targetException) throw targetException;
                if (cause instanceof Error targetError) throw targetError;
                throw new IllegalStateException(cause);
            }
        }
    }

    private static final class AgentExtensionClassLoader extends ClassLoader {
        private final Map<String, byte[]> classFiles;

        private AgentExtensionClassLoader(Map<String, byte[]> classFiles, ClassLoader parent) {
            super(parent);
            this.classFiles = Map.copyOf(classFiles);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classFiles.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
