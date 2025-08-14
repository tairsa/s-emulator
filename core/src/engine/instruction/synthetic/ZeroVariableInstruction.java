package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class ZeroVariableInstruction extends AbstractInstruction {
    public ZeroVariableInstruction(Label lineLabel, Variable v) {
        super(lineLabel, v, 1, InstructionKind.SYNTHETIC);
    }
    @Override public Label execute(ExecutionContext ctx) {
        ctx.set(variable(), 0);
        ctx.addCycles(cycles);
        return FixedLabel.EMPTY;
    }
    @Override public String render() { return variable().name() + " <- 0"; }
}
