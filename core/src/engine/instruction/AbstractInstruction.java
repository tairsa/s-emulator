package engine.instruction;

import engine.label.FixedLabel;
import engine.label.Label;
import engine.variable.Variable;


public abstract class AbstractInstruction implements SInstruction {
    protected final Label lineLabel;     // label declared ON this line (can be EMPTY)
    protected final Variable variable;   // may be null
    protected final int cycles;
    protected final InstructionKind kind;

    protected AbstractInstruction(Label lineLabel, Variable variable, int cycles, InstructionKind kind) {
        this.lineLabel = lineLabel == null ? FixedLabel.EMPTY : lineLabel;
        this.variable = variable;
        this.cycles = cycles;
        this.kind = kind;
    }
    @Override public Label lineLabel() { return lineLabel; }
    @Override public Variable variable() { return variable; }
    @Override public int cycles() { return cycles; }
    @Override public InstructionKind kind() { return kind; }
}