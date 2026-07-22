package expression.builder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;

/**
 * Wizard-style expression builder for reservoir operation rules.
 * Guides users through common rule patterns with dropdowns and form inputs.
 * 
 * The wizard converts user selections into Expression library AST nodes
 * (IF, GT, LT, ADD, etc.) and displays a live preview of the generated
 * expression string.
 */
public class ExpressionWizardPanel extends JPanel {

    // ====================================================================
    // Data Model
    // ====================================================================

    /** List of condition rows currently in the expression. */
    private final ArrayList<ConditionRow> conditions = new ArrayList<>();

    /** List of ELSE IF branches added by the user. */
    private final ArrayList<ElseIfBranch> elseIfBranches = new ArrayList<>();

    /** Whether an ELSE branch exists. */
    private boolean hasElseBranch = false;

    // ====================================================================
    // UI Components
    // ====================================================================

    private JComboBox<String> ruleTypeCombo;
    private JComboBox<String> zoneCombo;
    private JComboBox<String> actionTypeCombo;
    private JTextField actionValueField;
    private JComboBox<String> actionUnitCombo;
    private JTextArea expressionPreview;
    private JLabel statusLabel;
    private JPanel conditionsPanel;
    private JList<String> templateList;

    // ====================================================================
    // Dropdown Data
    // ====================================================================

    private static final String[] RULE_TYPES = {
        "Release Limit", "Elevation Target", "Storage Target",
        "Minimum Flow", "Maximum Flow", "Rate of Change"
    };

    private static final String[] ZONES = {
        "Normal Flood Control", "Flood Control", "Conservation",
        "Inactive", "Top of Dam", "All Zones"
    };

    private static final String[] VARIABLES = {
        "Pool Elevation", "Pool Storage", "Pool Area",
        "Reservoir Inflow", "Reservoir Outflow", "Spillway Flow",
        "Previous Release", "Previous Elevation",
        "Month", "Day", "Year", "Julian Day",
        "[State Variable...]", "[External Time Series...]"
    };

    private static final String[] COMPARATORS = {
        "is greater than",
        "is greater than or equal to",
        "is less than",
        "is less than or equal to",
        "is equal to",
        "is not equal to"
    };

    private static final String[] LOGIC_OPERATORS = { "AND", "OR" };

    private static final String[] ACTION_TYPES = {
        "Maximum Release", "Minimum Release", "Specified Release",
        "Release as % of Inflow", "Hold Previous Release",
        "Release = Inflow + Delta"
    };

    private static final String[] FLOW_UNITS = { "cfs", "m\u00B3/s", "AFD", "AF" };
    private static final String[] ELEV_UNITS = { "ft", "m", "ft (NAVD88)", "ft (NGVD29)" };

    // ====================================================================
    // Color Palette
    // ====================================================================

    private static final Color HEADER_BG      = new Color(0x2C, 0x5F, 0x8A);
    private static final Color ACCENT_LIGHT   = new Color(0xE8, 0xF0, 0xF8);
    private static final Color HIGHLIGHT      = new Color(0x4A, 0x90, 0xD9);
    private static final Color BORDER_COLOR   = new Color(0xCC, 0xCC, 0xCC);
    private static final Color TEXT_DARK      = new Color(0x33, 0x33, 0x33);
    private static final Color TEXT_LIGHT     = new Color(0x66, 0x66, 0x66);
    private static final Color GREEN_CHECK    = new Color(0x4C, 0xAF, 0x50);
    private static final Color RED_ERROR      = new Color(0xD3, 0x2F, 0x2F);
    private static final Color ORANGE_ACCENT  = new Color(0xE6, 0x51, 0x00);
    private static final Color ORANGE_LIGHT   = new Color(0xFF, 0xF3, 0xE0);
    private static final Color DARK_BG        = new Color(0x2D, 0x2D, 0x2D);
    private static final Color CODE_FUNC      = new Color(0x56, 0x9C, 0xD6);
    private static final Color CODE_LITERAL   = new Color(0xCE, 0x91, 0x78);
    private static final Color CODE_VAR       = new Color(0xDC, 0xDC, 0xAA);

    // ====================================================================
    // Constructor
    // ====================================================================

    public ExpressionWizardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        add(createMainPanel(), BorderLayout.CENTER);
        add(createPreviewPanel(), BorderLayout.SOUTH);
        add(createTemplatePanel(), BorderLayout.EAST);

