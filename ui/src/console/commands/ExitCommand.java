package console.commands;

import console.util.ConsoleContext;

import java.util.Scanner;

/** Exit the application. */
public final class ExitCommand implements ConsoleCommand {
    @Override public String key() { return "6"; }
    @Override public String label() { return "Exit"; }
    @Override public void execute(ConsoleContext ctx, Scanner sc) { System.out.println("Bye!"); System.exit(0); }
}