package expression.builder;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import javax.swing.*;
import usace.hec.expressions.ExpressionNode;

public class ExpressionNodeExplorer {
    private ExpressionNode<?> currentExpression;
    private ExpressionNodeTextBox textBox;
    private JLabel evaluationLabel;

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

        textBox = new ExpressionNodeTextBox();
        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);
        
        evaluationLabel = new JLabel("Evaluation: N/A");
        evaluationLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        evaluationLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        evaluationLabel.setForeground(new Color(0x2C, 0x5F, 0x8A));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(evaluationLabel, BorderLayout.NORTH);
        bottomPanel.add(textBox, BorderLayout.CENTER);

        // Wire the tree to instantiate the node with dummy children and update the UI
        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, (descriptor) -> {
            try {
                currentExpression = instantiateNodeWithDefaults(descriptor.getClazz());
                updateUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                currentExpression = null;
                updateUI();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableView, treeView);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.5);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void updateUI() {
        if (currentExpression != null) {
            // Insert/replace text at cursor instead of overwriting everything
            textBox.insertNodeAtCursor(currentExpression);
            
            try {
                Object result = currentExpression.evaluate();
                evaluationLabel.setText("Evaluation: " + (result != null ? result : "null"));
                evaluationLabel.setForeground(new Color(0x4C, 0xAF, 0x50));
            } catch (Exception e) {
                evaluationLabel.setText("Evaluation: Error (" + e.getClass().getSimpleName() + ")");
                evaluationLabel.setForeground(new Color(0xD3, 0x2F, 0x2F));
            }
        } else {
            evaluationLabel.setText("Evaluation: N/A (Failed to instantiate)");
            evaluationLabel.setForeground(Color.GRAY);
        }
    }

    /**
     * Instantiates an ExpressionNode class, automatically providing dummy child nodes
     * with default values based on the generic type T.
     */
    @SuppressWarnings("unchecked")
    private ExpressionNode<?> instantiateNodeWithDefaults(Class<? extends ExpressionNode<?>> clazz) throws Exception {
        TypeVariable<?>[] typeParams = clazz.getTypeParameters();
        Class<?> rawType = Object.class;
        
        if (typeParams.length > 0) {
            Type bound = typeParams[0].getBounds()[0];
            if (bound instanceof Class) {
                rawType = (Class<?>) bound;
            }
        }

        ExpressionNode<?> dummyLeaf = createDummyLeaf(rawType);
        String simpleName = clazz.getSimpleName();
        
        if (simpleName.equals("IfNode")) {
            ExpressionNode<?> boolLeaf = createDummyLeaf(Boolean.class);
            Constructor<?> ctor = clazz.getDeclaredConstructor(
                ExpressionNode.class, ExpressionNode.class, ExpressionNode.class
            );
            return (ExpressionNode<?>) ctor.newInstance(boolLeaf, dummyLeaf, dummyLeaf);
        } else if (clazz.getSuperclass() != null && clazz.getSuperclass().getSimpleName().equals("BinaryExpressionNode")) {
            Constructor<?> ctor = clazz.getDeclaredConstructor(
                ExpressionNode.class, ExpressionNode.class
            );
            return (ExpressionNode<?>) ctor.newInstance(dummyLeaf, dummyLeaf);
        } else if (clazz.getSuperclass() != null && clazz.getSuperclass().getSimpleName().equals("UnaryExpressionNode")) {
            Constructor<?> ctor = clazz.getDeclaredConstructor(ExpressionNode.class);
            return (ExpressionNode<?>) ctor.newInstance(dummyLeaf);
        } else {
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(String.class);
                return (ExpressionNode<?>) ctor.newInstance("Dummy");
            } catch (NoSuchMethodException e) {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                return (ExpressionNode<?>) ctor.newInstance();
            }
        }
    }

    /**
     * Creates a dummy ConstantLeafNode with a default value based on the target type T.
     */
    @SuppressWarnings("unchecked")
    private ExpressionNode<?> createDummyLeaf(Class<?> type) throws Exception {
        Class<?> leafClass = Class.forName("usace.hec.expressions.ConstantLeafNode");
        Object value;
        
        if (type == Boolean.class || type == boolean.class) {
            value = false;
        } else if (type == String.class) {
            value = "";
        } else {
            // Default for numbers (Double, Float, Integer, Long)
            value = 0.0;
        }
        
        // Use the constructor that accepts the specific generic type T
        Constructor<?>[] ctors = leafClass.getDeclaredConstructors();

        //Constructor<?> ctor = leafClass.getDeclaredConstructor(type);
        return (ExpressionNode<?>) ctors[0].newInstance(value);
    }
}