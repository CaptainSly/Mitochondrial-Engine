package com.duckcraftian.upf.pe.disc.problems;

import java.util.List;
import java.util.Optional;

public record SetupProblem(String path, SetupProblemKind kind, Optional<List<EntryProblem>> problems) {

    public enum SetupProblemKind {
        FILE_NOT_FOUND, WRONG_DISC, BAD_ENTRIES, NOT_RAW_IMAGE
    }

}