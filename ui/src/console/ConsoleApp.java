package console;

import console.commands.*;
import console.util.ConsoleContext;
import console.util.ProgramPrinter;
import engine.execution.ProgramExecutor;
import engine.io.ProgramParser;
import engine.io.XmlProgramParserJaxb;
import engine.program.SProgram;
import engine.execution.ProgramExecutorImpl;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * ConsoleApp main loop using the Command pattern. Each menu item is a separate class.
 * All I/O stays here (and in console.impl + console.util), while ConsoleContext remains I/O-free.
 */
public final class ConsoleApp {
    private final List<ConsoleCommand> commands;
    private final ConsoleContext ctx;


    public ConsoleApp() {
        ProgramParser parser = new XmlProgramParserJaxb();
        var expander = new engine.execution.SimpleProgramExpander();
        java.util.function.Function<SProgram, ProgramExecutor> executorFactory = ProgramExecutorImpl::new;
        this.ctx = new ConsoleContext(parser, expander, executorFactory);
        this.commands = List.of(
                new LoadProgramCommand(),
                new ShowProgramCommand(),
                new ExpandCommand(),
                new RunCommand(),
                new HistoryCommand(),
                new SaveStateCommand(),
                new LoadStateCommand(),
                new ExitCommand()
        );
    }


    public static void main(String[] args) {
        new ConsoleApp().run();
    }


    private void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            ProgramPrinter.printMenu(commands);
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            Optional<ConsoleCommand> cmd = commands.stream().filter(x -> x.key().equals(c)).findFirst();
            if (cmd.isEmpty()) {
                System.out.println("Invalid choice.");
                continue;
            }
            try
            {
                cmd.get().execute(ctx, sc);
            }
            catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }


}