package javax.xml.transform;

import java.util.Properties;

/**
 * KajiLibrary's javax.xml.transform.Transformer -- aplica una hoja de estilo a un documento.
 *
 * <p>Clase abstracta y no interfaz, y eso no es un accidente historico: define un unico metodo con
 * cuerpo, {@link #reset}, que se agrego en Java 6 con una implementacion por omision que lanza
 * {@link UnsupportedOperationException}. En 2001 no habia metodos `default`, asi que la unica forma
 * de agregar un miembro sin romper a todos los que ya implementaban la API era que fuera una clase.
 *
 * <p>Un `Transformer` **tiene estado y no se comparte entre hilos**: los parametros y las
 * propiedades de salida son suyos. Lo que se comparte es el {@link Templates} del que salio. Un
 * mismo transformador si se puede usar en varias transformaciones seguidas en el mismo hilo, y por
 * eso existe {@link #reset}: es mas barato limpiar que volver a pedirlo.
 *
 * <h2>Que hay escrito aca y que no</h2>
 *
 * <p>Todo lo que declara la clase; de implementacion, lo unico que el JDK tambien implementa. Los
 * trece metodos abstractos los provee un procesador de XSLT, y **esta biblioteca no trae ninguno**
 * -- ver el encabezado de {@link TransformerFactory}. No es que falte una pieza: la clase abstracta
 * es exactamente lo que la API define, y una subclase concreta seria un procesador de XSLT entero.
 */
public abstract class Transformer {

    /** Para las subclases; no hay estado que inicializar. */
    protected Transformer() {
    }

    /**
     * Deja el transformador como recien salido de {@link Templates#newTransformer}.
     *
     * <p>Borra los parametros y las propiedades de salida que se le hayan puesto; **no** toca el
     * {@link URIResolver} ni el {@link ErrorListener}, que son infraestructura del llamador y no
     * datos del trabajo. Es una distincion que se olvida y despues aparece como un oyente que dejo
     * de recibir errores a mitad de un lote.
     *
     * <p>La implementacion por omision lanza {@link UnsupportedOperationException}: el metodo llego
     * despues que la clase, y una subclase escrita antes no lo sabe hacer. Lanzar es lo correcto
     * --no hacer nada seria mentir sobre un objeto que quedo sucio--.
     *
     * @throws UnsupportedOperationException si la implementacion no lo soporta
     */
    public void reset() {
        // El paquete puede ser nulo --una subclase en el paquete por omision-- y un mensaje de error
        // que tira NullPointerException es peor que no tener mensaje.
        Package p = this.getClass().getPackage();
        String titulo = (p == null) ? null : p.getSpecificationTitle();
        String version = (p == null) ? null : p.getSpecificationVersion();
        throw new UnsupportedOperationException(
                "This Transformer, \"" + this.getClass().getName() + "\", does not support the reset functionality."
                        + "  Specification \"" + titulo + "\""
                        + " version \"" + version + "\"");
    }

    /**
     * Transforma {@code xmlSource} y escribe en {@code outputTarget}.
     *
     * @param xmlSource el documento de entrada
     * @param outputTarget donde dejar el resultado
     * @throws TransformerException si la transformacion falla
     */
    public abstract void transform(Source xmlSource, Result outputTarget) throws TransformerException;

    /**
     * Fija un parametro de la hoja de estilo.
     *
     * <p>El nombre puede venir calificado como {@code "{uri}local"}. Los parametros son del
     * transformador, no de la transformacion: sobreviven a {@link #transform} y hay que limpiarlos
     * con {@link #clearParameters} si el proximo trabajo no los quiere.
     *
     * @param name el nombre, posiblemente calificado
     * @param value el valor
     */
    public abstract void setParameter(String name, Object value);

    /**
     * El valor que se fijo con {@link #setParameter}, o null.
     *
     * <p>Devuelve lo que se puso desde Java, **no** lo que la hoja de estilo tenga como valor por
     * omision para ese parametro: son dos cosas distintas y esta API solo ve la primera.
     *
     * @param name el nombre, posiblemente calificado
     * @return el valor, o null si no se fijo
     */
    public abstract Object getParameter(String name);

    /** Borra todos los parametros fijados. */
    public abstract void clearParameters();

    /**
     * Quien resuelve los `href` de `document()`, `xsl:import` y `xsl:include`.
     *
     * @param resolver el resolvedor, o null para volver al de por omision
     */
    public abstract void setURIResolver(URIResolver resolver);

    /** El resolvedor en uso, o null. */
    public abstract URIResolver getURIResolver();

    /**
     * Fija de una vez todas las propiedades de serializacion.
     *
     * <p>Pasar {@code null} **restablece** las de la hoja de estilo; no las deja vacias. Y las
     * propiedades por omision de la tabla (las de {@link Properties#defaults}) no se copian: se
     * usan como respaldo, igual que en cualquier `Properties`.
     *
     * @param oformat las propiedades, o null para volver a las de la hoja de estilo
     * @throws IllegalArgumentException si alguna clave no se reconoce
     */
    public abstract void setOutputProperties(Properties oformat);

    /**
     * Una copia de las propiedades de salida en efecto.
     *
     * <p>Copia: modificarla no cambia nada. Para cambiar hay que volver a llamar a
     * {@link #setOutputProperties}.
     *
     * @return las propiedades, con las por omision abajo
     */
    public abstract Properties getOutputProperties();

    /**
     * Fija una sola propiedad de serializacion.
     *
     * <p>Las claves reconocidas son las de {@link OutputKeys} mas las de extension, que van
     * calificadas como {@code "{uri}local"}. Una clave desconocida **sin** calificar es un error;
     * una calificada que el procesador no entienda se ignora, porque puede ser de otro procesador.
     *
     * @param name la clave
     * @param value el valor
     * @throws IllegalArgumentException si la clave no se reconoce
     */
    public abstract void setOutputProperty(String name, String value) throws IllegalArgumentException;

    /**
     * El valor de una propiedad de salida.
     *
     * <p>Devuelve lo que se fijo con {@link #setOutputProperty} **o** lo que declaro la hoja de
     * estilo, no el valor por omision del metodo de salida. Una propiedad que nadie toco da null
     * aunque el serializador tenga un valor para ella.
     *
     * @param name la clave
     * @return el valor, o null
     * @throws IllegalArgumentException si la clave no se reconoce
     */
    public abstract String getOutputProperty(String name) throws IllegalArgumentException;

    /**
     * Quien recibe los avisos y errores de la transformacion.
     *
     * @param listener el oyente; no puede ser null
     * @throws IllegalArgumentException si es null
     */
    public abstract void setErrorListener(ErrorListener listener) throws IllegalArgumentException;

    /** El oyente en uso; nunca null. */
    public abstract ErrorListener getErrorListener();
}
