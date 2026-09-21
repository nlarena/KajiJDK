package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.StartDocument;

/**
 * The start of the document as an event.
 *
 * <p>It keeps the value and whether it was declared separately, for the two optional fields; see
 * {@link StartDocument}.
 */
final class EvtStartDocument extends EvtBase implements StartDocument {

    private final String systemId;
    private final String encoding;
    private final boolean declaredEncoding;
    private final String version;
    private final boolean standalone;
    private final boolean standaloneDeclared;

    EvtStartDocument(String systemId, String encoding, boolean declaredEncoding, String version,
            boolean standalone, boolean standaloneDeclared, Location location) {
        super(XMLStreamConstants.START_DOCUMENT, location);
        if (systemId == null) {
            this.systemId = "";
        } else {
            this.systemId = systemId;
        }
        if (encoding == null) {
            this.encoding = "UTF-8";
        } else {
            this.encoding = encoding;
        }
        this.declaredEncoding = declaredEncoding;
        if (version == null) {
            this.version = "1.0";
        } else {
            this.version = version;
        }
        this.standalone = standalone;
        this.standaloneDeclared = standaloneDeclared;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getCharacterEncodingScheme() {
        return encoding;
    }

    public boolean encodingSet() {
        return declaredEncoding;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public boolean standaloneSet() {
        return standaloneDeclared;
    }

    public String getVersion() {
        return version;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("the writer cannot be null");
        }
        try {
            writer.write("<?xml version=\"");
            writer.write(version);
            writer.write('"');
            if (declaredEncoding) {
                writer.write(" encoding=\"");
                writer.write(encoding);
                writer.write('"');
            }
            if (standaloneDeclared) {
                if (standalone) {
                    writer.write(" standalone=\"yes\"");
                } else {
                    writer.write(" standalone=\"no\"");
                }
            }
            writer.write("?>");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
