package ca.weblite.cn1libDoctor;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main extends JFrame {

    private JButton selectFileButton, inspectButton;
    private JLabel fileLabel;
    private JTextArea resultsArea;
    private File selectedFile;

    public Main() {
        setTitle("Cn1lib Doctor - 16KB Page Size Checker");

        // Create components
        selectFileButton = new JButton("Select cn1lib file...");
        fileLabel = new JLabel("No file selected");
        inspectButton = new JButton("Inspect");
        inspectButton.setEnabled(false);
        resultsArea = new JTextArea(20, 60);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // File selection
        selectFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".cn1lib");
                }
                @Override
                public String getDescription() {
                    return "Codename One Libraries (*.cn1lib)";
                }
            });

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                fileLabel.setText(selectedFile.getName());
                inspectButton.setEnabled(true);
                resultsArea.setText("");
            }
        });

        // Inspect action
        inspectButton.addActionListener(e -> {
            inspectButton.setEnabled(false);
            resultsArea.setText("Analyzing " + selectedFile.getName() + "...\n");

            SwingWorker<Cn1libAnalyzer.AnalysisResult, Void> worker = new SwingWorker<>() {
                @Override
                protected Cn1libAnalyzer.AnalysisResult doInBackground() throws Exception {
                    return Cn1libAnalyzer.analyze(selectedFile);
                }

                @Override
                protected void done() {
                    try {
                        Cn1libAnalyzer.AnalysisResult result = get();
                        displayResults(result);
                    } catch (Exception ex) {
                        resultsArea.setText("Error: " + ex.getMessage());
                        ex.printStackTrace();
                    } finally {
                        inspectButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        // Layout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(selectFileButton);
        topPanel.add(fileLabel);
        topPanel.add(inspectButton);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultsArea), BorderLayout.CENTER);

        // Add padding
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void displayResults(Cn1libAnalyzer.AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 16KB Page Size Analysis Results ===\n\n");

        if (!result.hasAnyLibraries()) {
            sb.append("✓ No Android native libraries (.so files) found.\n");
            sb.append("  This cn1lib is compatible with 16KB page sizes.\n");
        } else {
            sb.append(String.format("Found %d native libraries:\n\n", result.libraries.size()));

            for (Cn1libAnalyzer.NativeLibraryResult lib : result.libraries) {
                String status = lib.supports16KB ? "✓ SUPPORTED" : "✗ NOT SUPPORTED";
                sb.append(String.format("%s\n", status));
                sb.append(String.format("  Path: %s\n", lib.path));
                sb.append(String.format("  Alignment: 0x%x (%d bytes)\n", lib.alignment, lib.alignment));
                sb.append("\n");
            }

            sb.append("\n=== Summary ===\n");
            if (result.hasUnsupportedLibraries()) {
                sb.append("✗ This cn1lib has libraries that DO NOT support 16KB page sizes.\n");
                sb.append("  These libraries need to be recompiled with proper alignment.\n");
            } else {
                sb.append("✓ All libraries support 16KB page sizes!\n");
            }
        }

        if (!result.errors.isEmpty()) {
            sb.append("\n=== Errors ===\n");
            for (String error : result.errors) {
                sb.append("• " + error + "\n");
            }
        }

        resultsArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main frame = new Main();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
