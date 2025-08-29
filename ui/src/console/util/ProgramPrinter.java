package console.util;

import console.commands.ConsoleCommand;
import engine.instruction.InstructionKind;
import engine.instruction.SInstruction;
import engine.label.FixedLabel;
import engine.program.SProgram;

import java.util.List;

/**
 * UI-only printer utilities for console output. Keeps console.util.ConsoleContext free of I/O.
 */
public final class ProgramPrinter {
    private ProgramPrinter() {}


    public static void printProgram(SProgram prog) {
        var list = prog.instructions();
        var inputs = ConsoleContext.collectInputsInOrder(prog);
        var labels = ConsoleContext.collectLabelsInOrder(prog);
        System.out.println("Program: " + prog.name());
        System.out.println("Inputs used: " + (inputs.isEmpty() ? "-" : String.join(", ", inputs)));
        System.out.println("Labels used: " + (labels.isEmpty() ? "-" : String.join(", ", labels)));
        for (int i = 0; i < list.size(); i++) {
            SInstruction ins = list.get(i);
            String bOrS = (ins.kind() == InstructionKind.BASIC) ? "B" : "S";
            String label = (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY) ? "" : ins.lineLabel().labelName();
            String labelBox = String.format("[ %-5s ]", label);
            String line = String.format("#%d (%s) %s %s (%d)", i+1, bOrS, labelBox, ins.render(), ins.cycles());
            System.out.println(line);
        }
    }


    public static void printProgramExpand(SProgram prog, engine.execution.SimpleProgramExpander expander) {
        var list = prog.instructions();
        for (int i = 0; i < list.size(); i++) {
            SInstruction ins = list.get(i);
            String bOrS = (ins.kind() == InstructionKind.BASIC) ? "B" : "S";
            String label = (ins.lineLabel() == null || ins.lineLabel() == FixedLabel.EMPTY) ? "" : ins.lineLabel().labelName();
            String labelBox = String.format("[ %-5s ]", label);
            String line = String.format("#%d (%s) %s %s (%d)", i + 1, bOrS, labelBox, ins.render(), ins.cycles());
            var chain = expander.lineageOf(ins);
            if (chain != null && !chain.isEmpty()) {
                StringBuilder sb = new StringBuilder(line);
                for (var frame : chain) {
                    String lb = (frame.label == null || frame.label.isEmpty()) ? "" : frame.label;
                    String box = String.format("[ %-5s ]", lb);
                    sb.append(" >>> ")
                            .append(String.format("#%d (%c) %s %s (%d)", frame.id, frame.kind, box, frame.text, frame.cycles));
                }
                System.out.println(sb);
            } else {
                System.out.println(line);
            }
        }
    }

    public static void printMenu(List<ConsoleCommand> commands) {
        System.out.println("==== S-Emulator Menu =====");
        for (ConsoleCommand c : commands) {
            System.out.printf("%s) %s%n", c.key(), c.label());
        }
    }

}