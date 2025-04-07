import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * CHIP-8 Emulator Core
 * Implements CPU operations, memory management, and display handling
 */
public class Chip8 {
    private short pc;            // Program counter
    private byte[] memory;       // 4KB memory
    private byte[] V;           // 16 general-purpose registers
    private short I;            // Index register
    private Stack<Short> stack; // Call stack
    private byte delayTimer;    // Delay timer
    private byte soundTimer;    // Sound timer
    private boolean[][] gfx;    // 64x32 display buffer
    private boolean[] keys;     // 16-key input state

    /**
     * Initializes CHIP-8 virtual machine
     */
    public Chip8() {
        memory = new byte[4096];
        V = new byte[16];
        stack = new Stack<>();
        gfx = new boolean[64][32];
        keys = new boolean[16];
        pc = 0x200; // Program start address
    }

    /**
     * Loads ROM file into memory starting at 0x200
     * @param romPath Path to CHIP-8 ROM file
     * @throws IOException If ROM file cannot be read
     */
    public void loadRom(String romPath) throws IOException {
        byte[] romData = Files.readAllBytes(Paths.get(romPath));
        System.arraycopy(romData, 0, memory, 0x200, romData.length);
    }

    /**
     * Executes one emulation cycle (fetch-decode-execute)
     * Handles opcode processing and system timers
     */
    public void cycle() {
        // Fetch opcode (2 bytes)
        short opcode = (short) ((memory[pc] << 8) | (memory[pc + 1] & 0xFF));
        pc += 2;

        // Decode & Execute
        switch (opcode & 0xF000) {
            case 0x0000:
                if (opcode == 0x00E0) clearScreen();
                break;
            case 0x1000: // 1NNN: Jump
                pc = (short) (opcode & 0x0FFF);
                break;
            case 0x6000: // 6XNN: Set VX
                V[(opcode & 0x0F00) >> 8] = (byte) (opcode & 0x00FF);
                break;
            case 0xD000: // DXYN: Draw sprite
                drawSprite(opcode);
                break;
            case 0xA000: // ANNN: Set I
                I = (short) (opcode & 0x0FFF);
                break;
            case 0xE000: // EX9E/EXA1: Skip if key
                handleKeyOpcode(opcode);
                break;
            case 0xF000:
                handleFTimerOpcode(opcode);
                break;
        }

        // TODO: Update timers
    }

    /** Clears the display buffer */
    private void clearScreen() {
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                gfx[x][y] = false;
            }
        }
    }

    /**
     * Draws sprite to display with XOR-based collision
     * @param opcode Drawing instruction (DXYN format)
     */
    private void drawSprite(short opcode) {
        int xReg = (opcode & 0x0F00) >> 8;
        int yReg = (opcode & 0x00F0) >> 4;
        int height = opcode & 0x000F;
        
        int x = V[xReg] % 64;
        int y = V[yReg] % 32;
        V[0xF] = 0;

        for (int row = 0; row < height; row++) {
            byte sprite = memory[I + row];
            for (int col = 0; col < 8; col++) {
                if ((sprite & (0x80 >> col)) != 0) {
                    int px = (x + col) % 64;
                    int py = (y + row) % 32;
                    
                    if (gfx[px][py]) V[0xF] = 1;
                    gfx[px][py] ^= true;
                }
            }
        }
    }

    /** @return Current display buffer state */
    public boolean[][] getGfx() { return gfx; }

    /**
     * Updates key states
     * @param keys Current key states array
     */
    public void setKeys(boolean[] keys) {
        System.arraycopy(keys, 0, this.keys, 0, 16);
    }

    // TODO: Implement remaining opcodes
    private void handleKeyOpcode(short opcode) { /* ... */ }
    private void handleFTimerOpcode(short opcode) { /* ... */ }
}
