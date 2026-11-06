# Mitochondrial Engine

The Mitochondrial Engine is a Native Java PC Port of [Parasite Eve](https://en.wikipedia.org/wiki/Parasite_Eve_(video_game)) on the PS1.

It's written on top of the [UPF](/upf_readme.md), or Untitled Porting Framework. The framework is in active development alongside ME. The [Parasite Eve Decomp](https://github.com/khasinski/parasite-eve-decomp) was used to create the port, by translating the C code to Java. 


# Current Status

The following is implemented and working:
- Disc and Data verification and ripping
- Rooms and Rendering
    - Load rooms by any map number
    - Pre rendered backgrounds are drawn, animated tiles (interactable animated tiles) work.
    - Viewport moving, swapping
    - Camera freezing and Unfreezing
    - Room changes via scripts
    - 3D Models with Skeletal Animation. 
    - 4bpp/8bpp texture loading with original palettes
    - See-through surfaces
    - Attaching Models to one another for overlays, following
    - Models and rooms now have lighting
- Player 
    - Player Movement, walk, run and idle with original speeds (WASD + LShift)
- NPC
    - Per actor speed scale
    - Scripted Walking, Turning toward point, facing an actor, snapping to animation anchors
- Collision
    - Floor meshes with real heights and sloped floors
    - Circle against edges and slide against walls like the original game does
    - Scripts that block and unblock individual floor triangles
    - Actors blocking each other 
- Interaction
    - Touching/Nearly Touching an actor starts their contact script
    - Reading the controller from scripts (held, just pressed, just released) (F = 'x')
- Dialogue
    - "Message Boxes" with typed out spaces and per-section timing
    - Several Messages open at once, and choice prompts with a cursor (W/S to move, F to pick)
    - Hand Pointer shows up for choices
    - Controller Button Icons (Hasn't been seen in testing yet, but is implemented in)
- Scripting
    - A VM that runs the original room scripts: Currently 90 Op Handlers are implemented

The readme_images folder contains progress images.

![dialogue_test](readme_images/dialogue_test_wip.png)
![opera_scene_bug](readme_images/opera_hall_scene_bug.png)
![opera_scene_fix](readme_images/opera_hall_scene_fixed.png)

# System Requirements

Ram: Sure. 

Graphics Card: Anything that supports OpenGL 4.6

# Build Instructions

## Linux
To be included

## MacOS
See below

## Windows
See below

# FAQ

**Q:** Was AI used at all in the making of this port? 

**A:** As an assistant only. I use Claude, but I do not allow Claude to write any code. It can provide pseudo code, but can't write to any files, nor provide compilable Java Code. I started this port to do two things. Build a modular porting framework for different types of Console PC Ports, and to learn more about game development and design. 

**Q:** Do you provide the assets?

**A:** No. You'll need to have a *legal* copy of Parasite Eve 1 for the PS1. Both discs are required, and the Mitochondrial Engine will rip the assets from the disc, repacking them into a `.udh` file, or Untitled Porting Data Holder file. 

**Q:** Is there a Windows Binary?

**A:** Not built. If you clone the project, you can provide the Windows natives for LWJGL3, but at this point in time, I'm not sure if there is Linux semantics pushed in the code. I use Linux as my daily driver, not Windows. 

**Q:** Will there be a MacOS Binary?

**A:** No. The PS1 Shaders are utilizing OpenGL 4.6, and MacOS dropped support for OpenGL during version 4.1.

**Q:** Can I use UPF to port a game myself?

**A:** Technically yes. Parasite Eve's code that isn't shared by every PS1 game is contained directly inside a `pe` package in both the `upf-api` and `upf-core` projects. If you delete that, you can create a new package for the game you want to port and get started. UPF provides Jar Plugin Loading, Input, OpenGL Graphic Output. It's currently being built out, with Parasite Eve building out some of the common PS1 foundations. Ports and Plugins/Mods inherit the license as they utilize the `upf-api`. 

**Q:** What can I do with modding?

**A:** When the port is functional, and has been modernized you should be able to create custom items, weapons, armors, skills, enemies, etc. But currently, nothing. The current scope right now is to port the code, then once it's functional, modernize the code so it's no longer using PS1 style functionality, and uses more modern game dev approach. Allowing for finer grain control over the data, while still functioning the way the game played on the PS1. 

# License
"Copyright (C) 2026 Azraein". The license will be moved to GPLv3 in the future.

"Parasite Eve" Game Assets are not covered by the license and are not redistributed.

Do not use the names "Mitochondrial Engine" or "UPF" as your own in forks. 

The license may be subjected to changes. The Parasite Eve decomp is still choosing a license, and when they update their license, some code in the project may be subject to their new license. 

# References

 - [Severed Chains - Legend of Dragoon PC Port](https://github.com/Legend-of-Dragoon-Modding/Severed-Chains)
 - [Parasite Eve Decomp](https://github.com/khasinski/parasite-eve-decomp)
