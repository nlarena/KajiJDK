package javax.xml.stream;

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.stream.XMLOutputFactory -- la puerta de entrada a la escritura con StAX.
 *
 * <h2>Escribir es mas facil que leer, y por eso esto funciona entero</h2>
 *
 * <p>Un escritor de XML no analiza nada: recibe llamadas ya estructuradas --abri este elemento,
 * pone este atributo, escribi este texto-- y las convierte en caracteres, escapando lo que haga
 * falta. No hay gramatica que reconocer. Asi que aca no hay nada omitido: los dos modelos, el de
 * cursor y el de eventos, escriben de verdad.
 *
 * <h2>La unica propiedad, y lo que decide</h2>
 *
 * <p>{@link #IS_REPAIRING_NAMESPACES} es la que separa dos maneras muy distintas de usar la API.
 *
 * <p>Apagada --el valor por omision-- el escritor hace lo que se le dice y nada mas: si se escribe
 * un elemento con prefijo {@code p} y nadie declaro {@code p}, sale un documento con un prefijo sin
 * declarar, que no es XML valido. La responsabilidad de llamar a {@code writeNamespace} en el lugar
 * correcto es del llamador.
 *
 * <p>Encendida, el escritor se hace cargo: cuando ve un nombre calificado cuyo espacio de nombres
 * no esta declarado en el alcance actual, emite la declaracion el mismo, inventando un prefijo si
 * hace falta. A cambio, deja de respetar exactamente lo que se le pide --puede cambiar un prefijo
 * por otro-- lo cual es correcto en cuanto al significado pero cambia el texto.
 *
 * <p>La eleccion no es de estilo: en el modo reparador el llamador puede ignorar los espacios de
 * nombres por completo, y en el otro tiene que llevar la cuenta. Los dos estan implementados aca.
 */
public abstract class XMLOutputFactory {

    /**
     * {@code javax.xml.stream.isRepairingNamespaces}: si el escritor declara solo los espacios de
     * nombres que hagan falta.
     *
     * <p>Por omision false; ver el encabezado de la clase.
     */
    public static final String IS_REPAIRING_NAMESPACES = "javax.xml.stream.isRepairingNamespaces";

    /** La propiedad de sistema con que se enchufa otra implementacion. */
    static final String PROPERTY = "javax.xml.stream.XMLOutputFactory";

    /** Para las subclases. */
    protected XMLOutputFactory() {
    }

    // ---- descubrimiento ---------------------------------------------------------------------

    /**
     * La implementacion de la plataforma, sin mirar la configuracion.
     *
     * @return la fabrica de escritura de esta biblioteca; nunca null
     */
    public static XMLOutputFactory newDefaultFactory() {
        return new KajiOutputFactory();
    }

    /**
     * La fabrica configurada, o la de la plataforma si no hay ninguna.
     *
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLOutputFactory newInstance() {
        return newFactory();
    }

    /**
     * Lo mismo que {@link #newInstance()}, con el nombre nuevo.
     *
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLOutputFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLOutputFactory.class);
        if (f != null) {
            return (XMLOutputFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * La fabrica de <b>entrada</b> nombrada explicitamente.
     *
     * <p>Si, devuelve un {@link XMLInputFactory}, y no es una errata de esta biblioteca: la firma es
     * asi en la API original desde StAX 1.0. Fue un error de copiar y pegar en la especificacion, y
     * cuando se noto ya habia codigo compilado contra ella; cambiar el tipo de retorno rompe la
     * compatibilidad binaria, asi que quedo.
     *
     * <p>Se reproduce tal cual porque el contrato es el contrato: una fuente que compila con el JDK
     * tiene que compilar aca. Para conseguir una fabrica de salida por nombre esta
     * {@link #newFactory(String, ClassLoader)}, que es la que hace lo que uno espera.
     *
     * @param factoryId el nombre de la clase
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica de entrada nombrada
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) {
        return XMLInputFactory.newFactory(factoryId, classLoader);
    }

    /**
     * La fabrica de salida nombrada explicitamente.
     *
     * <p>La que hay que usar; ver {@link #newInstance(String, ClassLoader)}.
     *
     * @param factoryId el nombre de la clase; null cae en {@link #newFactory()}
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLOutputFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLOutputFactory)
                Factories.instantiate(factoryId, classLoader, XMLOutputFactory.class);
    }

    // ---- escritores -------------------------------------------------------------------------

    /**
     * Un escritor de cursor sobre un {@link Writer}.
     *
     * @param stream a donde escribir
     * @return el escritor
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException;

    /**
     * Un escritor de cursor sobre un flujo de bytes, en UTF-8.
     *
     * @param stream a donde escribir
     * @return el escritor
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream)
            throws XMLStreamException;

    /**
     * Un escritor de cursor sobre un flujo de bytes con la codificacion dada.
     *
     * @param stream a donde escribir
     * @param encoding la codificacion
     * @return el escritor
     * @throws XMLStreamException si la codificacion no se conoce
     */
    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * Un escritor de cursor sobre un {@link Result}.
     *
     * @param result a donde escribir
     * @return el escritor
     * @throws XMLStreamException si el tipo de {@code Result} no se soporta
     */
    public abstract XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException;

    /**
     * Un escritor de eventos sobre un {@link Result}.
     *
     * @param result a donde escribir
     * @return el escritor
     * @throws XMLStreamException si el tipo de {@code Result} no se soporta
     */
    public abstract XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException;

    /**
     * Un escritor de eventos sobre un flujo de bytes, en UTF-8.
     *
     * @param stream a donde escribir
     * @return el escritor
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream)
            throws XMLStreamException;

    /**
     * Un escritor de eventos sobre un flujo de bytes con la codificacion dada.
     *
     * @param stream a donde escribir
     * @param encoding la codificacion
     * @return el escritor
     * @throws XMLStreamException si la codificacion no se conoce
     */
    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * Un escritor de eventos sobre un {@link Writer}.
     *
     * @param stream a donde escribir
     * @return el escritor
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException;

    // ---- configuracion ----------------------------------------------------------------------

    /**
     * Cambia una propiedad de la fabrica.
     *
     * @param name el nombre de la propiedad
     * @param value el valor
     * @throws IllegalArgumentException si la propiedad no se conoce
     */
    public abstract void setProperty(String name, Object value) throws IllegalArgumentException;

    /**
     * El valor de una propiedad.
     *
     * @param name el nombre de la propiedad
     * @return el valor
     * @throws IllegalArgumentException si la propiedad no se conoce
     */
    public abstract Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Si la fabrica conoce una propiedad.
     *
     * @param name el nombre de la propiedad
     * @return true si la conoce
     */
    public abstract boolean isPropertySupported(String name);
}
