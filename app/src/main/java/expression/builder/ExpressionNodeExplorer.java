package expression.builder;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import javax.swing.*;
import usace.hec.expressions.ExpressionNode;

public class ExpressionNodeExplorer {
    /** The full expression tree, parsed from the text area content. */
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

        // Wire up the text-update callback: when user edits and exits,
        // re-parse the text and update the evaluation label.
        textBox.setTextUpdateListener((text) -> {
            try {
                currentExpression = parseExpressionText(text);
                updateEvaluationLabel();
            } catch (Exception e) {
                currentExpression = null;
                evaluationLabel.setText("Evaluation: Parse error (" + e.getClass().getSimpleName() + ")");
                evaluationLabel.setForeground(new Color(0xD3, 0x2F, 0x2F));
            }
        });

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
                ExpressionNode<?> newNode = instantiateNodeWithDefaults(descriptor.getClazz());
                // Replace the entire text with the new node's syntax
                currentExpression = newNode;
                textBox.setNodeText(newNode);
                updateEvaluationLabel();
            } catch (Exception ex) {
                ex.printStackTrace();
                currentExpression = null;
                textBox.setNodeText(null);
                evaluationLabel.setText("Evaluation: N/A (Failed to instantiate)");
                evaluationLabel.setForeground(Color.GRAY);
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

    /**
     * Updates only the evaluation label based on currentExpression.
     */
    private void updateEvaluationLabel() {
        if (currentExpression != null) {
            try {
                Object result = currentExpression.evaluate();
                evaluationLabel.setText("Evaluation: " + (result != null ? result : "null"));
                evaluationLabel.setForeground(new Color(0x4C, 0xAF, 0x50));
            } catch (Exception e) {
                evaluationLabel.setText("Evaluation: Error (" + e.getClass().getSimpleName() + ")");
                evaluationLabel.setForeground(new Color(0xD3, 0x2F, 0x2F));
            }
        } else {
            evaluationLabel.setText("Evaluation: N/A");
            evaluationLabel.setForeground(new Color(0x2C, 0x5F, 0x8A));
        }
    }

    /**
     * Parses the given text into an ExpressionNode tree using fromPreFixSyntax.
     * The return type is automatically guessed based on the text content.
     */
    @SuppressWarnings("unchecked")
    private ExpressionNode<?> parseExpressionText(String text) throws Exception {
        Class<?> returnType = guessExpressionType(text);
        
        // Use reflection to safely invoke the library's parsing method
        try {
            Method method = ExpressionNode.class.getMethod("fromPreFixSyntax", String.class, Class.class);
            return (ExpressionNode<?>) method.invoke(null, text, returnType);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("ExpressionNode.fromPreFixSyntax(String, Class) not found in library.", e);
        }
    }

    /**
     * Guesses the expected return type of an expression based on its text content.
     */
    private Class<?> guessExpressionType(String text) {
        String lower = text.toLowerCase();
        
        // Boolean indicators: literals, logical operators, or comparison operators
        if (lower.contains("true") || lower.contains("false") || 
            lower.contains(" and ") || lower.contains(" or ") || lower.contains(" not ") ||
            lower.contains("==") || lower.contains("!=") || 
            lower.contains(">=") || lower.contains("<=") ||
            lower.contains(">") || lower.contains("<")) {
            return Boolean.class;
        }
        
        // String indicators: quoted values
        if (text.contains("\"") || text.contains("'")) {
            return String.class;
        }
        
        // Default to numerical (covers most math expressions)
        return Double.class;
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

        Constructor<?>[] ctors = leafClass.getDeclaredConstructors();
        return (ExpressionNode<?>) ctors[0].newInstance(value);
    }
}