package engine.execution;

import engine.program.SProgram;

public interface ProgramExpander {
    SProgram expand(SProgram program, int degree);

}
