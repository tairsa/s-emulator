package engine.execution;

import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;
import engine.instruction.synthetic.*;

import engine.program.SProgram;

public final class DegreeCalculator {

    private DegreeCalculator() {}

    public static int maxDegree(SProgram p) {
        int max = 0;
        for (SInstruction ins : p.instructions()) {
            max = Math.max(max, degreeOf(ins));
        }
        return max;
    }

    public static int degreeOf(SInstruction i) {
        if (i.kind() == InstructionKind.BASIC) return 0;

        // Mid – נפתחות בפאזה 2 לצעד בסיס יחיד
        if (i instanceof ZeroVariableInstruction
                || i instanceof ConstantAssignmentInstruction
                || i instanceof JumpZeroInstruction
                || i instanceof GotoLabelInstruction) return 1;

        // High – צריך קודם לרדת ל‑Mid (פאזה 1), ואז לבסיס (פאזה 2)
        if (i instanceof AssignmentInstruction
                || i instanceof JumpEqualConstantInstruction
                || i instanceof JumpEqualVariableInstruction) return 2;

        // אם יש לך גם JumpNotEqualInstruction:
        // return 3;

        return 2; // ברירת מחדל סינטטי
    }
}
