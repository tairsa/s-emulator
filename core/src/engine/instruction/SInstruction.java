package engine.instruction;

import engine.label.Label;
import engine.variable.Variable;
import engine.execution.ExecutionContext;

/** One instruction line in the S-program. */
public interface SInstruction {
    /** Optional line label declared on this instruction (for GOTO/Jump targets). */
    Label lineLabel();

    /** Optional variable that this instruction operates on (may be null). */
    Variable variable();

    /** Instruction kind: BASIC or SYNTHETIC (affects printing B|S). */
    InstructionKind kind();

    /** Cycles cost for this instruction (per spec). */
    int cycles();

    /** Execute and return a next-label to jump to: EMPTY → fallthrough, EXIT → stop, user label → jump. */
    Label execute(ExecutionContext ctx);

    /** Render the command part for printing (without number/B|S/label/cycles). */
    String render();
}
