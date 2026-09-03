package javax.xml.transform.stream;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.transform.stream.StreamSource -- un documento XML que llega como bytes.
 *
 * <p>Es la implementacion de {@link Source} que no supone nada sobre el documento: no hay arbol, no
 * hay eventos, hay un flujo sin parsear. Por eso es la mas barata --el procesador parsea una sola
 * vez, a su modo-- y la unica que sirve cuando el documento todavia no se leyo.
 *
 * <p>Hay **tres** formas de decir de donde salen los bytes, y el orden en que el procesador las mira
 * es parte del contrato y no un detalle: primero el {@link Reader}, despues el {@link InputStream},
 * y al final el identificador de sistema. Poner dos es legal y gana el de mas arriba; poner solo la
 * URI hace que el procesador la abra el mismo.
 *
 * <p>Que el `Reader` le gane al `InputStream` tiene una consecuencia que muerde: un `Reader` ya
 * decidio la codificacion, asi que la declaracion `&lt;?xml encoding="..."?&gt;` del documento **no
 * se puede respetar**. Con un `InputStream` el parser la lee y elige; con un `Reader` llega tarde.
 * Cuando la codificacion importa, lo que se pasa es el flujo de bytes.
 *
 * <p>El identificador de sistema sigue haciendo falta aunque los bytes ya esten: es la URI base
 * contra la que se resuelven los `href` relativos del documento. Un flujo sin URI es un documento
 * que no puede tener referencias relativas.
 */
public class StreamSource implements Source {

    /**
     * El nombre con el que se le pregunta a una fabrica si acepta esta clase de fuente.
     *
     * <p>Nunca se le pasa a {@code setFeature}: es de solo lectura, y el `true` que devuelve
     * significa "sabe leer un StreamSource", no una opcion que se prenda.
     */
    public static final String FEATURE = "http://javax.xml.transform.stream.StreamSource/feature";

    private String publicId;
    private String systemId;
    private InputStream inputStream;
    private Reader reader;

    /**
     * Vacia, para llenarla despues con los `set`.
     *
     * <p>Existe porque hay codigo que arma la fuente en varios pasos --recibe la URI de un lado y el
     * flujo de otro-- y no puede pasar todo por el constructor.
     */
    public StreamSource() {
    }

    /**
     * Desde un flujo de bytes.
     *
     * @param inputStream de donde leer
     */
    public StreamSource(InputStream inputStream) {
        setInputStream(inputStream);
    }

    /**
     * Desde un flujo de bytes, con la URI base.
     *
     * @param inputStream de donde leer
     * @param systemId la URI base para las referencias relativas
     */
    public StreamSource(InputStream inputStream, String systemId) {
        setInputStream(inputStream);
        setSystemId(systemId);
    }

    /**
     * Desde un flujo de caracteres. Ojo con la codificacion: ver el encabezado.
     *
     * @param reader de donde leer
     */
    public StreamSource(Reader reader) {
        setReader(reader);
    }

    /**
     * Desde un flujo de caracteres, con la URI base.
     *
     * @param reader de donde leer
     * @param systemId la URI base para las referencias relativas
     */
    public StreamSource(Reader reader, String systemId) {
        setReader(reader);
        setSystemId(systemId);
    }

