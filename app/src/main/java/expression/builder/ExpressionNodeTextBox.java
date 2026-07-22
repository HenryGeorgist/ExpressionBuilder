package expression.builder;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import usace.hec.expressions.ExpressionNode;

public class ExpressionNodeTextBox extends JPanel {
    private final JTextArea textArea;
    private boolean isProgrammaticUpdate = false;

    /**
     * Callback invoked when the user finishes editing the text area
     * (focus lost after a document change). Passes the full text.
     */
    public interface TextUpdateListener {
        void onTextUpdated(String text);
    }

    private TextUpdateListener textUpdateListener;

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

        // Listen for user edits: when focus is lost, notify the explorer
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!isProgrammaticUpdate && textUpdateListener != null) {
                    String text = textArea.getText().trim();
                    if (!text.isEmpty()) {
                        textUpdateListener.onTextUpdated(text);
                    }
                }
                isProgrammaticUpdate = false;
            }
        });

        // Also listen for Enter key as a commit action
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.getModifiersEx() == 0)) {
                    if (textUpdateListener != null) {
                        String text = textArea.getText().trim();
                        if (!text.isEmpty()) {
                            textUpdateListener.onTextUpdated(text);
                        }
                    }
                }
            }
        });

        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    /**
     * Replaces the entire text area content with the PreFixSyntax of the node.
     * This ensures the text area always contains exactly one well-formed expression.
     */
    public void setNodeText(ExpressionNode<?> node) {
        isProgrammaticUpdate = true;
        String syntax = "";
        if (node != null) {
            try {
                syntax = node.PreFixSyntax();
            } catch (Exception e) {
                syntax = node.getClass().getSimpleName() + "()";
            }
        }
        textArea.setText(syntax);
        textArea.setCaretPosition(0);
    }

    /**
     * Appends a node's PreFixSyntax to the existing text with a space separator.
     * Use this when you want to build up an expression by adding terms.
     */
    public void appendNodeText(ExpressionNode<?> node) {
        isProgrammaticUpdate = true;
        String syntax = "";
        if (node != null) {
            try {
                syntax = node.PreFixSyntax();
            } catch (Exception e) {
                syntax = node.getClass().getSimpleName() + "()";
            }
        }
        if (syntax.isEmpty()) return;

        String existing = textArea.getText();
        if (!existing.isEmpty()) {
            textArea.setText(existing + " " + syntax);
        } else {
            textArea.setText(syntax);
        }
        textArea.requestFocusInWindow();
    }

    /**
     * Gets the full expression text from the text area.
     */
    public String getExpression() {
        return textArea.getText();
    }

    /**
     * Returns the text area for external access (e.g., focus requests).
     */
    public JTextArea getTextArea() {
        return textArea;
    }

    /**
     * Sets a callback to receive notifications when the user finishes editing.
     */
    public void setTextUpdateListener(TextUpdateListener listener) {
        this.textUpdateListener = listener;
    }
}