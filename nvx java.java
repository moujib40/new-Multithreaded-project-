
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FactorialCalculatorApp extends JFrame {

    private JTextField numberField;
    private JTextField threadsField;
    private JTextArea outputArea;
    private JButton calculateButton;

    public FactorialCalculatorApp() {
        setTitle("Multithreaded Factorial Calculator");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());

        inputPanel.add(new JLabel("Number:"));
        numberField = new JTextField(10);
        inputPanel.add(numberField);

        inputPanel.add(new JLabel("Threads:"));
        threadsField = new JTextField(5);
        inputPanel.add(threadsField);

        calculateButton = new JButton("Calculate");
        inputPanel.add(calculateButton);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateFactorial();
            }
        });
    }

    private void calculateFactorial() {
        String numStr = numberField.getText().trim();
        String threadsStr = threadsField.getText().trim();

        if (numStr.isEmpty() || threadsStr.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter both number and threads.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int n;
        int numThreads;

        try {
            n = Integer.parseInt(numStr);
            numThreads = Integer.parseInt(threadsStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid integers.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (n < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Factorial is not defined for negative numbers.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (numThreads <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Number of threads must be at least 1.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        calculateButton.setEnabled(false);
        outputArea.setText("Calculating...\n");

        SwingWorker<BigInteger, String> worker
                = new SwingWorker<BigInteger, String>() {

            @Override
            protected BigInteger doInBackground() throws Exception {

                long totalStartTime = System.nanoTime();

                if (n == 0 || n == 1) {

                    publish(n + "! = 1\n");

                    long totalEndTime = System.nanoTime();

                    double totalTime
                            = (totalEndTime - totalStartTime) / 1_000_000.0;

                    publish("\n========================================\n");
                    publish("Total Execution Time: "
                            + String.format("%.4f", totalTime)
                            + " ms\n");
                    publish("========================================\n");

                    return BigInteger.ONE;
                }

                StringBuilder formulation = new StringBuilder();

                if (n <= 50) {
                    for (int i = 1; i <= n; i++) {
                        formulation.append(i)
                                .append(i < n ? " × " : "");
                    }
                } else {
                    formulation.append("1 × 2 × ... × ")
                            .append(n - 1)
                            .append(" × ")
                            .append(n);
                }

                publish("\n" + n + "! = "
                        + formulation.toString() + "\n\n");

                Map<Integer, BigInteger> results
                        = new ConcurrentHashMap<>();

                ExecutorService executor
                        = Executors.newFixedThreadPool(
                                Math.min(numThreads, n)
                        );

                int baseChunkSize = n / numThreads;
                int remainder = n % numThreads;

                int currentStart = 1;

                for (int i = 1; i <= numThreads; i++) {

                    int chunkSize
                            = baseChunkSize
                            + (i <= remainder ? 1 : 0);

                    if (chunkSize == 0) {
                        continue;
                    }

                    int currentEnd
                            = currentStart + chunkSize - 1;

                    final int threadId = i;
                    final int start = currentStart;
                    final int end = currentEnd;

                    executor.submit(() -> {

                        // Start time for this thread
                        long threadStartTime
                                = System.nanoTime();

                        BigInteger result
                                = BigInteger.ONE;

                        for (int j = start; j <= end; j++) {
                            result = result.multiply(
                                    BigInteger.valueOf(j)
                            );
                        }

                        long threadEndTime
                                = System.nanoTime();

                        double threadTime
                                = (threadEndTime - threadStartTime)
                                / 1_000_000.0;

                        String numbers;

                        if (start == end) {
                            numbers = String.valueOf(start);
                        } else {

                            if (end - start > 50) {

                                numbers
                                        = start + " × "
                                        + (start + 1)
                                        + " × ... × "
                                        + (end - 1)
                                        + " × " + end;

                            } else {

                                StringBuilder sb
                                        = new StringBuilder();

                                for (int j = start;
                                        j <= end;
                                        j++) {

                                    sb.append(j)
                                            .append(
                                                    j < end
                                                            ? " × "
                                                            : ""
                                            );
                                }

                                numbers = sb.toString();
                            }
                        }

                        publish(
                                "Thread " + threadId
                                + ": " + numbers + "\n"
                                + "Thread " + threadId
                                + " Calculation Time: "
                                + String.format(
                                        "%.4f",
                                        threadTime
                                )
                                + " ms\n\n"
                        );

                        results.put(threadId, result);
                    });

                    currentStart = currentEnd + 1;
                }

                executor.shutdown();

                executor.awaitTermination(
                        1,
                        TimeUnit.HOURS
                );

                BigInteger finalResult
                        = BigInteger.ONE;

                for (int i = 1;
                        i <= numThreads;
                        i++) {

                    if (results.containsKey(i)) {
                        finalResult
                                = finalResult.multiply(
                                        results.get(i)
                                );
                    }
                }

                long totalEndTime
                        = System.nanoTime();

                double totalTime
                        = (totalEndTime - totalStartTime)
                        / 1_000_000.0;

                publish(
                        "========================================\n"
                );

                publish(
                        "TOTAL EXECUTION TIME: "
                        + String.format(
                                "%.4f",
                                totalTime
                        )
                        + " ms\n"
                );

                publish(
                        "========================================\n"
                );

                return finalResult;
            }

            @Override
            protected void process(
                    java.util.List<String> chunks) {

                for (String chunk : chunks) {
                    outputArea.append(chunk);
                }
            }

            @Override
            protected void done() {

                try {

                    BigInteger result = get();

                    outputArea.append(
                            "\nFinal Result: "
                            + result.toString()
                            + "\n"
                    );

                } catch (Exception ex) {

                    outputArea.append(
                            "\nError calculating factorial: "
                            + ex.getMessage()
                            + "\n"
                    );

                } finally {

                    calculateButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            FactorialCalculatorApp app
                    = new FactorialCalculatorApp();

            app.setVisible(true);
        });
    }
}
