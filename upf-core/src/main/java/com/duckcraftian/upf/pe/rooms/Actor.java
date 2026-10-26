package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.pe.gfx.PSXAnimation;
import com.duckcraftian.upf.pe.gfx.PSXModel;

public record Actor(PSXModel model, PSXAnimation animation, int[] position, int yaw) {
}