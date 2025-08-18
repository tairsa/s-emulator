package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.HasTarget;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class JumpEqualVariableInstruction extends AbstractInstruction implements HasTarget {
    public Variable getOther() {
        return other;
    }

    private final Variable other;
    private final Label target;
    public JumpEqualVariableInstruction(Label lineLabel, Variable v, Variable other, Label target) {
        super(lineLabel, v, 2, InstructionKind.SYNTHETIC);
        this.other = other;
        this.target = target == null ? FixedLabel.EXIT : target;
    }

    @Override public Label target() { return target; }
    @Override public Label execute(ExecutionContext ctx) {
        long a = ctx.get(variable());
        long b = ctx.get(other);
        ctx.addCycles(cycles);
        return (a == b) ? target : FixedLabel.EMPTY;
    }
    @Override public String render() {
        return "IF " + variable().name() + " = " + other.name() + " GOTO " + target.labelName();
    }
}