        // Initialize with one condition row
        addConditionRow();
        updateExpression();
    }

    // ====================================================================
    // Main Form Panel
    // ====================================================================

    private JPanel createMainPanel() {
        JPanel main = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        // --- Rule Type ---
        main.add(createLabel("Rule Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        ruleTypeCombo = new JComboBox<>(RULE_TYPES);
        ruleTypeCombo.setPreferredSize(new Dimension(200, 26));
        ruleTypeCombo.addActionListener(e -> updateExpression());
        main.add(ruleTypeCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        main.add(createInfoButton("Select the type of rule to create."), gbc);

        // --- Zone ---
        gbc.gridy = 1;
        gbc.gridx = 0;
        main.add(createLabel("Zone:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        zoneCombo = new JComboBox<>(ZONES);
        zoneCombo.setPreferredSize(new Dimension(200, 26));
        zoneCombo.addActionListener(e -> updateExpression());
        main.add(zoneCombo, gbc);

        // Separator
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        main.add(createSeparator(), gbc);

        // --- Condition Section ---
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        main.add(createSectionLabel("Condition:"), gbc);

        // Conditions scroll panel
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        conditionsPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(conditionsPanel);
        scrollPane.setPreferredSize(new Dimension(0, 80));
        scrollPane.setBorder(new LineBorder(new Color(0xE0, 0xE0, 0xE0)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scrollPane, gbc);

        // Add Condition + Logic buttons
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JButton addConditionBtn = createAccentButton("+ Add Condition");
        addConditionBtn.addActionListener(e -> addConditionRow());
        main.add(addConditionBtn, gbc);

        gbc.gridx = 1;
        JComboBox<String> logicCombo = new JComboBox<>(LOGIC_OPERATORS);
        logicCombo.setPreferredSize(new Dimension(70, 26));
        logicCombo.addActionListener(e -> updateExpression());
        main.add(logicCombo, gbc);

        // Separator
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        main.add(createSeparator(), gbc);

        // --- Then Apply Section ---
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        main.add(createSectionLabel("Then Apply:"), gbc);

        // Action row
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.weightx = 0;
        actionTypeCombo = new JComboBox<>(ACTION_TYPES);
        actionTypeCombo.setPreferredSize(new Dimension(160, 26));
        actionTypeCombo.addActionListener(e -> updateExpression());
        main.add(actionTypeCombo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        main.add(new JLabel("  of  "), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.5;
        actionValueField = new JTextField("4000");
        actionValueField.setPreferredSize(new Dimension(100, 26));
        actionValueField.setBorder(new LineBorder(HIGHLIGHT, 2));
        actionValueField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateExpression(); }
            public void insertUpdate(DocumentEvent e) { updateExpression(); }
            public void removeUpdate(DocumentEvent e) { updateExpression(); }
        });
        main.add(actionValueField, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        actionUnitCombo = new JComboBox<>(FLOW_UNITS);
        actionUnitCombo.setPreferredSize(new Dimension(70, 26));
        actionUnitCombo.addActionListener(e -> updateExpression());
        main.add(actionUnitCombo, gbc);

        // Separator
        gbc.gridy = 9;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        main.add(createSeparator(), gbc);

        // Add Else If / Else buttons
        gbc.gridy = 10;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JButton addElseIfBtn = createPlainButton("+ Add Else If Branch");
        addElseIfBtn.addActionListener(e -> addElseIfBranch());
        main.add(addElseIfBtn, gbc);

        gbc.gridx = 1;
        JButton addElseBtn = createPlainButton("+ Add Else Branch");
        addElseBtn.addActionListener(e -> addElseBranch());
        main.add(addElseBtn, gbc);

        return main;
    }

    // ====================================================================
    // Preview & Status Panel
    // ====================================================================

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 6));
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Status label
        statusLabel = new JLabel("  \u2713 Valid   |   Type: double   |   Variables: 1  ");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        statusLabel.setForeground(GREEN_CHECK);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusLabel.setBackground(ACCENT_LIGHT);
        statusLabel.setOpaque(true);
        panel.add(statusLabel, BorderLayout.NORTH);

        // Expression preview
        expressionPreview = new JTextArea(
            "IF(GT([PoolElev], 542.0 ft), 4000 cfs, [no else])");
        expressionPreview.setEditable(false);
        expressionPreview.setFont(new Font("Monospaced", Font.PLAIN, 12));
        expressionPreview.setLineWrap(true);
        expressionPreview.setWrapStyleWord(true);
        expressionPreview.setBackground(DARK_BG);
        expressionPreview.setForeground(CODE_FUNC);
        expressionPreview.setCaretColor(Color.WHITE);
        expressionPreview.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x44, 0x44, 0x44)),
            new EmptyBorder(8, 10, 8, 10)));
        expressionPreview.setPreferredSize(new Dimension(0, 60));

        JScrollPane previewScroll = new JScrollPane(expressionPreview);
        previewScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Generated Expression",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11)));
        panel.add(previewScroll, BorderLayout.CENTER);

        // Action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        btnPanel.add(createPlainButton("Test"));
        btnPanel.add(createPrimaryButton("Save"));
        btnPanel.add(createPlainButton("Cancel"));

        Component spacer = Box.createHorizontalGlue();
        btnPanel.add(spacer);

        JButton advancedBtn = createOrangeButton("Advanced Editor >");
        advancedBtn.addActionListener(e -> switchToAdvancedEditor());
        btnPanel.add(advancedBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ====================================================================
    // Template Panel (Right Side)
    // ====================================================================

    private JPanel createTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(160, 0));
        panel.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR),
            new EmptyBorder(8, 8, 8, 8)));
        panel.setBackground(new Color(0xFA, 0xFA, 0xFA));

        JLabel header = new JLabel("Templates");
        header.setFont(new Font("Dialog", Font.BOLD, 11));
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(header, BorderLayout.NORTH);

        String[] templates = {
            "Release Limit",
            "Inflow Percentile",
            "Spillway Equation",
            "Seasonal Schedule",
            "Min. Env. Flow"
        };

        templateList = new JList<>(templates);
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.setSelectedIndex(0);
        templateList.setFont(new Font("Dialog", Font.PLAIN, 11));
        templateList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadTemplate(templateList.getSelectedValue());
            }
        });

        panel.add(templateList, BorderLayout.CENTER);
        return panel;
    }

    // ====================================================================
    // Condition Row Management
    // ====================================================================

    private void addConditionRow() {
        ConditionRow row = new ConditionRow();
        conditions.add(row);
        conditionsPanel.add(row.getPanel());
        conditionsPanel.revalidate();
        conditionsPanel.repaint();
    }

    private void addElseIfBranch() {
        ElseIfBranch branch = new ElseIfBranch();
        elseIfBranches.add(branch);
        // In production, would add a collapsible panel for the branch
        updateExpression();
        JOptionPane.showMessageDialog(this,
            "ELSE IF branch added. In the full implementation, this would " +
            "display a collapsible panel with its own condition and action.",
            "Else If Added", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addElseBranch() {
        hasElseBranch = true;
        updateExpression();
        JOptionPane.showMessageDialog(this,
            "ELSE branch added. The expression will include a default else clause.",
            "Else Added", JOptionPane.INFORMATION_MESSAGE);
    }

    // ====================================================================
    // Expression Generation
    // ====================================================================

    /**
     * Rebuilds the expression string from the current form state and
     * updates the preview area and status bar.
     */
    private void updateExpression() {
        try {
            StringBuilder expr = new StringBuilder();
            ArrayList<String> openParens = new ArrayList<>();

            // Build the IF condition(s)
            expr.append("IF(");
            openParens.add("IF");

            if (!conditions.isEmpty()) {
                ConditionRow first = conditions.get(0);
                expr.append(comparatorToFunction(first.getComparator()))
                    .append("([")
                    .append(first.getVariable())
                    .append("], ")
                    .append(first.getValue())
                    .append(" ")
                    .append(first.getUnit())
                    .append(")");

                // Additional conditions are chained with AND/OR
                for (int i = 1; i < conditions.size(); i++) {
                    ConditionRow cond = conditions.get(i);
                    String logic = "AND"; // Would read from logic dropdown
                    expr.append(", ").append(logic).append("(")
                        .append(comparatorToFunction(cond.getComparator()))
                        .append("([")
                        .append(cond.getVariable())
                        .append("], ")
                        .append(cond.getValue())
                        .append(" ")
                        .append(cond.getUnit())
                        .append("))");
                }
            }

            // THEN clause
            expr.append(", ").append(buildActionExpression());

            // ELSE clause
            if (!elseIfBranches.isEmpty()) {
                expr.append(", ").append(buildElseIfExpression());
            } else if (hasElseBranch) {
                expr.append(", [ElseAction]");
            } else {
                expr.append(", [no else]");
            }

            // Close parens
            for (String p : openParens) {
                expr.append(")");
            }

            expressionPreview.setText(expr.toString());

            // Update status
            int varCount = countVariables(expr.toString());
            statusLabel.setText("  \u2713 Valid   |   Type: double   |   Variables: " + varCount);
            statusLabel.setForeground(GREEN_CHECK);

        } catch (Exception ex) {
            statusLabel.setText("  \u2717 Error: " + ex.getMessage());
            statusLabel.setForeground(RED_ERROR);
        }
    }

    private String buildActionExpression() {
        String action = (String) actionTypeCombo.getSelectedItem();
        String value = actionValueField.getText();
        String unit = (String) actionUnitCombo.getSelectedItem();

        switch (action) {
            case "Release as % of Inflow":
                return "MULTIPLY([Inflow], " + value + " / 100)";
            case "Hold Previous Release":
                return "[PreviousRelease]";
            case "Release = Inflow + Delta":
                return "ADD([Inflow], " + value + " " + unit + ")";
            default:
                return value + " " + unit;
        }
    }

    private String buildElseIfExpression() {
        // Simplified placeholder
        return "IF(LT([Inflow], [P50]), 6000 cfs, [Inflow])";
    }

    private String comparatorToFunction(String comparator) {
        return switch (comparator) {
            case "is greater than" -> "GT";
            case "is greater than or equal to" -> "GE";
            case "is less than" -> "LT";
            case "is less than or equal to" -> "LE";
            case "is equal to" -> "EQ";
            case "is not equal to" -> "NE";
            default -> "GT";
        };
    }

    private int countVariables(String expr) {
        int count = 0;
        int idx = 0;
        while ((idx = expr.indexOf("[", idx)) != -1) {
            count++;
            idx++;
        }
        return count;
    }

    // ====================================================================
    // Template Loading
    // ====================================================================

    private void loadTemplate(String templateName) {
        switch (templateName) {
            case "Release Limit" -> {
                ruleTypeCombo.setSelectedItem("Release Limit");
                zoneCombo.setSelectedItem("Normal Flood Control");
                actionTypeCombo.setSelectedItem("Maximum Release");
                actionValueField.setText("4000");
                actionUnitCombo.setSelectedItem("cfs");
            }
            case "Inflow Percentile" -> {
                ruleTypeCombo.setSelectedItem("Release Limit");
                zoneCombo.setSelectedItem("Normal Flood Control");
                actionTypeCombo.setSelectedItem("Maximum Release");
                actionValueField.setText("6000");
                actionUnitCombo.setSelectedItem("cfs");
            }
            case "Spillway Equation" -> {
                ruleTypeCombo.setSelectedItem("Release Limit");
                actionTypeCombo.setSelectedItem("Specified Release");
                actionValueField.setText("3.33");
                actionUnitCombo.setSelectedItem("cfs");
            }
            case "Seasonal Schedule" -> {
                ruleTypeCombo.setSelectedItem("Release Limit");
                actionTypeCombo.setSelectedItem("Maximum Release");
                actionValueField.setText("5000");
                actionUnitCombo.setSelectedItem("cfs");
            }
            case "Min. Env. Flow" -> {
                ruleTypeCombo.setSelectedItem("Minimum Flow");
                actionTypeCombo.setSelectedItem("Minimum Release");
                actionValueField.setText("500");
                actionUnitCombo.setSelectedItem("cfs");
            }
        }
        updateExpression();
    }

    // ====================================================================
    // Mode Switching
    // ====================================================================

    private void switchToAdvancedEditor() {
        String currentExpr = expressionPreview.getText();
        JOptionPane.showMessageDialog(this,
            "Switching to Advanced Card Editor...\n\n" +
            "Current expression will be preserved:\n" +
            currentExpr,
            "Switching Mode",
            JOptionPane.INFORMATION_MESSAGE);
        // In production: fire an event or call a callback to swap views
    }

    // ====================================================================
    // Inner Class: ConditionRow
    // ====================================================================

    /**
     * A single condition row with variable, comparator, value, and unit selectors.
     * Each row is a self-contained JPanel that can be added/removed from the
     * conditions panel.
     */
    private class ConditionRow {
        private final JComboBox<String> variableCombo;
        private final JComboBox<String> comparatorCombo;
        private final JTextField valueField;
        private final JComboBox<String> unitCombo;
        private final JPanel rowPanel;

        public ConditionRow() {
            rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setBackground(Color.WHITE);

            variableCombo = new JComboBox<>(VARIABLES);
            variableCombo.setPreferredSize(new Dimension(140, 24));
            variableCombo.addActionListener(e -> updateExpression());

            comparatorCombo = new JComboBox<>(COMPARATORS);
            comparatorCombo.setPreferredSize(new Dimension(165, 24));
            comparatorCombo.addActionListener(e -> updateExpression());

            valueField = new JTextField("542.0");
            valueField.setPreferredSize(new Dimension(80, 24));
            valueField.setBorder(new LineBorder(HIGHLIGHT, 2));
            valueField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { updateExpression(); }
                public void insertUpdate(DocumentEvent e) { updateExpression(); }
                public void removeUpdate(DocumentEvent e) { updateExpression(); }
            });

            unitCombo = new JComboBox<>(ELEV_UNITS);
            unitCombo.setPreferredSize(new Dimension(80, 24));
            unitCombo.addActionListener(e -> updateExpression());

            // Delete button
            JButton deleteBtn = new JButton("\u00D7");
            deleteBtn.setPreferredSize(new Dimension(24, 24));
            deleteBtn.setBackground(new Color(0xFF, 0xEB, 0xEE));
            deleteBtn.setForeground(RED_ERROR);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setToolTipText("Remove this condition");
            deleteBtn.addActionListener(e -> removeCondition());

            rowPanel.add(variableCombo);
            rowPanel.add(comparatorCombo);
            rowPanel.add(valueField);
            rowPanel.add(unitCombo);
            rowPanel.add(deleteBtn);
        }

        public JPanel getPanel() { return rowPanel; }
        public String getVariable() { return (String) variableCombo.getSelectedItem(); }
        public String getComparator() { return (String) comparatorCombo.getSelectedItem(); }
        public String getValue() { return valueField.getText().trim(); }
        public String getUnit() { return (String) unitCombo.getSelectedItem(); }

        private void removeCondition() {
            conditions.remove(this);
            conditionsPanel.remove(rowPanel);
            conditionsPanel.revalidate();
            conditionsPanel.repaint();
            updateExpression();
        }
    }

    // ====================================================================
    // Inner Class: ElseIfBranch
    // ====================================================================

    /**
     * Represents an ELSE IF branch with its own condition and action.
     */
    private class ElseIfBranch {
        private final ConditionRow condition;
        private final JComboBox<String> actionTypeCombo;
        private final JTextField actionValueField;
        private final JComboBox<String> actionUnitCombo;

        public ElseIfBranch() {
            condition = new ConditionRow();
            actionTypeCombo = new JComboBox<>(ACTION_TYPES);
            actionValueField = new JTextField("6000");
            actionUnitCombo = new JComboBox<>(FLOW_UNITS);
        }

        public String toExpression() {
            return "IF(" + comparatorToFunction(condition.getComparator())
                + "([" + condition.getVariable() + "], "
                + condition.getValue() + " " + condition.getUnit() + "), "
                + actionValueField.getText() + " "
                + actionUnitCombo.getSelectedItem() + ")";
        }
    }

    // ====================================================================
    // UI Helper Methods
    // ====================================================================

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 11));
        label.setForeground(TEXT_DARK);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 13));
        label.setForeground(TEXT_DARK);
        return label;
    }

    private JButton createInfoButton(String tooltip) {
        JButton btn = new JButton("?");
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setFont(new Font("Dialog", Font.BOLD, 13));
        btn.setBackground(ACCENT_LIGHT);
        btn.setForeground(HEADER_BG);
        btn.setBorder(new LineBorder(HEADER_BG, 1));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        return btn;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(BORDER_COLOR);
        return sep;
    }

    private JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.BOLD, 11));
        btn.setForeground(HEADER_BG);
        btn.setBackground(ACCENT_LIGHT);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(HEADER_BG, 1));
        return btn;
    }

    private JButton createPlainButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.PLAIN, 11));
        btn.setForeground(TEXT_DARK);
        btn.setBackground(new Color(0xE1, 0xE1, 0xE1));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(BORDER_COLOR, 1));
        return btn;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(80, 32));
        btn.setFont(new Font("Dialog", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(HEADER_BG);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(HEADER_BG, 1));
        return btn;
    }

    private JButton createOrangeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.BOLD, 11));
        btn.setForeground(ORANGE_ACCENT);
        btn.setBackground(ORANGE_LIGHT);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(ORANGE_ACCENT, 1));
        return btn;
    }
}