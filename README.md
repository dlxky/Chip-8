# CHIP-8 Emulator 🕹️

A Java implementation of a CHIP-8 interpreter/emulator. Currently in development with basic functionality for ROM execution, display rendering, and input handling.

## Features ✅

### Implemented
- **Core CPU Operations**:
  - 00E0 (Clear screen)
  - 1NNN (Jump)
  - 6XNN (Set register VX)
  - DXYN (Draw sprite)
  - ANNN (Set index register I)
  - EX9E/EXA1 (Skip if key pressed/not pressed)
  - FX29 (Font character address)
- **64x32 Pixel Display** with 10x scaling
- **Basic Input Handling** (partial key mapping)
- **ROM Loading** from binary files
- **Swing-based GUI**

### Planned Features 🚧
- Full opcode implementation
- Sound support (buzzer)
- Proper timer synchronization
- Complete keypad mapping
- Configurable clock speed
- Save/Load state functionality

## Installation ⚙️

### Requirements
- JDK 17+
- CHIP-8 ROM file (e.g., [PONG](https://github.com/dmatlack/chip8/tree/master/roms))

### Steps
1. Clone repository:
```bash
git clone https://github.com/yourusername/chip8-emulator.git
cd chip8-emulator
