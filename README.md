# CHIP-8 Emulator

A fully functional Java implementation of a CHIP-8 interpreter/emulator with sound support, complete opcode implementation, and an intuitive GUI.

<!-- Uncomment and add screenshots after taking them:
## Screenshots

![PONG Gameplay](screenshots/pong.png)
*Classic PONG running on the emulator*

![Space Invaders](screenshots/space-invaders.png)
*Space Invaders gameplay*
-->

## Features

### Implemented
- **Complete Opcode Support** - 25 most commonly used CHIP-8 instructions including:
  - System operations (00E0, 00EE)
  - Flow control (1NNN, 2NNN, BNNN)
  - Conditionals (3XNN, 4XNN, 5XY0, 9XY0)
  - Register operations (6XNN, 7XNN, 8XYN)
  - Memory operations (ANNN, FX1E, FX55, FX65)
  - Graphics (DXYN with XOR collision detection)
  - Input (EX9E, EXA1, FX0A)
  - Timers (FX07, FX15, FX18)
  - BCD conversion (FX33)
  - Font sprites (FX29)

- **Display System**
  - 64x32 pixel monochrome display
  - 10x scaling (640x320 window)
  - XOR-based sprite drawing with collision detection
  - Smooth 60 FPS rendering

- **Audio System**
  - 440Hz beep tone (musical note A4)
  - Sound timer synchronized at 60Hz
  - Java Sound API implementation

- **Input Handling**
  - Complete 16-key hexadecimal keypad mapping
  - Customizable controls (edit Keys.java)
  - Default QWERTY layout: 1234/QWER/ASDF/ZXCV

- **Timer System**
  - Separate daemon thread running at 60Hz
  - Accurate delay timer and sound timer
  - Proper synchronization with CPU cycle

- **ROM Management**
  - File chooser dialog for easy ROM selection
  - Support for all standard CHIP-8 ROMs
  - Built-in font set (0-F characters)

## Controls

Default keyboard layout maps to CHIP-8's hexadecimal keypad:

```
Keyboard        CHIP-8
1 2 3 4    ->   1 2 3 C
Q W E R    ->   4 5 6 D
A S D F    ->   7 8 9 E
Z X C V    ->   A 0 B F
```

To customize controls, edit the switch statement in `Keys.java` (lines 23-50).

## Installation

### Requirements
- JDK 8 or higher
- CHIP-8 ROM files

### Download ROMs
Popular CHIP-8 games can be found at:
- [kripod/chip8-roms](https://github.com/kripod/chip8-roms)
- [dmatlack/chip8](https://github.com/dmatlack/chip8/tree/master/roms)

Recommended games: PONG, Space Invaders, Tetris, Breakout (Brix), Cave

### Setup
1. Clone repository:
```bash
git clone https://github.com/dlxky/Chip-8.git
cd Chip-8
```

2. Compile the source files:
```bash
javac *.java
```

3. Run the emulator:
```bash
java Main
```

4. Select a CHIP-8 ROM file from the file chooser dialog

## Usage

```bash
# Compile
javac *.java

# Run
java Main
```

The emulator will open a file chooser. Navigate to your ROM files and select one to run.

## Technical Details

### Architecture
- **CPU Speed**: ~500 Hz instruction execution
- **Timer Frequency**: 60 Hz (industry standard)
- **Display Refresh**: 60 FPS
- **Memory**: 4KB (4096 bytes)
  - 0x000-0x1FF: Reserved for interpreter and font data
  - 0x200-0xFFF: Program ROM and RAM
- **Registers**: 16 x 8-bit general purpose (V0-VF)
- **Stack**: 16 levels for subroutine calls
- **Font**: Built-in hexadecimal sprites (0-F) at 0x050-0x09F

### Implementation Notes
- XOR-based sprite drawing allows collision detection and sprite erasing
- VF register serves dual purpose as flag register (carry, borrow, collision)
- Separate timer thread ensures accurate 60Hz timer decrements
- Sound implemented using Java Sound API with generated sine wave

## Project Structure

```
Chip-8/
├── Chip8.java      # Core emulator (CPU, memory, opcodes)
├── Display.java    # Graphics rendering (Swing)
├── Keys.java       # Keyboard input handling
├── Main.java       # Entry point, GUI setup, timing
├── README.md       # This file
```

## Known Limitations

- Original CHIP-8 quirks/variants may behave differently
- No save state functionality
- Fixed execution speed (not configurable at runtime)
- Some rare opcodes not implemented (sufficient for most games)

## Resources

- [CHIP-8 Technical Reference](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM)
- [Cowgod's CHIP-8 Reference](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM)
- [CHIP-8 ROM Collection](https://github.com/kripod/chip8-roms)

## License

This project is open source and available for educational purposes.

## Author

dlxky
