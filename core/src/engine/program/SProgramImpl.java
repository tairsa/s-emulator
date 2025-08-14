package engine.program;

import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SProgramImpl implements SProgram {
    private final String name;
    private final List<SInstruction> instructions;

    public SProgramImpl(String name, List<SInstruction> instructions) {
        this.name = Objects.requireNonNull(name);
        this.instructions = List.copyOf(Objects.requireNonNull(instructions));
    }
    @Override public String name() { return name; }
    @Override public List<SInstruction> instructions() { return instructions; }

    @Override public void validate() {
        Set<String> declared = new HashSet<>();
        for (SInstruction ins : instructions) {
            Label l = ins.lineLabel();
            if (l != null && l != FixedLabel.EMPTY) {
                if (!(l instanceof UserLabel)) throw new IllegalArgumentException("Illegal label: " + l.labelName());
                if (!declared.add(l.labelName())) throw new IllegalArgumentException("Duplicate label: " + l.labelName());
            }
        }
        // targets checked at parse time; here basic sanity
    }

    @Override public int totalCycles() {
        int sum = 0;
        for (SInstruction i : instructions) sum += i.cycles();
        return sum;
    }
}
