package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.plugin.JvmClassLoaderInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmRuntimeInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmThreadInfo;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntConsumer;

public final class RemoteAgentConnection implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    private RemoteAgentConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.socket.setSoTimeout(120000);
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    public synchronized Map<String, byte[]> loadClasses() throws Exception {
        return loadClasses(progress -> {
        });
    }

    public synchronized Map<String, byte[]> loadClasses(IntConsumer progress) throws Exception {
        output.writeByte(AgentServer.COMMAND_LIST_CLASSES);
        output.flush();
        checkResponse(progress);

        int count = input.readInt();
        if (count < 0 || count > 100000) throw new IOException("Invalid class count: " + count);
        Map<String, byte[]> classes = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = AgentServer.readString(input);
            int length = input.readInt();
            if (length < 0 || length > 256 * 1024 * 1024) {
                throw new IOException("Invalid class size for " + name + ": " + length);
            }
            byte[] bytes = input.readNBytes(length);
            if (bytes.length != length) throw new IOException("Target disconnected while loading " + name);
            classes.put(name, bytes);
            if (i % 25 == 0 || i + 1 == count) {
                progress.accept(count == 0 ? 100 : 80 + (i + 1) * 20 / count);
            }
        }
        if (count == 0) progress.accept(100);
        return classes;
    }

    public synchronized int redefineClasses(Map<String, byte[]> classes) throws Exception {
        output.writeByte(AgentServer.COMMAND_REDEFINE_CLASSES);
        output.writeInt(classes.size());
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            AgentServer.writeString(output, entry.getKey());
            output.writeInt(entry.getValue().length);
            output.write(entry.getValue());
        }
        output.flush();
        checkResponse();
        return input.readInt();
    }

    public synchronized void terminate() throws Exception {
        output.writeByte(AgentServer.COMMAND_TERMINATE);
        output.flush();
        checkResponse();
        socket.close();
    }

    public synchronized JvmRuntimeInfo getRuntimeInfo(boolean frozen) throws Exception {
        output.writeByte(AgentServer.COMMAND_RUNTIME_INFO);
        output.flush();
        checkResponse();
        return new JvmRuntimeInfo(
                AgentServer.readString(input), AgentServer.readString(input), AgentServer.readString(input),
                AgentServer.readString(input), AgentServer.readString(input), AgentServer.readString(input),
                input.readLong(), input.readLong(), input.readInt(),
                input.readLong(), input.readLong(), input.readLong(),
                input.readLong(), input.readLong(), input.readLong(),
                input.readInt(), input.readLong(), input.readLong(),
                input.readInt(), input.readInt(), input.readInt(), input.readLong(), frozen);
    }

    public synchronized List<JvmThreadInfo> getThreads(int maxStackDepth) throws Exception {
        if (maxStackDepth < 0 || maxStackDepth > 256) {
            throw new IllegalArgumentException("maxStackDepth must be between 0 and 256");
        }
        output.writeByte(AgentServer.COMMAND_THREADS);
        output.writeInt(maxStackDepth);
        output.flush();
        checkResponse();
        int count = readCount("thread", 100_000);
        List<JvmThreadInfo> threads = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long id = input.readLong();
            String name = AgentServer.readString(input);
            String state = AgentServer.readString(input);
            boolean daemon = input.readBoolean();
            int priority = input.readInt();
            String lockName = readNullableString();
            String lockOwnerName = readNullableString();
            int stackDepth = readCount("stack frame", 256);
            List<String> stack = new ArrayList<>(stackDepth);
            for (int frame = 0; frame < stackDepth; frame++) stack.add(AgentServer.readString(input));
            threads.add(new JvmThreadInfo(id, name, state, daemon, priority, lockName, lockOwnerName, stack));
        }
        return List.copyOf(threads);
    }

    public synchronized List<JvmClassLoaderInfo> getClassLoaders() throws Exception {
        output.writeByte(AgentServer.COMMAND_CLASS_LOADERS);
        output.flush();
        checkResponse();
        int count = readCount("class loader", 100_000);
        List<JvmClassLoaderInfo> loaders = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            loaders.add(new JvmClassLoaderInfo(AgentServer.readString(input), AgentServer.readString(input),
                    AgentServer.readString(input), readNullableString(), input.readInt(), input.readBoolean()));
        }
        return List.copyOf(loaders);
    }

    public synchronized Map<String, String> getSystemProperties() throws Exception {
        output.writeByte(AgentServer.COMMAND_SYSTEM_PROPERTIES);
        output.flush();
        checkResponse();
        int count = readCount("system property", 100_000);
        Map<String, String> properties = new LinkedHashMap<>(count);
        for (int index = 0; index < count; index++) {
            properties.put(AgentServer.readString(input), AgentServer.readString(input));
        }
        return Map.copyOf(properties);
    }

    public synchronized byte[] invokeAgentExtension(String extensionId, String entryClassName,
                                                     Map<String, byte[]> classFiles, byte[] request) throws Exception {
        if (extensionId == null || extensionId.isBlank()) {
            throw new IllegalArgumentException("extensionId cannot be blank");
        }
        if (entryClassName == null || entryClassName.isBlank()) {
            throw new IllegalArgumentException("entryClassName cannot be blank");
        }
        if (classFiles == null || !classFiles.containsKey(entryClassName)) {
            throw new IllegalArgumentException("classFiles must contain the entry class");
        }
        if (request == null || request.length > 32 * 1024 * 1024) {
            throw new IllegalArgumentException("Agent extension request exceeds 32 MiB");
        }

        TreeMap<String, byte[]> sortedClasses = new TreeMap<>(classFiles);
        validateExtensionClasses(sortedClasses);
        output.writeByte(AgentServer.COMMAND_AGENT_EXTENSION);
        AgentServer.writeString(output, extensionId);
        AgentServer.writeString(output, extensionRevision(entryClassName, sortedClasses));
        AgentServer.writeString(output, entryClassName);
        output.writeInt(sortedClasses.size());
        for (Map.Entry<String, byte[]> entry : sortedClasses.entrySet()) {
            AgentServer.writeString(output, entry.getKey());
            output.writeInt(entry.getValue().length);
            output.write(entry.getValue());
        }
        output.writeInt(request.length);
        output.write(request);
        output.flush();
        checkResponse();
        int length = input.readInt();
        if (length < 0 || length > 256 * 1024 * 1024) throw new IOException("Invalid extension response size");
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new IOException("Target disconnected while reading extension response");
        return bytes;
    }

    private static void validateExtensionClasses(Map<String, byte[]> classFiles) {
        if (classFiles.isEmpty() || classFiles.size() > 256) {
            throw new IllegalArgumentException("Agent extension must contain between 1 and 256 classes");
        }
        long totalSize = 0;
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                throw new IllegalArgumentException("Agent extension contains an invalid class entry");
            }
            if (entry.getValue().length > 8 * 1024 * 1024) {
                throw new IllegalArgumentException("Agent extension class exceeds 8 MiB: " + entry.getKey());
            }
            totalSize += entry.getValue().length;
        }
        if (totalSize > 32L * 1024 * 1024) {
            throw new IllegalArgumentException("Agent extension classes exceed 32 MiB");
        }
    }

    private static String extensionRevision(String entryClassName, Map<String, byte[]> classFiles) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(entryClassName.getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
            digest.update(entry.getValue());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private int readCount(String type, int maximum) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > maximum) throw new IOException("Invalid " + type + " count: " + count);
        return count;
    }

    private String readNullableString() throws Exception {
        return input.readBoolean() ? AgentServer.readString(input) : null;
    }

    private void checkResponse() throws Exception {
        checkResponse(progress -> {
        });
    }

    private void checkResponse(IntConsumer progress) throws Exception {
        while (true) {
            int response = input.readUnsignedByte();
            if (response == AgentServer.RESPONSE_PROGRESS) {
                progress.accept(input.readUnsignedByte());
                continue;
            }
            if (response == AgentServer.RESPONSE_ERROR) {
                throw new IOException("The target JVM rejected the request:\n\n" + AgentServer.readString(input));
            }
            if (response != AgentServer.RESPONSE_OK) throw new IOException("Invalid agent response: " + response);
            return;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (!socket.isClosed()) {
                output.writeByte(AgentServer.COMMAND_CLOSE);
                output.flush();
                checkResponse();
            }
        } catch (Exception ignored) {
        } finally {
            socket.close();
        }
    }

    public static RemoteAgentConnection connect(int port, String token) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 3000);
            RemoteAgentConnection connection = new RemoteAgentConnection(socket);
            connection.output.writeUTF(AgentServer.PROTOCOL);
            connection.output.writeUTF(token);
            connection.output.flush();
            int response = connection.input.readUnsignedByte();
            if (response == AgentServer.HANDSHAKE_BUSY) {
                throw new AgentBusyException("The JByteMod agent is already connected to another client");
            }
            if (response != AgentServer.HANDSHAKE_OK) {
                throw new IOException("The target rejected the reusable JByteMod agent connection");
            }
            return connection;
        } catch (IOException exception) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw exception;
        }
    }

    public static Listener listen() throws IOException {
        return new Listener();
    }

    public static final class AgentBusyException extends IOException {
        public AgentBusyException(String message) {
            super(message);
        }
    }

    public static final class Listener implements Closeable {
        private final ServerSocket server;

        private Listener() throws IOException {
            this.server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            this.server.setSoTimeout(15000);
        }

        public int getPort() {
            return server.getLocalPort();
        }

        public RemoteAgentConnection accept(String token) throws Exception {
            long deadline = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Socket socket = server.accept();
                    RemoteAgentConnection connection = new RemoteAgentConnection(socket);
                    String protocol = connection.input.readUTF();
                    String receivedToken = connection.input.readUTF();
                    if (AgentServer.PROTOCOL.equals(protocol) && token.equals(receivedToken)) return connection;
                    connection.close();
                } catch (SocketTimeoutException timeout) {
                    break;
                }
            }
            throw new IOException("The target JVM did not open the JByteMod agent connection");
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
