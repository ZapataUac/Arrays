package minesweeper;

public enum Difficulty {
    BEGINNER   ("Principiante",  9,  9,  10),
    INTERMEDIATE("Intermedio",  16, 16,  40),
    EXPERT     ("Experto",      16, 30,  99),
    CUSTOM     ("Personalizado", 0,  0,   0);

    public final String label;
    public final int rows;
    public final int cols;
    public final int mines;

    Difficulty(String label, int rows, int cols, int mines) {
        this.label = label;
        this.rows  = rows;
        this.cols  = cols;
        this.mines = mines;
    }

    @Override public String toString() { return label; }
}
