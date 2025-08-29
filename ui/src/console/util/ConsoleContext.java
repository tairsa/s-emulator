package console.util;

import console.state.ConsoleState;
import engine.execution.ProgramExecutor;
import engine.instruction.SInstruction;
import engine.instruction.InstructionKind;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.program.SProgram;
import engine.variable.Variable;


import java.util.*;
import java.util.function.Function;


/**
 * Holds shared state (program, history, expander, parser, executor factory) and common **logic-only** utilities.
 * This class must not perform any I/O (no printing / reading from console).
 */
public final class ConsoleContext {
    private final List<HistoryItem> history;
    private final engine.execution.SimpleProgramExpander expander;
    private final engine.io.ProgramParser parser;
    private final Function<SProgram, ProgramExecutor> executorFactory;
    private ConsoleState state = new ConsoleState();

    public ConsoleContext(engine.io.ProgramParser parser,
                          engine.execution.SimpleProgramExpander expander,
                          Function<SProgram, ProgramExecutor> executorFactory) {
        this.parser = parser;
        this.expander = expander;
        this.executorFactory = executorFactory;
        this.history = new ArrayList<>();
    }


    public Optional<SProgram> program() {
        return Optional.ofNullable(state.program());   // <-- קורא מה-ConsoleState
    }

    /** Sets a new program and clears previous run history to keep state consistent. */
    public void setProgram(engine.program.SProgram p, byte[] xmlBytes, String path) {
        state.setProgram(p, xmlBytes, path);
        this.history.clear();

    }



    public engine.io.ProgramParser parser() { return parser; }
    public engine.execution.SimpleProgramExpander expander() { return expander; }
    public Function<SProgram, ProgramExecutor> executorFactory() { return executorFactory; }
    public List<HistoryItem> history() { return history; }


    // ===== Logic utilities (no I/O) =====
    public static boolean hasExpandable(SProgram p, engine.execution.SimpleProgramExpander expander) {
        for (SInstruction ins : p.instructions()) {
            if (ins.kind() == InstructionKind.SYNTHETIC) return true;
        }
        return false;
    }

    public void recordRun(int degree, java.util.List<Long> inputs, long y, long cycles) {
        int id = history.size() + 1;
        var h = new HistoryItem(id, degree, new java.util.ArrayList<>(inputs), y, cycles);
        history.add(h);
        state.history().add(new console.state.ConsoleState.HistoryItemDTO(id, degree,
                new java.util.ArrayList<>(inputs), y, cycles));
    }


    public void replaceState(ConsoleState s) {
        this.state = (s != null) ? s : new console.state.ConsoleState();
//        this.history.clear();
        for (var d : this.state.history()) {
            this.history.add(new HistoryItem(
                    d.id, d.degree, new java.util.ArrayList<>(d.inputs), d.y, d.cycles));
        }

    }

    /** Compute max expansion degree that yields a program with no synthetic instructions. */
    public int maxExpansionDegree(SProgram p) {
        for (int d = 0; d <= 6; d++) {
            SProgram cand = expander.expand(p, d);
            if (!hasExpandable(cand, expander)) return d;
        }
        return 6;
    }


    public SProgram expandToDegree(SProgram p, int degree) { return (degree <= 0) ? p : expander.expand(p, degree); }


    public static List<String> collectInputsInOrder(SProgram prog) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (SInstruction ins : prog.instructions()) {
            addIfX(seen, ins.variable());
            if (ins instanceof engine.instruction.synthetic.JumpEqualVariableInstruction jev) addIfX(seen, jev.getOther());
            if (ins instanceof engine.instruction.synthetic.AssignmentInstruction asg) {
                addIfX(seen, asg.getFrom()); addIfX(seen, asg.variable());
            }
        }
        return new ArrayList<>(seen);
    }


    private static void addIfX(Set<String> seen, Variable v) {
        if (v == null) return; String t = v.toString(); if (t.startsWith("x")) seen.add(t);
    }

    public ConsoleState state() { return this.state; }




    public static List<String> collectLabelsInOrder(SProgram prog) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (SInstruction ins : prog.instructions()) {
            Label ll = ins.lineLabel();
            if (ll != null && ll != FixedLabel.EMPTY && ll != FixedLabel.EXIT) seen.add(ll.labelName());
        }
        if (programUsesExit(prog)) seen.add("EXIT");
        return new ArrayList<>(seen);
    }


    private static boolean programUsesExit(SProgram prog) {
        for (SInstruction ins : prog.instructions()) if (ins instanceof engine.instruction.HasTarget jt && jt.target() == FixedLabel.EXIT) return true;
        return false;
    }


    /** Immutable record of one run. */
    public static final class HistoryItem {
        public final int id; public final int degree; public final List<Long> inputs; public final long y; public final long cycles;
        public HistoryItem(int id, int degree, List<Long> inputs, long y, long cycles) { this.id=id; this.degree=degree; this.inputs=inputs; this.y=y; this.cycles=cycles; }
    }



}