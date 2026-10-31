# Untitled Porting Framework

The Untitled Porting Framework, or UPF, is a LWJGL3 Framework of sorts that utilizes SDL and Java 25 to facilitate in the porting of the following consoles.
- PlayStation 1
- GameBoy Advance
- GameCube
- Nintendo DS

# Current Status

The current status of the UPF is as follows:
- Input exists and is wired into the main "Engine" runner.
- ShaderPrograms, Framebuffers, Meshes, Textures, TextureAtlases, can be created and used. 
- A Plugin System does exist, and does *work* to an extent. Most the system isn't hooked up beyond the initalizers, as you'll need to provide and extend anyway while creating your port/game. 
- A replacable Game class, that's hooked up and ready for creating your project. 
- PS1 Disc Interfacing

What is missing:
- GBA, GC, NDS abstractions, helpers, and systems.
- Most of the PS1 abstractions, helpers, and systems.
- Gamepad Support (In Progress)