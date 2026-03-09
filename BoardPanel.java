package minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.function.BiConsumer;

public class BoardPanel extends JPanel {

    private static final int CELL = 28;

    // Classic Windows 95 Minesweeper color palette
    private static final Color BG         = new Color(192, 192, 192);
    private static final Color DARK_GRAY  = new Color(128, 128, 128);
    private static final Color LIGHT      = new Color(255, 255, 255);
    private static final Color MINE_BG    = new Color(255,   0,   0);
    private static final Color MISS_BG    = new Color(255,   0,   0);
    private static final Color REVEALED   = new Color(189, 189, 189);

    private static final Color[] NUM_COLORS = {
        null,
        new Color(0,   0, 255),   // 1 – blue
        new Color(0, 128,   0),   // 2 – green
        new Color(255,   0,   0), // 3 – red
        new Color(0,   0, 128),   // 4 – dark blue
        new Color(128,   0,   0), // 5 – dark red
        new Color(0, 128, 128),   // 6 – teal
        new Color(0,   0,   0),   // 7 – black
        new Color(128, 128, 128)  // 8 – gray
    };

    private GameModel model;

    // Callbacks from board to window
    private BiConsumer<Integer,Integer> onReveal;
    private BiConsumer<Integer,Integer> onFlag;
    private BiConsumer<Integer,Integer> onChord;
    private Runnable onFirstClick;

    // For the "pressed" visual on mouse-down
    private int pressR = -1, pressC = -1;
    // For the "worried face" while mouse is held
    private boolean mouseHeld = false;
    private Runnable onMousePress;
    private Runnable onMouseRelease;

    // Exploded mine cell
    private int explodeR = -1, explodeC = -1;

    public BoardPanel() {
        setBackground(BG);
        setupMouse();
    }

    public void setModel(GameModel m) {
        this.model   = m;
        this.explodeR = -1;
        this.explodeC = -1;
        pressR = -1; pressC = -1;
        int w = m.getCols() * CELL;
        int h = m.getRows() * CELL;
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }

    public void setExplodedCell(int r, int c) { explodeR = r; explodeC = c; }

    public void setOnReveal(BiConsumer<Integer,Integer> cb)  { this.onReveal = cb; }
    public void setOnFlag(BiConsumer<Integer,Integer> cb)    { this.onFlag   = cb; }
    public void setOnChord(BiConsumer<Integer,Integer> cb)   { this.onChord  = cb; }
    public void setOnMousePress(Runnable cb)                 { this.onMousePress   = cb; }
    public void setOnMouseRelease(Runnable cb)               { this.onMouseRelease = cb; }

