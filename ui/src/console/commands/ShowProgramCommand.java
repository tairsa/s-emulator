package console.commands;

import console.util.ConsoleContext;
import console.util.ProgramPrinter;
import engine.program.SProgram;

import java.util.Scanner;

/** Shows the currently loaded program. */
public final class ShowProgramCommand implements ConsoleCommand {
    @Override public String key() { return "2"; }
    @Override public String label() { return "Show program"; }
    @Override public void execute(ConsoleContext ctx, Scanner sc) {
        var opt = ctx.program();
        if (opt.isEmpty()) { System.out.println("No program loaded."); return; }
        SProgram p = opt.get();
        ProgramPrinter.printProgram(p);
    }
}
