package expression.builder;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import usace.hec.expressions.ExpressionNode;

public class ExpressionNodeTextBox extends JPanel {
    private final JTextArea textArea;

    public ExpressionNodeTextBox() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Expression Preview"));

        textArea = new JTextArea();
        textArea.setEditable(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(245, 245, 245));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        textArea.setText("");

        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void insertAtCursor(String text) {
        int offset = textArea.getCaretPosition();
        textArea.replaceRange(text, offset, offset);
        textArea.setCaretPosition(offset + text.length());
        textArea.requestFocusInWindow();
    }

    public void insertNodeAtCursor(ExpressionNode<?> node) {
        if (node != null) {
            try {
                // Use the library's PreFixSyntax method as requested
                String syntax = node.PreFixSyntax();
                insertAtCursor(syntax);
            } catch (Exception e) {
                // Fallback if method signature differs or throws
                insertAtCursor(node.getClass().getSimpleName() + "()");
            }
        }
    }

    public String getExpression() {
        return textArea.getText();
    }
}