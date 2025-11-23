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
                // CHIP-8 Keypad Layout (what the original hardware had):
                // 1 2 3 C
                // 4 5 6 D
                // 7 8 9 E
                // A 0 B F
                
                // Default Keyboard Mapping (QWERTY layout):
                // 1 2 3 4
                // Q W E R
                // A S D F
                // Z X C V
                
                // CUSTOM CONTROLS - MODIFY THIS SECTION FOR YOUR PREFERENCE
                //
                // To change controls, edit the characters in the 'case' statements below.
                // For example, to map CHIP-8 key 0x5 to 'i' instead of 'w':
                // Change: case 'w': keys[0x5] = pressed; break;
                // To:     case 'i': keys[0x5] = pressed; break;
                //
                // Note: For special keys like arrow keys, you would need to modify the
                // keyPressed/keyReleased methods to use e.getKeyCode() instead of getKeyChar()
                
                switch (keyChar) {
                    // Top row: maps to CHIP-8 keys 1 2 3 C
                    case '1': keys[0x1] = pressed; break;
                    case '2': keys[0x2] = pressed; break;
                    case '3': keys[0x3] = pressed; break;
                    case '4': keys[0xC] = pressed; break;
                    
                    // Second row: maps to CHIP-8 keys 4 5 6 D
                    case 'q': keys[0x4] = pressed; break;
                    case 'w': keys[0x5] = pressed; break;
                    case 'e': keys[0x6] = pressed; break;
                    case 'r': keys[0xD] = pressed; break;
                    
                    // Third row: maps to CHIP-8 keys 7 8 9 E
                    case 'a': keys[0x7] = pressed; break;
                    case 's': keys[0x8] = pressed; break;
                    case 'd': keys[0x9] = pressed; break;
                    case 'f': keys[0xE] = pressed; break;
                    
                    // Bottom row: maps to CHIP-8 keys A 0 B F
                    case 'z': keys[0xA] = pressed; break;
                    case 'x': keys[0x0] = pressed; break;
                    case 'c': keys[0xB] = pressed; break;
                    case 'v': keys[0xF] = pressed; break;
                }
                // END CUSTOM CONTROLS SECTION
                
                // Send the updated key states to the CHIP-8 emulator core
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

