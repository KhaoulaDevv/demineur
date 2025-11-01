package com.rk.demo;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DemineurApp {

    private static Timer timer;
    private static boolean timerStarted = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int rows = getUserInput("Enter number of rows (m):");
            int cols = getUserInput("Enter number of columns (n):");

            if (rows <= 0 || cols <= 0) {
                JOptionPane.showMessageDialog(null, "Invalid dimensions. Exiting.");
                System.exit(0);
            }

            int maxMines = rows * cols - 1;
            int mines = getUserInputWithMax("Enter number of mines (-1 values):", maxMines);

            if (mines < 0) {
                JOptionPane.showMessageDialog(null, "Invalid number of mines. Exiting.");
                System.exit(0);
            }

            JFrame frame = new JFrame("Minesweeper-like Grid");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JLabel timerLabel = new JLabel("Time: 00:00", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
            frame.add(timerLabel, BorderLayout.NORTH);

            final int[] seconds = {0};
            timer = new Timer(1000, e -> {
                seconds[0]++;
                int m = seconds[0] / 60;
                int s = seconds[0] % 60;
                timerLabel.setText(String.format("Time: %02d:%02d", m, s));
            });

            MinesweeperGridPanel gridPanel = new MinesweeperGridPanel(rows, cols, mines, timer);
            gridPanel.setStartTimerCallback(() -> {
                if (!timerStarted) {
                    timer.start();
                    timerStarted = true;
                }
            });

            frame.add(gridPanel, BorderLayout.CENTER);

            frame.setSize(cols * 50, rows * 50 + 60);
            frame.setMinimumSize(new Dimension(cols * 30, rows * 30 + 60));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static int getUserInput(String message) {
        while (true) {
            String input = JOptionPane.showInputDialog(null, message);
            if (input == null) return -1;
            try {
                int value = Integer.parseInt(input);
                if (value > 0) return value;
                JOptionPane.showMessageDialog(null, "Please enter a positive number.");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number. Try again.");
            }
        }
    }

    private static int getUserInputWithMax(String message, int max) {
        while (true) {
            String input = JOptionPane.showInputDialog(null, message + " (Max " + max + "):");
            if (input == null) return -1;
            try {
                int value = Integer.parseInt(input);
                if (value >= 0 && value <= max) return value;
                JOptionPane.showMessageDialog(null, "Please enter a number between 0 and " + max + ".");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number. Try again.");
            }
        }
    }
}

class MinesweeperGridPanel extends JPanel {
    private final int rows;
    private final int cols;
    private final int minesCount;
    private Runnable startTimerCallback = () -> {};
    private boolean firstClicked = false;
    private int[][] buttonNumbers;
    private SquareButton[][] buttons;
    private Timer gameTimer;

    private boolean gameOver = false;
    private int cellsRevealed = 0;

    public MinesweeperGridPanel(int rows, int cols, int minesCount, Timer timer) {
        this.rows = rows;
        this.cols = cols;
        this.minesCount = minesCount;
        this.gameTimer = timer;
        this.buttonNumbers = new int[rows][cols]; // store numbers after first click
        this.buttons = new SquareButton[rows][cols];

        setLayout(new GridLayout(rows, cols, 2, 2));

        for (int i = 0; i < rows * cols; i++) {
            int r = i / cols;
            int c = i % cols;

            SquareButton btn = new SquareButton("");
            buttons[r][c] = btn;

            btn.setFont(new Font("Arial", Font.BOLD, 16));
            btn.setBackground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (gameOver) return; // no clicks after game over

                    startTimerCallback.run();

                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Right-click only works if button is NOT LIGHT_GRAY (revealed)
                        if (!btn.getBackground().equals(Color.LIGHT_GRAY)) {
                            if (btn.getBackground().equals(Color.BLACK)) {
                                btn.setBackground(Color.WHITE);
                                btn.setText("");
                            } else {
                                btn.setBackground(Color.BLACK);
                                btn.setText("");
                            }
                        }
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        // Left-click only if button is white (unrevealed) and not orange flagged
                        if (btn.getBackground().equals(Color.WHITE)) {
                            revealCell(r, c);
                        }
                    }
                }
            });

            add(btn);
        }
    }

    private void revealCell(int r, int c) {
        SquareButton btn = buttons[r][c];

        if (!firstClicked) {
            firstClicked = true;
            // First clicked cell is 0 and place mines randomly elsewhere
            buttonNumbers[r][c] = 0;
            placeMinesRandomly(r, c);
            calculateNumbers();
            gameTimer.start();
        }

        if (btn.getBackground().equals(Color.LIGHT_GRAY) || btn.getBackground().equals(Color.BLACK)) return; // already revealed or flagged

        btn.setBackground(Color.LIGHT_GRAY);
        cellsRevealed++;

        if (buttonNumbers[r][c] == -1) {
            // Clicked on a mine - game over
            btn.setText("X");
            btn.setForeground(Color.BLACK);
            gameOver();
        } else {
            int num = buttonNumbers[r][c];
            btn.setText(num == 0 ? "" : String.valueOf(num));
            btn.setForeground(num == 0 ? Color.LIGHT_GRAY : Color.BLUE);

            if (num == 0) {
                // Reveal all adjacent cells recursively if zero
                revealAdjacentZeros(r, c);
            }
            checkWin();
        }
    }

    private void revealAdjacentZeros(int r, int c) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr;
                int nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    SquareButton btn = buttons[nr][nc];
                    if (btn.getBackground().equals(Color.WHITE) && !btn.getBackground().equals(Color.BLACK)) {
                        btn.setBackground(Color.LIGHT_GRAY);
                        cellsRevealed++;
                        int val = buttonNumbers[nr][nc];
                        btn.setText(val == 0 ? "" : String.valueOf(val));
                        btn.setForeground(val == 0 ? Color.LIGHT_GRAY : Color.BLUE);
                        if (val == 0) {
                            revealAdjacentZeros(nr, nc);
                        }
                    }
                }
            }
        }
    }

    private void gameOver() {
        gameOver = true;
        gameTimer.stop();

        // Reveal all mines
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (buttonNumbers[r][c] == -1) {
                    SquareButton btn = buttons[r][c];
                    btn.setBackground(Color.LIGHT_GRAY);
                    btn.setText("X");
                    btn.setForeground(Color.BLACK);
                }
            }
        }

        JOptionPane.showMessageDialog(this, "Game Over! You clicked on a mine.");
    }

    private void checkWin() {
        // Win if all non-mine cells are revealed
        int totalCells = rows * cols;
        int nonMineCells = totalCells - minesCount;
        if (cellsRevealed == nonMineCells) {
            gameOver = true;
            gameTimer.stop();
            JOptionPane.showMessageDialog(this, "Congratulations! You won!");
        }
    }

    private void placeMinesRandomly(int firstClickRow, int firstClickCol) {
        Random rand = new Random();
        java.util.List<Point> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!(r == firstClickRow && c == firstClickCol)) {
                    candidates.add(new Point(r, c));
                }
            }
        }
        Collections.shuffle(candidates, rand);

        for (int i = 0; i < minesCount; i++) {
            Point p = candidates.get(i);
            buttonNumbers[p.x][p.y] = -1; // Place mine (-1)
        }
    }

    private void calculateNumbers() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (buttonNumbers[r][c] == -1) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && buttonNumbers[nr][nc] == -1) {
                            count++;
                        }
                    }
                }
                buttonNumbers[r][c] = count;
            }
        }
    }

    public void setStartTimerCallback(Runnable callback) {
        this.startTimerCallback = callback;
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();

        int buttonWidth = w / cols;
        int buttonHeight = h / rows;
        int size = Math.min(buttonWidth, buttonHeight);

        for (Component comp : getComponents()) {
            comp.setSize(size, size);
        }

        super.doLayout();
    }

    @Override
    public Dimension getPreferredSize() {
        int buttonSize = 50;
        int width = cols * buttonSize + (cols - 1) * 2;
        int height = rows * buttonSize + (rows - 1) * 2;
        return new Dimension(width, height);
    }
}

class SquareButton extends JButton {
    public SquareButton(String text) {
        super(text);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int size = Math.min(d.width, d.height);
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
