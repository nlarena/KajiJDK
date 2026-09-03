package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;

/** Una referencia a entidad sin expandir, como evento. */
final class EvtEntityRef extends EvtBase implements EntityReference {

    private final String name;
    private final EntityDeclaration declaration;

    EvtEntityRef(String name, EntityDeclaration declaration, Location location) {
        super(XMLStreamConstants.ENTITY_REFERENCE, location);
        this.name = name;
        this.declaration = declaration;
    }

    public String getName() {
        return name;
    }

    public EntityDeclaration getDeclaration() {
        return declaration;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            writer.write('&');
            writer.write(name);
            writer.write(';');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
