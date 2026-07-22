package expression.builder;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.UIManager;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not load system Look and Feel: " + e.getMessage());
        }

        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("HEC Expression Builder - Wizard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ExpressionWizardPanel wizardPanel = new ExpressionWizardPanel();
        frame.add(wizardPanel);

        frame.setSize(950, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}