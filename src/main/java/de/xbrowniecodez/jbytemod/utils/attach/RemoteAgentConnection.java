package de.xbrowniecodez.jbytemod.utils.attach;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RemoteAgentConnection implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    private RemoteAgentConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.socket.setSoTimeout(30000);
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    public synchronized Map<String, byte[]> loadClasses() throws Exception {
        output.writeByte(AgentServer.COMMAND_LIST_CLASSES);
        output.flush();
        checkResponse();

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
        }
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

    private void checkResponse() throws Exception {
        int response = input.readUnsignedByte();
        if (response == AgentServer.RESPONSE_ERROR) {
            throw new IOException("The target JVM rejected the request:\n\n" + AgentServer.readString(input));
        }
        if (response != AgentServer.RESPONSE_OK) throw new IOException("Invalid agent response: " + response);
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
