package expression.builder;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

public class ExpressionNodeTreeView extends JPanel {
    public ExpressionNodeTreeView(List<ExpressionNodeRegistry.NodeDescriptor> nodes, Consumer<ExpressionNodeRegistry.NodeDescriptor> onNodeSelected) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Operator Hierarchy"));

        Map<String, List<ExpressionNodeRegistry.NodeDescriptor>> byCategory = new TreeMap<>();
        for (ExpressionNodeRegistry.NodeDescriptor n : nodes) {
            byCategory.computeIfAbsent(n.getCategory(), k -> new ArrayList<>()).add(n);
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ExpressionOperators");
        for (Map.Entry<String, List<ExpressionNodeRegistry.NodeDescriptor>> entry : byCategory.entrySet()) {
            DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(entry.getKey());
            for (ExpressionNodeRegistry.NodeDescriptor n : entry.getValue()) {
                catNode.add(new DefaultMutableTreeNode(n.getOpName()));
            }
            root.add(catNode);
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode != null && !selectedNode.isRoot()) {
                String opName = selectedNode.getUserObject().toString();
                for (ExpressionNodeRegistry.NodeDescriptor n : nodes) {
                    if (n.getOpName().equals(opName)) {
                        onNodeSelected.accept(n);
                        break;
                    }
                }
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }
}