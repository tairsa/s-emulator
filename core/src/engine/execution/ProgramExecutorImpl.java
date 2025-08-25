package engine.execution;

import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.program.SProgram;
import engine.variable.Variable;
import engine.variable.VariableType;

import java.util.*;

public final class ProgramExecutorImpl implements ProgramExecutor {
    private final SProgram program;
    private long lastCycles = 0;

    // שדות חדשים לצילום מצב אחרון (לא משפיעים על הלוגיקה)
    private Map<Variable, Long> lastSnapshot = Collections.emptyMap();
    private Map<String, Long> lastSnapshotPretty = Collections.emptyMap();
    private List<Long> lastInputs = Collections.emptyList();

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

            Label next = ins.execute(ctx); // instruction adds cycles via ctx.addCycles(...)
            if (next == FixedLabel.EXIT) break;

            if (next == FixedLabel.EMPTY) {
                ip++;
            } else {
                int j = ctx.resolveLabel(next);
                if (j == -1) break; // EXIT
                ip = j;
            }
        }

        // נשמרים כרגיל
        lastCycles = ctx.cycles();

        // ✨ חדש: צילום מצב של כל המשתנים + מיון ידידותי להצגה
        lastSnapshot = new HashMap<>(ctx.snapshot());
        lastSnapshotPretty = buildOrderedView(lastSnapshot);
        lastInputs = ctx.providedInputs();

        // ערך y כתוצאת הריצה
        return lastSnapshot.getOrDefault(new Variable("y", VariableType.RESULT), 0L);
    }

    @Override public long cycles() { return lastCycles; }
    @Override public java.util.List<Long> lastProvidedInputs() { return lastInputs; }

    /** החזרה גולמית Variable->value מהריצה האחרונה (לוגינג/טסטים). */
    public Map<Variable, Long> lastVariablesRaw() { return lastSnapshot; }

    @Override
    public Map<String, Long> lastVariablesOrdered() {
        Map<String, Long> ordered = new LinkedHashMap<>();

        // y ראשון
        ordered.put("y", lastSnapshot.getOrDefault(new Variable("y", VariableType.RESULT), 0L));

        for (int i = 0; i < lastInputs.size(); i++) {
            ordered.put("x" + (i + 1), lastInputs.get(i));
        }

        lastSnapshot.entrySet().stream()
                .filter(e -> e.getKey().type() == VariableType.TEMP)
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(e -> ordered.put(e.getKey().toString(), e.getValue()));

        return ordered;
    }

    private static Map<String, Long> buildOrderedView(Map<Variable, Long> snap) {
        List<Map.Entry<Variable, Long>> list = new ArrayList<>(snap.entrySet());

        Comparator<Map.Entry<Variable, Long>> cmp =
                Comparator.<Map.Entry<Variable, Long>>comparingInt(e -> {
                            VariableType t = e.getKey().type();
                            if (t == VariableType.RESULT) return 0; // y
                            if (t == VariableType.INPUT)  return 1; // x_i
                            if (t == VariableType.TEMP)   return 2; // z_i
                            return 3;
                        })
                        .thenComparingInt((Map.Entry<Variable, Long> e) -> extractIndex(e.getKey().name()))
                        .thenComparing((Map.Entry<Variable, Long> e) -> e.getKey().name());

        list.sort(cmp);

        Map<String, Long> ordered = new LinkedHashMap<>();
        for (var e : list) ordered.put(e.getKey().name(), e.getValue());
        return ordered;
    }

    private static int extractIndex(String name) {
        // "x12" -> 12, "z3" -> 3, "y" או ללא ספרות -> 0
        int i = name.length() - 1;
        while (i >= 0 && Character.isDigit(name.charAt(i))) i--;
        if (i == name.length() - 1) return 0;
        try { return Integer.parseInt(name.substring(i + 1)); }
        catch (Exception ignored) { return 0; }
    }
}
