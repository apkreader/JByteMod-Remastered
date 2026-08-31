package de.xbrowniecodez.jbytemod.utils.task;

import com.sun.tools.attach.VirtualMachine;
import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.utils.attach.AgentServer;
import de.xbrowniecodez.jbytemod.utils.attach.InjectUtils;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteAgentConnection;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteJarArchive;
import me.grax.jbytemod.ui.PageEndPanel;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;

public class AttachTask extends SwingWorker<RemoteJarArchive, Integer> {

    private final VirtualMachine vm;
    private final JByteMod jbm;
    private final PageEndPanel jpb;

    public AttachTask(JByteMod jbm, VirtualMachine vm) {
        this.jbm = jbm;
        this.vm = vm;
        this.jpb = jbm.getPageEndPanel();
    }

    @Override
    protected RemoteJarArchive doInBackground() throws Exception {
        return attach(vm, this::publish);
    }

    public static RemoteJarArchive attach(VirtualMachine vm, IntConsumer progress) throws Exception {
        progress.accept(1);
        try {
            RemoteAgentConnection connection = connectToReusableAgent(vm);
            if (connection == null) {
                File temp = File.createTempFile("jvm", ".jar");
                temp.deleteOnExit();
                File errorFile = new File(temp.getAbsolutePath() + ".error.log");
                errorFile.deleteOnExit();
                try (RemoteAgentConnection.Listener listener = RemoteAgentConnection.listen()) {
                    InjectUtils.createAgentJar(temp);
                    progress.accept(12);
                    String token = UUID.randomUUID().toString();
                    String encodedPath = Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(temp.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                    String options = encodedPath + ";" + listener.getPort() + ";" + token;
                    progress.accept(18);
                    try {
                        vm.loadAgent(temp.getAbsolutePath(), options);
                    } catch (Exception exception) {
                        if (errorFile.isFile()) {
                            String targetError = Files.readString(errorFile.toPath(), StandardCharsets.UTF_8);
                            throw new IOException("The agent failed inside the target JVM:\n\n" + targetError, exception);
                        }
                        throw exception;
                    }
                    connection = listener.accept(token);
                }
            }
            progress.accept(22);
            return createArchive(connection, vm.id(), progress);
        } finally {
            vm.detach();
        }
    }

    private static RemoteAgentConnection connectToReusableAgent(VirtualMachine vm) throws Exception {
        Properties properties = vm.getAgentProperties();
        if (!AgentServer.PROTOCOL.equals(properties.getProperty(AgentServer.PROTOCOL_PROPERTY))) return null;

        String portValue = properties.getProperty(AgentServer.PORT_PROPERTY);
        String token = properties.getProperty(AgentServer.TOKEN_PROPERTY);
        if (portValue == null || token == null || token.isBlank()) return null;
        try {
            return RemoteAgentConnection.connect(Integer.parseInt(portValue), token);
        } catch (RemoteAgentConnection.AgentBusyException exception) {
            throw exception;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static RemoteJarArchive createArchive(RemoteAgentConnection connection, String processId,
                                                  IntConsumer progress) throws Exception {
        try {
            RemoteJarArchive archive = new RemoteJarArchive(connection, Long.parseLong(processId),
                    value -> progress.accept(25 + value * 74 / 100));
            progress.accept(100);
            return archive;
        } catch (Exception exception) {
            connection.close();
            throw exception;
        }
    }

    @Override
    protected void done() {
        try {
            jbm.connectToAgent(get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            new me.grax.jbytemod.utils.ErrorDisplay(cause);
        }
    }

    @Override
    protected void process(List<Integer> chunks) {
        int i = chunks.get(chunks.size() - 1);
        jpb.setValue(i);
        super.process(chunks);
    }
}
