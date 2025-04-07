import javax.swing.*;
import java.awt.*;

/**
 * Renders CHIP-8 display using Swing
 * Provides 10x scaled view of 64x32 pixel buffer
 */
public class Display extends JPanel {
    private static final int SCALE = 10;
    private boolean[][] gfx;

    /**
     * Creates display component
     * @param gfx Reference to CHIP-8 graphics buffer
     */
    public Display(boolean[][] gfx) {
        this.gfx = gfx;
        setPreferredSize(new Dimension(64 * SCALE, 32 * SCALE));
        setBackground(Color.BLACK);
    }

    /**
     * Renders current display state
     * @param g Graphics context for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                if (gfx[x][y]) {
                    g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
                }
            }
        }
    }

    /** Triggers display refresh */
    public void updateScreen() {
        repaint();
    }
}
