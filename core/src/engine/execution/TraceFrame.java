package engine.execution;

public final class TraceFrame {
    public final int id;
    public final char kind;
    public final String label;
    public final String text;
    public final long cycles;
    public TraceFrame(int id, char kind, String label, String text, long cycles) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.text = text;
        this.cycles = cycles;
    }
}
