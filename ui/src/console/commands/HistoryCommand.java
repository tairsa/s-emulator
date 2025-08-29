package console.commands;

import console.util.ConsoleContext;

import java.util.Scanner;

/** Show run history records. */
public final class HistoryCommand implements ConsoleCommand {
    @Override public String key() { return "5"; }
    @Override public String label() { return "History"; }
    @Override public void execute(ConsoleContext ctx, Scanner sc) {
        if (ctx.history().isEmpty()) { System.out.println("(No runs yet.)"); return; }
        for (var h : ctx.history()) {
            System.out.printf("#%d | degree:%d | inputs:%s | y:%d | cycles:%d%n", h.id, h.degree, h.inputs, h.y, h.cycles);
        }
    }
}