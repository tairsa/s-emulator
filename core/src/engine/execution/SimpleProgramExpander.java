package engine.execution;

import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;

// BASIC
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;

// SYNTHETIC
import engine.instruction.synthetic.AssignmentInstruction;
import engine.instruction.synthetic.ConstantAssignmentInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.JumpEqualConstantInstruction;
import engine.instruction.synthetic.JumpEqualVariableInstruction;
import engine.instruction.synthetic.JumpZeroInstruction;
import engine.instruction.synthetic.ZeroVariableInstruction;

import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;
import engine.program.SProgram;
import engine.program.SProgramImpl;
import engine.variable.Variable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Expander רקורסיבי במעברים: בכל "דרגה" מבצעים צעד-הרחבה אחד (oneStep) לכל שורה,
 * ושומרים ייחוס (lineage) להורה לצורך הדפסה עם <<<.
 *
 * תבניות oneStep:
 *  - ZERO / CONST / GOTO / JUMP_ZERO → נפתחות לבסיס בצעד אחד.
 *  - ASSIGNMENT → דורש בסה"כ 2 דרגות.
 *  - JUMP_EQUAL_* → משתמש ב-ASSIGNMENT ולכן דורש בסה"כ 3 דרגות.
 */
public final class SimpleProgramExpander implements ProgramExpander {

    // ===== Lineage לצורך <<< =====
    public static final class TraceFrame {
        public final int id;        // # שורה בדרגה הקודמת
        public final char kind;     // 'S' או 'B'
        public final String label;  // שם תווית או ""
        public final String text;   // ins.render()
        public final long cycles;   // ins.cycles()
        public TraceFrame(int id, char kind, String label, String text, long cycles) {
            this.id = id; this.kind = kind; this.label = label; this.text = text; this.cycles = cycles;
        }
    }

    private SProgram lastProgram = null;
    private IdentityHashMap<SInstruction, List<TraceFrame>> lastLineage = new IdentityHashMap<>();

    public List<TraceFrame> lineageOf(SInstruction ins) {
        List<TraceFrame> l = lastLineage.get(ins);
        return (l == null) ? List.of() : l;
    }

    // ===== API =====
    @Override
    public SProgram expand(SProgram program, int degree) {
        if (degree <= 0) {
            this.lastProgram = program;
            this.lastLineage = new IdentityHashMap<>();
            return program;
        }

        SProgram cur = program;
        IdentityHashMap<SInstruction, List<TraceFrame>> prevLineage = new IdentityHashMap<>();

        // מעבר אחד לכל דרגה
        for (int pass = 1; pass <= degree; pass++) {
            NameGen names = new NameGen(cur.instructions());
            List<SInstruction> in = cur.instructions();
            List<SInstruction> next = new ArrayList<>();
            IdentityHashMap<SInstruction, List<TraceFrame>> nextLineage = new IdentityHashMap<>();

            for (int idx = 0; idx < in.size(); idx++) {
                SInstruction ins = in.get(idx);

                String lblTxt = (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY)
                        ? "" : ins.lineLabel().labelName();
                char kindCh = (ins.kind() == InstructionKind.SYNTHETIC) ? 'S' : 'B';
                TraceFrame parent = new TraceFrame(idx + 1, kindCh, lblTxt, ins.render(), ins.cycles());

                List<TraceFrame> parentChain = prevLineage.get(ins);
                if (parentChain == null) parentChain = List.of();

                List<SInstruction> step = oneStep(ins, names);
                if (step == null) step = List.of();
                // חשוב: להעתיק לרשימה ניתנת לשינוי לפני set/remove
                step = new ArrayList<>(step);
                step.removeIf(Objects::isNull);

                if (step.isEmpty()) {
                    next.add(ins);
                    nextLineage.put(ins, parentChain);
                } else {
                    Label orig = ins.lineLabel();
                    if (orig != null && orig != FixedLabel.EMPTY) {
                        step.set(0, cloneWithLabel(step.get(0), orig));
                    }
                    for (SInstruction child : step) {
                        next.add(child);
                        ArrayList<TraceFrame> chain = new ArrayList<>();
                        chain.add(parent);
                        chain.addAll(parentChain);
                        nextLineage.put(child, chain);
                    }
                }
            }

            cur = new SProgramImpl(cur.name(), next);
            cur.validate();
            prevLineage = nextLineage;
        }

        this.lastProgram = cur;
        this.lastLineage = prevLineage;
        return cur;
    }

