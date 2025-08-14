package engine.execution;

public interface ProgramExecutor {
    /** Run and return y. */
    long run(Long... inputs);

    /** Total cycles counted in the last run. */
    long cycles();
}
