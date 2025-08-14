package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class JumpZeroInstruction extends AbstractInstruction {
    private final Label target;
    public JumpZeroInstruction(Label lineLabel, Variable v, Label target) {
        super(lineLabel, v, 2, InstructionKind.SYNTHETIC);
        this.target = target == null ? FixedLabel.EXIT : target;
    }
    @Override public Label execute(ExecutionContext ctx) {
        long val = ctx.get(variable());
        ctx.addCycles(cycles);
        return (val == 0) ? target : FixedLabel.EMPTY;
    }
    @Override public String render() { return "IF " + variable().name() + " = 0 GOTO " + target.labelName(); }
}
