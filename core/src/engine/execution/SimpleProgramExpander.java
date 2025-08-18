package engine.execution;

import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.ConstantAssignmentInstruction;
import engine.instruction.synthetic.GotoLabelInstruction;
import engine.instruction.synthetic.ZeroVariableInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;
import engine.program.SProgram;
import engine.program.SProgramImpl;
import engine.variable.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SimpleProgramExpander implements ProgramExpander {

    @Override
    public SProgram expand(SProgram program, int degree) {
        SProgram out = program;
        for (int d = 0; d < Math.max(1, degree); d++) {
            out = expandOnce(out);
        }
        return out;
    }

    private SProgram expandOnce(SProgram program) {
        // אוסף תוויות קיימות כדי לא להתנגש
        FreshLabeler fresh = FreshLabeler.fromProgram(program);
        Variable zt = FreshTempAllocator.pickFreeZ(program); // z-זמני לשימוש ב-GOTO

        List<SInstruction> out = new ArrayList<>();
        for (SInstruction ins : program.instructions()) {
            if (ins.kind() == InstructionKind.BASIC) {
                // בסיסית – מעתיקים כמו שהיא
                out.add(ins);
                continue;
            }

            // סינתטיות שנרחיב עכשיו:
            if (ins instanceof ZeroVariableInstruction zv) {
                expandZeroVariable(zv, fresh, out);
            } else if (ins instanceof ConstantAssignmentInstruction ca) {
                expandConstantAssignment(ca, fresh, out);
            } else if (ins instanceof GotoLabelInstruction go) {
                expandGoto(go, zt, out);
            } else {
                // סינתטיות אחרות — משאירים בינתיים כמו שהן (שלב 2 נרחיב גם אותן)
                out.add(ins);
            }
        }

        SProgramImpl p2 = new SProgramImpl(program.name() + " [expanded]", out);
        p2.validate();
        return p2;
    }

    /* ---------- expansions ---------- */

    private void expandZeroVariable(ZeroVariableInstruction zins,
                                    FreshLabeler fresh,
                                    List<SInstruction> out) {
        Variable z = zins.variable();
        // התווית המקורית עוברת לשורה הראשונה בבלוק
        Label Lchk = (zins.lineLabel() == null || zins.lineLabel() == FixedLabel.EMPTY)
                ? fresh.next() : zins.lineLabel();
        Label Ldec = fresh.next();

        // Lchk: if z != 0 goto Ldec
        out.add(new JumpNotZeroInstruction(Lchk, z, Ldec));
        //        fallthrough when z == 0
        out.add(new NeutralInstruction(FixedLabel.EMPTY));
        // Ldec: z <- z - 1
        out.add(new DecreaseInstruction(Ldec, z));
        //        loop while z != 0
        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, z, Ldec));
    }

    private void expandConstantAssignment(ConstantAssignmentInstruction ca,
                                          FreshLabeler fresh,
                                          List<SInstruction> out) {
        // v <- 0  (בעזרת ההרחבה של אפס)
        ZeroVariableInstruction zv = new ZeroVariableInstruction(ca.lineLabel(), ca.variable());
        expandZeroVariable(zv, fresh, out);

        // ואז K פעמים INCREASE v
        for (long i = 0; i < ca.getConstant(); i++) {
            out.add(new IncreaseInstruction(FixedLabel.EMPTY, ca.variable()));
        }
    }

    private void expandGoto(GotoLabelInstruction go, Variable zt, List<SInstruction> out) {
        // Unconditional branch באמצעות zt זמני שחוזר ל-0
        // התווית המקורית על השורה הראשונה
        out.add(new IncreaseInstruction(go.lineLabel(), zt));
        out.add(new JumpNotZeroInstruction(FixedLabel.EMPTY, zt, go.target()));
        out.add(new DecreaseInstruction(FixedLabel.EMPTY, zt));
    }

    /* ---------- helpers ---------- */

    // מחלקה לעשיית תוויות חדשות E1,E2,... בלי התנגשויות
    static final class FreshLabeler {
        private final Set<String> used = new HashSet<>();
        private int counter = 1;

        static FreshLabeler fromProgram(SProgram p) {
            FreshLabeler f = new FreshLabeler();
            for (SInstruction ins : p.instructions()) {
                Label l = ins.lineLabel();
                if (l != null && l != FixedLabel.EMPTY && l != FixedLabel.EXIT) {
                    f.used.add(l.labelName());
                }
            }
            return f;
        }
        Label next() {
            while (true) {
                String name = "E" + counter++;
                if (!used.contains(name)) {
                    used.add(name);
                    return new UserLabel(name);
                }
            }
        }
    }

    // בוחר zN פנוי לשימוש זמני (zt)
    static final class FreshTempAllocator {
        static Variable pickFreeZ(SProgram p) {
            int max = 0;
            for (SInstruction ins : p.instructions()) {
                if (ins.variable() != null) {
                    String t = ins.variable().toString();
                    if (t.startsWith("z")) max = Math.max(max, parseIdx(t));
                }
                // אפשר להוסיף כאן גם other()/from() אם קיימים — לא קריטי בשלב הזה
            }
            int next = max + 1;
            return Variable.ofToken("z" + next);
        }
        private static int parseIdx(String s) {
            try { return Integer.parseInt(s.replaceAll("\\D+", "")); }
            catch (Exception e) { return 0; }
        }
    }
}