    /**
     * Desde una URI, que el procesador abre el mismo.
     *
     * @param systemId la URI del documento
     */
    public StreamSource(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Desde un archivo. La URI se arma con {@link #setSystemId(File)}.
     *
     * @param f el archivo
     */
    public StreamSource(File f) {
        setSystemId(f);
    }

    // ---- de donde salen los bytes ------------------------------------------------------------

    /**
     * Fija el flujo de bytes.
     *
     * @param inputStream de donde leer, o null
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /** El flujo de bytes, o null. */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Fija el flujo de caracteres, que le gana al de bytes.
     *
     * @param reader de donde leer, o null
     */
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /** El flujo de caracteres, o null. */
    public Reader getReader() {
        return reader;
    }

    // ---- identificacion ----------------------------------------------------------------------

    /**
     * Fija el identificador publico.
     *
     * <p>Es puramente informativo --sirve para los mensajes de error-- porque el procesador resuelve
     * por la URI. Que exista igual es herencia de SAX, donde un catalogo puede mapearlo.
     *
     * @param publicId el identificador, o null
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /** El identificador publico, o null. */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Fija la URI base.
     *
     * @param systemId la URI, o null
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** La URI base, o null. */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Fija la URI base a partir de un archivo, convertido a {@code file:}.
     *
     * <p>La conversion pasa por {@link File#toURI()} y despues a ASCII, que es lo que hace falta:
     * una URI con un espacio o una eñe no es una URI valida hasta que esos caracteres estan
     * percent-encoded, y un `href` relativo resuelto contra una base invalida no da nada.
     *
     * <p><b>Techo, y esta afuera de esta clase.</b> El JDK no delega en {@code File.toURI()} aca:
     * hace su propia conversion de ruta a URI. Delegar es lo correcto igual --una sola fuente de
     * verdad-- pero hoy arrastra dos defectos de las clases de las que depende, y conviene que
     * quede escrito: {@code java.net.URI} **no percent-encodea** (un espacio sale crudo), y
     * {@code File.getAbsolutePath()} no puede resolver una ruta relativa porque {@code user.dir} no
     * esta definida en esta VM. Con una ruta absoluta y sin caracteres que escapar, la salida
     * coincide con la del JDK. Arreglarlo aca seria tapar el agujero en la hoja en vez de en la
     * raiz; el dia que esas dos se arreglen, este metodo ya esta bien.
     *
     * @param f el archivo
     */
    public void setSystemId(File f) {
        this.systemId = f.toURI().toASCIIString();
    }

    // ---- vacia o no --------------------------------------------------------------------------

    /**
     * Si esta fuente no tiene documento ninguno.
     *
     * <p>Vacia es **las dos cosas a la vez**: ni bytes por leer, ni URI que abrir. Y la URI cuenta
     * por {@code null}, no por su contenido: la cadena vacia es una URI --mala, pero puesta a
     * proposito-- y una fuente que la lleva no es una fuente sin nada.
     */
    public boolean isEmpty() {
        return flujoVacio() && systemId == null;
    }

    /**
     * Si el flujo, si lo hay, no tiene un solo caracter.
     *
     * <p>Mirar esto **sin consumir nada** es todo el problema, porque quien pregunte "esta vacia?"
     * espera despues poder leerla entera. Se resuelve con `mark`/`reset`: se marca, se lee un
     * caracter, se vuelve.
     *
     * <p>Un flujo que no soporta marcas no se puede mirar sin romperlo, y ahi la respuesta es
     * **"no esta vacia"**. No es una suposicion optimista sino la unica segura de las dos: decir
     * que esta vacia haria que el llamador la descarte, y un documento perdido es peor que un
     * documento vacio que igual se intenta leer.
     */
    private boolean flujoVacio() {
        boolean vacio = true;
        if (inputStream != null) {
            // La marca se chequea **antes** de leer, no despues: sobre un flujo que no la soporta,
            // `mark` no hace nada y el `read` de prueba se comeria el primer byte del documento --
            // justo lo que este metodo promete no hacer.
            //
            // Y `catch (Throwable)` en vez de `catch (IOException)`: el `InputStream` de esta
            // biblioteca no declara `IOException` en `read`/`mark`/`reset`, asi que nombrarla en el
            // `catch` no compila aca --y si compilaria contra el JDK, donde si la declara--.
            // `Throwable` es lo unico que vale en los dos.
            if (!inputStream.markSupported()) {
                return false;
            }
            try {
                inputStream.mark(1);
                vacio = (inputStream.read() == -1);
                inputStream.reset();
            } catch (Throwable noSePuedeMirar) {
                vacio = false;
            }
        }
        if (reader != null) {
            if (!reader.markSupported()) {
                return false;
            }
            try {
                reader.mark(1);
                vacio = (reader.read() == -1);
                reader.reset();
            } catch (Throwable noSePuedeMirar) {
                vacio = false;
            }
        }
        return vacio;
    }
}
