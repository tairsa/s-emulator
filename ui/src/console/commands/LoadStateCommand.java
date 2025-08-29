package console.commands;


import console.state.ConsoleState;
import console.util.ConsoleContext;
import engine.io.ProgramParser;

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public final class LoadStateCommand implements ConsoleCommand {
    @Override public String key()   { return "8"; }
    @Override public String label() { return "Load saved system"; }

    @Override
    public void execute(ConsoleContext ctx, Scanner sc) {
        System.out.print("Enter saved file path: ");
        Path path = Path.of(sc.nextLine().trim());
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            ConsoleState s = (ConsoleState) in.readObject();       // ⬅ קריאה קצרה
            ProgramParser parser = ctx.parser();
            s.reparseProgramIfNeeded(parser);                      // ⬅ בונה SProgram מה-bytes
            ctx.replaceState(s);                                   // ⬅ מחליף state באפליקציה
            System.out.println("Loaded: " + path);
        } catch (Exception e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }
}
