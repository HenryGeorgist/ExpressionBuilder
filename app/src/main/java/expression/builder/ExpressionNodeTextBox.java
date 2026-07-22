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

    /**
     * Converts the node to PreFixSyntax and inserts/replaces text at the caret position.
     */
    public void insertNodeAtCursor(ExpressionNode<?> node) {
        String syntax = "";
        if (node != null) {
            try {
                syntax = node.PreFixSyntax();
            } catch (Exception e) {
                syntax = node.getClass().getSimpleName() + "()";
            }
        }
        insertText(syntax);
    }

    private void insertText(String text) {
        if (text == null) return;
        
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        
        // replaceRange handles both cursor insertion (start == end) 
        // and selection replacement (start != end)
        textArea.replaceRange(text, start, end);
        textArea.setCaretPosition(start + text.length());
        textArea.requestFocusInWindow();
    }

    public String getExpression() {
        return textArea.getText();
    }
}