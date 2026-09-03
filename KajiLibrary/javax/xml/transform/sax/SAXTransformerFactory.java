package javax.xml.transform.sax;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.xml.sax.XMLFilter;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXTransformerFactory -- la fabrica de las piezas SAX.
 *
 * <p>Extiende {@link TransformerFactory} con lo que hace falta para transformar <b>por eventos</b>:
 * los {@link TransformerHandler}, los {@link TemplatesHandler} y los filtros.
 *
 * <h2>Como se consigue</h2>
 *
 * <p>No tiene {@code newInstance} propio. Se pide un {@code TransformerFactory} normal, se le
 * pregunta por {@link #FEATURE} y, si dice que si, se lo convierte con un cast. Es incomodo y es
 * deliberado: esta clase llego despues, y agregarle un {@code newInstance} a la jerarquia habria
 * obligado a toda implementacion existente a soportar la parte SAX.
 *
 * <p>{@link #FEATURE_XMLFILTER} es una segunda pregunta, para los dos {@code newXMLFilter}: una
 * implementacion puede dar los manejadores y no los filtros.
 *
 * <h2>Las dos formas de cada cosa</h2>
 *
 * <p>Casi todo viene por duplicado, con {@link Source} y con {@link Templates}, y la diferencia es de
 * costo. Con {@code Source} la hoja de estilo se compila <b>en cada llamada</b>; con
 * {@code Templates} ya esta compilada y se reusa. Para una hoja que se aplica muchas veces, esa es
 * toda la diferencia de rendimiento que hay para ganar.
 *
 * <p>{@link #newTransformerHandler()} sin argumentos da uno que no transforma nada: copia la entrada
 * a la salida. Suena inutil y no lo es -- es la forma de convertir una cadena de eventos en un
 * documento, o de serializarla, usando solo las propiedades de salida del transformador.
 */
public abstract class SAXTransformerFactory extends TransformerFactory {

    /** Con esto se pregunta si una fabrica es de estas. Ver la nota de la clase. */
    public static final String FEATURE =
        "http://javax.xml.transform.sax.SAXTransformerFactory/feature";

    /** Y con esto, si ademas sabe hacer filtros. */
    public static final String FEATURE_XMLFILTER =
        "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter";

    /** Para las subclases. */
    protected SAXTransformerFactory() {
    }

    /**
     * Un manejador que aplica esa hoja de estilo.
     *
     * <p>La compila en esta llamada; ver la nota de la clase sobre el costo.
     *
     * @throws TransformerConfigurationException si la hoja esta mal
     */
    public abstract TransformerHandler newTransformerHandler(Source src)
        throws TransformerConfigurationException;

    /** Idem, con la hoja ya compilada. */
    public abstract TransformerHandler newTransformerHandler(Templates templates)
        throws TransformerConfigurationException;

    /** Uno que copia la entrada a la salida. Ver la nota de la clase sobre para que sirve. */
    public abstract TransformerHandler newTransformerHandler()
        throws TransformerConfigurationException;

    /** Un manejador que compila una hoja de estilo que llega por eventos. */
    public abstract TemplatesHandler newTemplatesHandler()
        throws TransformerConfigurationException;

    /**
     * Un filtro SAX que aplica esa hoja de estilo.
     *
     * <p>Un {@link XMLFilter} se encadena con {@code setParent}, asi que esto deja meter una
     * transformacion adentro de una cadena de lectura ya armada, sin tocar el resto.
     */
    public abstract XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException;

    /** Idem, con la hoja ya compilada. */
    public abstract XMLFilter newXMLFilter(Templates templates)
        throws TransformerConfigurationException;
}
