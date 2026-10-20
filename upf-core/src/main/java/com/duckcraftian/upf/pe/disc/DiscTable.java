package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.ps1.disc.RawDiscEntry;

public record DiscTable(PERipper.DiscId discId, String executableName, RawDiscEntry[] rows) {
}