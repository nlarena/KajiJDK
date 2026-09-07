package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import javax.swing.text.html.HTMLEditorKit;

/**
 * El analizador que se usa cuando nadie pide otro.
 *
 * <h2>Que agrega sobre {@link DocumentParser}</h2>
 *
 * <p>La DTD. Un {@code DocumentParser} necesita una y no sabe de donde sacarla; este arma la de
 * HTML 3.2 una sola vez y la comparte entre todos los analizadores del programa.
 *
 * <p>Compartirla es seguro porque, una vez armada, la DTD no cambia: el analisis solo la consulta.
 * Armar una por documento costaria ochenta elementos y quinientas entidades cada vez.
 *
 * <h2>De donde sale la DTD</h2>
 *
 * <p>De {@link Html32}, que la tiene escrita como datos. El JDK la lee de un archivo binario de su
 * imagen; aca no hay archivo que buscar. Ver la nota de esa clase.
 */
public class ParserDelegator extends HTMLEditorKit.Parser implements Serializable {

    private static DTD dtd = null;

    /** Arma la DTD de siempre, si todavia no estaba. */
    protected static synchronized void setDefaultDTD() {
        if (dtd == null) {
            dtd = createDTD(new DTD("html32"), "html32");
        }
    }

    /** Llena esa DTD con el HTML 3.2 y la registra con ese nombre. */
    protected static DTD createDTD(DTD dtd, String name) {
        Html32.llenar(dtd);
        DTD.putDTDHash(name, dtd);
        return dtd;
    }

    /** Un analizador listo para usar. */
    public ParserDelegator() {
        setDefaultDTD();
    }

    /** Analiza y reenvia a quien escucha. */
    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet)
            throws IOException {
        // Uno nuevo por llamada: el analizador guarda el estado del documento que esta leyendo, y
        // compartirlo entre dos lecturas a la vez mezclaria los dos arboles.
        new DocumentParser(dtd).parse(r, cb, ignoreCharSet);
    }
}
