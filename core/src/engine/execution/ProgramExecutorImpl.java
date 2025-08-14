package engine.execution;

import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.program.SProgram;
import engine.variable.Variable;
import engine.variable.VariableType;

public final class ProgramExecutorImpl implements ProgramExecutor {
    private final SProgram program;
    private long lastCycles = 0;

    public ProgramExecutorImpl(SProgram program) { this.program = program; }

    @Override
    public long run(Long... inputs) {
        program.validate();

        ExecutionContext ctx = new ExecutionContext();
        ctx.initializeInputs(inputs);
        ctx.buildLabelIndex(program);

        int ip = 0;
        while (ip >= 0 && ip < program.instructions().size()) {
            ctx.setIp(ip);
            SInstruction ins = program.instructions().get(ip);

            Label next = ins.execute(ctx); // instruction adds its own cycles to ctx
            if (next == FixedLabel.EXIT) break;
            if (next == FixedLabel.EMPTY) {
                ip++;
            } else {
                int j = ctx.resolveLabel(next);
                if (j == -1) break; // EXIT
                ip = j;
            }
        }
        lastCycles = ctx.cycles();

        // y value:
        Variable y = new Variable("y", VariableType.RESULT);
        return ctx.get(y);
    }

    @Override public long cycles() { return lastCycles; }
}
