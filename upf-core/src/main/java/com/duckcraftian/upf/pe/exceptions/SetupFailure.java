package com.duckcraftian.upf.pe.exceptions;

import com.duckcraftian.upf.pe.disc.problems.SetupProblem;

import java.util.List;

public class SetupFailure extends RuntimeException {

    private List<SetupProblem> problems;

    public SetupFailure(List<SetupProblem> problems) {
        this.problems = problems;
        super(problems.toString());
    }

    public List<SetupProblem> getProblems() {
        return problems;
    }
}