package console.commands;

import console.util.ConsoleContext;

import java.util.Scanner;


/**
 * A single executable menu command.
 */
public interface ConsoleCommand {
    /** Unique menu key, e.g., "1" */
    String key();
    /** Text shown in the menu */
    String label();
    /** Execute the command using the shared console context */
    void execute(ConsoleContext ctx, Scanner sc) throws Exception;
}