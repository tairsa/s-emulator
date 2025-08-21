package engine.execution;

public enum ExpansionPhase {
    PHASE1,        // High -> Mid (פותח ASSIGNMENT/JEQ_* בלבד)
    PHASE2_PLUS    // Mid -> Basic (פותח ZERO/CONST/JZ/GOTO לבסיס)
}