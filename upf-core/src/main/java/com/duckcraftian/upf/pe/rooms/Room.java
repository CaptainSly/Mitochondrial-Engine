package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.ps1.data_types.PSXImage;

import java.util.List;
import java.util.Map;

public record Room(List<BackgroundTile> tiles, List<PSXImage> images, List<PSXViewport> viewports,
                   Map<Integer, PEModel> models) {
}