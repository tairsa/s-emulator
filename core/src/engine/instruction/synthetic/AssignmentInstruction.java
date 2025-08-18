package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class AssignmentInstruction extends AbstractInstruction {
    public Variable getFrom() {
        return from;
    }

    private final Variable from;
    public AssignmentInstruction(Label lineLabel, Variable to, Variable from) {
        super(lineLabel, to, 4, InstructionKind.SYNTHETIC);
        this.from = from;
    }
    @Override public Label execute(ExecutionContext ctx) {
        long v = ctx.get(from);
        ctx.set(variable(), v);
        ctx.addCycles(cycles);
        return FixedLabel.EMPTY;
    }
    @Override public String render() { return variable().name() + " <- " + from.name(); }
}
