package engine.execution.rules;

import engine.instruction.SInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.execution.naming.NameAllocator;

import java.util.List;

public final class GotoLabelRule implements ExpansionRule {
    @Override public boolean supports(SInstruction ins) { return ins instanceof GotoLabelInstruction; }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var g = (GotoLabelInstruction) ins;
        var tmp = names.freshTemp();
        return List.of(
                new IncreaseInstruction(g.lineLabel(), tmp),
                new JumpNotZeroInstruction(NameAllocator.empty(), tmp, g.target())
        );
    }
}
