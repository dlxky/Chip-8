import java.io.IOException;
import javax.swing.*;

/**
 * Main entry point for the CHIP-8 emulator application
 * Handles GUI setup and emulation timing
 */
public class Main {
    /**
     * Application entry point
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        Chip8 chip8 = new Chip8();
        try {
            chip8.loadRom("roms/PONG");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "ROM load failed: " + e.getMessage());
            return;
        }

        // GUI setup
        JFrame frame = new JFrame("CHIP-8 Emulator");
        Display display = new Display(chip8.getGfx());
        Keys keyboard = new Keys(frame, chip8);

        frame.add(display);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Emulation loop (16ms ≈ 60Hz)
        new Timer(16, e -> {
            chip8.cycle();
            display.updateScreen();
        }).start();
    }
}