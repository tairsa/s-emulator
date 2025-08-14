package engine.io;

import engine.instruction.InstructionKind;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class XmlProgramParser implements ProgramParser {

    @Override
    public SProgram parse(Path xmlPath) throws ProgramParseException {
        try {
            if (xmlPath == null || !Files.exists(xmlPath) || !xmlPath.toString().toLowerCase().endsWith(".xml"))
                throw new ProgramParseException("XML file not found or invalid: " + xmlPath);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlPath.toFile());
            Element root = doc.getDocumentElement();
            if (!"S-Program".equals(root.getNodeName()))
                throw new ProgramParseException("Root element must be <S-Program>");

            String progName = attr(root, "name", "PROGRAM");
            List<SInstruction> list = new ArrayList<>();

            // Each <S-Instruction> line
            NodeList nodes = root.getElementsByTagName("S-Instruction");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);

                InstructionKind kind = "basic".equalsIgnoreCase(attr(e, "type", "")) ?
                        InstructionKind.BASIC : InstructionKind.SYNTHETIC;
                String name = attr(e, "name", "").trim();
                Variable variable = Variable.ofToken(textOpt(child(e, "S-Variable")));
                Label lineLabel = parseLineLabel(e);

                Map<String,String> args = parseArgs(e);

                SInstruction ins = buildInstruction(kind, name, lineLabel, variable, args);
                list.add(ins);
            }

            // Validate that all targets exist (labels), except EXIT
            Set<String> declared = new HashSet<>();
            for (SInstruction ins : list) {
                if (ins.lineLabel() != null && ins.lineLabel() != FixedLabel.EMPTY)
                    declared.add(ins.lineLabel().labelName());
            }
            for (SInstruction ins : list) {
                String r = ins.render();
                // crude scan for "GOTO Lxx"
                int idx = r.indexOf("GOTO ");
                if (idx >= 0) {
                    String t = r.substring(idx + 5).trim();
                    if (!"EXIT".equals(t) && !declared.contains(t)) {
                        throw new ProgramParseException("Unknown label target: " + t);
                    }
                }
            }

            return new SProgramImpl(progName, list);
        } catch (ProgramParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProgramParseException("Failed to parse XML: " + ex.getMessage(), ex);
        }
    }

    private static String attr(Element e, String name, String def) {
        return e.hasAttribute(name) ? e.getAttribute(name) : def;
    }

    private static String textOpt(Element e) {
        if (e == null) return null;
        String t = e.getTextContent();
        return (t == null) ? null : t.trim();
    }

    private static Element child(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        for (int i=0;i<nl.getLength();i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == parent) return (Element) n;
        }
        return null;
    }

    private static Label parseLineLabel(Element insEl) {
        String s = textOpt(child(insEl, "S-Label"));
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        if ("EXIT".equalsIgnoreCase(s)) return FixedLabel.EXIT;
        return new UserLabel(s);
    }

    private static Map<String,String> parseArgs(Element insEl) {
        Map<String,String> map = new HashMap<>();
        Element args = child(insEl, "S-Instruction-Arguments");
        if (args == null) return map;
        NodeList nl = args.getElementsByTagName("S-Instruction-Argument");
        for (int i=0;i<nl.getLength();i++) {
            Element a = (Element) nl.item(i);
            String n = a.getAttribute("name");
            String v = a.getAttribute("value");
            if (n != null && !n.isBlank()) map.put(n, v == null ? "" : v);
        }
        return map;
    }

    private static Label toLabel(String token) {
        if (token == null || token.isBlank()) return FixedLabel.EXIT;
        if ("EXIT".equalsIgnoreCase(token)) return FixedLabel.EXIT;
        return new UserLabel(token);
    }

    private static SInstruction buildInstruction(InstructionKind kind, String name, Label lineLabel, Variable v, Map<String,String> args)
            throws ProgramParseException {
        // normalize name
        String N = name.toUpperCase(Locale.ROOT);

        if (kind == InstructionKind.BASIC) {
            return switch (N) {
                case "INCREASE" -> reqVar(lineLabel, v, new IncreaseInstruction(lineLabel, v));
                case "DECREASE" -> reqVar(lineLabel, v, new DecreaseInstruction(lineLabel, v));
                case "NEUTRAL"  -> new NeutralInstruction(lineLabel);
                case "JUMP_NOT_ZERO" -> {
                    Label tgt = toLabel(args.getOrDefault("gotoLabel", "EXIT"));
                    if (v == null) throw new ProgramParseException("JUMP_NOT_ZERO requires variable");
                    yield new JumpNotZeroInstruction(lineLabel, v, tgt);
                }
                default -> throw new ProgramParseException("Unknown BASIC instruction: " + N);
            };
        } else {
            return switch (N) {
                case "ZERO_VARIABLE" -> reqVar(lineLabel, v, new ZeroVariableInstruction(lineLabel, v));
                case "GOTO_LABEL" -> new GotoLabelInstruction(lineLabel, toLabel(args.getOrDefault("gotoLabel", "EXIT")));
                case "ASSIGNMENT" -> {
                    Variable from = Variable.ofToken(args.get("assignedVariable"));
                    if (v == null || from == null) throw new ProgramParseException("ASSIGNMENT requires variable and assignedVariable");
                    yield new AssignmentInstruction(lineLabel, v, from);
                }
                case "CONSTANT_ASSIGNMENT" -> {
                    if (v == null) throw new ProgramParseException("CONSTANT_ASSIGNMENT requires variable");
                    long K = parseNonNegative(args.getOrDefault("constantValue", "0"));
                    yield new ConstantAssignmentInstruction(lineLabel, v, K);
                }
                case "JUMP_ZERO" -> {
                    if (v == null) throw new ProgramParseException("JUMP_ZERO requires variable");
                    Label tgt = toLabel(args.getOrDefault("JZLabel", "EXIT"));
                    yield new JumpZeroInstruction(lineLabel, v, tgt);
                }
                case "JUMP_EQUAL_CONSTANT" -> {
                    if (v == null) throw new ProgramParseException("JUMP_EQUAL_CONSTANT requires variable");
                    long K = parseNonNegative(args.getOrDefault("constantValue", "0"));
                    Label tgt = toLabel(args.getOrDefault("JEConstantLabel", "EXIT"));
                    yield new JumpEqualConstantInstruction(lineLabel, v, K, tgt);
                }
                case "JUMP_EQUAL_VARIABLE" -> {
                    if (v == null) throw new ProgramParseException("JUMP_EQUAL_VARIABLE requires variable");
                    Variable other = Variable.ofToken(args.get("variableName"));
                    if (other == null) throw new ProgramParseException("JUMP_EQUAL_VARIABLE requires variableName");
                    Label tgt = toLabel(args.getOrDefault("JEVariableLabel", "EXIT"));
                    yield new JumpEqualVariableInstruction(lineLabel, v, other, tgt);
                }
                default -> throw new ProgramParseException("Unknown SYNTHETIC instruction: " + N);
            };
        }
    }

    private static long parseNonNegative(String s) throws ProgramParseException {
        try {
            long v = Long.parseLong(s.trim());
            if (v < 0) v = 0;
            return v;
        } catch (Exception e) {
            throw new ProgramParseException("Illegal constant value: " + s);
        }
    }

    private static SInstruction reqVar(Label l, Variable v, SInstruction inst) throws ProgramParseException {
        if (v == null) throw new ProgramParseException("Instruction requires variable on line with label: " + (l == null ? "" : l.labelName()));
        return inst;
    }
}
