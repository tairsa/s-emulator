package console;

import engine.execution.ProgramExecutor;
import engine.execution.ProgramExecutorImpl;
import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;
import engine.io.ProgramParseException;
import engine.io.ProgramParser;
import engine.io.XmlProgramParser;
import engine.label.FixedLabel;
import engine.program.SProgram;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;


public final class ConsoleApp {
    private SProgram program;

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("=== S-Emulator (Console) ===");
            System.out.println("1) Load program XML");
            System.out.println("2) Show program");
            System.out.println("3) Run");
            System.out.println("4) Exit");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShow();
                    case "3" -> doRun(sc);
                    case "4" -> { System.out.println("Bye!"); return; }
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
        List<SInstruction> list = program.instructions();
        System.out.println("Program: " + program.name());
        for (int i = 0; i < list.size(); i++) {
            SInstruction ins = list.get(i);
            String bOrS = ins.kind() == InstructionKind.BASIC ? "B" : "S";
            String label = (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY) ? "" : ins.lineLabel().labelName();
            String labelBox = String.format("[ %-5s ]", label);
            System.out.printf("#%d (%s) %s %s (%d)%n",
                    (i + 1), bOrS, labelBox, ins.render(), ins.cycles());
        }
        System.out.println("Total static cycles: " + program.totalCycles());
    }

    private void doRun(Scanner sc) {
        if (program == null) { System.out.println("No program loaded."); return; }
        System.out.print("Enter inputs (comma separated, e.g. 3,0,5): ");
        String line = sc.nextLine().trim();
        Long[] inputs = parseInputs(line);

        ProgramExecutor exec = new ProgramExecutorImpl(program);
        long y = exec.run(inputs);
        System.out.println("=== Result ===");
        System.out.println("y = " + y);
        System.out.println("cycles = " + exec.cycles());
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
}
