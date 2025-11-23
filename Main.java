import java.io.IOException;
import java.io.File;
import javax.swing.*;
import javax.sound.sampled.*;

/**
 * CHIP-8 EMULATOR - MAIN ENTRY POINT
 * 
 * This is where the program starts and coordinates all the components.
 * 
 * EXECUTION ORDER:
 * 1. Show file chooser so user can select a ROM file
 * 2. Create Chip8 instance (initializes memory, registers, display buffer)
 * 3. Load ROM into memory starting at address 0x200
 * 4. Initialize sound system (generates 440Hz beep audio)
 * 5. Create GUI (window, display panel, keyboard input handler)
 * 6. Start timer thread running at 60Hz (updates timers, manages sound)
 * 7. Start emulation loop running at about 500Hz (executes instructions, updates display)
 * 
 * DATA FLOW:
 * Main creates Chip8 which executes instructions and updates the graphics buffer
 * Display reads from the graphics buffer and renders it to the screen
 * Keys listens for keyboard input and updates the key state in Chip8
 * 
 * THREADING MODEL:
 * - Main Thread: Handles GUI events and display rendering (managed by Swing)
 * - Timer Thread: Runs at 60Hz to decrement timers and manage sound playback
 * - Swing Timer: Runs at ~500Hz to execute CHIP-8 instructions via chip8.cycle()
 */
public class Main {
    private static Clip beepClip;  // Audio clip for the CHIP-8 buzzer sound
    
    /**
     * Application entry point
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        // ROM SELECTION
        // Open a file chooser dialog so the user can select a CHIP-8 ROM file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CHIP-8 ROM");
        fileChooser.setCurrentDirectory(new File(".")); // Start in current directory
        
        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("No ROM selected. Exiting.");
            return;
        }
        
        File selectedRom = fileChooser.getSelectedFile();
        
        // CHIP-8 INITIALIZATION
        // Create a new CHIP-8 instance (this allocates 4KB memory, 16 registers, etc.)
        Chip8 chip8 = new Chip8();
        
        // Load the ROM file into memory starting at address 0x200
        // CHIP-8 programs always start execution at 0x200 by convention
        try {
            chip8.loadRom(selectedRom.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "ROM load failed: " + e.getMessage());
            return;
        }

        // SOUND SYSTEM SETUP
        // Generate a 440Hz beep tone for the CHIP-8 sound timer
        initializeSound();

        // GUI SETUP
        // Create the main window with the ROM name displayed in the title bar
        JFrame frame = new JFrame("CHIP-8 Emulator - " + selectedRom.getName());
        
        // Create the display panel (it gets a reference to CHIP-8's graphics buffer)
        // The Display will read from chip8.getGfx() to render the 64x32 pixel screen
        Display display = new Display(chip8.getGfx());
        
        // Create the keyboard input handler (attaches listeners to the frame)
        // The Keys object will write to chip8.setKeys() whenever keys are pressed/released
        @SuppressWarnings("unused")
        Keys keyboard = new Keys(frame, chip8); // Must keep this variable so listener stays active

        // Assemble the window and make it visible
        frame.add(display);
        frame.pack();  // Size the window to fit the display (640x320 pixels)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // TIMER THREAD (60Hz - Standard CHIP-8 Timer Frequency)
        // This thread runs independently from instruction execution
        // It handles: delay timer decrement, sound timer decrement, audio playback
        Thread timerThread = new Thread(() -> {
            while (true) {
                // Decrement the delay timer if it's greater than 0
                // Games use this timer for delays and timing game logic
                chip8.updateDelayTimer();
                
                // Decrement the sound timer if it's greater than 0
                // The buzzer should play whenever this timer is active
                chip8.updateSoundTimer();
                
                // Control audio based on sound timer state
                if (chip8.getSoundTimer() > 0) {
                    playBeep();  // Start or continue playing the 440Hz tone
                } else {
                    stopBeep();  // Stop the audio when sound timer reaches 0
                }
                
                try {
                    Thread.sleep(1000 / 60); // Sleep for 16.67ms to maintain 60Hz frequency
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timerThread.setDaemon(true);  // Daemon thread will automatically die when program exits
        timerThread.start();

        // EMULATION LOOP (~500Hz - Fast Instruction Execution)
        // Swing Timer repeatedly executes at 2ms intervals
        // Each tick performs: fetch opcode, decode instruction, execute, update display
        new Timer(2, e -> {
            chip8.cycle();           // Execute one CHIP-8 instruction
            display.updateScreen();  // Trigger a screen redraw if needed
        }).start();
    }
    
    /**
     * SOUND SYSTEM - Generates 440Hz beep for CHIP-8 buzzer
     * 
     * Creates an audio clip using Java Sound API.
     * The beep plays when soundTimer is greater than 0, stops when soundTimer equals 0.
     */
    private static void initializeSound() {
        try {
            // Define audio parameters
            int sampleRate = 8000;      // 8000 samples per second (8kHz)
            int duration = 100;          // 100 milliseconds clip length
            byte[] buffer = new byte[sampleRate * duration / 1000];
            
            // Generate a sine wave at 440Hz (this is the musical note A4)
            for (int i = 0; i < buffer.length; i++) {
                // Calculate the angle for the sine wave formula: 2*pi * sample_number * frequency / sample_rate
                double angle = 2.0 * Math.PI * i * 440 / sampleRate;
                // Convert sine wave output (range -1 to 1) to byte range (-127 to 127)
                buffer[i] = (byte) (Math.sin(angle) * 127);
            }
            
            // Create audio format specification: 8-bit, mono, signed, big-endian
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            // Load the audio clip into memory for instant playback
            beepClip = (Clip) AudioSystem.getLine(info);
            beepClip.open(format, buffer, 0, buffer.length);
        } catch (LineUnavailableException e) {
            System.err.println("Sound system unavailable: " + e.getMessage());
        }
    }
    
    /**
     * Starts playing the beep sound (loops continuously)
     * Called when soundTimer is greater than 0
     */
    private static void playBeep() {
        if (beepClip != null && !beepClip.isRunning()) {
            beepClip.setFramePosition(0);            // Reset playback to the beginning
            beepClip.loop(Clip.LOOP_CONTINUOUSLY);   // Loop the sound until we stop it
        }
    }
    
    /**
     * Stops the beep sound
     * Called when soundTimer reaches 0
     */
    private static void stopBeep() {
        if (beepClip != null && beepClip.isRunning()) {
            beepClip.stop();
        }
    }
}