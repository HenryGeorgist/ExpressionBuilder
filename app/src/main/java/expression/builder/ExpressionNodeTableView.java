package expression.builder;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

public class ExpressionNodeTableView extends JPanel {
    private final ExpressionNodeTableModel model;
    private final TableRowSorter<ExpressionNodeTableModel> sorter;

    public ExpressionNodeTableView(List<ExpressionNodeRegistry.NodeDescriptor> nodes) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Node Table"));

        model = new ExpressionNodeTableModel(nodes);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(6).setPreferredWidth(60);

        sorter = new TableRowSorter<>(model);
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
                    sorter.setRowFilter(new RowFilter<ExpressionNodeTableModel, Integer>() {
                        @Override
                        public boolean include(Entry<? extends ExpressionNodeTableModel, ? extends Integer> entry) {
                            int row = entry.getIdentifier();
                            for (int i = 0; i < model.getColumnCount(); i++) {
                                if (String.valueOf(model.getValueAt(row, i)).toLowerCase().contains(lower)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
            }
        });

        JPanel controls = new JPanel(new BorderLayout(10, 5));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        controls.add(new JLabel("ExpressionNode Explorer"), BorderLayout.WEST);
        controls.add(searchField, BorderLayout.CENTER);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupPrompt(JTextField field, String prompt) {
        field.setText(prompt);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(prompt)) { field.setText(""); field.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) { field.setText(prompt); field.setForeground(Color.GRAY); }
            }
        });
    }

    static class ExpressionNodeTableModel extends AbstractTableModel {
        private final List<ExpressionNodeRegistry.NodeDescriptor> data;
        private final String[] columns = {"Category", "Class", "Operator", "Infix", "Leaf", "Binary", "Unary"};

        ExpressionNodeTableModel(List<ExpressionNodeRegistry.NodeDescriptor> data) { this.data = data; }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            ExpressionNodeRegistry.NodeDescriptor d = data.get(row);
            return switch (col) {
                case 0 -> d.getCategory();
                case 1 -> d.getSimpleName();
                case 2 -> d.getOpName();
                case 3 -> d.getInfixName();
                case 4 -> d.isLeaf();
                case 5 -> d.isBinary();
                case 6 -> d.isUnary();
                default -> "";
            };
        }
    }
}