package engine.execution.naming;

import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;
import engine.variable.Variable;

import java.util.List;

public final class NameAllocator {
    private int nextLabel = 1, nextTemp = 1;

    public NameAllocator(List<SInstruction> program) {
        for (SInstruction ins : program) {
            Label l = ins.lineLabel();
            if (l instanceof UserLabel ul) {
                nextLabel = Math.max(nextLabel, parseIdx(ul.labelName()) + 1);
            }
            var v = ins.variable();
            if (v != null) {
                var s = v.toString();
                if (s.startsWith("z")) nextTemp = Math.max(nextTemp, parseIdx(s) + 1);
            }
        }
    }

    private Label pendingSkip = null;

    public void offerSkipLabel(Label lbl) {
        if (lbl != null && lbl != FixedLabel.EMPTY) pendingSkip = lbl;
    }

    public Label borrowSkipOrFresh() {
        if (pendingSkip != null) {
            Label l = pendingSkip;
            pendingSkip = null;
            return l;
        }
        return freshLabel();
    }

    public Label freshLabel()        { return new UserLabel("L" + (nextLabel++)); }
    public Variable freshTemp()      { return Variable.ofToken("z" + (nextTemp++)); }
    public static Label empty()      { return FixedLabel.EMPTY; }

    private static int parseIdx(String s) {
        try {
            String d = s.replaceAll("\\D+", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception e) { return 0; }
    }
}
