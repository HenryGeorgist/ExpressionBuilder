package usace.hec.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ExpressionNodeExplorer {
    public static void main(String[] args) {
        // Swing must run on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
            catch (Exception e) { e.printStackTrace(); }
            new ExpressionNodeExplorer().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        List<Map<String, String>> rawNodes = ExpressionNodeConsoleApp.discoverNodes();
        if (rawNodes == null) rawNodes = List.of();

        JFrame frame = new JFrame("HEC ExpressionNode Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 450);
        frame.setLocationRelativeTo(null);

        // 1. Table Model
        NodeTableModel model = new NodeTableModel(rawNodes);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        
        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Category
        table.getColumnModel().getColumn(1).setPreferredWidth(160); // Class
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // Operator
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Infix
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Type

        // 2. Filtering Setup
        TableRowSorter<NodeTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField searchField = new JTextField(20);
        setupPrompt(searchField, "Filter by name, operator, or category...");
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }

            private void applyFilter() {
                String text = searchField.getText();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    String lower = text.toLowerCase();
                    sorter.setRowFilter((row) -> {
                        for (int i = 0; i < model.getColumnCount(); i++) {
                            String val = String.valueOf(model.getValueAt(row.getIndex(), i));
                            if (val.toLowerCase().contains(lower)) return true;
                        }
                        return false;
                    });
                }
            }
        });

        // 3. Layout
        JPanel controls = new JPanel(new BorderLayout(10, 5));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controls.add(new JLabel("ExpressionNode Explorer"), BorderLayout.WEST);
        controls.add(searchField, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(controls, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.setVisible(true);
    }
    
    // Helper to mimic JavaFX promptText in Swing
    private void setupPrompt(JTextField field, String prompt) {
        field.setText(prompt);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(prompt)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(prompt);
                    field.setForeground(Color.GRAY);
                }
            }
        });
    }

    // Custom TableModel wrapping List<Map<String, String>>
    static class NodeTableModel extends AbstractTableModel {
        private final List<Map<String, String>> data;
        private final String[] columns = {"Category", "Class", "Operator", "Infix", "Type"};
        private final String[] keys = {"category", "simpleName", "opName", "infixName", "type"};

        NodeTableModel(List<Map<String, String>> data) {
            this.data = data;
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) { return data.get(row).get(keys[col]); }
    }
}