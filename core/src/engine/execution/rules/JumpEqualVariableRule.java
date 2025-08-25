package engine.execution.rules;

import engine.execution.naming.NameAllocator;
import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.AssignmentInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.JumpEqualVariableInstruction;
import engine.instruction.synthetic.JumpZeroInstruction;
import engine.label.FixedLabel;
import engine.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public final class JumpEqualVariableRule implements ExpansionRule {
    @Override
    public boolean supports(SInstruction i) {
        return i instanceof JumpEqualVariableInstruction;
    }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var jv = (JumpEqualVariableInstruction) ins;
        var z1 = names.freshTemp();
        var z2 = names.freshTemp();
        var L2 = names.freshLabel();
        var L3 = names.freshLabel();
        var Lend = names.freshLabel();

        List<SInstruction> out = new ArrayList<>();
        out.add(new AssignmentInstruction(FixedLabel.EMPTY, z1, jv.variable()));
        out.add(new AssignmentInstruction(FixedLabel.EMPTY, z2, jv.getOther()));

        out.add(new JumpZeroInstruction(L2, z1, L3));
        out.add(new JumpZeroInstruction(FixedLabel.EMPTY, z2, Lend));
        out.add(new DecreaseInstruction(FixedLabel.EMPTY, z1));
        out.add(new DecreaseInstruction(FixedLabel.EMPTY, z2));
        out.add(new GotoLabelInstruction(FixedLabel.EMPTY, L2));
        out.add(new JumpZeroInstruction(L3, z2, jv.target()));
        out.add(new NeutralInstruction(Lend, Variable.ofToken("y")));
        return out;
    }
}
