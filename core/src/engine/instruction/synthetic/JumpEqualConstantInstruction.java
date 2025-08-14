package engine.instruction.synthetic;

import engine.execution.ExecutionContext;
import engine.instruction.AbstractInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;

public final class JumpEqualConstantInstruction extends AbstractInstruction {
    private final long constant;
    private final Label target;
    public JumpEqualConstantInstruction(Label lineLabel, Variable v, long constant, Label target) {
        super(lineLabel, v, 2, InstructionKind.SYNTHETIC);
        this.constant = Math.max(0, constant);
        this.target = target == null ? FixedLabel.EXIT : target;
    }
    @Override public Label execute(ExecutionContext ctx) {
        long val = ctx.get(variable());
        ctx.addCycles(cycles);
        return (val == constant) ? target : FixedLabel.EMPTY;
    }
    @Override public String render() {
        return "IF " + variable().name() + " = " + constant + " GOTO " + target.labelName();
    }
}
