package de.xbrowniecodez.jbytemod.ui;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.graph.PatchedHierarchicalLayout;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

public final class CallGraphPanel extends JPanel {
    private static final int MAX_NODES = 500;

    private final JByteMod jByteMod;
    private final mxGraph graph = new mxGraph() {
        @Override
        public String getToolTipForCell(Object cell) {
            if (!(cell instanceof mxCell graphCell)) return null;
            if (graphCell.getValue() instanceof MethodVertex vertex) {
                MethodKey method = vertex.method();
                return method.owner().replace('/', '.') + "." + method.name() + method.descriptor()
                        + (vertex.target() == null ? " (external)" : "");
            }
            if (graphCell.getValue() instanceof CallSiteVertex vertex) {
                CallSite call = vertex.call();
                return "Instruction " + call.instructionIndex() + " in "
                        + call.source().owner().replace('/', '.') + "." + call.source().name()
                        + call.source().descriptor();
            }
            return null;
        }
    };
    private final mxGraphComponent graphComponent;
    private final JComboBox<Direction> direction = new JComboBox<>(Direction.values());
    private final JSpinner depth = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
    private final JCheckBox externalCalls = new JCheckBox("External calls");
    private final JLabel status = new JLabel("Select a method to explore its calls");

    private ClassNode rootClass;
    private MethodNode rootMethod;
    private SwingWorker<GraphData, Void> worker;
    private long requestId;

    public CallGraphPanel(JByteMod jByteMod) {
        this.jByteMod = jByteMod;
        setLayout(new BorderLayout());

        configureGraph();
        Color background = jByteMod.getOptions().get("use_dark_theme").getBoolean()
                ? new Color(33, 37, 43) : Color.WHITE;
        graphComponent = new CallGraphComponent(graph, background);
        graphComponent.getVerticalScrollBar().setUnitIncrement(16);
        graphComponent.getHorizontalScrollBar().setUnitIncrement(16);

        add(createToolbar(), BorderLayout.NORTH);
        add(graphComponent, BorderLayout.CENTER);
    }

