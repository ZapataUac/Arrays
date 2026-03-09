package minesweeper;

import java.util.Random;

public class GameModel {

    public enum CellState { HIDDEN, REVEALED, FLAGGED, QUESTION }
    public enum GameState  { IDLE, PLAYING, WON, LOST }

    private final int rows;
    private final int cols;
    private final int totalMines;

    private boolean[][] mines;
    private int[][]     adjacent;   // count of adjacent mines
    private CellState[][] cellState;

    private GameState gameState = GameState.IDLE;
    private int flagsPlaced  = 0;
    private int cellsRevealed = 0;
    private boolean firstClick = true;

    public GameModel(int rows, int cols, int totalMines) {
        this.rows       = rows;
        this.cols       = cols;
        this.totalMines = totalMines;
        init();
    }

    private void init() {
        mines     = new boolean[rows][cols];
        adjacent  = new int[rows][cols];
        cellState = new CellState[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                cellState[r][c] = CellState.HIDDEN;
        flagsPlaced   = 0;
        cellsRevealed = 0;
        firstClick    = true;
        gameState     = GameState.IDLE;
    }

    // Place mines AFTER first click so first click is never a mine
    private void placeMines(int safeR, int safeC) {
        Random rng = new Random();
        int placed = 0;
        while (placed < totalMines) {
            int r = rng.nextInt(rows);
            int c = rng.nextInt(cols);
            if (!mines[r][c] && !(r == safeR && c == safeC)) {
                mines[r][c] = true;
                placed++;
            }
        }
        computeAdjacent();
    }

    private void computeAdjacent() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (mines[r][c]) { adjacent[r][c] = -1; continue; }
                int count = 0;
                for (int[] d : neighbors(r, c))
                    if (mines[d[0]][d[1]]) count++;
                adjacent[r][c] = count;
            }
    }

    /** Left-click: reveal cell */
    public void reveal(int r, int c) {
        if (gameState == GameState.WON || gameState == GameState.LOST) return;
        if (cellState[r][c] != CellState.HIDDEN) return;

        if (firstClick) {
            placeMines(r, c);
            gameState  = GameState.PLAYING;
            firstClick = false;
        }

        if (mines[r][c]) {
            cellState[r][c] = CellState.REVEALED;
            gameState = GameState.LOST;
            return;
        }

        floodReveal(r, c);
        checkWin();
    }

    private void floodReveal(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        if (cellState[r][c] != CellState.HIDDEN) return;
        cellState[r][c] = CellState.REVEALED;
        cellsRevealed++;
        if (adjacent[r][c] == 0)
            for (int[] d : neighbors(r, c))
                floodReveal(d[0], d[1]);
    }

    /** Right-click: cycle HIDDEN → FLAGGED → QUESTION → HIDDEN */
    public void cycleFlag(int r, int c) {
        if (gameState == GameState.WON || gameState == GameState.LOST) return;
        switch (cellState[r][c]) {
            case HIDDEN   -> { cellState[r][c] = CellState.FLAGGED;  flagsPlaced++; }
            case FLAGGED  -> { cellState[r][c] = CellState.QUESTION; flagsPlaced--; }
            case QUESTION -> { cellState[r][c] = CellState.HIDDEN; }
            default       -> {} // REVEALED: do nothing
        }
    }

    /** Chord: if revealed cell with N adjacent flags == N, auto-reveal neighbors */
    public void chord(int r, int c) {
        if (gameState == GameState.WON || gameState == GameState.LOST) return;
        if (cellState[r][c] != CellState.REVEALED) return;
        if (adjacent[r][c] <= 0) return;
        int flags = 0;
        for (int[] d : neighbors(r, c))
            if (cellState[d[0]][d[1]] == CellState.FLAGGED) flags++;
        if (flags == adjacent[r][c])
            for (int[] d : neighbors(r, c))
                if (cellState[d[0]][d[1]] == CellState.HIDDEN)
                    reveal(d[0], d[1]);
    }

    private void checkWin() {
        int safeCells = rows * cols - totalMines;
        if (cellsRevealed == safeCells) gameState = GameState.WON;
    }

    private int[][] neighbors(int r, int c) {
        int[][] list = new int[8][2]; int n = 0;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols)
                    list[n++] = new int[]{nr, nc};
            }
        return java.util.Arrays.copyOf(list, n);
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int       getRows()                    { return rows; }
    public int       getCols()                    { return cols; }
    public int       getTotalMines()              { return totalMines; }
    public int       getFlagsPlaced()             { return flagsPlaced; }
    public int       getMinesRemaining()          { return totalMines - flagsPlaced; }
    public GameState getGameState()               { return gameState; }
    public boolean   isMine(int r, int c)         { return mines[r][c]; }
    public int       getAdjacent(int r, int c)    { return adjacent[r][c]; }
    public CellState getCellState(int r, int c)   { return cellState[r][c]; }
    public boolean   isFirstClick()               { return firstClick; }
}
