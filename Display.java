import javax.swing.*;
import java.awt.*;

/**
 * DISPLAY RENDERER - Converts CHIP-8 Graphics Buffer to Screen
 * 
 * This class handles rendering the CHIP-8's 64x32 pixel display to the actual window.
 * 
 * CHIP-8 Display Specifications:
 * - Resolution: 64x32 pixels (monochrome display)
 * - Colors: Black for off pixels, White for on pixels
 * - Scaling: 10x (each CHIP-8 pixel becomes a 10x10 square on screen)
 * - Final window size: 640x320 pixels
 * 
 * DATA FLOW:
 * 1. Chip8.drawSprite() writes to the gfx[64][32] array (true = white, false = black)
 * 2. Main loop calls display.updateScreen()
 * 3. updateScreen() calls repaint()
 * 4. Swing automatically calls paintComponent()
 * 5. paintComponent() reads the gfx array and draws white rectangles for on pixels
 * 
 * holds a REFERENCE to Chip8's graphics buffer,
 * so it always displays the current state of the emulator's screen in real-time.
 */
public class Display extends JPanel {
    private static final int SCALE = 10;  // Each CHIP-8 pixel becomes 10x10 screen pixels
    private boolean[][] gfx;              // Reference to CHIP-8's display buffer

    /**
     * Creates the display component
     * @param gfx Reference to CHIP-8 graphics buffer (gfx[64][32])
     */
    public Display(boolean[][] gfx) {
        this.gfx = gfx;  // Store reference (not a copy, so we see live updates)
        setPreferredSize(new Dimension(64 * SCALE, 32 * SCALE));  // Set window size to 640x320
        setBackground(Color.BLACK);  // Black background for off pixels
    }

    /**
     * PAINT COMPONENT - Render Graphics Buffer to Screen
     * 
     * This method is called automatically by Swing whenever repaint() is triggered.
     * It reads the CHIP-8 graphics buffer and draws it to the screen.
     * 
     * Algorithm:
     * 1. Clear the screen with black background (super.paintComponent does this)
     * 2. Set the drawing color to white
     * 3. Loop through all 64x32 pixels in the gfx buffer
     * 4. If a pixel is ON (true), draw a 10x10 white rectangle at that position
     * 5. If a pixel is OFF (false), we skip it and the black background shows through
     * 
     * Performance: We check 2048 pixels per frame, running at approximately 60 FPS
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // Paint the black background first
        g.setColor(Color.WHITE);  // Set color to white for the ON pixels
        
        // Scan through the entire display buffer
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                if (gfx[x][y]) {  // Check if this pixel is ON
                    // Draw a scaled rectangle: CHIP-8 position (x,y) maps to screen position (x*10, y*10)
                    g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
                }
                // If pixel is OFF, we don't draw anything (black background shows)
            }
        }
    }

    /**
     * Triggers a display refresh
     * Called by the Main.java emulation loop after each chip8.cycle()
     */
    public void updateScreen() {
        repaint();  // Tell Swing to call paintComponent() on the next rendering frame
    }
}
