package engine.label;

import java.util.Objects;

public final class UserLabel implements Label {
    private final String text; // e.g., L1, L2, ...

    public UserLabel(String text) {
        if (text == null || !text.matches("L\\d+")) {
            throw new IllegalArgumentException("Illegal label: " + text);
        }
        this.text = text;
    }

    @Override
    public String labelName() { return text; }

    @Override
    public String toString() { return text; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserLabel)) return false;
        UserLabel that = (UserLabel) o;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() { return Objects.hash(text); }
}