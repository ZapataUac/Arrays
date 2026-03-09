package minesweeper;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class GameWindow extends JFrame {

    // ── Game state ────────────────────────────────────────────────────────────
    private GameModel       model;
    private Difficulty      difficulty = Difficulty.BEGINNER;
    private int             customRows = 16, customCols = 16, customMines = 40;

    // ── UI components ─────────────────────────────────────────────────────────
    private BoardPanel          board;
    private SmileyButton        smiley;
    private SevenSegmentDisplay mineCounter;
    private SevenSegmentDisplay timerDisplay;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timer  gameTimer;
    private int    elapsedSeconds = 0;
    private boolean timerRunning  = false;

    // ── Colors (classic Win95 gray) ───────────────────────────────────────────
    private static final Color WIN95_BG = new Color(192, 192, 192);

    public GameWindow() {
        setTitle("Buscaminas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(WIN95_BG);

        buildMenu();
        buildUI();
        newGame(difficulty);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Menu ──────────────────────────────────────────────────────────────────
    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(WIN95_BG);

        JMenu gameMenu = new JMenu("Juego");
        gameMenu.setBackground(WIN95_BG);

        JMenuItem newItem = new JMenuItem("Nuevo juego  (F2)");
        newItem.addActionListener(e -> newGame(difficulty));
        gameMenu.add(newItem);
        gameMenu.addSeparator();

        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : new Difficulty[]{
                Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.EXPERT}) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    d.label + "  (" + d.cols + "x" + d.rows + ", " + d.mines + " minas)");
            item.setBackground(WIN95_BG);
            if (d == difficulty) item.setSelected(true);
            group.add(item);
            gameMenu.add(item);
            item.addActionListener(e -> {
                difficulty = d;
                newGame(d);
            });
        }

        JRadioButtonMenuItem customItem = new JRadioButtonMenuItem("Personalizado…");
        customItem.setBackground(WIN95_BG);
        group.add(customItem);
        gameMenu.add(customItem);
        customItem.addActionListener(e -> showCustomDialog());

        gameMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> System.exit(0));
        gameMenu.add(exitItem);

        bar.add(gameMenu);

        JMenu helpMenu = new JMenu("Ayuda");
        JMenuItem aboutItem = new JMenuItem("Acerca de…");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Buscaminas en Java\n\n" +
                "Click izquierdo → revelar celda\n" +
                "Click derecho  → marcar bandera / signo de interrogación\n" +
                "Click en número revelado → autocompletar vecinos\n" +
                "F2 → nuevo juego", "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        bar.add(helpMenu);

        setJMenuBar(bar);

        // F2 = new game
        getRootPane().registerKeyboardAction(
                e -> newGame(difficulty),
                KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ── UI Layout ─────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(WIN95_BG);
        outer.setBorder(createRaisedBorder(6));

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new GridBagLayout());
        topBar.setBackground(WIN95_BG);
        topBar.setBorder(createSunkenBorder(3));
        topBar.setPreferredSize(new Dimension(10, 56));

        mineCounter  = new SevenSegmentDisplay();
        timerDisplay = new SevenSegmentDisplay();

        smiley = new SmileyButton();
        smiley.addActionListener(e -> newGame(difficulty));
        smiley.setToolTipText("Nuevo juego (F2)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        topBar.add(mineCounter, gbc);

        gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.insets = new Insets(4, 0, 4, 0);
        topBar.add(smiley, gbc);

        gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(4, 4, 4, 8);
        topBar.add(timerDisplay, gbc);

        outer.add(topBar, BorderLayout.NORTH);

        // ── Board area ───────────────────────────────────────────────────────
        board = new BoardPanel();
        board.setOnReveal((r, c) -> handleReveal(r, c));
        board.setOnFlag((r, c)   -> handleFlag(r, c));
        board.setOnChord((r, c)  -> handleChord(r, c));
        board.setOnMousePress(()  -> { if (model.getGameState() == GameModel.GameState.PLAYING
                || model.getGameState() == GameModel.GameState.IDLE)
                    smiley.setFace(SmileyButton.Face.WORRIED); });
        board.setOnMouseRelease(() -> {
            GameModel.GameState s = model.getGameState();
            if (s == GameModel.GameState.WON)  smiley.setFace(SmileyButton.Face.COOL);
            else if (s == GameModel.GameState.LOST) smiley.setFace(SmileyButton.Face.DEAD);
            else smiley.setFace(SmileyButton.Face.NORMAL);
        });

        JPanel boardWrapper = new JPanel(new BorderLayout());
        boardWrapper.setBackground(WIN95_BG);
        boardWrapper.setBorder(createSunkenBorder(3));
        boardWrapper.add(board, BorderLayout.CENTER);
        outer.add(boardWrapper, BorderLayout.CENTER);

        // Outer padding
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WIN95_BG);
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        root.add(outer);
        setContentPane(root);

        // ── Game Timer ───────────────────────────────────────────────────────
        gameTimer = new Timer(1000, e -> {
            elapsedSeconds++;
            timerDisplay.setValue(Math.min(999, elapsedSeconds));
        });
    }

    // ── New game ──────────────────────────────────────────────────────────────
    private void newGame(Difficulty d) {
        gameTimer.stop();
        elapsedSeconds = 0;
        timerRunning   = false;
        timerDisplay.setValue(0);

        int rows, cols, mines;
        if (d == Difficulty.CUSTOM) {
            rows  = customRows;
            cols  = customCols;
            mines = customMines;
        } else {
            rows  = d.rows;
            cols  = d.cols;
            mines = d.mines;
        }

        model = new GameModel(rows, cols, mines);
        mineCounter.setValue(mines);
        smiley.setFace(SmileyButton.Face.NORMAL);
        board.setModel(model);

        pack();
        setLocationRelativeTo(null);
    }

    // ── Game actions ──────────────────────────────────────────────────────────
    private void handleReveal(int r, int c) {
        if (model.getGameState() == GameModel.GameState.LOST ||
            model.getGameState() == GameModel.GameState.WON) return;

        boolean wasIdle = model.isFirstClick();
        model.reveal(r, c);

        // Start timer on first click
        if (wasIdle && !model.isFirstClick() && !timerRunning) {
            timerRunning = true;
            gameTimer.start();
        }

        mineCounter.setValue(model.getMinesRemaining());
        board.repaint();
        checkEndGame(r, c);
    }

    private void handleFlag(int r, int c) {
        if (model.getGameState() == GameModel.GameState.LOST ||
            model.getGameState() == GameModel.GameState.WON) return;
        model.cycleFlag(r, c);
        mineCounter.setValue(model.getMinesRemaining());
        board.repaint();
    }

    private void handleChord(int r, int c) {
        if (model.getGameState() == GameModel.GameState.LOST ||
            model.getGameState() == GameModel.GameState.WON) return;
        model.chord(r, c);
        mineCounter.setValue(model.getMinesRemaining());
        board.repaint();
        checkEndGame(r, c);
    }

    private void checkEndGame(int lastR, int lastC) {
        GameModel.GameState state = model.getGameState();

        if (state == GameModel.GameState.LOST) {
            gameTimer.stop();
            board.setExplodedCell(lastR, lastC);
            smiley.setFace(SmileyButton.Face.DEAD);
            board.repaint();
            // slight delay before showing dialog
            Timer delay = new Timer(300, e -> showLostDialog());
            delay.setRepeats(false);
            delay.start();
        } else if (state == GameModel.GameState.WON) {
            gameTimer.stop();
            smiley.setFace(SmileyButton.Face.COOL);
            // Flag all remaining mines automatically
            for (int r = 0; r < model.getRows(); r++)
                for (int c = 0; c < model.getCols(); c++)
                    if (model.isMine(r, c) &&
                        model.getCellState(r, c) != GameModel.CellState.FLAGGED)
                        model.cycleFlag(r, c);
            mineCounter.setValue(0);
            board.repaint();
            Timer delay = new Timer(400, e -> showWonDialog());
            delay.setRepeats(false);
            delay.start();
        }
    }

    private void showLostDialog() {
        int choice = JOptionPane.showConfirmDialog(this,
                "💥 ¡Pisaste una mina!\nTiempo: " + elapsedSeconds + " segundos\n\n¿Jugar de nuevo?",
                "¡Boom!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) newGame(difficulty);
    }

    private void showWonDialog() {
        JOptionPane.showMessageDialog(this,
                "😎 ¡Ganaste!\nTiempo: " + elapsedSeconds + " segundos\n" +
                "Minas marcadas: " + model.getTotalMines(),
                "¡Victoria!", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Custom dialog ─────────────────────────────────────────────────────────
    private void showCustomDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JSpinner rowSpin  = new JSpinner(new SpinnerNumberModel(customRows,  5, 30, 1));
        JSpinner colSpin  = new JSpinner(new SpinnerNumberModel(customCols,  5, 50, 1));
        JSpinner mineSpin = new JSpinner(new SpinnerNumberModel(customMines, 1, 800, 1));
        panel.add(new JLabel("Filas (5–30):"));   panel.add(rowSpin);
        panel.add(new JLabel("Columnas (5–50):")); panel.add(colSpin);
        panel.add(new JLabel("Minas:"));           panel.add(mineSpin);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Tablero personalizado", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            customRows  = (int) rowSpin.getValue();
            customCols  = (int) colSpin.getValue();
            customMines = Math.min((int) mineSpin.getValue(),
                                   customRows * customCols - 9);
            difficulty = Difficulty.CUSTOM;
            newGame(Difficulty.CUSTOM);
        }
    }

    // ── Border helpers ────────────────────────────────────────────────────────
    private Border createRaisedBorder(int thickness) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(thickness, thickness, 0, 0, Color.WHITE),
            BorderFactory.createMatteBorder(0, 0, thickness, thickness, new Color(128,128,128))
        );
    }

    private Border createSunkenBorder(int thickness) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(thickness, thickness, 0, 0, new Color(128,128,128)),
            BorderFactory.createMatteBorder(0, 0, thickness, thickness, Color.WHITE)
        );
    }
}
