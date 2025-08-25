package engine.execution.rules;

import engine.instruction.SInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.synthetic.ConstantAssignmentInstruction;
import engine.instruction.synthetic.ZeroVariableInstruction;
import engine.execution.naming.NameAllocator;
import engine.label.FixedLabel;

import java.util.ArrayList;
import java.util.List;

public final class ConstantAssignmentRule implements ExpansionRule {
    @Override public boolean supports(SInstruction ins) { return ins instanceof ConstantAssignmentInstruction; }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var c = (ConstantAssignmentInstruction) ins;
        List<SInstruction> out = new ArrayList<>();
        out.add(new ZeroVariableInstruction(c.lineLabel(), c.variable())); // להשאיר כסינטטי!
        for (int k = 0; k < c.getConstant(); k++) {
            out.add(new IncreaseInstruction(FixedLabel.EMPTY, c.variable()));
        }
        return out;
    }
}
