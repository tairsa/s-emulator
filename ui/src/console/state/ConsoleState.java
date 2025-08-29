package console.state;

import engine.io.ProgramParser;
import engine.program.SProgram;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot + runtime holder of the console state.
 * משמשת לשמירה וטעינה של מצב המערכת (בונוס).
 */
public final class ConsoleState implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- runtime only (לא נשמר בסריאליזציה) ---
    private transient SProgram program;

    // --- serialized fields ---
    /** תוכן ה-XML כ-Bytes */
    private byte[] programXmlBytes;

    /** נתיב המקור של קובץ ה-XML (לא חובה, לשימוש תצוגה/דיבאג). */
    private String lastLoadedXmlPath;

    /** היסטוריית ריצות */
    private final List<HistoryItemDTO> history = new ArrayList<>();

    /** מזהה הבא לריצה חדשה */
    private int nextHistoryId = 1;

    // --- API ---

    /** טעינת תוכנית רגילה: שומרת תוכנית, bytes ונתיב; מאפסת היסטוריה */
    public void setProgram(SProgram p, byte[] xmlBytes, String sourcePathOrNull) {
        this.program = p;
        this.programXmlBytes = xmlBytes;
        this.lastLoadedXmlPath = sourcePathOrNull;
        this.history.clear();
        this.nextHistoryId = 1;
    }

    /** שחזור ה-SProgram מה-bytes אחרי טעינת snapshot */
    public void reparseProgramIfNeeded(ProgramParser parser) throws Exception {
        if (programXmlBytes != null) {
            Path tmp = Files.createTempFile("program-", ".xml");
            try {
                Files.write(tmp, programXmlBytes);
                this.program = parser.parse(tmp); // parser שלך תומך רק ב-Path
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
            }
        } else {
            this.program = null;
        }
    }

    // --- Getters/Setters ---

    public SProgram program() { return program; }
    public byte[] programXmlBytes() { return programXmlBytes; }
    public String lastLoadedXmlPath() { return lastLoadedXmlPath; }

    public List<HistoryItemDTO> history() { return history; }

    public int nextHistoryId() { return nextHistoryId; }
    public void setNextHistoryId(int nextId) { this.nextHistoryId = nextId; }

    public void addHistoryItem(HistoryItemDTO item) {
        history.add(item);
        nextHistoryId = Math.max(nextHistoryId, item.id + 1);
    }

    // --- DTO להיסטוריה ---

    public static final class HistoryItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public int degree;
        public List<Long> inputs;
        public long y;
        public long cycles;

        public HistoryItemDTO() {}
        public HistoryItemDTO(int id, int degree, List<Long> inputs, long y, long cycles) {
            this.id = id; this.degree = degree; this.inputs = inputs; this.y = y; this.cycles = cycles;
        }
    }
}
