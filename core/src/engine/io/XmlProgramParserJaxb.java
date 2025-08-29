package engine.io;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.nio.file.Path;

public class XmlProgramParserJaxb implements ProgramParser {
    @Override
    public engine.program.SProgram parse(Path xmlPath) throws ProgramParseException {
        try {
            JAXBContext ctx = JAXBContext.newInstance(engine.io.jaxb.SProgram.class);
            Unmarshaller u = ctx.createUnmarshaller();

            var dtoRoot = (engine.io.jaxb.SProgram) u.unmarshal(xmlPath.toFile());

            return XmlJaxbMapper.map(dtoRoot);

        } catch (Exception e) {
            throw new ProgramParseException("Failed to parse XML : " + e.getMessage(), e);
        }
    }
}
