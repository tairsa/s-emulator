package engine.execution;

import engine.execution.naming.NameAllocator;
import engine.execution.rules.*;
import engine.instruction.SInstruction;
import engine.instruction.InstructionKind;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.*;
import engine.program.SProgram;
import engine.program.SProgramImpl;
import engine.label.FixedLabel;
import engine.label.Label;

import java.util.*;

/**
 * מבצע הרחבת תוכניות בסריקת רוחב (BFS).
 * בכל דרגה: מרחיבים את *כל* הפקודות הסינטטיות הקיימות.
 */
public final class SimpleProgramExpander implements ProgramExpander {


    private SProgram lastProgram = null;
    private IdentityHashMap<SInstruction, List<TraceFrame>> lastLineage = new IdentityHashMap<>();

    public List<TraceFrame> lineageOf(SInstruction ins) {
        return lastLineage.getOrDefault(ins, List.of());
    }

    // רשימת כל חוקי ההרחבה (אפשר להוסיף עוד בהמשך)
    private final List<ExpansionRule> rules = List.of(
            new ZeroVariableRule(),
            new ConstantAssignmentRule(),
            new GotoLabelRule(),
            new JumpZeroRule(),
            new AssignmentRule(),
            new JumpEqualConstantRule(),
            new JumpEqualVariableRule()
    );

    @Override
    public SProgram expand(SProgram program, int degree) {
        if (degree <= 0) {
            lastProgram = program;
            lastLineage = new IdentityHashMap<>();
            return program;
        }

        SProgram cur = program;
        IdentityHashMap<SInstruction, List<TraceFrame>> prevLineage = new IdentityHashMap<>();

        // BFS: בכל pass מרחיבים את כל הסינטטיות
        for (int pass = 1; pass <= degree; pass++) {
            var names = new NameAllocator(cur.instructions());
            var in = cur.instructions();
            var next = new ArrayList<SInstruction>();
            var nextLineage = new IdentityHashMap<SInstruction, List<TraceFrame>>();

            for (int idx = 0; idx < in.size(); idx++) {
                var ins = in.get(idx);

                var parent = new TraceFrame(
                        idx + 1,
                        (ins.kind() == InstructionKind.SYNTHETIC ? 'S' : 'B'),
                        (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY) ? "" : ins.lineLabel().labelName(),
                        ins.render(),
                        ins.cycles()
                );

                var parentChain = prevLineage.getOrDefault(ins, List.of());

                List<SInstruction> step = null;
                for (var r : rules) {
                    if (r.supports(ins)) {
                        step = r.expandOneStep(ins, names);
                        break;
                    }
                }
                if (ins instanceof JumpZeroInstruction) {
                    SInstruction nextOrig = (idx + 1 < in.size()) ? in.get(idx + 1) : null;
                    if (nextOrig instanceof GotoLabelInstruction g) {
                        var lbl = g.lineLabel();
                        if (lbl != null && lbl != FixedLabel.EMPTY) {
                            names.offerSkipLabel(lbl);
                        }
                    }
                }
                // === NEW: דאגה לרשימה ניתנת לשינוי ===
                if (step == null) {
                    step = new ArrayList<>();                 // ריקה ומודיפבילית
                } else {
                    step = new ArrayList<>(step);             // להפוך כל מה שחזר ל־ArrayList
                    step.removeIf(Objects::isNull);           // ניקוי בטיחותי
                }
                // === END NEW ===

                if (step.isEmpty()) {
                    next.add(ins);
                    nextLineage.put(ins, parentChain);
                } else {
                    // לשמור תווית המקור על הילד הראשון
                    if (ins.lineLabel() != null && ins.lineLabel() != FixedLabel.EMPTY) {
                        var first = step.get(0);                          // בטוח: step מודיפבילית
                        step.set(0, cloneWithLabel(first, ins.lineLabel()));
                    }
                    for (var child : step) {
                        next.add(child);
                        var chain = new ArrayList<TraceFrame>(1 + parentChain.size());
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

    private static SInstruction cloneWithLabel(SInstruction i, Label label) {
        // אם כבר יש את אותה תווית – אין מה לשכפל
        if (i.lineLabel() == label) return i;

        // BASIC
        if (i instanceof IncreaseInstruction inc)
            return new IncreaseInstruction(label, inc.variable());
        if (i instanceof DecreaseInstruction dec)
            return new DecreaseInstruction(label, dec.variable());
        if (i instanceof JumpNotZeroInstruction jnz)
            return new JumpNotZeroInstruction(label, jnz.variable(), jnz.target());
        if (i instanceof NeutralInstruction neu)
            return new NeutralInstruction(label, neu.variable());

        // SYNTHETIC
        if (i instanceof ZeroVariableInstruction z)
            return new ZeroVariableInstruction(label, z.variable());
        if (i instanceof ConstantAssignmentInstruction c)
            return new ConstantAssignmentInstruction(label, c.variable(), c.getConstant());
        if (i instanceof GotoLabelInstruction g)
            return new GotoLabelInstruction(label, g.target());
        if (i instanceof JumpZeroInstruction jz)
            return new JumpZeroInstruction(label, jz.variable(), jz.target());
        if (i instanceof AssignmentInstruction a)
            return new AssignmentInstruction(label, a.variable(), a.getFrom());
        if (i instanceof JumpEqualConstantInstruction jc)
            return new JumpEqualConstantInstruction(label, jc.variable(), jc.getConstant(), jc.target());
        if (i instanceof JumpEqualVariableInstruction jv)
            return new JumpEqualVariableInstruction(label, jv.variable(), jv.getOther(), jv.target());
        return i;
    }
}
