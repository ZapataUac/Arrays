package minesweeper;

import javax.swing.*;
import java.awt.*;

/**
 * Classic red-on-black 7-segment LCD display (3 digits).
 */
public class SevenSegmentDisplay extends JPanel {

    private int value = 0;
    private static final Color BG_COLOR  = new Color(10,  10,  10);
    private static final Color ON_COLOR  = new Color(255,  30,  30);
    private static final Color OFF_COLOR = new Color( 40,   5,   5);

    // Segment definitions per digit [0-9]
    // Segments: top, top-left, top-right, middle, bot-left, bot-right, bottom
    private static final boolean[][] SEGMENTS = {
        {true,  true,  true,  false, true,  true,  true },  // 0
        {false, false, true,  false, false, true,  false},  // 1
        {true,  false, true,  true,  true,  false, true },  // 2
        {true,  false, true,  true,  false, true,  true },  // 3
        {false, true,  true,  true,  false, true,  false},  // 4
        {true,  true,  false, true,  false, true,  true },  // 5
        {true,  true,  false, true,  true,  true,  true },  // 6
        {true,  false, true,  false, false, true,  false},  // 7
        {true,  true,  true,  true,  true,  true,  true },  // 8
        {true,  true,  true,  true,  false, true,  true },  // 9
    };

    private static final int DIGIT_W = 19;
    private static final int DIGIT_H = 34;
    private static final int GAP     = 3;
    private static final int SEG_T   = 3;   // segment thickness
    private static final int MARGIN  = 5;

    public SevenSegmentDisplay() {
        setPreferredSize(new Dimension(MARGIN * 2 + DIGIT_W * 3 + GAP * 2, DIGIT_H + MARGIN * 2));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createLoweredBevelBorder());
    }

    public void setValue(int v) {
        this.value = Math.max(-99, Math.min(999, v));
        repaint();
    }

    public int getValue() { return value; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean negative = value < 0;
        int abs = Math.abs(value);
        int d0 = negative ? 11 : (abs / 100) % 10;      // hundreds (or minus)
        int d1 = (abs / 10) % 10;
        int d2 = abs % 10;

        int x = MARGIN;
        int y = MARGIN;

        // Draw minus sign if negative
        if (negative) {
            drawMinus(g2, x, y);
        } else {
            drawDigit(g2, x, y, d0);
        }
        x += DIGIT_W + GAP;
        drawDigit(g2, x, y, d1);
        x += DIGIT_W + GAP;
        drawDigit(g2, x, y, d2);
    }

    private void drawDigit(Graphics2D g, int x, int y, int d) {
        boolean[] segs = SEGMENTS[d % 10];
        int w = DIGIT_W, h = DIGIT_H, t = SEG_T;
        int hw = w / 2;

        // top
        drawHSeg(g, x + t, y,           w - t * 2, t, segs[0]);
        // top-left
        drawVSeg(g, x,     y + t,        h / 2 - t, t, segs[1]);
        // top-right
        drawVSeg(g, x + w - t, y + t,   h / 2 - t, t, segs[2]);
        // middle
        drawHSeg(g, x + t, y + h / 2,   w - t * 2, t, segs[3]);
        // bot-left
        drawVSeg(g, x,     y + h / 2 + t, h / 2 - t, t, segs[4]);
        // bot-right
        drawVSeg(g, x + w - t, y + h / 2 + t, h / 2 - t, t, segs[5]);
        // bottom
        drawHSeg(g, x + t, y + h - t,   w - t * 2, t, segs[6]);
    }

    private void drawHSeg(Graphics2D g, int x, int y, int len, int t, boolean on) {
        g.setColor(on ? ON_COLOR : OFF_COLOR);
        g.fillRect(x, y, len, t);
    }

    private void drawVSeg(Graphics2D g, int x, int y, int len, int t, boolean on) {
        g.setColor(on ? ON_COLOR : OFF_COLOR);
        g.fillRect(x, y, t, len);
    }

    private void drawMinus(Graphics2D g, int x, int y) {
        int w = DIGIT_W, h = DIGIT_H, t = SEG_T;
        // only middle segment for minus
        drawHSeg(g, x + t, y + h / 2, w - t * 2, t, true);
    }
}
