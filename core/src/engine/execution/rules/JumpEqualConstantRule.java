package engine.execution.rules;

import engine.execution.naming.NameAllocator;
import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.AssignmentInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.JumpEqualConstantInstruction;
import engine.instruction.synthetic.JumpZeroInstruction;
import engine.label.FixedLabel;
import engine.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public final class JumpEqualConstantRule implements ExpansionRule {
    @Override
    public boolean supports(SInstruction i) {
        return i instanceof JumpEqualConstantInstruction;
    }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var jc = (JumpEqualConstantInstruction) ins;
        var z = names.freshTemp();
        var L1 = names.freshLabel();

        List<SInstruction> out = new ArrayList<>();
        out.add(new AssignmentInstruction(FixedLabel.EMPTY, z, jc.variable()));
        for (int k = 0; k < jc.getConstant(); k++) {
            out.add(new JumpZeroInstruction(FixedLabel.EMPTY, z, L1));
            out.add(new DecreaseInstruction(FixedLabel.EMPTY, z));
        }
        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, z, L1));
        out.add(new GotoLabelInstruction(FixedLabel.EMPTY, jc.target()));
        out.add(new NeutralInstruction(L1, Variable.ofToken("y")));
        return out;
    }
}