    private void setupMouse() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (model == null) return;
                int[] rc = cellAt(e.getX(), e.getY());
                if (rc == null) return;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    pressR = rc[0]; pressC = rc[1];
                    mouseHeld = true;
                    if (onMousePress != null) onMousePress.run();
                    repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (model == null) return;
                mouseHeld = false;
                if (onMouseRelease != null) onMouseRelease.run();
                int[] rc = cellAt(e.getX(), e.getY());
                pressR = -1; pressC = -1;
                repaint();
                if (rc == null) return;
                int r = rc[0], c = rc[1];
                if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown()) {
                    // chord if already revealed, else reveal
                    if (model.getCellState(r, c) == GameModel.CellState.REVEALED) {
                        if (onChord != null) onChord.accept(r, c);
                    } else {
                        if (onReveal != null) onReveal.accept(r, c);
                    }
                } else if (SwingUtilities.isRightMouseButton(e) || e.isControlDown()) {
                    if (onFlag != null) onFlag.accept(r, c);
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                if (mouseHeld) {
                    mouseHeld = false;
                    if (onMouseRelease != null) onMouseRelease.run();
                    pressR = -1; pressC = -1;
                    repaint();
                }
            }
        });
    }

    private int[] cellAt(int px, int py) {
        if (model == null) return null;
        int r = py / CELL, c = px / CELL;
        if (r < 0 || r >= model.getRows() || c < 0 || c >= model.getCols()) return null;
        return new int[]{r, c};
    }

    public boolean isMouseHeld() { return mouseHeld; }

    // ── Painting ──────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (model == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int r = 0; r < model.getRows(); r++)
            for (int c = 0; c < model.getCols(); c++)
                drawCell(g2, r, c);
    }

    private void drawCell(Graphics2D g, int r, int c) {
        int x = c * CELL, y = r * CELL;
        GameModel.CellState state = model.getCellState(r, c);
        GameModel.GameState gstate = model.getGameState();

        boolean isPressed = (r == pressR && c == pressC && mouseHeld
                             && state == GameModel.CellState.HIDDEN);

        // ── Background ──────────────────────────────────────────────────────
        if (state == GameModel.CellState.REVEALED) {
            g.setColor(REVEALED);
            g.fillRect(x, y, CELL, CELL);
            drawInsetBorder(g, x, y, CELL);
        } else if (isPressed) {
            g.setColor(REVEALED);
            g.fillRect(x, y, CELL, CELL);
            drawInsetBorder(g, x, y, CELL);
        } else {
            // Raised 3-D button
            g.setColor(BG);
            g.fillRect(x, y, CELL, CELL);
            drawRaisedBorder(g, x, y, CELL);
        }

        // ── Content ─────────────────────────────────────────────────────────
        if (state == GameModel.CellState.REVEALED) {
            if (model.isMine(r, c)) {
                // Exploded mine – red background
                boolean isExplodedCell = (r == explodeR && c == explodeC);
                if (isExplodedCell) {
                    g.setColor(MINE_BG);
                    g.fillRect(x + 2, y + 2, CELL - 4, CELL - 4);
                }
                drawMine(g, x, y, isExplodedCell);
            } else {
                int adj = model.getAdjacent(r, c);
                if (adj > 0) drawNumber(g, x, y, adj);
            }
        } else if (state == GameModel.CellState.FLAGGED) {
            drawFlag(g, x, y);
        } else if (state == GameModel.CellState.QUESTION) {
            drawQuestion(g, x, y);
        }

        // In LOST state: reveal all mines and show wrong flags
        if (gstate == GameModel.GameState.LOST) {
            if (state == GameModel.CellState.HIDDEN && model.isMine(r, c)) {
                g.setColor(BG);
                g.fillRect(x, y, CELL, CELL);
                drawRaisedBorder(g, x, y, CELL);
                drawMine(g, x, y, false);
            }
            if (state == GameModel.CellState.FLAGGED && !model.isMine(r, c)) {
                // Wrong flag – draw X over flag
                g.setColor(BG);
                g.fillRect(x, y, CELL, CELL);
                drawRaisedBorder(g, x, y, CELL);
                drawFlag(g, x, y);
                drawWrongFlag(g, x, y);
            }
        }
    }

    // ── 3-D border helpers ────────────────────────────────────────────────────
    private void drawRaisedBorder(Graphics2D g, int x, int y, int size) {
        int s = size;
        // top & left – white (highlight)
        g.setColor(LIGHT);
        g.drawLine(x,       y,       x+s-2, y      ); // top
        g.drawLine(x,       y,       x,     y+s-2  ); // left
        g.setColor(LIGHT);
        g.drawLine(x+1,     y+1,     x+s-3, y+1    );
        g.drawLine(x+1,     y+1,     x+1,   y+s-3  );
        // bottom & right – dark gray (shadow)
        g.setColor(DARK_GRAY);
        g.drawLine(x+1,     y+s-1,   x+s-1, y+s-1  ); // bottom
        g.drawLine(x+s-1,   y+1,     x+s-1, y+s-1  ); // right
        g.setColor(Color.BLACK);
        g.drawLine(x,       y+s-1,   x+s-1, y+s-1  );
        g.drawLine(x+s-1,   y,       x+s-1, y+s-1  );
    }

    private void drawInsetBorder(Graphics2D g, int x, int y, int size) {
        int s = size;
        g.setColor(DARK_GRAY);
        g.drawLine(x,       y,       x+s-2, y      );
        g.drawLine(x,       y,       x,     y+s-2  );
        g.setColor(LIGHT);
        g.drawLine(x+1,     y+s-1,   x+s-1, y+s-1  );
        g.drawLine(x+s-1,   y+1,     x+s-1, y+s-1  );
    }

    // ── Draw elements ─────────────────────────────────────────────────────────
    private void drawNumber(Graphics2D g, int x, int y, int n) {
        g.setColor(NUM_COLORS[Math.min(n, 8)]);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String s = String.valueOf(n);
        int tx = x + (CELL - fm.stringWidth(s)) / 2;
        int ty = y + (CELL + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(s, tx, ty);
    }

    private void drawMine(Graphics2D g, int x, int y, boolean exploded) {
        int cx = x + CELL / 2, cy = y + CELL / 2;
        int r  = 5;
        // spikes
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4;
            int x1 = (int)(cx + (r) * Math.cos(angle));
            int y1 = (int)(cy + (r) * Math.sin(angle));
            int x2 = (int)(cx + (r + 4) * Math.cos(angle));
            int y2 = (int)(cy + (r + 4) * Math.sin(angle));
            g.drawLine(x1, y1, x2, y2);
        }
        g.setStroke(new BasicStroke(1));
        // body
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        // shine
        g.setColor(Color.WHITE);
        g.fillOval(cx - r + 2, cy - r + 2, 3, 3);
    }

    private void drawFlag(Graphics2D g, int x, int y) {
        int px = x + 7, py = y + 5;
        // pole
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawLine(px, py, px, py + 15);
        g.setStroke(new BasicStroke(1));
        // flag triangle
        int[] fx = {px, px + 10, px};
        int[] fy = {py, py + 5,  py + 10};
        g.setColor(Color.RED);
        g.fillPolygon(fx, fy, 3);
        // base
        g.setColor(Color.BLACK);
        g.fillRect(px - 4, py + 15, 10, 2);
    }

    private void drawQuestion(Graphics2D g, int x, int y) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        String s = "?";
        int tx = x + (CELL - fm.stringWidth(s)) / 2;
        int ty = y + (CELL + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(s, tx, ty);
    }

    private void drawWrongFlag(Graphics2D g, int x, int y) {
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(x + 5,  y + 5,  x + CELL - 5, y + CELL - 5);
        g.drawLine(x + CELL - 5, y + 5, x + 5, y + CELL - 5);
        g.setStroke(new BasicStroke(1));
    }
}
