import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Handles keyboard input mapping for CHIP-8 emulator
 * Maps PC keyboard inputs to CHIP-8's hexadecimal keypad
 */
public class Keys {
    private boolean[] keys = new boolean[16]; // CHIP-8 keypad (0x0-0xF)
    private Chip8 chip8;

    /**
     * Initializes keyboard input handler
     * @param frame The parent JFrame for key listener attachment
     * @param chip8 Reference to the CHIP-8 emulator core
     */
    public Keys(JFrame frame, Chip8 chip8) {
        this.chip8 = chip8;
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyEvent(e.getKeyChar(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyEvent(e.getKeyChar(), false);
            }

            private void handleKeyEvent(char keyChar, boolean pressed) {
                switch (keyChar) {
                    case 'w': keys[0x1] = pressed; break;
                    case 'j': keys[0x2] = pressed; break;
                    case 's': keys[0x3] = pressed; break;
                    case 'k': keys[0xC] = pressed; break;
                }
                chip8.setKeys(keys);
            }
        });
    }

    /**
     * Checks if a specific CHIP-8 key is pressed
     * @param key CHIP-8 key code (0x0-0xF)
     * @return True if the specified key is currently pressed
     */
    public boolean isKeyPressed(int key) {
        return keys[key];
    }
}

