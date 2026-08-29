package me.grax.jbytemod.ui.graph;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.Main;
import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.analysis.block.Block;
import me.grax.jbytemod.analysis.converter.Converter;
import me.grax.jbytemod.ui.graph.CFGraph.CFGComponent;
import me.grax.jbytemod.utils.ErrorDisplay;
import me.grax.jbytemod.utils.ImageUtils;
import org.objectweb.asm.tree.MethodNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class ControlFlowPanel extends JPanel {

    private static final int MAX_GRAPH_BLOCKS = 1000;
    private static final String EXCEPTION_COLOR = "#FFA500";
    private static final String EDGE_COLOR = "#111111";
    private static final String JUMP_COLOR = "#39698a";
    private static final String JUMP_COLOR_GREEN = "#388a47";
    private static final String JUMP_COLOR_RED = "#8a3e38";
    private static final String JUMP_COLOR_PURPLE = "#ff71388a";
    private static final String JUMP_COLOR_PINK = "#ba057a";

    private MethodNode methodNode;
    private final List<Block> controlFlow = new ArrayList<>();
    private final CFGraph graph;
    private final CFGComponent graphComponent;
    private JScrollPane scrollPane;
    private final HashMap<Block, mxCell> existingBlocks = new HashMap<>();
    private SwingWorker<ArrayList<Block>, Void> graphWorker;
    private long graphRequest;

    public ControlFlowPanel(JByteMod jbm) {
        Color backgroundColor = jbm.getOptions().get("use_dark_theme").getBoolean() ? new Color(33, 37, 43) : Color.WHITE;
        this.setLayout(new BorderLayout(0, 0));

        graph = new CFGraph(backgroundColor);
        graphComponent = graph.getComponent();

        setupUI(jbm, backgroundColor);
    }

    private void setupUI(JByteMod jbm, Color backgroundColor) {
        JPanel labelPanel = createLabelPanel(jbm);
        this.add(labelPanel, BorderLayout.NORTH);

        JPanel graphContainer = createGraphContainer(backgroundColor);
        scrollPane = new JScrollPane(graphContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createLabelPanel(JByteMod jbm) {
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBorder(new EmptyBorder(1, 5, 0, 1));

        JLabel titleLabel = new JLabel(jbm.getLanguageRes().getResource("ctrl_flow_vis"));
        labelPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(createSaveButton(jbm));
        buttonPanel.add(createReloadButton(jbm));

        labelPanel.add(buttonPanel, BorderLayout.EAST);
        return labelPanel;
    }

    private JButton createSaveButton(JByteMod jbm) {
        JButton saveButton = new JButton(jbm.getLanguageRes().getResource("save"));
        saveButton.addActionListener(e -> saveGraph(jbm));
        return saveButton;
    }

    private JButton createReloadButton(JByteMod jbm) {
        JButton reloadButton = new JButton(jbm.getLanguageRes().getResource("reload"));
        reloadButton.addActionListener(e -> generateList());
        return reloadButton;
    }

    private JPanel createGraphContainer(Color backgroundColor) {
        JPanel innerPanel = new JPanel(new BorderLayout(0, 0));
        innerPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        innerPanel.setBackground(backgroundColor);
        innerPanel.add(graphComponent, BorderLayout.CENTER);
        return innerPanel;
    }

    public void generateList() {
        final MethodNode targetMethod = methodNode;
        final long request;
        synchronized (this) {
            graphRequest++;
            request = graphRequest;
            if (graphWorker != null) {
                graphWorker.cancel(true);
            }
        }

        if (targetMethod == null || targetMethod.instructions.size() == 0) {
            clearGraph();
            return;
        }

        final boolean simplify = Main.INSTANCE.getJByteMod().getOptions().get("simplify_graph").getBoolean();
        final boolean removeRedundant = Main.INSTANCE.getJByteMod().getOptions().get("remove_redundant").getBoolean();
        final int maxRedundantInput = Main.INSTANCE.getJByteMod().getOptions().get("max_redundant_input").getInteger();

        SwingWorker<ArrayList<Block>, Void> worker = new SwingWorker<ArrayList<Block>, Void>() {
            @Override
            protected ArrayList<Block> doInBackground() {
                return new Converter(targetMethod).convert(simplify, removeRedundant, true, maxRedundantInput);
            }

            @Override
            protected void done() {
                synchronized (ControlFlowPanel.this) {
                    if (isCancelled() || request != graphRequest || targetMethod != methodNode) {
                        return;
                    }
                    graphWorker = null;
                }

                try {
                    ArrayList<Block> blocks = get();
                    if (blocks.size() > MAX_GRAPH_BLOCKS) {
                        throw new IllegalArgumentException("Control flow graph has too many blocks to render ("
                                + blocks.size() + ", maximum " + MAX_GRAPH_BLOCKS + ")");
                    }
                    controlFlow.clear();
                    controlFlow.addAll(blocks);
                    updateGraph();
                } catch (Exception e) {
                    new ErrorDisplay(e);
                    clearGraph();
                }
            }
        };

        synchronized (this) {
            if (request == graphRequest) {
                graphWorker = worker;
            } else {
                worker.cancel(true);
                return;
            }
        }
        worker.execute();
    }

    private void updateGraph() {
        graph.getModel().beginUpdate();
        try {
            graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
            existingBlocks.clear();

            if (!controlFlow.isEmpty()) {
                Block firstBlock = controlFlow.get(0);
                addBlock((mxCell) graph.getDefaultParent(), firstBlock, null);
            }

            applyLayout();
        } finally {
            graph.getModel().endUpdate();
        }

        this.revalidate();
        this.repaint();
    }

    private void applyLayout() {
        PatchedHierarchicalLayout layout = new PatchedHierarchicalLayout(graph);
        layout.setFineTuning(controlFlow.size() <= 250);
        layout.setIntraCellSpacing(25d);
        layout.setInterRankCellSpacing(80d);
        layout.setDisableEdgeStyle(true);
        layout.setParallelEdgeSpacing(100d);
        layout.setUseBoundingBox(true);
        layout.execute(graph.getDefaultParent());
    }

    private mxCell addBlock(mxCell parent, Block block, BlockVertex inputVertex) {
        if (existingBlocks.containsKey(block)) {
            mxCell cachedBlock = existingBlocks.get(block);
            if (inputVertex != null) {
                ((BlockVertex) cachedBlock.getValue()).addInput(inputVertex);
            }
            return cachedBlock;
        }

        BlockVertex vertex = new BlockVertex(methodNode, block, block.getNodes(), block.getLabel(),
                methodNode.instructions.indexOf(block.getNodes().get(0)));
        if (inputVertex != null) {
            vertex.addInput(inputVertex);
        }
        vertex.setupText();

        mxCell cell = (mxCell) graph.insertVertex(parent, null, vertex, 150, 10, 80, 40, "fillColor=#21252B;fontColor=#e8e8e8;strokeColor=#9297a1");
        graph.updateCellSize(cell);

        existingBlocks.put(block, cell);
        addBlockEdges(parent, block, cell);

        return cell;
    }

    private void addBlockEdges(mxCell parent, Block block, mxCell cell) {
        for (int i = 0; i < block.getOutput().size(); i++) {
            Block nextBlock = block.getOutput().get(i);
            String edgeColor = getEdgeColor(block, i);
            if (nextBlock.equals(block)) {
                graph.insertEdge(parent, null, null, cell, cell, "strokeColor=" + edgeColor + ";");
            } else {
                mxCell nextCell = addBlock(parent, nextBlock, (BlockVertex) cell.getValue());
                graph.insertEdge(parent, null, null, cell, nextCell, "strokeColor=" + edgeColor + ";");
            }
        }
        for (int i = 0; i < block.getExceptions().size(); i++) {
            Block nextBlock = block.getExceptions().get(i);
            String label = block.exceptionTypes.get(nextBlock);
            if (label == null) {
                label = "java/lang/Throwable";
            } else if (label.contains("java/lang/")) {
                label = label.replace("java/lang/", "");
            }

            if (nextBlock.equals(block)) {
                graph.insertEdge(parent, null, label, cell, cell, "strokeColor=" + EXCEPTION_COLOR + ";dashed=1;");
            } else {
                mxCell nextCell = addBlock(parent, nextBlock, (BlockVertex) cell.getValue());
                graph.insertEdge(parent, null, label, cell, nextCell, "strokeColor=" + EXCEPTION_COLOR + ";dashed=1;");
            }
        }
    }

    private String getEdgeColor(Block block, int index) {
        if (block.endsWithJump()) {
            return block.getOutput().size() > 1 ? (index == 0 ? JUMP_COLOR_GREEN : JUMP_COLOR_RED) : JUMP_COLOR;
        }
        if (block.endsWithSwitch()) {
            return index == 0 ? JUMP_COLOR_PINK : JUMP_COLOR_PURPLE;
        }
        return EDGE_COLOR;
    }

    public void clear() {
        synchronized (this) {
            graphRequest++;
            if (graphWorker != null) {
                graphWorker.cancel(true);
                graphWorker = null;
            }
        }
        clearGraph();
    }

    private void clearGraph() {
        graph.getModel().beginUpdate();
        try {
            graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
            existingBlocks.clear();
            controlFlow.clear();
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void saveGraph(JByteMod jbm) {
        if (methodNode == null) return;

        JFileChooser fileChooser = createFileChooser();
        if (fileChooser.showSaveDialog(ControlFlowPanel.this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            String fileType = ((FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];
            saveImageFile(outputFile, fileType, jbm);
        }
    }

    private JFileChooser createFileChooser() {
        File defaultDir = new File(System.getProperty("user.home") + File.separator + "Desktop");
        JFileChooser fileChooser = new JFileChooser(defaultDir);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Bitmap image file (.bmp)", "bmp"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Portable Network Graphics (.png)", "png"));

        String defaultFileName = methodNode.name.length() < 32 ? methodNode.name : "method";
        fileChooser.setSelectedFile(new File(defaultDir, defaultFileName + ".bmp"));
        return fileChooser;
    }

    private void saveImageFile(File outputFile, String fileType, JByteMod jbm) {
        Main.INSTANCE.getLogger().log("Saving graph as " + fileType + " file (" + outputFile.getName() + ")");
        BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, graph.getComponent().getBackground(), true, null);
        try {
            ImageIO.write(ImageUtils.watermark(image), fileType, outputFile);
        } catch (IOException e) {
            new ErrorDisplay(e);
        }
    }
}
