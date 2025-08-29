package console.commands;


import console.util.ConsoleContext;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public final class SaveStateCommand implements ConsoleCommand {
    @Override public String key()   { return "6"; }                // מספר תפריט פנוי
    @Override public String label() { return "Save system state"; }

    @Override
    public void execute(ConsoleContext ctx, Scanner sc) {
        System.out.print("Enter EXISTING folder path: ");
        Path dir = Path.of(sc.nextLine().trim()).normalize();
        if (!Files.isDirectory(dir)) {
            System.out.println("Save failed: folder does not exist.");
            return;
        }
        if (!Files.isWritable(dir)) {
            System.out.println("Save failed: folder is not writable.");
            return;
        }

        System.out.print("Enter file name (without extension): ");
        String name = sc.nextLine();
        if (name.isBlank()) { System.out.println("Save failed: empty file name."); return; }

        Path file = dir.resolve(name );
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(ctx.state());
            System.out.println("Saved: " + file);
        } catch (Exception e) {
            System.out.println("Save failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
