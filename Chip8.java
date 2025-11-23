import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.Random;

/**
 * CHIP-8 EMULATOR CORE - Virtual CPU Implementation
 * 
 * This class emulates the CHIP-8 virtual machine, originally designed in 1977.
 * CHIP-8 was an interpreted programming language used on early microcomputers.
 * 
 * MEMORY MAP:
 * 0x000-0x1FF: Reserved for interpreter (font data stored at 0x50-0x9F)
 * 0x200-0xFFF: Program ROM and RAM (3584 bytes available)
 * 
 * REGISTERS:
 * V0-VF: 16 general-purpose 8-bit registers (VF doubles as a flag register)
 * I: 16-bit index register (typically used for memory operations)
 * PC: 16-bit program counter (tracks which instruction to execute next)
 * 
 * TIMERS (both decrement at 60Hz when non-zero):
 * Delay Timer: General purpose timing for game logic
 * Sound Timer: Audio buzzer plays when value is greater than 0
 * 
 * DISPLAY:
 * 64x32 monochrome pixels (1 = white/on, 0 = black/off)
 * Drawing uses XOR logic, which allows collision detection and sprite erasing
 * 
 * INPUT:
 * 16-key hexadecimal keypad (keys 0x0 through 0xF)
 */
public class Chip8 {
    // CPU STATE VARIABLES
    // These represent the internal state of the CHIP-8 virtual machine
    
    private short pc;            // Program counter - tracks which instruction to execute next
    private byte[] memory;       // 4KB of memory (addresses 0x000 to 0xFFF)
    private byte[] V;           // 16 general-purpose registers (V0 through VF)
    private short I;            // Index register - stores memory addresses for operations
    private Stack<Short> stack; // Call stack for subroutines (supports up to 16 levels)
    private byte delayTimer;    // Delay timer - decrements at 60Hz
    private byte soundTimer;    // Sound timer - beep plays while this is greater than 0
    private boolean[][] gfx;    // 64x32 display buffer (true = white pixel, false = black)
    private boolean[] keys;     // 16-key input state (true = currently pressed)
    private Random random;      // Random number generator for CXNN opcode
    private boolean drawFlag;   // Signals when the display needs to be redrawn
    
