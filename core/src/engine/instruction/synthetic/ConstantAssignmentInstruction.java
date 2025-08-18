package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class ConstantAssignmentInstruction extends AbstractInstruction {
    public long getConstant() {
        return constant;
    }

    private final long constant;
    public ConstantAssignmentInstruction(Label lineLabel, Variable to, long constant) {
        super(lineLabel, to, 2, InstructionKind.SYNTHETIC);
        this.constant = Math.max(0, constant);
    }
    @Override public Label execute(ExecutionContext ctx) {
        ctx.set(variable(), constant);
        ctx.addCycles(cycles);
        return FixedLabel.EMPTY;
    }
    @Override public String render() { return variable().name() + " <- " + constant; }
}
