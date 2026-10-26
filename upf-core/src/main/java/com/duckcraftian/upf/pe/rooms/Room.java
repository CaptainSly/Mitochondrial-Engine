package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.ps1.data_types.PSXImage;

import java.util.List;

public record Room(List<BackgroundTile> tiles, List<PSXImage> images, List<PSXViewport> viewports, List<Actor> actors) {
}