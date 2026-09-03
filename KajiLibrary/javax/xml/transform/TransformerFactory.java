package javax.xml.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.transform.TransformerFactory -- de donde salen los transformadores.
 *
 * <p>Es el punto de entrada de toda la API de XSLT y, mas interesante, el ejemplo canonico del
 * patron de fabrica conectable de JAXP: el codigo de la aplicacion nombra **esta** clase abstracta y
 * nunca a un procesador concreto, y {@link #newInstance()} descubre en tiempo de ejecucion cual hay
 * instalado. Cambiar de Xalan a Saxon es cambiar el classpath, no el codigo.
 *
 * <h2>El orden de busqueda, que es el contrato</h2>
 *
 * <p>{@link #newInstance()} mira, y se queda con el primero que encuentre:
 *
 * <ol>
 *   <li>la propiedad de sistema {@code javax.xml.transform.TransformerFactory};
 *   <li>el archivo {@code $java.home/conf/jaxp.properties}, con esa misma clave;
 *   <li>los proveedores declarados via {@link ServiceLoader}, o sea
 *       {@code META-INF/services/javax.xml.transform.TransformerFactory} en el classpath;
 *   <li>la implementacion por omision de la plataforma.
 * </ol>
 *
 * <p>El orden no es arbitrario y explica de que sirve cada escalon: la propiedad de sistema le gana
 * a todo porque es lo que uno puede cambiar sin tocar el despliegue, el archivo es la configuracion
 * de la instalacion, y el `ServiceLoader` es lo que trae un jar por el solo hecho de estar. Los tres
 * primeros estan implementados aca tal cual. **El cuarto no existe en esta biblioteca**, y de ahi
 * sale todo lo que sigue.
 *
 * <h2>Que hay escrito aca y que no, y por que</h2>
 *
 * <p>La API esta entera: las trece operaciones abstractas, los tres puntos de entrada estaticos y el
 * constructor protegido. Lo que **no** hay es un procesador de XSLT. XSLT es un lenguaje de
 * transformacion completo --su propia sintaxis, su modelo de arbol, y XPath adentro-- y escribirlo
 * es un proyecto aparte, no un miembro de esta clase.
 *
 * <p>Asi que {@link #newInstance()} recorre los tres escalones que si estan y, cuando ninguno da
 * nada, lanza {@link TransformerFactoryConfigurationError} -- que es **exactamente** lo que el JDK
 * hace cuando no encuentra implementacion, y por eso no es un stub sino el camino previsto por el
 * contrato. Que el JDK real casi nunca lo tome es un accidente de que trae Xalan adentro, no otra
 * regla.
 *
 * <p>La alternativa tentadora --devolver una fabrica cuyo `Transformer` copie la entrada en la
 * salida-- se descarto a proposito, y vale dejar dicho el motivo porque parece mas util: un
 * transformador que no transforma **falla en silencio**. El llamador recibe un documento, lo da por
 * transformado, y la hoja de estilo no se aplico nunca. Un error al construir la fabrica se ve en la
 * primera corrida; un documento sin transformar se ve cuando ya esta en produccion. Entre las dos,
 * la unica honesta es la que rompe temprano.
 */
public abstract class TransformerFactory {

    /** La clave, que es a la vez el nombre del servicio y el de la propiedad de sistema. */
    private static final String CLAVE = "javax.xml.transform.TransformerFactory";

    /** Para las subclases; no hay estado que inicializar. */
    protected TransformerFactory() {
    }

    // ---- descubrimiento ----------------------------------------------------------------------

    /**
     * La implementacion **de la plataforma**, sin mirar la configuracion.
     *
     * <p>Se salta los cuatro escalones de {@link #newInstance()} a proposito: existe para que una
     * pieza que necesita el procesador de referencia --y no el que la aplicacion haya enchufado--
     * lo pueda pedir. Es el escape de la conectabilidad, no un atajo.
     *
     * <p>Aca no hay ninguna, asi que siempre falla. Ver el encabezado de la clase.
     *
     * @return nunca vuelve
     * @throws TransformerFactoryConfigurationError siempre: esta biblioteca no trae XSLT
     */
    public static TransformerFactory newDefaultInstance() {
        throw new TransformerFactoryConfigurationError(
                "No system-default TransformerFactory: this runtime ships no XSLT processor");
    }

    /**
     * La fabrica configurada, buscada en los cuatro escalones del encabezado.
     *
     * @return la fabrica encontrada
     * @throws TransformerFactoryConfigurationError si ninguno de los escalones da una
     */
    public static TransformerFactory newInstance() throws TransformerFactoryConfigurationError {
        // 1. La propiedad de sistema.
        String nombre = null;
        try {
            nombre = System.getProperty(CLAVE);
        } catch (SecurityException ignorada) {
            // Sin permiso para leerla es lo mismo que no estar puesta: se sigue al proximo escalon.
        }
        if (nombre != null && nombre.length() > 0) {
            return instanciar(nombre, null);
        }

        // 2. $java.home/conf/jaxp.properties.
        nombre = deJaxpProperties();
        if (nombre != null && nombre.length() > 0) {
            return instanciar(nombre, null);
        }

        // 3. Los proveedores declarados en el classpath.
        TransformerFactory delServicio = deServiceLoader();
        if (delServicio != null) {
            return delServicio;
        }

        // 4. La implementacion por omision, que aca no existe.
        throw new TransformerFactoryConfigurationError("Provider for " + CLAVE + " cannot be found");
    }

    /**
     * Una fabrica de una clase nombrada, sin descubrimiento ninguno.
     *
     * <p>Para cuando la aplicacion necesita **dos** procesadores a la vez y no le sirve que haya uno
     * solo elegido globalmente.
     *
     * @param factoryClassName el nombre completo de la clase
     * @param classLoader con que cargarla; null usa el que corresponda por omision
     * @return la fabrica
     * @throws TransformerFactoryConfigurationError si la clase no esta o no se puede instanciar
     */
    public static TransformerFactory newInstance(String factoryClassName, ClassLoader classLoader)
            throws TransformerFactoryConfigurationError {
        if (factoryClassName == null) {
            // El JDK llega aca con una NullPointerException de adentro y la reporta envuelta; se
            // reproduce el mismo texto porque hay codigo que lo lee.
            NullPointerException e = new NullPointerException();
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        return instanciar(factoryClassName, classLoader);
    }

    // ---- las tuercas del descubrimiento ------------------------------------------------------

    /**
     * Carga e instancia la clase nombrada, con los mensajes de error que el contrato define.
     *
     * <p>Los dos casos se distinguen porque se arreglan distinto: **not found** es un jar que falta,
     * **could not be instantiated** es una clase que esta pero no sirve --sin constructor sin
     * argumentos, o que no es una `TransformerFactory`--.
     */
    private static TransformerFactory instanciar(String nombre, ClassLoader loader) {
        Class<?> clase;
        try {
            if (loader == null) {
                clase = Class.forName(nombre);
            } else {
                clase = Class.forName(nombre, false, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new TransformerFactoryConfigurationError(e, "Provider " + nombre + " not found");
        }
        Object objeto;
        try {
            objeto = clase.newInstance();
        } catch (Exception e) {
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + nombre + " could not be instantiated: " + e);
        }
        if (!(objeto instanceof TransformerFactory)) {
            ClassCastException e = new ClassCastException(nombre + " cannot be cast to " + CLAVE);
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + nombre + " could not be instantiated: " + e);
        }
        return (TransformerFactory) objeto;
    }

    /**
     * El nombre de clase que declare {@code $java.home/conf/jaxp.properties}, o null.
     *
     * <p>Sin cache a proposito: el JDK lee el archivo una sola vez por VM, y esa es una decision de
     * rendimiento que aca no compra nada --este camino se recorre cuando alguien pide una fabrica,
     * no en un bucle-- y que a cambio hace imposible probarlo.
     *
     * <p>Cualquier fallo de lectura devuelve null en vez de propagar: el archivo es **opcional**, y
     * que no se pueda leer no es un error de configuracion sino la ausencia de configuracion. En
     * esta VM {@code java.home} no esta definida, asi que este escalon no aporta nada todavia; el
     * codigo esta escrito para el dia que lo este.
     */
    private static String deJaxpProperties() {
        try {
            String home = System.getProperty("java.home");
            if (home == null) {
                return null;
            }
            File f = new File(new File(new File(home), "conf"), "jaxp.properties");
            if (!f.exists()) {
                return null;
            }
            Properties props = new Properties();
            InputStream in = new FileInputStream(f);
            try {
                props.load(in);
            } finally {
                in.close();
            }
            return props.getProperty(CLAVE);
        } catch (Throwable ignorada) {
            return null;
        }
    }

    /**
     * La primera fabrica que declare un proveedor del classpath, o null si no hay ninguno.
     *
     * <p>Hoy siempre da null, y no por un atajo de aca: el {@link ServiceLoader} de esta biblioteca
     * no puede enumerar {@code META-INF/services} porque nuestro {@code ClassLoader} no tiene
     * recursos. La maquinaria esta enchufada donde va, asi que el dia que los recursos existan este
     * escalon empieza a encontrar proveedores sin tocar una linea.
     */
    private static TransformerFactory deServiceLoader() {
        try {
            ServiceLoader<TransformerFactory> sl = ServiceLoader.load(TransformerFactory.class);
            Iterator<TransformerFactory> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignorada) {
            // Un proveedor roto no puede impedir que se pruebe el escalon siguiente.
        }
        return null;
    }

    // ---- el contrato de la fabrica -----------------------------------------------------------

    /**
     * Un transformador que aplica la hoja de estilo de {@code source}.
     *
     * @param source la hoja de estilo
     * @return el transformador
     * @throws TransformerConfigurationException si la hoja no se puede compilar
     */
    public abstract Transformer newTransformer(Source source) throws TransformerConfigurationException;

    /**
     * Un transformador **de copia**: sin hoja de estilo, mueve la entrada a la salida.
     *
     * <p>Es la unica transformacion identidad que la API define, y esta bien que exista porque el
     * llamador la pide explicitamente y sabe lo que recibe. Sirve para serializar: se le da un
     * arbol y un flujo, y se aprovechan las propiedades de {@link OutputKeys} sin escribir un
     * serializador.
     *
     * @return el transformador de copia
     * @throws TransformerConfigurationException si no se puede construir
     */
    public abstract Transformer newTransformer() throws TransformerConfigurationException;

    /**
     * Compila la hoja de estilo una vez para reusarla muchas.
     *
     * @param source la hoja de estilo
     * @return la hoja compilada
     * @throws TransformerConfigurationException si no se puede compilar
     */
    public abstract Templates newTemplates(Source source) throws TransformerConfigurationException;

    /**
     * La hoja de estilo que el propio documento se asocia con {@code &lt;?xml-stylesheet?&gt;}.
     *
     * <p>Los tres criterios --medio, titulo, juego de caracteres-- filtran entre varias
     * instrucciones; null en cualquiera significa "no me importa esa". Devuelve null si ninguna
     * coincide, que no es un error: un documento no tiene por que traer hoja de estilo.
     *
     * @param source el documento
     * @param media el medio buscado, o null
     * @param title el titulo buscado, o null
     * @param charset el juego de caracteres buscado, o null
     * @return la fuente de la hoja de estilo, o null
     * @throws TransformerConfigurationException si el documento no se puede leer
     */
    public abstract Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws TransformerConfigurationException;

    /**
     * Quien resuelve los `href` de los transformadores que salgan de aca.
     *
     * @param resolver el resolvedor, o null para volver al de por omision
     */
    public abstract void setURIResolver(URIResolver resolver);

    /** El resolvedor en uso, o null. */
    public abstract URIResolver getURIResolver();

    /**
     * Prende o apaga una caracteristica.
     *
     * <p>La unica que la spec obliga a soportar es
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, y a esa **no se le puede decir que
     * no**: una implementacion que la tenga prendida no esta obligada a dejar apagarla, porque el
     * modo seguro puede venir impuesto por el entorno.
     *
     * @param name el nombre de la caracteristica
     * @param value si se quiere prendida
     * @throws TransformerConfigurationException si no se reconoce o no se puede poner asi
     */
    public abstract void setFeature(String name, boolean value) throws TransformerConfigurationException;

    /**
     * Si una caracteristica esta prendida.
     *
     * <p>Un {@code false} es ambiguo a proposito: puede ser "esta apagada" o "no la conozco". La
     * API no distingue.
     *
     * @param name el nombre de la caracteristica
     * @return si esta soportada y prendida
     */
    public abstract boolean getFeature(String name);

    /**
     * Fija un atributo especifico de la implementacion.
     *
     * @param name el nombre del atributo
     * @param value el valor
     * @throws IllegalArgumentException si no se reconoce
     */
    public abstract void setAttribute(String name, Object value);

    /**
     * El valor de un atributo especifico de la implementacion.
     *
     * @param name el nombre del atributo
     * @return el valor
     * @throws IllegalArgumentException si no se reconoce
     */
    public abstract Object getAttribute(String name);

    /**
     * Quien recibe los errores **de compilar hojas de estilo**.
     *
     * <p>No es el mismo oyente que el del {@link Transformer}: aca se reportan los problemas de
     * armar la transformacion, alla los de correrla.
     *
     * @param listener el oyente; no puede ser null
     * @throws IllegalArgumentException si es null
     */
    public abstract void setErrorListener(ErrorListener listener);

    /** El oyente en uso; nunca null. */
    public abstract ErrorListener getErrorListener();
}
