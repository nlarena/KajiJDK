package javax.swing.text.rtf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * The editor kit for RTF.
 *
 * <h2>What RTF is and why it is here</h2>
 *
 * <p>It is the format word processors pass styled documents to each other with. Everything goes
 * in printable characters: the marks are words that start with a backslash
 * (<code>\b</code> for bold, <code>\par</code> for a paragraph) and the groups go between
 * braces.
 *
 * <p>It is in the library because a {@code JTextPane} can read and write RTF just by changing
 * its editor kit, without the program knowing anything about the format.
 *
 * <h2>A byte format with a character reader</h2>
 *
 * <p>The four methods are two pairs: one of bytes and one of characters. RTF is ASCII by
 * definition -- what does not fit goes escaped as <code>\'xx</code> --, so the two pairs can
 * share the same work without losing anything.
 *
 * <h2>How far it goes</h2>
 *
 * <p>It reads and writes the text with bold, italic, underline and paragraphs. What it does not
 * do is tables, embedded images or named typefaces: they are the part of the format that needs
 * resource tables at the start of the file, and this library does not build them yet. A document
 * with those is read all the same, without those attributes.
 */
public class RTFEditorKit extends StyledEditorKit {

    /** An RTF editor kit. */
    public RTFEditorKit() {
        super();
    }

    /** Always {@code text/rtf}. */
    public String getContentType() {
        return "text/rtf";
    }

    /** Reads RTF from a byte stream. */
    public void read(InputStream in, Document doc, int pos) throws IOException,
            BadLocationException {
        read(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.US_ASCII),
                doc, pos);
    }

    /** Writes the document as RTF to a byte stream. */
    public void write(OutputStream out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        Writer w = new java.io.OutputStreamWriter(out,
                java.nio.charset.StandardCharsets.US_ASCII);
        write(w, doc, pos, len);
        w.flush();
    }

    /**
     * Reads RTF and puts it into the document.
     *
     * @throws IOException if the stream fails.
     * @throws BadLocationException if the position does not exist.
     */
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        StringBuilder text = new StringBuilder();
        javax.swing.text.MutableAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '{' || c == '}') {
                // The groups delimit the attributes' scope; here they are only skipped.
                continue;
            }
            if (c != '\\') {
                text.append((char) c);
                continue;
            }
            flush(doc, pos, text, attr);
            pos = doc.getLength();
            c = readControl(in, attr, doc, pos);
            if (c == -1) {
                break;
            }
            if (c != ' ') {
                text.append((char) c);
            }
        }
        flush(doc, pos, text, attr);
    }

    /** Puts what was gathered into the document and empties the gatherer. */
    private void flush(Document doc, int pos, StringBuilder text,
            javax.swing.text.AttributeSet attr) throws BadLocationException {
        if (text.length() == 0) {
            return;
        }
        doc.insertString(Math.min(pos, doc.getLength()), text.toString(), attr);
        text.setLength(0);
    }

    /**
     * Reads a control word and applies it.
     *
     * @return the character that ended it, or -1 if the stream ran out.
     */
    private int readControl(Reader in, javax.swing.text.MutableAttributeSet attr, Document doc,
            int pos) throws IOException, BadLocationException {
        StringBuilder word = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && Character.isLetter((char) c)) {
            word.append((char) c);
        }
        StringBuilder number = new StringBuilder();
        while (c != -1 && (Character.isDigit((char) c) || c == '-')) {
            number.append((char) c);
            c = in.read();
        }
        apply(word.toString(), number.toString(), attr, doc);
        return c;
    }

    /** Applies a control word to the attributes that hold now. */
    private void apply(String word, String number,
            javax.swing.text.MutableAttributeSet attr, Document doc)
            throws BadLocationException {
        boolean on = !"0".equals(number);
        if (word.equals("b")) {
            StyleConstants.setBold(attr, on);
        } else if (word.equals("i")) {
            StyleConstants.setItalic(attr, on);
        } else if (word.equals("ul")) {
            StyleConstants.setUnderline(attr, on);
        } else if (word.equals("ulnone")) {
            StyleConstants.setUnderline(attr, false);
        } else if (word.equals("fs") && number.length() > 0) {
            // In RTF the size goes in half points.
            try {
                StyleConstants.setFontSize(attr, Integer.parseInt(number) / 2);
            } catch (NumberFormatException nfe) {
                // A size that is not understood leaves the one that was there.
            }
        } else if (word.equals("par")) {
            doc.insertString(doc.getLength(), "\n", attr);
        } else if (word.equals("plain")) {
            attr.removeAttributes(attr);
        }
    }

    /** Writes the document as RTF. */
    public void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        out.write("{\\rtf1\\ansi\n");
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;

        if (doc instanceof StyledDocument) {
            StyledDocument sd = (StyledDocument) doc;
            int i = pos;
            int end = pos + len;
            while (i < end) {
                Element e = sd.getCharacterElement(i);
                int upTo = Math.min(e.getEndOffset(), end);
                javax.swing.text.AttributeSet a = e.getAttributes();
                bold = mark(out, "b", StyleConstants.isBold(a), bold);
                italic = mark(out, "i", StyleConstants.isItalic(a), italic);
                underline = mark(out, "ul", StyleConstants.isUnderline(a), underline);
                out.write(escape(doc.getText(i, upTo - i)));
                i = upTo;
            }
        } else {
            out.write(escape(doc.getText(pos, len)));
        }
        out.write("}\n");
    }

    /** Writes the mark only if the state changes; repeating it does nothing and takes room. */
    private boolean mark(Writer out, String word, boolean wanted, boolean current)
            throws IOException {
        if (wanted == current) {
            return current;
        }
        out.write("\\" + word + (wanted ? "" : "0") + " ");
        return wanted;
    }

    /**
     * Escapes what in RTF cannot be written as it is.
     *
     * <p>The format's three marks ({@code \}, <code>{</code>, <code>}</code>) and everything that
     * is not ASCII, which goes as <code>\'xx</code>. The end of line is written as
     * <code>\par</code>: a bare line break in the file means nothing in RTF.
     */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\par\n");
            } else if (c < 32 || c > 126) {
                String h = Integer.toHexString(c & 0xFF);
                sb.append("\\'").append((h.length() == 1) ? "0" + h : h);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
