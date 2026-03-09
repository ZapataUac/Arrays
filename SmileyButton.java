package minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * The classic smiley face button with 4 states:
 * NORMAL  – smile
 * WORRIED – open-mouth "O" face while mouse button is held
 * DEAD    – X eyes on game-over
 * COOL    – sunglasses on win
 */
public class SmileyButton extends JButton {

    public enum Face { NORMAL, WORRIED, DEAD, COOL }

    private Face face = Face.NORMAL;
    private boolean pressed = false;

    private static final Color FACE_YELLOW = new Color(255, 220,   0);
    private static final Color FACE_DARK   = new Color( 30,  30,  30);
    private static final Color FACE_STROKE = new Color(100, 100,   0);
    private static final Color BLUSH       = new Color(255, 140, 140, 160);
    private static final Color SUNGLASS    = new Color(  0,   0,   0);
    private static final Color SUNGLASS_LENS= new Color( 20, 20, 80, 200);

    public SmileyButton() {
        setPreferredSize(new Dimension(42, 42));
        setMinimumSize(new Dimension(42, 42));
        setMaximumSize(new Dimension(42, 42));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { pressed = true;  repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    public void setFace(Face f) {
        this.face = f;
        repaint();
    }

    public Face getFace() { return face; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int pad = 3;
        int offset = pressed ? 1 : 0;

        // 3-D raised button border
        Color bg = new Color(192, 192, 192);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        if (!pressed) {
            g2.setColor(Color.WHITE);
            g2.drawLine(0, 0, w-2, 0);
            g2.drawLine(0, 0, 0, h-2);
            g2.setColor(new Color(128,128,128));
            g2.drawLine(1, h-1, w-1, h-1);
            g2.drawLine(w-1, 1, w-1, h-1);
            g2.setColor(Color.BLACK);
            g2.drawLine(0, h-1, w-1, h-1);
            g2.drawLine(w-1, 0, w-1, h-1);
        } else {
            g2.setColor(new Color(128,128,128));
            g2.drawLine(0, 0, w-2, 0);
            g2.drawLine(0, 0, 0, h-2);
        }

        // Face circle
        int fx = pad + offset, fy = pad + offset;
        int fw = w - pad * 2 - 1, fh = h - pad * 2 - 1;
        int cx = fx + fw/2, cy = fy + fh/2;
        int fr = Math.min(fw, fh) / 2;

        g2.setColor(FACE_YELLOW);
        g2.fillOval(fx, fy, fw, fh);

        // outline
        g2.setColor(FACE_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(fx, fy, fw, fh);
        g2.setStroke(new BasicStroke(1f));

        switch (face) {
            case NORMAL  -> drawNormal(g2, cx, cy, fr);
            case WORRIED -> drawWorried(g2, cx, cy, fr);
            case DEAD    -> drawDead(g2, cx, cy, fr);
            case COOL    -> drawCool(g2, cx, cy, fr);
        }
    }

    // ── Face expressions ─────────────────────────────────────────────────────
    private void drawNormal(Graphics2D g, int cx, int cy, int r) {
        drawEyes(g, cx, cy, r, false);
        // smile arc
        g.setColor(FACE_DARK);
        g.setStroke(new BasicStroke(2f));
        g.drawArc(cx - r/2, cy, r, r/2, 0, -180);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawWorried(Graphics2D g, int cx, int cy, int r) {
        drawEyes(g, cx, cy, r, false);
        // open mouth "O"
        g.setColor(FACE_DARK);
        g.fillOval(cx - 4, cy + r/3, 8, 8);
    }

    private void drawDead(Graphics2D g, int cx, int cy, int r) {
        int ex = r / 3;
        // X eyes
        g.setColor(FACE_DARK);
        g.setStroke(new BasicStroke(2f));
        // left eye X
        g.drawLine(cx - ex - 3, cy - ex - 3, cx - ex + 3, cy - ex + 3);
        g.drawLine(cx - ex + 3, cy - ex - 3, cx - ex - 3, cy - ex + 3);
        // right eye X
        g.drawLine(cx + ex - 3, cy - ex - 3, cx + ex + 3, cy - ex + 3);
        g.drawLine(cx + ex + 3, cy - ex - 3, cx + ex - 3, cy - ex + 3);
        g.setStroke(new BasicStroke(1f));
        // sad mouth
        g.drawArc(cx - r/2, cy + r/4, r, r/3, 0, 180);
    }

    private void drawCool(Graphics2D g, int cx, int cy, int r) {
        // Sunglasses bar
        int ey = cy - r/4;
        g.setColor(SUNGLASS);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx - r/2, ey, cx + r/2, ey); // bridge
        // Left lens
        g.setColor(SUNGLASS_LENS);
        g.fillRoundRect(cx - r/2 - 5, ey - 4, r/2 + 1, 9, 4, 4);
        g.setColor(SUNGLASS);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(cx - r/2 - 5, ey - 4, r/2 + 1, 9, 4, 4);
        // Right lens
        g.setColor(SUNGLASS_LENS);
        g.fillRoundRect(cx + 2, ey - 4, r/2 + 1, 9, 4, 4);
        g.setColor(SUNGLASS);
        g.drawRoundRect(cx + 2, ey - 4, r/2 + 1, 9, 4, 4);
        g.setStroke(new BasicStroke(1f));
        // Big smile
        g.setStroke(new BasicStroke(2f));
        g.drawArc(cx - r/2, cy, r, r/2, 0, -180);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawEyes(Graphics2D g, int cx, int cy, int r, boolean worried) {
        int ex = r / 3;
        g.setColor(FACE_DARK);
        g.fillOval(cx - ex - 3, cy - ex - 2, 5, 5);
        g.fillOval(cx + ex - 2, cy - ex - 2, 5, 5);
    }
}
