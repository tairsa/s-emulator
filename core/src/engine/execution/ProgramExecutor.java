package engine.execution;

import java.util.List;
import java.util.Map;

public interface ProgramExecutor {
    /** Run and return y. */
    long run(Long... inputs);

    /** Total cycles counted in the last run. */
    long cycles();
    Map<String,Long> lastVariablesOrdered();
    java.util.List<Long> lastProvidedInputs();


}
