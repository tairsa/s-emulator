package engine.execution.rules;

import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.AssignmentInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.ZeroVariableInstruction;
import engine.execution.naming.NameAllocator;
import engine.label.FixedLabel;
import engine.variable.Variable;


import java.util.ArrayList;
import java.util.List;

public final class AssignmentRule implements ExpansionRule {
    @Override public boolean supports(SInstruction ins) { return ins instanceof AssignmentInstruction; }

    @Override
    public List<SInstruction> expandOneStep(SInstruction ins, NameAllocator names) {
        var a = (AssignmentInstruction) ins;
        var to = a.variable();
        var from = a.getFrom();
        var z = names.freshTemp();

        var L1 = names.freshLabel();
        var L2 = names.freshLabel();
        var L3 = names.freshLabel();
        List<SInstruction> out = new ArrayList<>();

        // היה: out.addAll(new ZeroVariableRule().expandOneStep(...));
        out.add(new ZeroVariableInstruction(FixedLabel.EMPTY, to)); // להשאיר כסינטטי

        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, from, L1));

        out.add(new GotoLabelInstruction(FixedLabel.EMPTY, L3));

        out.add(new DecreaseInstruction(L1, from));
        out.add(new IncreaseInstruction(FixedLabel.EMPTY, z));
        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, from, L1));

        out.add(new DecreaseInstruction(L2, z));
        out.add(new IncreaseInstruction(FixedLabel.EMPTY, to));
        out.add(new IncreaseInstruction(FixedLabel.EMPTY, from));
        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, z, L2));

        out.add(new NeutralInstruction(L3, to));
        return out;
    }
}
