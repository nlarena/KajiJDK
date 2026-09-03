package javax.xml.transform.sax;

import javax.xml.transform.Templates;
import org.xml.sax.ContentHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.TemplatesHandler -- compila una hoja de estilo desde eventos.
 *
 * <p>Un {@link ContentHandler} al que se le manda una hoja de estilo por eventos SAX y que, cuando
 * termina, entrega la hoja ya compilada en {@link #getTemplates}.
 *
 * <p>Sirve para lo mismo que un {@code TransformerFactory.newTemplates(Source)}, con una diferencia
 * que a veces decide: la hoja de estilo puede venir de algo que <b>no es un archivo</b> -- la salida
 * de otra transformacion, un filtro, un flujo que se esta generando-- sin tener que escribirla en
 * ningun lado primero.
 *
 * <p>{@link #getTemplates} antes de que termine el documento no tiene sentido y devuelve null: hasta
 * el {@code endDocument} no hay hoja compilada.
 *
 * <p>El identificador de sistema hay que ponerlo <b>antes</b> de empezar a mandar eventos, porque es
 * contra el que se resuelven los {@code xsl:import} y {@code xsl:include} que aparezcan.
 */
public interface TemplatesHandler extends ContentHandler {

    /**
     * La hoja compilada.
     *
     * @return null si el documento todavia no termino
     */
    Templates getTemplates();

    /** De donde sale la hoja. Ponerlo antes del primer evento; ver la nota de la clase. */
    void setSystemId(String systemID);

    /** Ver {@link #setSystemId}. */
    String getSystemId();
}