    private void configureGraph() {
        graph.setAutoOrigin(true);
        graph.setAutoSizeCells(true);
        graph.setHtmlLabels(true);
        graph.setLabelsClipped(true);
        graph.setAllowDanglingEdges(false);
        graph.setCellsEditable(false);
        graph.setCellsMovable(false);
        graph.setCellsResizable(false);
        graph.setCellsDisconnectable(false);
        graph.setConnectableEdges(false);
        graph.setDropEnabled(false);

        Map<String, Object> edgeStyle = graph.getStylesheet().getDefaultEdgeStyle();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#748092");
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        toolbar.add(status, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(new JLabel("Direction:"));
        controls.add(direction);
        controls.add(new JLabel("Depth:"));
        controls.add(depth);
        controls.add(externalCalls);
        JButton reload = new JButton("Reload");
        reload.addActionListener(event -> generateGraph());
        controls.add(reload);
        toolbar.add(controls, BorderLayout.EAST);

        direction.addActionListener(event -> generateGraph());
        depth.addChangeListener(event -> generateGraph());
        externalCalls.addActionListener(event -> generateGraph());
        return toolbar;
    }

    public void setRoot(ClassNode classNode, MethodNode methodNode) {
        rootClass = classNode;
        rootMethod = methodNode;
    }

    public void generateGraph() {
        ClassNode selectedClass = rootClass;
        MethodNode selectedMethod = rootMethod;
        if (selectedClass == null || selectedMethod == null || jByteMod.getJarArchive() == null
                || jByteMod.getJarArchive().getClasses() == null) {
            clear();
            return;
        }

        Map<String, ClassNode> classes = new LinkedHashMap<>(jByteMod.getJarArchive().getClasses());
        Direction selectedDirection = (Direction) direction.getSelectedItem();
        int selectedDepth = (Integer) depth.getValue();
        boolean includeExternal = externalCalls.isSelected();
        MethodKey root = new MethodKey(selectedClass.name, selectedMethod.name, selectedMethod.desc);
        long currentRequest;
        synchronized (this) {
            requestId++;
            currentRequest = requestId;
            if (worker != null) worker.cancel(true);
        }

        status.setText("Building call graph...");
        SwingWorker<GraphData, Void> nextWorker = new SwingWorker<>() {
            @Override
            protected GraphData doInBackground() throws Exception {
                return buildGraph(classes, root, selectedDirection, selectedDepth, includeExternal);
            }

            @Override
            protected void done() {
                synchronized (CallGraphPanel.this) {
                    if (isCancelled() || currentRequest != requestId) return;
                    worker = null;
                }
                try {
                    renderGraph(get());
                } catch (CancellationException ignored) {
                } catch (Exception exception) {
                    clearCells();
                    status.setText("Could not build call graph");
                    new ErrorDisplay(exception);
                }
            }
        };

        synchronized (this) {
            if (currentRequest != requestId) return;
            worker = nextWorker;
        }
        nextWorker.execute();
    }

    public void clear() {
        synchronized (this) {
            requestId++;
            if (worker != null) {
                worker.cancel(true);
                worker = null;
            }
        }
        clearCells();
        status.setText("Select a method to explore its calls");
    }

    private GraphData buildGraph(Map<String, ClassNode> classes, MethodKey root, Direction selectedDirection,
                                 int selectedDepth, boolean includeExternal) throws InterruptedException {
        Map<MethodKey, MethodTarget> methods = new LinkedHashMap<>();
        Map<MethodKey, List<CallSite>> outgoing = new HashMap<>();
        Map<MethodKey, List<CallSite>> incoming = new HashMap<>();

        for (ClassNode classNode : classes.values()) {
            for (MethodNode methodNode : classNode.methods) {
                checkInterrupted();
                MethodKey source = new MethodKey(classNode.name, methodNode.name, methodNode.desc);
                methods.put(source, new MethodTarget(classNode, methodNode));
                List<CallSite> calls = outgoing.computeIfAbsent(source, ignored -> new ArrayList<>());
                int instructionIndex = 0;
                for (AbstractInsnNode instruction : methodNode.instructions) {
                    if (instruction instanceof MethodInsnNode invocation) {
                        calls.add(new CallSite(source,
                                new MethodKey(invocation.owner, invocation.name, invocation.desc),
                                instruction, instructionIndex));
                    } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                        collectHandles(calls, source, instruction, instructionIndex, List.of(dynamic.bsmArgs));
                    }
                    instructionIndex++;
                }
                for (CallSite call : calls) {
                    incoming.computeIfAbsent(call.target(), ignored -> new ArrayList<>()).add(call);
                }
            }
        }

        Set<MethodKey> nodes = new LinkedHashSet<>();
        Set<CallSite> edges = new LinkedHashSet<>();
        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();
        Map<MethodKey, Integer> visitedDepth = new HashMap<>();
        nodes.add(root);
        queue.add(new NodeDepth(root, 0));
        visitedDepth.put(root, 0);
        boolean clipped = false;

        while (!queue.isEmpty()) {
            checkInterrupted();
            NodeDepth current = queue.removeFirst();
            if (current.depth() >= selectedDepth) continue;

            if (selectedDirection != Direction.CALLERS) {
                clipped |= addConnections(current, outgoing.getOrDefault(current.method(), List.of()), true,
                        methods, includeExternal, nodes, edges, queue, visitedDepth);
            }
            if (selectedDirection != Direction.CALLEES) {
                clipped |= addConnections(current, incoming.getOrDefault(current.method(), List.of()), false,
                        methods, includeExternal, nodes, edges, queue, visitedDepth);
            }
            if (nodes.size() >= MAX_NODES) {
                clipped = true;
                break;
            }
        }
        return new GraphData(root, methods, nodes, edges, clipped);
    }