    // ===== oneStep =====
    private List<SInstruction> oneStep(SInstruction i, NameGen names) {
        if (i.kind() == InstructionKind.BASIC) return null;

        // ZERO v  →  JNZ v L1 ; L1: DEC v ; JNZ v L1
        if (i instanceof ZeroVariableInstruction z) {
            Label L1 = names.freshLabel();
            return List.of(
                    new JumpNotZeroInstruction(safeLabel(z.lineLabel()), z.variable(), L1),
                    new DecreaseInstruction(L1, z.variable()),
                    new JumpNotZeroInstruction(FixedLabel.EMPTY, z.variable(), L1)
            );
        }

        // CONST v <- c  →  ZERO v  +  c×INC v
        if (i instanceof ConstantAssignmentInstruction c) {
            List<SInstruction> out = new ArrayList<>();
            out.addAll(oneStep(new ZeroVariableInstruction(safeLabel(c.lineLabel()), c.variable()), names));
            for (int k = 0; k < c.getConstant(); k++) {
                out.add(new IncreaseInstruction(FixedLabel.EMPTY, c.variable()));
            }
            return out;
        }

        // GOTO T  →  tmp<-1 ; JNZ tmp T
        if (i instanceof GotoLabelInstruction g) {
            Variable tmp = names.freshTemp();
            List<SInstruction> out = new ArrayList<>();
            out.addAll(oneStep(new ConstantAssignmentInstruction(safeLabel(g.lineLabel()), tmp, 1), names));
            out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, tmp, g.target()));
            return out;
        }

        // JZ v -> T  →  JNZ v Lskip ; GOTO T ; Lskip: NEUTRAL(v)
        if (i instanceof JumpZeroInstruction jz) {
            Label Lskip = names.freshLabel();
            List<SInstruction> out = new ArrayList<>();
            out.add(new JumpNotZeroInstruction(safeLabel(jz.lineLabel()), jz.variable(), Lskip));
            out.addAll(oneStep(new GotoLabelInstruction(FixedLabel.EMPTY, jz.target()), names));
            out.add(new NeutralInstruction(Lskip, jz.variable()));
            return out;
        }

        // ASSIGNMENT to <- from  (צעד ראשון: ביניים; הבא יהפוך לבסיס)
        if (i instanceof AssignmentInstruction a) {
            Variable to = a.variable();
            Variable from = a.getFrom();
            Variable z = names.freshTemp();

            Label Lcopy = names.freshLabel();
            Label Lrestore = names.freshLabel();
            Label Lend = names.freshLabel();

            List<SInstruction> out = new ArrayList<>();
            out.add(new ZeroVariableInstruction(FixedLabel.EMPTY, to));

            out.add(new JumpZeroInstruction(FixedLabel.EMPTY, from, Lrestore));
            out.add(new DecreaseInstruction(Lcopy, from));
            out.add(new IncreaseInstruction(FixedLabel.EMPTY, to));
            out.add(new IncreaseInstruction(FixedLabel.EMPTY, z));
            out.add(new GotoLabelInstruction(FixedLabel.EMPTY, Lcopy));

            out.add(new JumpZeroInstruction(Lrestore, z, Lend));
            out.add(new DecreaseInstruction(FixedLabel.EMPTY, z));
            out.add(new IncreaseInstruction(FixedLabel.EMPTY, from));
            out.add(new GotoLabelInstruction(FixedLabel.EMPTY, Lrestore));

            out.add(new NeutralInstruction(Lend, to));
            return out;
        }

        // JEQ v==c -> T  (משתמש ב-ASSIGNMENT לזמני ואז JZ)
        if (i instanceof JumpEqualConstantInstruction jc) {
            Variable z = names.freshTemp();
            List<SInstruction> out = new ArrayList<>();
            out.add(new AssignmentInstruction(FixedLabel.EMPTY, z, jc.variable()));
            for (int k = 0; k < jc.getConstant(); k++) {
                out.add(new DecreaseInstruction(FixedLabel.EMPTY, z));
            }
            out.add(new JumpZeroInstruction(FixedLabel.EMPTY, z, jc.target()));
            return out;
        }

        // JEQ a==b -> T  (שתי השמות לזמניים, לולאת הנמכה, בדיקה)
        if (i instanceof JumpEqualVariableInstruction jv) {
            Variable z1 = names.freshTemp();
            Variable z2 = names.freshTemp();
            Label L = names.freshLabel();
            Label Lcheck = names.freshLabel();
            Label Lend = names.freshLabel();

            List<SInstruction> out = new ArrayList<>();
            out.add(new AssignmentInstruction(FixedLabel.EMPTY, z1, jv.variable()));
            out.add(new AssignmentInstruction(FixedLabel.EMPTY, z2, jv.getOther()));

            out.add(new JumpZeroInstruction(L, z1, Lcheck));
            out.add(new JumpZeroInstruction(FixedLabel.EMPTY, z2, Lend)); // z1!=0 && z2==0 => לא שווים
            out.add(new DecreaseInstruction(FixedLabel.EMPTY, z1));
            out.add(new DecreaseInstruction(FixedLabel.EMPTY, z2));
            out.add(new GotoLabelInstruction(FixedLabel.EMPTY, L));

            out.add(new JumpZeroInstruction(Lcheck, z2, jv.target())); // שניהם 0 => שווים
            out.add(new NeutralInstruction(Lend, z1));
            return out;
        }

        return null;
    }

    // ===== Helpers =====
    private static Label safeLabel(Label l) { return (l == null) ? FixedLabel.EMPTY : l; }

    private static SInstruction cloneWithLabel(SInstruction i, Label label) {
        if (i instanceof IncreaseInstruction inc) return new IncreaseInstruction(label, inc.variable());
        if (i instanceof DecreaseInstruction dec) return new DecreaseInstruction(label, dec.variable());
        if (i instanceof JumpNotZeroInstruction jnz) return new JumpNotZeroInstruction(label, jnz.variable(), jnz.target());
        if (i instanceof NeutralInstruction n) return new NeutralInstruction(label, n.variable());

        if (i instanceof ZeroVariableInstruction z) return new ZeroVariableInstruction(label, z.variable());
        if (i instanceof ConstantAssignmentInstruction c) return new ConstantAssignmentInstruction(label, c.variable(), c.getConstant());
        if (i instanceof GotoLabelInstruction g) return new GotoLabelInstruction(label, g.target());
        if (i instanceof JumpZeroInstruction jz) return new JumpZeroInstruction(label, jz.variable(), jz.target());
        if (i instanceof AssignmentInstruction a) return new AssignmentInstruction(label, a.variable(), a.getFrom());
        if (i instanceof JumpEqualConstantInstruction jc) return new JumpEqualConstantInstruction(label, jc.variable(), jc.getConstant(), jc.target());
        if (i instanceof JumpEqualVariableInstruction jv) return new JumpEqualVariableInstruction(label, jv.variable(), jv.getOther(), jv.target());
        return i;
    }

    private static final class NameGen {
        private int nextLabel = 1;
        private int nextTemp = 1;
        NameGen(List<SInstruction> prog) {
            for (SInstruction ins : prog) {
                if (ins.lineLabel() instanceof UserLabel ul) {
                    nextLabel = Math.max(nextLabel, parseIdx(ul.labelName()) + 1);
                }
                Variable v = ins.variable();
                if (v != null) {
                    String s = v.toString();
                    if (s.startsWith("z")) nextTemp = Math.max(nextTemp, parseIdx(s) + 1);
                }
            }
        }
        Label freshLabel() { return new UserLabel("L" + (nextLabel++)); }
        Variable freshTemp() { return Variable.ofToken("z" + (nextTemp++)); }
        private static int parseIdx(String s) {
            try {
                String d = s.replaceAll("\\D+", "");
                return d.isEmpty() ? 0 : Integer.parseInt(d);
            } catch (Exception e) { return 0; }
        }
    }
}
