package engine.execution.rules;

import engine.execution.naming.NameAllocator;
import engine.instruction.SInstruction;
import engine.label.Label;

import java.util.List;

public interface ExpansionRule {
    boolean supports(SInstruction ins);
    List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names);
}
