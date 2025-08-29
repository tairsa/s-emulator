package console.commands;

import console.util.ConsoleContext;
import engine.io.ProgramParseException;
import engine.io.ProgramParser;
import engine.program.SProgram;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Command to load a program XML file.
 * When a new program is loaded successfully, the history is cleared.
 */
public final class LoadProgramCommand implements ConsoleCommand {
    @Override public String key() { return "1"; }
    @Override public String label() { return "Load program XML"; }
    @Override
    public void execute(ConsoleContext ctx, Scanner sc) throws ProgramParseException {
        System.out.print("Enter XML full path: ");
        String p = sc.nextLine().trim();
        Path path = Path.of(p);

        ProgramParser parser = ctx.parser();

        // הפרסור שלך עובד עם Path
        SProgram program = parser.parse(path);

        // וגם שומרים bytes כדי שנוכל לשחזר אחרי load של snapshot
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ProgramParseException("Failed to read XML bytes: " + e.getMessage(), e);
        }

        ctx.setProgram(program, bytes, p);
        System.out.println("Program loaded: " + program.name());
    }
}