package engine.execution;

import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;
import engine.program.SProgram;
import engine.variable.Variable;
import engine.variable.VariableType;

import java.util.*;

public final class ExecutionContext {
    private final Map<Variable, Long> values = new HashMap<>();
    private final Map<String, Integer> labelToIndex = new HashMap<>();
    private final List<Long> providedInputs = new ArrayList<>();   // ← חדש
    private int ip;
    private long cycles;

    public ExecutionContext() { this.ip = 0; this.cycles = 0; }

    public void initializeInputs(Long... inputs) {
        // y = 0
        values.put(new Variable("y", VariableType.RESULT), 0L);
        // x_i from inputs (negative → 0)
        for (int i = 0; i < inputs.length; i++) {
            long v = inputs[i] == null ? 0L : Math.max(0L, inputs[i]);
            providedInputs.add(v);
            values.put(new Variable("x" + (i + 1), VariableType.INPUT), v);
        }
        // z? → 0 on first access (lazy via get)
    }
    public List<Long> providedInputs() {
        return java.util.Collections.unmodifiableList(providedInputs);
    }

    public void buildLabelIndex(SProgram program) {
        List<SInstruction> list = program.instructions();
        for (int i = 0; i < list.size(); i++) {
            Label l = list.get(i).lineLabel();
            if (l != null && l != FixedLabel.EMPTY) {
                labelToIndex.put(l.labelName(), i);
            }
        }
    }

    public long get(Variable v) {
        if (v == null) return 0L;
        return values.computeIfAbsent(v, vv -> 0L);
    }

    public void set(Variable v, long val) { values.put(v, Math.max(0L, val)); }
    public void inc(Variable v) { set(v, get(v) + 1); }
    public void dec(Variable v) { set(v, Math.max(0L, get(v) - 1)); }

    public int resolveLabel(Label l) {
        if (l == null || l == FixedLabel.EMPTY) return -2; // fallthrough
        if (l == FixedLabel.EXIT) return -1;               // stop
        if (l instanceof UserLabel) {
            Integer idx = labelToIndex.get(l.labelName());
            if (idx == null) throw new IllegalStateException("Unknown label: " + l.labelName());
            return idx;
        }
        throw new IllegalArgumentException("Unsupported label: " + l);
    }

    public int ip() { return ip; }
    public void setIp(int newIp) { this.ip = newIp; }
    public long cycles() { return cycles; }
    public void addCycles(int c) { this.cycles += c; }
    public Map<Variable, Long> snapshot() { return Collections.unmodifiableMap(values); }
}
