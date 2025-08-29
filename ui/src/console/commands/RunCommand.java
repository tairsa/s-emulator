package console.commands;


import console.util.ConsoleContext;
import console.util.ProgramPrinter;
import engine.execution.ProgramExecutor;
import engine.program.SProgram;
import console.util.ConsoleIO;



import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;


/** Run the (expanded) program with user-provided inputs and print results. */
public final class RunCommand implements ConsoleCommand {
    @Override public String key() { return "4"; }
    @Override public String label() { return "Run"; }
    @Override public void execute(ConsoleContext ctx, Scanner sc) {
        var opt = ctx.program();
        if (opt.isEmpty()) { System.out.println("No program loaded."); return; }
        SProgram program = opt.get();
        int maxDeg = ctx.maxExpansionDegree(program);
        System.out.print("Enter degree for run (0.." + maxDeg + ", 0 = no expansion): ");
        int degree = ConsoleIO.readInt(sc);

        // Strict validation: reject out-of-range input
        if (degree < 0 || degree > maxDeg) {
            System.out.println("Invalid degree. Must be between 0 and " + maxDeg + ".");
            return; // do not run
        }

        SProgram expanded = ctx.expandToDegree(program, degree);
        if (degree < 0) degree = 0; if (degree > maxDeg) degree = maxDeg;


        List<String> xsOrder = ConsoleContext.collectInputsInOrder(program);
        String xsPrompt = xsOrder.isEmpty() ? "x1..xn" : xsOrder.stream().collect(Collectors.joining(", "));
        System.out.println("Program's inputs: " + xsPrompt);
        System.out.print("Enter inputs (comma separated, e.g. 3,0,5): ");
        Long[] inputs =  ConsoleIO.parseInputs(sc.nextLine().trim());


        ProgramExecutor exec = ctx.executorFactory().apply(expanded);
        long y = exec.run(inputs);
        long cycles = exec.cycles();


        System.out.println("=== Program Running ===");
        ProgramPrinter.printProgramExpand(expanded, ctx.expander());
        System.out.println("=== Result ===");
        System.out.println("y = " + y);
        System.out.println("=== Program's Variables ===");
        exec.lastVariablesOrdered().forEach((k,v) -> System.out.println(k + " = " + v));
        System.out.println("cycles = " + cycles);


//        ctx.history().add(new ConsoleContext.HistoryItem(
//                ctx.history().size()+1, degree, java.util.Arrays.asList(inputs), y, cycles));
        java.util.List<Long> inputList = java.util.Arrays.asList(inputs);
        ctx.recordRun(degree, inputList, y, cycles);
    }
}