    private boolean addConnections(NodeDepth current, Collection<CallSite> adjacent, boolean outgoingEdge,
                                   Map<MethodKey, MethodTarget> methods, boolean includeExternal,
                                   Set<MethodKey> nodes, Set<CallSite> edges, ArrayDeque<NodeDepth> queue,
                                   Map<MethodKey, Integer> visitedDepth) {
        boolean clipped = false;
        for (CallSite call : adjacent) {
            MethodKey next = outgoingEdge ? call.target() : call.source();
            if (!includeExternal && !methods.containsKey(next)) continue;
            if (!nodes.contains(next) && nodes.size() >= MAX_NODES) {
                clipped = true;
                continue;
            }
            nodes.add(next);
            edges.add(call);
            int nextDepth = current.depth() + 1;
            Integer previousDepth = visitedDepth.get(next);
            if (methods.containsKey(next) && (previousDepth == null || nextDepth < previousDepth)) {
                visitedDepth.put(next, nextDepth);
                queue.addLast(new NodeDepth(next, nextDepth));
            }
        }
        return clipped;
    }

    private static void collectHandles(List<CallSite> calls, MethodKey source, AbstractInsnNode instruction,
                                       int instructionIndex, Collection<?> values) {
        for (Object value : values) {
            if (value instanceof Handle handle && handle.getTag() >= Opcodes.H_INVOKEVIRTUAL
                    && handle.getTag() <= Opcodes.H_INVOKEINTERFACE) {
                calls.add(new CallSite(source, new MethodKey(handle.getOwner(), handle.getName(), handle.getDesc()),
                        instruction, instructionIndex));
            } else if (value instanceof ConstantDynamic dynamic) {
                List<Object> arguments = new ArrayList<>(dynamic.getBootstrapMethodArgumentCount());
                for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
                    arguments.add(dynamic.getBootstrapMethodArgument(i));
                }
                collectHandles(calls, source, instruction, instructionIndex, arguments);
            }
        }
    }

    private void renderGraph(GraphData data) {
        graph.getModel().beginUpdate();
        try {
            clearCells();
            Object parent = graph.getDefaultParent();
            Map<MethodKey, Object> cells = new LinkedHashMap<>();
            for (MethodKey method : data.nodes()) {
                boolean root = method.equals(data.root());
                boolean local = data.methods().containsKey(method);
                String style = root
                        ? "fillColor=#8A5A22;fontColor=#FFFFFF;strokeColor=#E5A84B;strokeWidth=2;fontStyle=1"
                        : local
                        ? "fillColor=#2F5D7C;fontColor=#FFFFFF;strokeColor=#74A7C9"
                        : "fillColor=#3E434B;fontColor=#C8CDD5;strokeColor=#6C737D;dashed=1";
                Object cell = graph.insertVertex(parent, null, new MethodVertex(method, data.methods().get(method)),
                        0, 0, 220, 38, style + ";fontSize=12;spacing=6;whiteSpace=wrap;overflow=hidden");
                cells.put(method, cell);
            }
            for (CallSite call : data.edges()) {
                Object source = cells.get(call.source());
                Object target = cells.get(call.target());
                if (source != null && target != null) {
                    graph.insertEdge(parent, null,
                            new CallSiteVertex(call, data.methods().get(call.source())), source, target,
                            "fontColor=#AEB8C5;fontSize=10;labelBackgroundColor=#21252B;"
                                    + "labelBorderColor=#4A5260;spacing=2");
                }
            }

            PatchedHierarchicalLayout layout = new PatchedHierarchicalLayout(graph);
            layout.setFineTuning(data.nodes().size() <= 200);
            layout.setIntraCellSpacing(35);
            layout.setInterRankCellSpacing(90);
            layout.setParallelEdgeSpacing(60);
            layout.setDisableEdgeStyle(true);
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }

        status.setText(data.nodes().size() + " methods, " + data.edges().size() + " calls"
                + (data.clipped() ? " (limited to " + MAX_NODES + " methods)" : ""));
        graphComponent.zoomActual();
        graphComponent.revalidate();
        graphComponent.repaint();
    }

    private void clearCells() {
        Object[] cells = graph.getChildCells(graph.getDefaultParent(), true, true);
        if (cells.length > 0) graph.removeCells(cells);
    }

    private void navigate(MethodVertex vertex) {
        if (vertex.target() == null) return;
        jByteMod.getTabbedPane().getEditorTab().getCodeButton().doClick();
        jByteMod.selectMethod(vertex.target().classNode(), vertex.target().methodNode());
    }

    private void navigate(CallSiteVertex vertex) {
        if (vertex.caller() == null) return;
        jByteMod.getTabbedPane().getEditorTab().getCodeButton().doClick();
        jByteMod.selectMethod(vertex.caller().classNode(), vertex.caller().methodNode());
        jByteMod.getCodeList().selectInstruction(vertex.call().instruction());
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }

    private enum Direction {
        BOTH("Both"),
        CALLERS("Callers"),
        CALLEES("Callees");

        private final String label;

        Direction(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final class CallGraphComponent extends mxGraphComponent {
        private CallGraphComponent(mxGraph graph, Color background) {
            super(graph);
            getViewport().setBackground(background);
            getGraphControl().setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            setBorder(BorderFactory.createEmptyBorder());
            setConnectable(false);
            setDragEnabled(false);
            setImportEnabled(false);
            setExportEnabled(false);
            getGraphHandler().setMoveEnabled(false);
            getGraphHandler().setCloneEnabled(false);
            setToolTips(true);
            setZoomFactor(1.1);

            MouseAdapter listener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 2) return;
                    Object cell = getCellAt(event.getX(), event.getY());
                    if (cell instanceof mxCell graphCell && graphCell.getValue() instanceof MethodVertex vertex) {
                        navigate(vertex);
                    } else if (cell instanceof mxCell graphCell
                            && graphCell.getValue() instanceof CallSiteVertex vertex) {
                        navigate(vertex);
                    }
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    if (!SwingUtilities.isRightMouseButton(event)) return;
                    Object cell = getCellAt(event.getX(), event.getY());
                    if (!(cell instanceof mxCell graphCell)) return;

                    JPopupMenu menu = new JPopupMenu();
                    if (graphCell.getValue() instanceof CallSiteVertex vertex) {
                        JMenuItem goToCall = new JMenuItem("Go to calling instruction");
                        goToCall.addActionListener(ignored -> navigate(vertex));
                        menu.add(goToCall);
                    } else if (graphCell.getValue() instanceof MethodVertex vertex && vertex.target() != null) {
                        JMenuItem goToMethod = new JMenuItem("Go to method");
                        goToMethod.addActionListener(ignored -> navigate(vertex));
                        menu.add(goToMethod);
                    }
                    if (menu.getComponentCount() > 0) {
                        menu.show(getGraphControl(), event.getX(), event.getY());
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    if (!event.isControlDown()) return;
                    if (event.getWheelRotation() < 0) zoomIn();
                    else zoomOut();
                    event.consume();
                }
            };
            getGraphControl().addMouseListener(listener);
            getGraphControl().addMouseWheelListener(listener);
        }

    }

    private record MethodKey(String owner, String name, String descriptor) {
        private MethodKey {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(name);
            Objects.requireNonNull(descriptor);
        }
    }

    private record MethodTarget(ClassNode classNode, MethodNode methodNode) {
    }

    private record MethodVertex(MethodKey method, MethodTarget target) {
        @Override
        public String toString() {
            String owner = method.owner();
            int separator = owner.lastIndexOf('/');
            String simpleOwner = separator < 0 ? owner : owner.substring(separator + 1);
            return shorten(simpleOwner, 22) + "." + shorten(method.name(), 26);
        }

        private static String shorten(String value, int maximumLength) {
            if (value.length() <= maximumLength) return value;
            return value.substring(0, maximumLength - 1) + "…";
        }
    }

    private record CallSite(MethodKey source, MethodKey target, AbstractInsnNode instruction, int instructionIndex) {
    }

    private record CallSiteVertex(CallSite call, MethodTarget caller) {
        @Override
        public String toString() {
            return "@" + call.instructionIndex();
        }
    }

    private record NodeDepth(MethodKey method, int depth) {
    }

    private record GraphData(MethodKey root, Map<MethodKey, MethodTarget> methods, Set<MethodKey> nodes,
                             Set<CallSite> edges, boolean clipped) {
    }
}
