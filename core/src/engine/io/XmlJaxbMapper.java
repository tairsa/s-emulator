package engine.io;

import engine.instruction.SInstruction;
import engine.instruction.basic.DecreaseInstruction;
import engine.instruction.basic.IncreaseInstruction;
import engine.instruction.basic.JumpNotZeroInstruction;
import engine.instruction.basic.NeutralInstruction;
import engine.instruction.synthetic.*;
import engine.label.FixedLabel;
import engine.label.Label;
import engine.label.UserLabel;
import engine.program.SProgram;
import engine.program.SProgramImpl;
import engine.variable.Variable;

import java.util.*;

final class XmlJaxbMapper {

    static SProgram map(engine.io.jaxb.SProgram dto) throws ProgramParseException {
        String progName = (dto.getName() == null) ? "PROGRAM" : dto.getName().trim();
        var out = new ArrayList<SInstruction>();

        var list = dto.getSInstructions().getSInstruction(); // רשימת ההוראות שנוצרה ע״י xjc
        for (engine.io.jaxb.SInstruction i : list) {
            out.add(mapOne(i));
        }
        return new SProgramImpl(progName, out);
    }

    private static SInstruction mapOne(engine.io.jaxb.SInstruction i) throws ProgramParseException {
        Label lineLabel = toLineLabelStrict(i.getSLabel());

        String name = up(i.getName());
        String type = up(i.getType());
        String varTok = trimToNull(i.getSVariable());
        Variable var = (varTok == null) ? null : Variable.ofToken(varTok);

        Map<String,String> args = lowerArgMap(i);

        if ("BASIC".equals(type)) {
            return switch (name) {
                case "INCREASE" -> reqVar(lineLabel, var, new IncreaseInstruction(lineLabel, var));
                case "DECREASE" -> reqVar(lineLabel, var, new DecreaseInstruction(lineLabel, var));
                case "NEUTRAL"  -> reqVar(lineLabel, var, new NeutralInstruction(lineLabel, var));

                case "JUMP_NOT_ZERO" -> {
                    if (var == null) throw new ProgramParseException("JUMP_NOT_ZERO requires variable");
                    Label tgt = toTargetLabel(args.getOrDefault("jnzlabel", "EXIT"));
                    yield new JumpNotZeroInstruction(lineLabel, var, tgt);
                }

                default -> throw new ProgramParseException("Unknown BASIC instruction: " + name);
            };
        } else {
            return switch (name) {
                case "ZERO_VARIABLE" -> reqVar(lineLabel, var, new ZeroVariableInstruction(lineLabel, var));

                case "GOTO_LABEL", "GOTO" -> {
                    Label tgt = toTargetLabel(args.getOrDefault("gotolabel", "EXIT"));
                    yield new GotoLabelInstruction(lineLabel, tgt);
                }

                case "ASSIGNMENT" -> {
                    if (var == null) throw new ProgramParseException("ASSIGNMENT requires variable");
                    String rhsTok = reqArg(name, "assignedVariable",
                            firstNonBlank(args.get("assignedvariable"), args.get("rhsvariable")));
                    Variable rhs = Variable.ofToken(rhsTok);
                    yield new AssignmentInstruction(lineLabel, var, rhs);
                }

                case "CONSTANT_ASSIGNMENT" -> {
                    if (var == null) throw new ProgramParseException("CONSTANT_ASSIGNMENT requires variable");
                    long k = parseNonNegative(reqArg(name, "constantValue",
                            firstNonBlank(args.get("constantvalue"), args.get("constant"))));
                    yield new ConstantAssignmentInstruction(lineLabel, var, k);
                }

                case "JUMP_ZERO" -> {
                    if (var == null) throw new ProgramParseException("JUMP_ZERO requires variable");
                    Label tgt = toTargetLabel(args.getOrDefault("jzlabel", "EXIT"));
                    yield new JumpZeroInstruction(lineLabel, var, tgt);
                }

                case "JUMP_EQUAL_CONSTANT" -> {
                    if (var == null) throw new ProgramParseException("JUMP_EQUAL_CONSTANT requires variable");
                    long k = parseNonNegative(reqArg(name, "constantValue",
                            firstNonBlank(args.get("constantvalue"), args.get("constant"))));
                    Label tgt = toTargetLabel(args.getOrDefault("jeconstantlabel", "EXIT"));
                    yield new JumpEqualConstantInstruction(lineLabel, var, k, tgt);
                }

                case "JUMP_EQUAL_VARIABLE" -> {
                    if (var == null) throw new ProgramParseException("JUMP_EQUAL_VARIABLE requires variable");
                    String otherTok = reqArg(name, "variableName",
                            firstNonBlank(args.get("variablename"), args.get("othervariable")));
                    Variable other = Variable.ofToken(otherTok);
                    Label tgt = toTargetLabel(args.getOrDefault("jevariablelabel", "EXIT"));
                    yield new JumpEqualVariableInstruction(lineLabel, var, other, tgt);
                }

                default -> throw new ProgramParseException("Unknown SYNTHETIC instruction: " + name);
            };
        }
    }


    private static String up(String s) { return (s == null) ? "" : s.trim().toUpperCase(Locale.ROOT); }
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static Label toLineLabelStrict(String s) throws ProgramParseException {
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        String t = s.trim();
        if ("EXIT".equalsIgnoreCase(t) || "EMPTY".equalsIgnoreCase(t))
            throw new ProgramParseException("Line label cannot be " + t);
        return new UserLabel(t);
    }

    private static Label toTargetLabel(String token) {
        if (token == null || token.isBlank()) return FixedLabel.EXIT;
        String t = token.trim();
        if ("EXIT".equalsIgnoreCase(t))  return FixedLabel.EXIT;
        if ("EMPTY".equalsIgnoreCase(t)) return FixedLabel.EMPTY;
        return new UserLabel(t);
    }

    private static Map<String,String> lowerArgMap(engine.io.jaxb.SInstruction i) {
        Map<String,String> m = new HashMap<>();
        var args = i.getSInstructionArguments();
        if (args == null || args.getSInstructionArgument() == null) return m;
        for (var a : args.getSInstructionArgument()) {
            String n = a.getName();
            String v = a.getValue();
            if (n != null && !n.isBlank()) {
                m.put(n.trim().toLowerCase(Locale.ROOT), v == null ? "" : v.trim());
            }
        }
        return m;
    }

    private static long parseNonNegative(String s) throws ProgramParseException {
        if (s == null || s.isBlank()) throw new ProgramParseException("Illegal numeric value: " + s);
        try {
            long v = Long.parseLong(s.trim());
            return (v < 0) ? 0 : v;
        } catch (Exception e) {
            throw new ProgramParseException("Illegal numeric value: " + s);
        }
    }

    private static <T extends SInstruction> T reqVar(Label l, Variable v, T inst) throws ProgramParseException {
        if (v == null)
            throw new ProgramParseException("Instruction requires <S-Variable> at " +
                    ((l == null || l == FixedLabel.EMPTY) ? "unlabeled line" : l.labelName()));
        return inst;
    }

    private static String reqArg(String ins, String key, String v) throws ProgramParseException {
        if (v == null || v.isBlank())
            throw new ProgramParseException("Missing arg '" + key + "' in " + ins);
        return v;
    }
}
