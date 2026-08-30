package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.plugin.JvmClassLoaderInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmRuntimeInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmThreadInfo;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            }
        } catch (Exception ignored) {
        } finally {
            socket.close();
        }
    }

    public static Listener listen() throws IOException {
        return new Listener();
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
