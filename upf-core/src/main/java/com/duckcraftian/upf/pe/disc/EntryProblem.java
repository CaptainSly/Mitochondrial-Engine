package com.duckcraftian.upf.pe.disc;

public record EntryProblem(String path, EntryProblemKind kind) {

    public enum EntryProblemKind {
        MISSING, WRONG_LAYOUT, BAD_HASH
    }


}