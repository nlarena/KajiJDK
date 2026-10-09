package org.xml.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

// KajiLibrary's org.xml.sax.InputSource -- "here is the XML, and here is what can be known about
// where it came from".
//
// It is a mutable bag of four independent things: a public identifier, a system identifier, and
// at most one of a byte stream or a character stream, plus an encoding name that only applies to
// the byte stream. There is a precedence among them and it is the source of almost all the
// confusion, so:
//
//   1. the character stream, if it is there, wins: the characters come already decoded, and any
//      encoding set on this object is ignored (just like that of the XML declaration);
//   2. if not, the byte stream, decoded with getEncoding() if it is set, or sniffing the
//      declaration if it is not;
//   3. if not, the system identifier is opened as a URI.
//
// The system identifier matters even when a stream is given: it is what the relative references
// inside the document resolve against. Giving a parser a byte stream and no system identifier is
// legal and routinely produces a "cannot resolve relative URI" later on.
//
// isEmpty() is the test of "did the EntityResolver return me nothing?": an InputSource with no
// identifiers and no readable content, which is the conventional way of saying "reject this
// external entity". It is conservative on purpose: every stream it cannot rewind and prove empty
// counts as non-empty, and it needs mark/reset support to say otherwise.
public class InputSource {

    private String publicId;
    private String systemId;
    private InputStream byteStream;
    private String encoding;
    private Reader characterStream;

    // An empty source; the caller is expected to fill it in.
    public InputSource() {
    }

    // A source that names a URI the parser has to open on its own.
    public InputSource(String systemId) {
        setSystemId(systemId);
    }

    // A source over raw bytes. Also set the system identifier if the document has relative
    // references, and the encoding if the bytes do not announce it.
    public InputSource(InputStream byteStream) {
        setByteStream(byteStream);
    }

    // A source over characters already decoded; the encoding is not consulted.
    public InputSource(Reader characterStream) {
        setCharacterStream(characterStream);
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public InputStream getByteStream() {
        return byteStream;
    }

    // The character encoding of the byte stream, if it is known. It is ignored when a character
    // stream is set, and it is ignored when the parser opens the system identifier on its own.
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setCharacterStream(Reader characterStream) {
        this.characterStream = characterStream;
    }

    public Reader getCharacterStream() {
        return characterStream;
    }

    // True when this source names nothing and carries no content: no public identifier, no system
    // identifier, and whatever stream it has provably at its end. See the note of the class.
    public boolean isEmpty() {
        return (publicId == null && systemId == null && isStreamEmpty());
    }

    // The "provably empty" is working in earnest here. A stream that cannot be rewound throws from
    // reset(), and an unreadable one throws from read(); in both cases the answer is false, because
    // the one thing that cannot be done is to assert that a source is empty when what happened is
    // that it could not be looked at.
    private boolean isStreamEmpty() {
        boolean empty = true;
        try {
            if (byteStream != null) {
                byteStream.reset();
                int bytesRead = byteStream.available();
                if (bytesRead > 0) {
                    return false;
                }
            }
            if (characterStream != null) {
                characterStream.reset();
                int c = characterStream.read();
                characterStream.reset();
                if (c != -1) {
                    return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return empty;
    }
}
