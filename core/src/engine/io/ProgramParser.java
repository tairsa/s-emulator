package engine.io;

import engine.program.SProgram;

import java.nio.file.Path;

public interface ProgramParser {
    SProgram parse(Path xmlPath) throws ProgramParseException;
}
