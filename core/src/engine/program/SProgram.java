package engine.program;

import engine.instruction.SInstruction;
import engine.variable.Variable;

import java.util.List;

public interface SProgram {
    String name();
    List<SInstruction> instructions();
    void validate();       // label existence, duplicates, etc.
    int totalCycles();     // sum of cycles (static)

}
