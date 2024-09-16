package me.grax.jbytemod.ui.graph;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import de.xbrowniecodez.jbytemod.Main;
import lombok.Getter;
import me.grax.jbytemod.analysis.block.Block;
import me.grax.jbytemod.ui.dialogue.InsnEditDialogue;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

@Getter
public class CFGraph extends mxGraph {
    private final CFGComponent component;

    public CFGraph(Color backgroundColor) {
        this.component = new CFGComponent(this, backgroundColor);
        setAutoOrigin(true);
        setAutoSizeCells(true);
        setHtmlLabels(true);
        setAllowDanglingEdges(true);
        setStyles();
        this.resetEdgesOnMove = true;
    }

    @Override
    public mxRectangle getPreferredSizeForCell(Object cell) {
        mxRectangle size = super.getPreferredSizeForCell(cell);
        size.setWidth(size.getWidth() + 10); // Some items touch the border
        return size;
    }

    private void setStyles() {
        Map<String, Object> edgeStyle = this.getStylesheet().getDefaultEdgeStyle();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_VERTICAL);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
        edgeStyle.put(mxConstants.STYLE_TARGET_PERIMETER_SPACING, 1d);

        Map<String, Object> vertexStyle = this.getStylesheet().getDefaultVertexStyle();
        vertexStyle.put(mxConstants.STYLE_SHADOW, true);

        mxStylesheet stylesheet = new mxStylesheet();
        stylesheet.setDefaultEdgeStyle(edgeStyle);
        stylesheet.setDefaultVertexStyle(vertexStyle);
        this.setStylesheet(stylesheet);
    }

    public static class CFGComponent extends mxGraphComponent {

        private JScrollPane scrollPane;

        public CFGComponent(mxGraph graph, Color backgroundColor) {
            super(graph);
            this.getViewport().setBackground(backgroundColor);
            this.setEnabled(false);
            this.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.setZoomFactor(1.1);

            addMouseListeners();
        }

        private void addMouseListeners() {
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePressed(e);
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    handleMouseWheelMoved(e);
                }
            };

            this.getGraphControl().addMouseListener(mouseAdapter);
            this.getGraphControl().addMouseMotionListener(mouseAdapter);
            this.getGraphControl().addMouseWheelListener(mouseAdapter);
        }

        private void handleMousePressed(MouseEvent e) {
            mxCell cell = (mxCell) getCellAt(e.getX(), e.getY());
            if (cell != null && cell.getValue() instanceof BlockVertex) {
                BlockVertex blockVertex = (BlockVertex) cell.getValue();

                if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e, blockVertex);
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    handleDoubleClick(e, cell, blockVertex);
                }
            }
        }

        private void showContextMenu(MouseEvent e, BlockVertex blockVertex) {
            JPopupMenu menu = new JPopupMenu();

            JMenuItem goToDecompiledCode = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("go_to_dec"));
            goToDecompiledCode.addActionListener(event -> navigateToDecompiledCode(blockVertex));

            menu.add(goToDecompiledCode);
            menu.show(Main.INSTANCE.getJByteMod(), e.getXOnScreen(), e.getYOnScreen());
        }

        private void navigateToDecompiledCode(BlockVertex blockVertex) {
            Main.INSTANCE.getJByteMod().getCodeList().setSelectedIndex(blockVertex.getListIndex());
            Main.INSTANCE.getJByteMod().getTabbedPane().getEditorTab().getCodeButton().doClick();
        }

        private void handleDoubleClick(MouseEvent e, mxCell cell, BlockVertex blockVertex) {
            mxCellState cellState = getGraph().getView().getState(cell);
            double clickPosition = (double) (e.getY() - cellState.getY());
            double clickRatio = clickPosition / cellState.getHeight();
            Block block = blockVertex.getBlock();

            int instructionIndex = (int) Math.floor(block.getNodes().size() * clickRatio);
            try {
                AbstractInsnNode instruction = block.getNodes().get(instructionIndex);
                if (InsnEditDialogue.canEdit(instruction) && new InsnEditDialogue(Main.INSTANCE.getJByteMod().getCurrentMethod(), instruction).open()) {
                    Main.INSTANCE.getJByteMod().getControlFlowPanel().generateList();
                }
            } catch (Exception ex) {
                new ErrorDisplay(ex);
            }
            e.consume();
        }

        private void handleMouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            } else if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(
                        scrollPane.getVerticalScrollBar().getValue() + e.getUnitsToScroll() * scrollPane.getVerticalScrollBar().getUnitIncrement()
                );
            }
        }

        @Override
        public void zoomIn() {
            if (getGraph().getView().getScale() < 4) {
                zoom(zoomFactor);
            }
        }

        @Override
        public void zoomOut() {
            double scale = getGraph().getView().getScale();
            if (scrollPane != null && (scrollPane.getVerticalScrollBar().isVisible() || scale >= 1) && scale > 0.3) {
                zoom(1 / zoomFactor);
            }
        }
    }
}