    // CHIP-8 font set (0-F), each character is 5 bytes
    private static final byte[] FONT_SET = {
        (byte)0xF0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xF0, // 0
        (byte)0x20, (byte)0x60, (byte)0x20, (byte)0x20, (byte)0x70, // 1
        (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x80, (byte)0xF0, // 2
        (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x10, (byte)0xF0, // 3
        (byte)0x90, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0x10, // 4
        (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x10, (byte)0xF0, // 5
        (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x90, (byte)0xF0, // 6
        (byte)0xF0, (byte)0x10, (byte)0x20, (byte)0x40, (byte)0x40, // 7
        (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0xF0, // 8
        (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0xF0, // 9
        (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0x90, // A
        (byte)0xE0, (byte)0x90, (byte)0xE0, (byte)0x90, (byte)0xE0, // B
        (byte)0xF0, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xF0, // C
        (byte)0xE0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xE0, // D
        (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0xF0, // E
        (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0x80  // F
    };

    /**
     * Initializes CHIP-8 virtual machine
     */
    public Chip8() {
        memory = new byte[4096];
        V = new byte[16];
        stack = new Stack<>();
        gfx = new boolean[64][32];
        keys = new boolean[16];
        random = new Random();
        pc = 0x200; // Program start address
        drawFlag = false;
        
        // Load font set into memory (0x000 - 0x1FF is reserved for system)
        System.arraycopy(FONT_SET, 0, memory, 0x50, FONT_SET.length);
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
     * CYCLE - Execute One Instruction (Fetch-Decode-Execute)
     * 
     * This is the main execution loop of the emulator, called approximately 500 times per second.
     * It implements the classic CPU cycle: fetch an instruction, decode what it means, then execute it.
     * 
     * FETCH: Read 2-byte opcode from memory at the current PC address
     * DECODE: Determine which instruction to run based on the opcode pattern
     * EXECUTE: Perform the instruction's action
     * 
     * OPCODE FORMAT (all instructions are 16 bits / 2 bytes):
     * - First nibble (masked with 0xF000): Instruction category
     * - X (masked with 0x0F00): First register identifier
     * - Y (masked with 0x00F0): Second register identifier  
     * - N (masked with 0x000F): 4-bit immediate value
     * - NN (masked with 0x00FF): 8-bit immediate value
     * - NNN (masked with 0x0FFF): 12-bit memory address
     */
    public void cycle() {
        // FETCH: Read opcode from memory (combine 2 bytes into a 16-bit value)
        // We need to combine memory[PC] and memory[PC+1] into a single short
        // The & 0xFF mask ensures bytes are treated as unsigned (prevents sign extension)
        short opcode = (short) (((memory[pc] & 0xFF) << 8) | (memory[pc + 1] & 0xFF));
        pc += 2;  // Move to next instruction (all CHIP-8 instructions are 2 bytes)

        // DECODE & EXECUTE: Match the opcode pattern and perform the corresponding action
        // We examine the first nibble (high 4 bits) to categorize the instruction
        switch (opcode & 0xF000) {
            case 0x0000:  // System instructions
                if (opcode == 0x00E0) { // 00E0: Clear screen
                    clearScreen();
                } else if (opcode == 0x00EE) { // 00EE: Return from subroutine
                    pc = stack.pop();  // Retrieve the return address from the stack
                }
                break;
            
            case 0x1000: // 1NNN: Jump to address NNN
                pc = (short) (opcode & 0x0FFF);  // Set program counter to NNN
                break;
            
            case 0x2000: // 2NNN: Call subroutine at NNN
                stack.push(pc);  // Save current address so we can return later
                pc = (short) (opcode & 0x0FFF);  // Jump to the subroutine
                break;
            
            case 0x3000: // 3XNN: Skip next instruction if VX == NN
                if ((V[(opcode & 0x0F00) >> 8] & 0xFF) == (opcode & 0x00FF)) {
                    pc += 2;  // Skip the next instruction by advancing PC by 2 bytes
                }
                break;
            
            case 0x4000: // 4XNN: Skip next instruction if VX != NN
                if ((V[(opcode & 0x0F00) >> 8] & 0xFF) != (opcode & 0x00FF)) {
                    pc += 2;
                }
                break;
            
            case 0x5000: // 5XY0: Skip next instruction if VX == VY
                if (V[(opcode & 0x0F00) >> 8] == V[(opcode & 0x00F0) >> 4]) {
                    pc += 2;
                }
                break;
            
            case 0x6000: // 6XNN: Set register VX to NN
                V[(opcode & 0x0F00) >> 8] = (byte) (opcode & 0x00FF);
                break;
            
            case 0x7000: // 7XNN: Add NN to VX (carry flag not affected)
                V[(opcode & 0x0F00) >> 8] += (byte) (opcode & 0x00FF);
                break;
            
            case 0x8000: // 8XYN: Arithmetic and logic operations (delegated to separate method)
                handleArithmeticOpcode(opcode);
                break;
            
            case 0x9000: // 9XY0: Skip next instruction if VX != VY
                if (V[(opcode & 0x0F00) >> 8] != V[(opcode & 0x00F0) >> 4]) {
                    pc += 2;
                }
                break;
            
            case 0xA000: // ANNN: Set index register I to address NNN
                I = (short) (opcode & 0x0FFF);  // I is typically used for memory operations
                break;
            
            case 0xB000: // BNNN: Jump to address NNN plus V0
                pc = (short) ((opcode & 0x0FFF) + (V[0] & 0xFF));
                break;
            
            case 0xC000: // CXNN: Set VX to a random byte AND NN
                V[(opcode & 0x0F00) >> 8] = (byte) (random.nextInt(256) & (opcode & 0x00FF));
                break;
            
            case 0xD000: // DXYN: Draw sprite (see drawSprite method for details)
                drawSprite(opcode);
                break;
            
            case 0xE000: // EX9E/EXA1: Keyboard input operations
                handleKeyOpcode(opcode);
                break;
            
            case 0xF000: // FX**: Timer and memory operations
                handleFTimerOpcode(opcode);
                break;
        }
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
     * DXYN - Draw Sprite
     * 
     * Draws an N-pixel tall sprite at coordinates (VX, VY).
     * Sprite data is read from memory starting at address I.
     * Each sprite is 8 pixels wide (1 byte) and N pixels tall.
     * 
     * COLLISION DETECTION:
     * - VF is set to 1 if any screen pixel is turned OFF during drawing (collision detected)
     * - VF is set to 0 if no collision occurs
     * 
     * XOR DRAWING LOGIC:
     * - Uses XOR operation: sprite_pixel XOR screen_pixel
     * - If screen pixel is OFF and sprite pixel is ON: turn screen pixel ON
     * - If screen pixel is ON and sprite pixel is ON: turn screen pixel OFF (this is a collision)
     * - This XOR behavior allows sprites to be "erased" by drawing them again at the same location
     * 
     * WRAPPING BEHAVIOR:
     * - Sprites that go off the edge wrap around to the opposite side (modulo arithmetic)
     * 
     * DATA FLOW:
     * 1. Read sprite bytes from memory addresses I through I+height-1
     * 2. For each bit in the sprite, XOR it with the corresponding screen pixel
     * 3. Set VF flag if any collision is detected
     * 4. Set drawFlag to trigger a screen redraw
     */
    private void drawSprite(short opcode) {
        // Extract parameters from the opcode
        int xReg = (opcode & 0x0F00) >> 8;  // Which register contains the X coordinate
        int yReg = (opcode & 0x00F0) >> 4;  // Which register contains the Y coordinate
        int height = opcode & 0x000F;        // Height of sprite in pixels (1 to 15)
        
        // Read the actual coordinates from the specified registers
        int x = V[xReg] & 0xFF;  // Starting X position (0-63)
        int y = V[yReg] & 0xFF;  // Starting Y position (0-31)
        V[0xF] = 0;  // Initialize collision flag to 0 (assume no collision)

        // Draw the sprite row by row
        for (int row = 0; row < height; row++) {
            byte sprite = memory[I + row];  // Fetch one row of sprite data (8 pixels as bits)
            
            // Process each of the 8 pixels in this row
            for (int col = 0; col < 8; col++) {
                // Check if this bit/pixel in the sprite is set to 1
                // 0x80 is binary 10000000, we shift it right to test each bit position
                if ((sprite & (0x80 >> col)) != 0) {
                    // Calculate screen position with wrapping (modulo ensures we stay within bounds)
                    int px = (x + col) % 64;
                    int py = (y + row) % 32;
                    
                    // Collision detection: if the screen pixel was already ON, we have a collision
                    if (gfx[px][py]) V[0xF] = 1;
                    
                    // XOR operation: flip the pixel state (ON becomes OFF, OFF becomes ON)
                    gfx[px][py] ^= true;
                }
            }
        }
        drawFlag = true;  // Mark that the display needs to be refreshed
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

    /**
     * 8XYN - Arithmetic and Logic Operations
     * 
     * All these operations work with registers VX and VY.
     * VF (register 15) serves as a special flag register that stores:
     * - Carry flag for addition (1 if overflow occurred)
     * - NOT borrow flag for subtraction (1 if no borrow needed)
     * - The bit that was shifted out during shift operations
     * 
     * use (& 0xFF) to convert signed bytes to unsigned integers before arithmetic.
     * This prevents Java's sign extension from causing incorrect results.
     */
    private void handleArithmeticOpcode(short opcode) {
        int x = (opcode & 0x0F00) >> 8;  // Extract X register index from opcode
        int y = (opcode & 0x00F0) >> 4;  // Extract Y register index from opcode
        
        switch (opcode & 0x000F) {
            case 0x0: // 8XY0: VX = VY (simple assignment)
                V[x] = V[y];
                break;
            
            case 0x1: // 8XY1: VX = VX OR VY (bitwise OR operation)
                V[x] |= V[y];
                break;
            
            case 0x2: // 8XY2: VX = VX AND VY (bitwise AND operation)
                V[x] &= V[y];
                break;
            
            case 0x3: // 8XY3: VX = VX XOR VY (bitwise XOR operation)
                V[x] ^= V[y];
                break;
            
            case 0x4: // 8XY4: VX = VX + VY, VF = carry (1 if overflow, 0 otherwise)
                int sum = (V[x] & 0xFF) + (V[y] & 0xFF);
                V[0xF] = (byte) (sum > 255 ? 1 : 0);  // Set carry flag if result exceeds 8 bits
                V[x] = (byte) sum;  // Store only the lower 8 bits
                break;
            
            case 0x5: // 8XY5: VX = VX - VY, VF = NOT borrow (1 if VX >= VY, 0 if borrow needed)
                V[0xF] = (byte) ((V[x] & 0xFF) >= (V[y] & 0xFF) ? 1 : 0);
                V[x] -= V[y];
                break;
            
            case 0x6: // 8XY6: VX = VX >> 1 (shift right by 1), VF = bit that was shifted out
                V[0xF] = (byte) (V[x] & 0x1);  // Save the least significant bit before shifting
                V[x] = (byte) ((V[x] & 0xFF) >> 1);  // Perform the right shift
                break;
            
            case 0x7: // 8XY7: VX = VY - VX, VF = NOT borrow (1 if VY >= VX, 0 if borrow needed)
                V[0xF] = (byte) ((V[y] & 0xFF) >= (V[x] & 0xFF) ? 1 : 0);
                V[x] = (byte) ((V[y] & 0xFF) - (V[x] & 0xFF));
                break;
            
            case 0xE: // 8XYE: VX = VX << 1 (shift left by 1), VF = bit that was shifted out
                V[0xF] = (byte) ((V[x] & 0x80) >> 7);  // Save the most significant bit before shifting
                V[x] = (byte) ((V[x] & 0xFF) << 1);  // Perform the left shift
                break;
        }
    }

    /**
     * Handles EX9E and EXA1 key press opcodes
     */
    private void handleKeyOpcode(short opcode) {
        int x = (opcode & 0x0F00) >> 8;
        int key = V[x] & 0xF;
        
        if ((opcode & 0x00FF) == 0x9E) { // EX9E: Skip if key VX is pressed
            if (keys[key]) {
                pc += 2;
            }
        } else if ((opcode & 0x00FF) == 0xA1) { // EXA1: Skip if key VX is not pressed
            if (!keys[key]) {
                pc += 2;
            }
        }
    }

    /**
     * FX** - Timer, Input, and Memory Operations
     * 
     * This group of instructions handles various special operations:
     * - Reading and writing timer values (delay timer and sound timer)
     * - Blocking until a key press is detected
     * - Getting the memory address of font characters
     * - BCD (Binary-Coded Decimal) conversion for displaying numbers
     * - Bulk register save/load operations
     */
    private void handleFTimerOpcode(short opcode) {
        int x = (opcode & 0x0F00) >> 8;  // Extract X register index from opcode
        
        switch (opcode & 0x00FF) {
            case 0x07: // FX07: VX = delay timer value
                V[x] = delayTimer;
                break;
            
            case 0x0A: // FX0A: Wait for key press, then store the key value in VX (blocking operation)
                // This instruction halts execution until a key is pressed
                // Scan all 16 keys to check if any are currently pressed
                boolean keyPressed = false;
                for (int i = 0; i < 16; i++) {
                    if (keys[i]) {
                        V[x] = (byte) i;  // Store the hex value of the pressed key
                        keyPressed = true;
                        break;
                    }
                }
                // If no key is pressed, we decrement PC to repeat this same instruction
                // This creates a blocking wait until a key is pressed
                if (!keyPressed) {
                    pc -= 2;
                }
                break;
            
            case 0x15: // FX15: Set delay timer to VX
                delayTimer = V[x];  // Timer will decrement at 60Hz until it reaches 0
                break;
            
            case 0x18: // FX18: Set sound timer to VX
                soundTimer = V[x];  // Audio beeps while sound timer > 0, decrements at 60Hz
                break;
            
            case 0x1E: // FX1E: I = I + VX (add VX to the index register)
                I += (V[x] & 0xFF);
                break;
            
            case 0x29: // FX29: Set I to the memory address of the font character in VX
                // Font sprites are stored in memory starting at 0x50
                // Each character is 5 bytes tall, so character N starts at 0x50 + (N * 5)
                // Example: '0' is at 0x50, '1' is at 0x55, '2' is at 0x5A, etc.
                I = (short) (0x50 + ((V[x] & 0xF) * 5));
                break;
            
            case 0x33: // FX33: Store BCD (Binary-Coded Decimal) representation of VX in memory
                // Takes the value in VX and converts it to 3 decimal digits
                // Stores hundreds digit at memory[I], tens at memory[I+1], ones at memory[I+2]
                // Example: if VX = 157, then memory[I]=1, memory[I+1]=5, memory[I+2]=7
                int value = V[x] & 0xFF;
                memory[I] = (byte) (value / 100);           // Hundreds place
                memory[I + 1] = (byte) ((value / 10) % 10); // Tens place
                memory[I + 2] = (byte) (value % 10);        // Ones place
                break;
            
            case 0x55: // FX55: Store registers V0 through VX in memory starting at address I
                // This is like a bulk save operation for the registers
                for (int i = 0; i <= x; i++) {
                    memory[I + i] = V[i];
                }
                break;
            
            case 0x65: // FX65: Load registers V0 through VX from memory starting at address I
                // This is like a bulk load operation for the registers
                for (int i = 0; i <= x; i++) {
                    V[i] = memory[I + i];
                }
                break;
        }
    }
    
    /**
     * Decrements delay timer (called at 60Hz)
     */
    public void updateDelayTimer() {
        if (delayTimer > 0) {
            delayTimer--;
        }
    }
    
    /**
     * Decrements sound timer (called at 60Hz)
     */
    public void updateSoundTimer() {
        if (soundTimer > 0) {
            soundTimer--;
        }
    }
    
    /**
     * @return Current sound timer value (for sound generation)
     */
    public byte getSoundTimer() {
        return soundTimer;
    }
    
    /**
     * @return True if screen needs to be redrawn
     */
    public boolean needsRedraw() {
        boolean temp = drawFlag;
        drawFlag = false;
        return temp;
    }
}
