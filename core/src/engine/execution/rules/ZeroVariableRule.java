package engine.execution.rules;

import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.synthetic.ZeroVariableInstruction;
import engine.execution.naming.NameAllocator;
import engine.label.FixedLabel;
import engine.label.Label;

import java.util.List;

public final class ZeroVariableRule implements ExpansionRule {
    @Override public boolean supports(SInstruction ins) { return ins instanceof ZeroVariableInstruction; }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var z = (ZeroVariableInstruction) ins;
        Label L = names.freshLabel();
        return List.of(
                new DecreaseInstruction(L, z.variable()),                 // L: v <- v - 1
                new JumpNotZeroInstruction(FixedLabel.EMPTY, z.variable(), L) // IF v != 0 GOTO L
        );
    }
}
