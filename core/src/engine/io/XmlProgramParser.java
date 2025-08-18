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
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class XmlProgramParser implements ProgramParser {

    @Override
    public SProgram parse(Path xmlPath) throws ProgramParseException {
        try {
            if (xmlPath == null)
                throw new ProgramParseException("XML path is null");
            if (!Files.exists(xmlPath) || !Files.isRegularFile(xmlPath))
                throw new ProgramParseException("XML file not found: " + xmlPath);
            String fileName = xmlPath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            if (dot < 0 || !fileName.substring(dot + 1).equalsIgnoreCase("xml"))
                throw new ProgramParseException("File must have .xml extension: " + fileName);

            // Secure DOM factory (XXE-safe)
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {}

            var builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlPath.toFile());

            Element root = doc.getDocumentElement();
            if (root == null || !"S-Program".equals(root.getNodeName()))
                throw new ProgramParseException("Root element must be <S-Program>");

            String progName = attr(root, "name", "PROGRAM").trim();
            List<SInstruction> instructions = new ArrayList<>();

            // iterate only direct child S-Instruction nodes
            NodeList nodes = root.getElementsByTagName("S-Instruction");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);


                String typeAttr = attr(e, "type", "");
                InstructionKind kind = "basic".equalsIgnoreCase(typeAttr)
                        ? InstructionKind.BASIC : InstructionKind.SYNTHETIC;

                String name = attr(e, "name", "").trim();

                Variable variable = null;
                Element varEl = child(e, "S-Variable");
                String varToken = textOpt(varEl);
                if (varToken != null && !varToken.isBlank()) {
                    variable = Variable.ofToken(varToken);
                }

                Label lineLabel = parseLineLabel(e);

                Map<String, String> args = parseArgs(e);

                SInstruction ins = buildInstruction(kind, name, lineLabel, variable, args);
                instructions.add(ins);
            }

            SProgramImpl program = new SProgramImpl(progName, instructions);
            program.validate(); // בדיקות כפילות/חוקיות של לייבלים וכו'
            return program;

        } catch (ProgramParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProgramParseException("Failed to parse XML: " + ex.getMessage(), ex);
        }
    }

    /* ---------- helpers ---------- */

    private static String attr(Element e, String name, String def) {
        return e.hasAttribute(name) ? e.getAttribute(name) : def;
    }

    private static String textOpt(Element e) {
        if (e == null) return null;
        String t = e.getTextContent();
        return (t == null) ? null : t.trim();
    }

    // returns first direct child with tag name
    private static Element child(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == parent) return (Element) n;
        }
        return null;
    }

    // Line label (of the instruction itself) – EXIT/EMPTY are forbidden here
    private static Label parseLineLabel(Element insEl) throws ProgramParseException {
        String s = textOpt(child(insEl, "S-Label"));
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        if ("EXIT".equalsIgnoreCase(s) || "EMPTY".equalsIgnoreCase(s))
            throw new ProgramParseException("Line label cannot be " + s);
        return new UserLabel(s);
    }

    // Normalize argument keys to lowercase; trim values
    private static Map<String, String> parseArgs(Element insEl) {
        Map<String, String> map = new HashMap<>();
        Element args = child(insEl, "S-Instruction-Arguments");
        if (args == null) return map;
        NodeList nl = args.getElementsByTagName("S-Instruction-Argument");
        for (int i = 0; i < nl.getLength(); i++) {
            Element a = (Element) nl.item(i);
            String n = a.getAttribute("name");
            String v = a.getAttribute("value");
            if (n != null && !n.isBlank()) {
                map.put(n.trim().toLowerCase(Locale.ROOT), v == null ? "" : v.trim());
            }
        }
        return map;
    }

    // Jump target (arguments) – EXIT/EMPTY allowed
    private static Label toLabel(String token) {
        if (token == null || token.isBlank()) return FixedLabel.EXIT;
        String t = token.trim();
        if ("EXIT".equalsIgnoreCase(t))  return FixedLabel.EXIT;
        if ("EMPTY".equalsIgnoreCase(t)) return FixedLabel.EMPTY;
        return new UserLabel(t);
    }

    private static SInstruction buildInstruction(
            InstructionKind kind, String name, Label lineLabel, Variable v, Map<String, String> args
    ) throws ProgramParseException {
        String N = name.toUpperCase(Locale.ROOT);

        if (kind == InstructionKind.BASIC) {
            return switch (N) {
                case "INCREASE" -> reqVar(lineLabel, v, new IncreaseInstruction(lineLabel, v));
                case "DECREASE" -> reqVar(lineLabel, v, new DecreaseInstruction(lineLabel, v));
                case "NEUTRAL"  -> reqVar(lineLabel, v, new NeutralInstruction(lineLabel, v));
                case "JUMP_NOT_ZERO" -> {
                    if (v == null) throw new ProgramParseException("JUMP_NOT_ZERO requires variable");
                    Label tgt = toLabel(args.getOrDefault("jnzlabel", "EXIT"));
                    yield new JumpNotZeroInstruction(lineLabel, v, tgt);
                }
                default -> throw new ProgramParseException("Unknown BASIC instruction: " + N);
            };
        } else {
            return switch (N) {
                case "ZERO_VARIABLE" -> reqVar(lineLabel, v, new ZeroVariableInstruction(lineLabel, v));
                case "GOTO_LABEL"    -> new GotoLabelInstruction(lineLabel, toLabel(args.getOrDefault("gotolabel", "EXIT")));
                case "ASSIGNMENT" -> {
                    Variable from = Variable.ofToken(args.get("assignedvariable"));
                    if (v == null || from == null)
                        throw new ProgramParseException("ASSIGNMENT requires variable and assignedVariable");
                    yield new AssignmentInstruction(lineLabel, v, from);
                }
                case "CONSTANT_ASSIGNMENT" -> {
                    if (v == null) throw new ProgramParseException("CONSTANT_ASSIGNMENT requires variable");
                    long K = parseNonNegative(args.getOrDefault("constantvalue",
                            args.getOrDefault("constant", "0")));
                    yield new ConstantAssignmentInstruction(lineLabel, v, K);
                }
                case "JUMP_ZERO" -> {
                    if (v == null) throw new ProgramParseException("JUMP_ZERO requires variable");
                    Label tgt = toLabel(args.getOrDefault("jzlabel", "EXIT"));
                    yield new JumpZeroInstruction(lineLabel, v, tgt);
                }
                case "JUMP_EQUAL_CONSTANT" -> {
                    if (v == null) throw new ProgramParseException("JUMP_EQUAL_CONSTANT requires variable");
                    long K = parseNonNegative(args.getOrDefault("constantvalue",
                            args.getOrDefault("constant", "0")));
                    Label tgt = toLabel(args.getOrDefault("jeconstantlabel", "EXIT"));
                    yield new JumpEqualConstantInstruction(lineLabel, v, K, tgt);
                }
                case "JUMP_EQUAL_VARIABLE" -> {
                    if (v == null) throw new ProgramParseException("JUMP_EQUAL_VARIABLE requires variable");
                    Variable other = Variable.ofToken(args.get("variablename"));
                    if (other == null) throw new ProgramParseException("JUMP_EQUAL_VARIABLE requires variableName");
                    Label tgt = toLabel(args.getOrDefault("jevariablelabel", "EXIT"));
                    yield new JumpEqualVariableInstruction(lineLabel, v, other, tgt);
                }
                default -> throw new ProgramParseException("Unknown SYNTHETIC instruction: " + N);
            };
        }
    }

    private static long parseNonNegative(String s) throws ProgramParseException {
        try {
            long v = Long.parseLong(s.trim());
            return (v < 0) ? 0 : v;
        } catch (Exception e) {
            throw new ProgramParseException("Illegal numeric value: " + s);
        }
    }

    private static SInstruction reqVar(Label l, Variable v, SInstruction inst) throws ProgramParseException {
        if (v == null)
            throw new ProgramParseException("Instruction requires <S-Variable> at " +
                    ((l == null || l == FixedLabel.EMPTY) ? "unlabeled line" : l.labelName()));
        return inst;
    }
}
