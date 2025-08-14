package engine.variable;

import java.util.Objects;


public final class Variable {
    private final String name;
    private final VariableType type;

    public Variable(String name, VariableType type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }
    public String name() { return name; }
    public VariableType type() { return type; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable v)) return false;
        return name.equals(v.name) && type == v.type;
    }
    @Override public int hashCode() { return Objects.hash(name, type); }
    @Override public String toString() { return name; }

    public static Variable ofToken(String token) {
        if (token == null || token.isBlank()) return null;
        if (token.equals("y")) return new Variable("y", VariableType.RESULT);
        if (token.startsWith("x")) return new Variable(token, VariableType.INPUT);
        if (token.startsWith("z")) return new Variable(token, VariableType.TEMP);
        throw new IllegalArgumentException("Illegal variable name: " + token);
    }
}