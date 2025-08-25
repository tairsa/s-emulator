package engine.execution.rules;

import engine.instruction.SInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.JumpZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.execution.naming.NameAllocator;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

import java.util.List;

public final class JumpZeroRule implements ExpansionRule {
    @Override public boolean supports(SInstruction ins) { return ins instanceof JumpZeroInstruction; }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var jz = (JumpZeroInstruction) ins;
        var L1 = names.freshLabel();


        return List.of(
                new JumpNotZeroInstruction(jz.lineLabel(), jz.variable(), L1),
                new GotoLabelInstruction(FixedLabel.EMPTY,jz.target()),
                new NeutralInstruction(L1, Variable.ofToken("y"))
        );
    }
}
