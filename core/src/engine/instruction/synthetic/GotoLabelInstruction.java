package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.HasTarget;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;

public final class GotoLabelInstruction extends AbstractInstruction implements HasTarget {
    private final Label target;
    public GotoLabelInstruction(Label lineLabel, Label target) {
        super(lineLabel, null, 1, InstructionKind.SYNTHETIC);
        this.target = target == null ? FixedLabel.EXIT : target;
    }
    @Override public Label target() { return target; }
    @Override public Label execute(ExecutionContext ctx) {
        ctx.addCycles(cycles);
        return target;
    }
    @Override public String render() { return "GOTO " + target.labelName(); }
}
