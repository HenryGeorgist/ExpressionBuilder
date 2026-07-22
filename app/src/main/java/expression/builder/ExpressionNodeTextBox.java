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

    public void setNodeText(ExpressionNode<?> node) {
        isProgrammaticUpdate = true;
        String syntax = node != null ? node.PreFixSyntax() : "";
        textArea.setText(syntax);
        textArea.setCaretPosition(0);
    }

    /**
     * Inserts a node's PreFixSyntax at the current caret position.
     * Automatically adds a leading space if needed.
     */
    public void insertNodeAtCursor(ExpressionNode<?> node) {
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

        int caretPos = textArea.getCaretPosition();
        String existing = textArea.getText();
        
        // Add a space before if inserting in the middle and previous char isn't whitespace
        if (caretPos > 0 && !existing.isEmpty()) {
            char prevChar = existing.charAt(caretPos - 1);
            if (!Character.isWhitespace(prevChar)) {
                syntax = " " + syntax;
            }
        }
        
        textArea.replaceRange(syntax, caretPos, caretPos);
        textArea.setCaretPosition(caretPos + syntax.length());
        textArea.requestFocusInWindow();
    }

    public String getExpression() {
        return textArea.getText();
    }

    public void setTextUpdateListener(TextUpdateListener listener) {
        this.textUpdateListener = listener;
    }
}