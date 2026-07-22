package expression.builder;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import usace.hec.expressions.ExpressionNode;

public class ExpressionNodeExplorer {
    // Store the actual library ExpressionNode instance
    private ExpressionNode<?> currentExpression;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            new ExpressionNodeExplorer().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        List<ExpressionNodeRegistry.NodeDescriptor> nodes = ExpressionNodeRegistry.discoverAllNodes();

        JFrame frame = new JFrame("HEC ExpressionNode Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);

        ExpressionNodeTextBox textBox = new ExpressionNodeTextBox();
        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);
        
        // Wire the tree to store the ExpressionNode and insert its PreFixSyntax at cursor
        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, (node) -> {
            currentExpression = node;
            textBox.insertNodeAtCursor(node);
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableView, treeView);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.5);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(textBox, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}