package console;

import engine.execution.ProgramExecutor;
import engine.execution.ProgramExecutorImpl;
import engine.instruction.HasTarget;
import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;
import engine.io.ProgramParseException;
import engine.io.ProgramParser;
import engine.io.XmlProgramParser;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.program.SProgram;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public final class ConsoleApp {
    private SProgram program;

    private final List<HistoryItem> history = new ArrayList<>();

    private static final class HistoryItem {
        final int id;
        final int degree;
        final List<Long> inputs;
        final long y;
        final long cycles;
        HistoryItem(int id, int degree, List<Long> inputs, long y, long cycles) {
            this.id = id; this.degree = degree; this.inputs = inputs; this.y = y; this.cycles = cycles;
        }
    }

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("=== S-Emulator (Console) ===");
            System.out.println("1) Load program XML");
            System.out.println("2) Show program");
            System.out.println("3) Expand");      // ← חדש
            System.out.println("4) Run");
            System.out.println("5) History");     // ← חדש
            System.out.println("6) Exit");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShow();
                    case "3" -> doExpand(sc);   // ← חדש
                    case "4" -> doRun(sc);
                    case "5" -> doHistory();   // ← חדש
                    case "6" -> { System.out.println("Bye!"); return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void doLoad(Scanner sc) throws ProgramParseException {
        System.out.print("Enter XML full path: ");
        String p = sc.nextLine().trim();
        ProgramParser parser = new XmlProgramParser();
        program = parser.parse(Path.of(p));
        System.out.println("Program loaded: " + program.name());
    }

    private void doShow() {
        if (program == null) { System.out.println("No program loaded."); return; }
        System.out.println("Program: " + program.name());
        printProgram(program);
    }

    private void doExpand(Scanner sc) {
        if (program == null) { System.out.println("No program loaded."); return; }
        System.out.print("Enter degree (0..): ");
        int degree = readInt(sc);

        // בשלב זה, לפני Expander אמיתי, נציג את אותה תוכנית כ-"מורחבת"
        System.out.println("Expanded program (degree " + degree + "):");
        printProgram(program);

        // כשתממשי Expander:
        // ProgramExpander expander = new SimpleExpander();
        // SProgram expanded = expander.expand(program, degree);
        // printProgram(expanded);
    }

    private void printProgram(SProgram prog) {
        var list = prog.instructions();

        var inputs = collectInputsInOrder(prog);
        var labels = collectLabelsInOrder(prog);

        System.out.println("Inputs used: " + (inputs.isEmpty() ? "-" : String.join(", ", inputs)));
        System.out.println("Labels used: " + (labels.isEmpty() ? "-" : String.join(", ", labels)));

        for (int i = 0; i < list.size(); i++) {
            SInstruction ins = list.get(i);
            String bOrS = (ins.kind() == InstructionKind.BASIC) ? "B" : "S";
            String label = (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY)
                    ? "" : ins.lineLabel().labelName();
            String labelBox = String.format("[ %-5s ]", label);
            String line = String.format("#%d (%s) %s  %s  (%d)",
                    i+1, bOrS, labelBox, ins.render(), ins.cycles());
            System.out.println(line);
        }
//        System.out.println("Total static cycles: " + prog.totalCycles());
    }

    private static java.util.List<String> collectInputsInOrder(SProgram prog) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (SInstruction ins : prog.instructions()) {
            addIfX(seen, ins.variable());
            if (ins instanceof engine.instruction.synthetic.JumpEqualVariableInstruction jev)
                addIfX(seen, jev.getOther());
            if (ins instanceof engine.instruction.synthetic.AssignmentInstruction asg) {
                addIfX(seen, asg.getFrom());
                addIfX(seen, asg.variable()); // אם עושים השמה ל-x*
            }
        }
        return new java.util.ArrayList<>(seen);
    }
    private static void addIfX(java.util.Set<String> seen, engine.variable.Variable v) {
        if (v == null) return;
        String t = v.toString();
        if (t.startsWith("x")) seen.add(t);
    }


    private static java.util.List<String> collectLabelsInOrder(SProgram prog) {
        // 1) אוספים רק תוויות שורה (מוגדרות) לפי סדר הופעה
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (SInstruction ins : prog.instructions()) {
            Label ll = ins.lineLabel();
            if (ll != null && ll != FixedLabel.EMPTY && ll != FixedLabel.EXIT) {
                seen.add(ll.labelName());
            }
        }

        // 2) אם יש יעד קפיצה EXIT – מוסיפים אותו בסוף
        if (programUsesExit(prog)) {
            seen.add("EXIT");
        }

        return new java.util.ArrayList<>(seen);
    }

    private static boolean programUsesExit(SProgram prog) {
        for (SInstruction ins : prog.instructions()) {
            Label tgt = jumpTargetOf(ins);          // יעד קפיצה אם יש
            if (tgt == FixedLabel.EXIT) return true;
        }
        return false;
    }

    // מזהה יעד קפיצה (פשטנו לרשימת סוגים ידועים)
    private static Label jumpTargetOf(SInstruction ins) {
        if (ins instanceof HasTarget jt) {
            Label tgt = jt.target();
            return tgt;
        }
        return FixedLabel.EMPTY;
    }


    private void doRun(Scanner sc) {
        if (program == null) { System.out.println("No program loaded."); return; }
        System.out.print("Enter inputs (comma separated, e.g. 3,0,5): ");
        String line = sc.nextLine().trim();
        Long[] inputs = parseInputs(line);

        // כרגע אין שימוש בדרגה בריצה, אבל אם תרצי לאסוף אותה להיסטוריה – נשאל:
        System.out.print("Enter degree for run (0..): ");
        int degree = readInt(sc);

        ProgramExecutor exec = new ProgramExecutorImpl(program);
        long y = exec.run(inputs);
        long cycles = exec.cycles();

        System.out.println("=== Result ===");
        System.out.println("y = " + y);
        System.out.println("cycles = " + cycles);

        // שמירה להיסטוריה
        history.add(new HistoryItem(
                history.size() + 1,
                degree,
                toList(inputs),
                y,
                cycles
        ));
    }

    // ======== חדש: History ========
    private void doHistory() {
        if (history.isEmpty()) { System.out.println("(No runs yet.)"); return; }
        System.out.println("# | degree | inputs | y | cycles");
        for (HistoryItem h : history) {
            System.out.printf("%d | %d | %s | %d | %d%n", h.id, h.degree, h.inputs, h.y, h.cycles);
        }
    }

    // ======== עוזרים ========
    private static int readInt(Scanner sc) {
        while (true) {
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException nfe) { System.out.print("Not a number, try again: "); }
        }
    }

    private static Long[] parseInputs(String line) {
        if (line.isEmpty()) return new Long[0];
        String[] parts = line.split(",");
        Long[] arr = new Long[parts.length];
        for (int i=0;i<parts.length;i++) {
            try { arr[i] = Long.parseLong(parts[i].trim()); }
            catch (Exception e) { arr[i] = 0L; }
        }
        return arr;
    }
    private static List<Long> toList(Long[] a) {
        return a == null ? List.of() : Arrays.asList(a);
    }
}