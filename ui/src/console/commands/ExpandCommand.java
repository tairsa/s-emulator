package console.commands;

import console.util.ConsoleContext;
import console.util.ProgramPrinter;
import engine.program.SProgram;


import java.util.Scanner;


/** Expand the program by a chosen degree and print it with lineage. */
public final class ExpandCommand implements ConsoleCommand {
    @Override public String key() { return "3"; }
    @Override public String label() { return "Expand"; }
    @Override public void execute(ConsoleContext ctx, Scanner sc) {
        var opt = ctx.program();
        if (opt.isEmpty()) { System.out.println("No program loaded."); return; }
        SProgram program = opt.get();
        int maxDeg = ctx.maxExpansionDegree(program);
        if (maxDeg == 0) { System.out.println("Nothing to expand (no supported synthetic instructions). "); return; }
        System.out.println("Max expansion degree: " + maxDeg);
        System.out.print("Enter degree (1.." + maxDeg + "): ");
        int degree = readInt(sc);

        // Strict validation: reject out-of-range input
        if (degree < 1 || degree > maxDeg) {
            System.out.println("Invalid degree. Must be between 0 and " + maxDeg + ".");
            return; // do not run
        }

        SProgram expanded = ctx.expandToDegree(program, degree);
        System.out.println("=== Expanded program (degree " + degree + ") ===");
        ProgramPrinter.printProgramExpand(expanded, ctx.expander());
    }


    private static int readInt(Scanner sc) {
        while (true) {
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException nfe) { System.out.print("Not a number, try again: "); }
        }
    }
}