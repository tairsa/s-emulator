package engine.instruction.basic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;

public final class NeutralInstruction extends AbstractInstruction {
    public NeutralInstruction(Label lineLabel) { super(lineLabel, null, 0, InstructionKind.BASIC); }
    @Override public Label execute(ExecutionContext ctx) {
        ctx.addCycles(cycles);
        return FixedLabel.EMPTY;
    }
    @Override public String render() { return "NEUTRAL"; }
}
