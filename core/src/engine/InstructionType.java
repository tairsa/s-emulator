package engine;

public enum InstructionType
{
    // Basic
    INCREASE(true, 1),
    DECREASE(true, 1),
    JUMP_NOT_ZERO(true, 2),
    NEUTRAL(true, 0),
    // Synthetic (exercise 1 scope)
    ZERO_VARIABLE(false, 1),
    GOTO_LABEL(false, 1),
    ASSIGNMENT(false, 4),
    CONSTANT_ASSIGNMENT(false, 2),
    JUMP_ZERO(false, 2),
    JUMP_EQUAL_CONSTANT(false, 2),
    JUMP_EQUAL_VARIABLE(false, 2);

    public final boolean basic;
    public final int cycles;

    InstructionType(boolean basic, int cycles) {
        this.basic = basic;
        this.cycles = cycles;
    }
}
