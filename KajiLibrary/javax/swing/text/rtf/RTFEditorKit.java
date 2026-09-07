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
 * El juego de edicion para RTF.
 *
 * <h2>Que es RTF y por que esta aca</h2>
 *
 * <p>Es el formato con el que los procesadores de texto se pasan documentos con estilos. Todo va en
 * caracteres imprimibles: las marcas son palabras que empiezan con barra invertida
 * (<code>\b</code> para negrita, <code>\par</code> para parrafo) y los grupos van entre llaves.
 *
 * <p>Esta en la biblioteca porque un {@code JTextPane} puede leer y escribir RTF con solo cambiarle
 * el juego de edicion, sin que el programa sepa nada del formato.
 *
 * <h2>Un formato de bytes con un lector de caracteres</h2>
 *
 * <p>Los cuatro metodos son dos pares: uno de bytes y uno de caracteres. RTF es ASCII por
 * definicion -- lo que no entra va escapado como <code>\'xx</code> --, asi que los dos pares
 * pueden compartir el mismo trabajo sin perder nada.
 *
 * <h2>Hasta donde llega</h2>
 *
 * <p>Lee y escribe el texto con negrita, cursiva, subrayado y parrafos. Lo que no hace es tablas,
 * imagenes incrustadas ni tipografias con nombre: son la parte del formato que necesita tablas de
 * recursos al principio del archivo, y esta biblioteca todavia no las arma. Un documento con eso se
 * lee igual, sin esos atributos.
 */
public class RTFEditorKit extends StyledEditorKit {

    /** Un juego de edicion de RTF. */
    public RTFEditorKit() {
        super();
    }

    /** Siempre {@code text/rtf}. */
    public String getContentType() {
        return "text/rtf";
    }

    /** Lee RTF de un flujo de bytes. */
    public void read(InputStream in, Document doc, int pos) throws IOException,
            BadLocationException {
        read(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.US_ASCII),
                doc, pos);
    }

    /** Escribe el documento como RTF en un flujo de bytes. */
    public void write(OutputStream out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        Writer w = new java.io.OutputStreamWriter(out,
                java.nio.charset.StandardCharsets.US_ASCII);
        write(w, doc, pos, len);
        w.flush();
    }

    /**
     * Lee RTF y lo mete en el documento.
     *
     * @throws IOException si el flujo falla.
     * @throws BadLocationException si la posicion no existe.
     */
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        StringBuilder texto = new StringBuilder();
        javax.swing.text.MutableAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '{' || c == '}') {
                // Los grupos delimitan el alcance de los atributos; aca solo se saltean.
                continue;
            }
            if (c != '\\') {
                texto.append((char) c);
                continue;
            }
            volcar(doc, pos, texto, attr);
            pos = doc.getLength();
            c = leerControl(in, attr, doc, pos);
            if (c == -1) {
                break;
            }
            if (c != ' ') {
                texto.append((char) c);
            }
        }
        volcar(doc, pos, texto, attr);
    }

    /** Mete lo juntado en el documento y vacia el juntador. */
    private void volcar(Document doc, int pos, StringBuilder texto,
            javax.swing.text.AttributeSet attr) throws BadLocationException {
        if (texto.length() == 0) {
            return;
        }
        doc.insertString(Math.min(pos, doc.getLength()), texto.toString(), attr);
        texto.setLength(0);
    }

    /**
     * Lee una palabra de control y la aplica.
     *
     * @return el caracter que la termino, o -1 si se acabo el flujo.
     */
    private int leerControl(Reader in, javax.swing.text.MutableAttributeSet attr, Document doc,
            int pos) throws IOException, BadLocationException {
        StringBuilder palabra = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && Character.isLetter((char) c)) {
            palabra.append((char) c);
        }
        StringBuilder numero = new StringBuilder();
        while (c != -1 && (Character.isDigit((char) c) || c == '-')) {
            numero.append((char) c);
            c = in.read();
        }
        aplicar(palabra.toString(), numero.toString(), attr, doc);
        return c;
    }

    /** Aplica una palabra de control a los atributos que valen ahora. */
    private void aplicar(String palabra, String numero,
            javax.swing.text.MutableAttributeSet attr, Document doc)
            throws BadLocationException {
        boolean prende = !"0".equals(numero);
        if (palabra.equals("b")) {
            StyleConstants.setBold(attr, prende);
        } else if (palabra.equals("i")) {
            StyleConstants.setItalic(attr, prende);
        } else if (palabra.equals("ul")) {
            StyleConstants.setUnderline(attr, prende);
        } else if (palabra.equals("ulnone")) {
            StyleConstants.setUnderline(attr, false);
        } else if (palabra.equals("fs") && numero.length() > 0) {
            // En RTF el tamano va en medios puntos.
            try {
                StyleConstants.setFontSize(attr, Integer.parseInt(numero) / 2);
            } catch (NumberFormatException nfe) {
                // Un tamano que no se entiende deja el que estaba.
            }
        } else if (palabra.equals("par")) {
            doc.insertString(doc.getLength(), "\n", attr);
        } else if (palabra.equals("plain")) {
            attr.removeAttributes(attr);
        }
    }

    /** Escribe el documento como RTF. */
    public void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        out.write("{\\rtf1\\ansi\n");
        boolean negrita = false;
        boolean cursiva = false;
        boolean subrayado = false;

        if (doc instanceof StyledDocument) {
            StyledDocument sd = (StyledDocument) doc;
            int i = pos;
            int fin = pos + len;
            while (i < fin) {
                Element e = sd.getCharacterElement(i);
                int hasta = Math.min(e.getEndOffset(), fin);
                javax.swing.text.AttributeSet a = e.getAttributes();
                negrita = marca(out, "b", StyleConstants.isBold(a), negrita);
                cursiva = marca(out, "i", StyleConstants.isItalic(a), cursiva);
                subrayado = marca(out, "ul", StyleConstants.isUnderline(a), subrayado);
                out.write(escapar(doc.getText(i, hasta - i)));
                i = hasta;
            }
        } else {
            out.write(escapar(doc.getText(pos, len)));
        }
        out.write("}\n");
    }

    /** Escribe la marca solo si el estado cambia; repetirla no hace nada y ocupa. */
    private boolean marca(Writer out, String palabra, boolean quiere, boolean esta)
            throws IOException {
        if (quiere == esta) {
            return esta;
        }
        out.write("\\" + palabra + (quiere ? "" : "0") + " ");
        return quiere;
    }

    /**
     * Escapa lo que en RTF no se puede escribir tal cual.
     *
     * <p>Las tres marcas del formato ({@code \}, <code>{</code>, <code>}</code>) y todo lo que no
     * sea ASCII, que va como <code>\'xx</code>. El fin de linea se escribe como
     * <code>\par</code>: un salto de linea suelto en el archivo no significa nada en RTF.
     */
    private static String escapar(String s) {
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
