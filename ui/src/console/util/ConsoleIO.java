package console.util;

import java.util.Scanner;

public final class ConsoleIO {
    private ConsoleIO() {}

    public static int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException nfe) {
                System.out.print("Not a number, try again: ");
            }
        }
    }

    public static Long[] parseInputs(String line) {
        if (line.isEmpty()) return new Long[0];
        String[] parts = line.split(",");
        Long[] arr = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { arr[i] = Long.parseLong(parts[i].trim()); }
            catch (Exception e) { arr[i] = 0L; }
        }
        return arr;
    }
}
