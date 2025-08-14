package engine.label;

public enum FixedLabel implements Label {
    EMPTY(""), EXIT("EXIT");

    private final String text;
    FixedLabel(String text) { this.text = text; }

    @Override
    public String labelName() { return text; }

    @Override
    public String toString() { return text; }
}
