package jdk.jfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un conjunto de ajustes con nombre: lo que un archivo {@code .jfc} contiene.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Una grabacion util configura cientos de ajustes —cada evento tiene los suyos— y nadie los
 * escribe a mano. El JDK trae dos configuraciones armadas, {@code default} y {@code profile}, y la
 * diferencia entre ellas es cuanto cuestan: la primera esta pensada para dejarla puesta en
 * produccion, la segunda para una investigacion con la maquina dedicada.
 *
 * <p>Elegir entre esas dos es la decision que casi todo el mundo toma, y esta clase es como se
 * nombran.
 *
 * <h2>El formato</h2>
 *
 * <p>Un {@code .jfc} es XML: un elemento raiz con nombre, etiqueta y proveedor, y adentro un
 * {@code <event>} por tipo con un {@code <setting>} por perilla. {@link #getSettings} devuelve eso
 * aplanado a {@code "jdk.CPULoad#period" -> "1 s"}, que es la forma en que los ajustes viajan por
 * el resto de la API.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>{@link #create(Reader)} y {@link #create(Path)} <strong>leen y analizan de verdad</strong> un
 * archivo {@code .jfc}: el analizador esta escrito aca y devuelve una configuracion con sus ajustes
 * cargados. Se puede usar para inspeccionar una configuracion sin JFR.
 *
 * <p>{@link #getConfigurations} y {@link #getConfiguration(String)} son las que no pueden dar nada:
 * buscan en el directorio {@code lib/jfr} de la instalacion del JDK, que esta biblioteca no tiene.
 * La lista sale vacia y la busqueda por nombre falla diciendo que no hay ninguna instalada — no
 * inventan una configuracion vacia con ese nombre, que es lo que confundiria al que llama.
 *
 * @since 9
 */
public final class Configuration {

    private final String nombre;
    private final String etiqueta;
    private final String descripcion;
    private final String proveedor;
    private final String contenido;
    private final Map<String, String> ajustes;

    private Configuration(final String nombre, final String etiqueta, final String descripcion,
            final String proveedor, final String contenido, final Map<String, String> ajustes) {
        this.nombre = nombre;
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
        this.proveedor = proveedor;
        this.contenido = contenido;
        this.ajustes = Collections.unmodifiableMap(ajustes);
    }

    /**
     * Los ajustes, con la clave {@code "evento#ajuste"}.
     *
     * @return los ajustes
     */
    public Map<String, String> getSettings() {
        return ajustes;
    }

    /**
     * El nombre de la configuracion.
     *
     * @return el nombre, o {@code null} si el archivo no lo trae
     */
    public String getName() {
        return nombre;
    }

    /**
     * El nombre legible.
     *
     * @return la etiqueta, o {@code null}
     */
    public String getLabel() {
        return etiqueta;
    }

    /**
     * La explicacion de para que sirve esta configuracion.
     *
     * @return la descripcion, o {@code null}
     */
    public String getDescription() {
        return descripcion;
    }

    /**
     * Quien la escribio.
     *
     * @return el proveedor, o {@code null}
     */
    public String getProvider() {
        return proveedor;
    }

    /**
     * El texto original del archivo.
     *
     * <p>Se guarda entero, sin volver a generarlo desde los ajustes: asi una configuracion que se
     * lee y se vuelve a escribir queda igual que estaba, con sus comentarios y su orden.
     *
     * @return el contenido
     */
    public String getContents() {
        return contenido;
    }

    /**
     * Lee una configuracion de un archivo.
     *
     * @param path el archivo
     * @return la configuracion
     * @throws IOException si no se pudo leer
     * @throws ParseException si el XML esta mal formado
     * @throws NullPointerException si es {@code null}
     */
    public static Configuration create(final Path path) throws IOException, ParseException {
        Objects.requireNonNull(path, "path");
        final Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            return create(r);
        } finally {
            r.close();
        }
    }

    /**
     * Lee una configuracion de un flujo.
     *
     * @param reader de donde leer
     * @return la configuracion
     * @throws IOException si no se pudo leer
     * @throws ParseException si el XML esta mal formado
     * @throws NullPointerException si es {@code null}
     */
    public static Configuration create(final Reader reader) throws IOException, ParseException {
        Objects.requireNonNull(reader, "reader");
        final StringBuilder sb = new StringBuilder();
        final BufferedReader br = reader instanceof BufferedReader
                ? (BufferedReader) reader : new BufferedReader(reader);
        final char[] buf = new char[4096];
        int n;
        while ((n = br.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        return analizar(sb.toString());
    }

    /**
     * Una configuracion instalada, por nombre.
     *
     * @param name el nombre, por ejemplo {@code "default"}
     * @return la configuracion
     * @throws IOException si no hay configuraciones instaladas o no se pudo leer
     * @throws ParseException si el XML esta mal formado
     */
    public static Configuration getConfiguration(final String name)
            throws IOException, ParseException {
        Objects.requireNonNull(name, "name");
        throw new IOException(
                "no hay configuraciones instaladas: las trae el directorio lib/jfr de una "
                + "instalacion del JDK, que esta biblioteca no incluye. Para leer una propia, "
                + "usar create(Path)");
    }

    /**
     * Las configuraciones instaladas.
     *
     * <p>Vacia en esta biblioteca. Es una lista y no un fallo porque el contrato no permite avisar,
     * y porque "no hay ninguna instalada" es una respuesta legitima que el que llama tiene que
     * poder manejar igual.
     *
     * @return la lista, vacia
     */
    public static List<Configuration> getConfigurations() {
        return Collections.emptyList();
    }

    // ---- el analizador ----
    //
    // Es un analizador de XML acotado a la forma de un .jfc y no un analizador general: la
    // alternativa era arrastrar un parser completo para leer un archivo cuya estructura son tres
    // elementos. Reconoce etiquetas de apertura con atributos, texto, cierres y comentarios, que es
    // todo lo que un .jfc usa.

    private static Configuration analizar(final String xml) throws ParseException {
        String nombre = null;
        String etiqueta = null;
        String descripcion = null;
        String proveedor = null;
        String eventoActual = null;
        String ajusteActual = null;
        final Map<String, String> ajustes = new LinkedHashMap<String, String>();

        int i = 0;
        while (i < xml.length()) {
            final int abre = xml.indexOf('<', i);
            if (abre < 0) {
                break;
            }
            if (xml.startsWith("<!--", abre)) {
                final int fin = xml.indexOf("-->", abre);
                if (fin < 0) {
                    throw new ParseException("comentario sin cerrar", abre);
                }
                i = fin + 3;
                continue;
            }
            if (xml.startsWith("<?", abre) || xml.startsWith("<!", abre)) {
                final int fin = xml.indexOf('>', abre);
                if (fin < 0) {
                    throw new ParseException("declaracion sin cerrar", abre);
                }
                i = fin + 1;
                continue;
            }
            final int cierra = xml.indexOf('>', abre);
            if (cierra < 0) {
                throw new ParseException("etiqueta sin cerrar", abre);
            }
            String etq = xml.substring(abre + 1, cierra).trim();
            final boolean vacia = etq.endsWith("/");
            if (vacia) {
                etq = etq.substring(0, etq.length() - 1).trim();
            }

            if (etq.startsWith("/")) {
                final String cerrado = etq.substring(1).trim();
                if ("setting".equals(cerrado)) {
                    ajusteActual = null;
                } else if ("event".equals(cerrado)) {
                    eventoActual = null;
                }
                i = cierra + 1;
                continue;
            }

            final int sp = primerEspacio(etq);
            final String elem = sp < 0 ? etq : etq.substring(0, sp);
            final Map<String, String> attrs =
                    sp < 0 ? Collections.<String, String>emptyMap() : atributos(etq.substring(sp));

            if ("configuration".equals(elem)) {
                nombre = attrs.get("name");
                etiqueta = attrs.get("label");
                descripcion = attrs.get("description");
                proveedor = attrs.get("provider");
            } else if ("event".equals(elem)) {
                eventoActual = attrs.get("name");
            } else if ("setting".equals(elem)) {
                ajusteActual = attrs.get("name");
                if (vacia && eventoActual != null && ajusteActual != null) {
                    // <setting name="x"/> sin texto: el ajuste queda en cadena vacia, que es lo que
                    // el formato significa con eso.
                    ajustes.put(eventoActual + "#" + ajusteActual, "");
                    ajusteActual = null;
                }
            }

            i = cierra + 1;

            if (ajusteActual != null && !vacia && eventoActual != null) {
                final int prox = xml.indexOf('<', i);
                final String txt = prox < 0 ? xml.substring(i) : xml.substring(i, prox);
                ajustes.put(eventoActual + "#" + ajusteActual, txt.trim());
            }
        }

        return new Configuration(nombre, etiqueta, descripcion, proveedor, xml, ajustes);
    }

    private static int primerEspacio(final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                return i;
            }
        }
        return -1;
    }

    /** Atributos {@code clave="valor"}, con comillas simples o dobles. */
    private static Map<String, String> atributos(final String s) throws ParseException {
        final Map<String, String> out = new LinkedHashMap<String, String>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= s.length()) {
                break;
            }
            final int igual = s.indexOf('=', i);
            if (igual < 0) {
                break;
            }
            final String clave = s.substring(i, igual).trim();
            int j = igual + 1;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
                j++;
            }
            if (j >= s.length() || (s.charAt(j) != '"' && s.charAt(j) != '\'')) {
                throw new ParseException("atributo sin comillas: " + clave, j);
            }
            final char comilla = s.charAt(j);
            final int fin = s.indexOf(comilla, j + 1);
            if (fin < 0) {
                throw new ParseException("atributo sin cerrar: " + clave, j);
            }
            out.put(clave, desescapar(s.substring(j + 1, fin)));
            i = fin + 1;
        }
        return out;
    }

    /** Las cinco entidades que XML define; un {@code .jfc} no usa otras. */
    private static String desescapar(final String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }

    /** Para las pruebas: analiza un texto sin pasar por un archivo. */
    static Configuration desdeTexto(final String xml) throws IOException, ParseException {
        return create(new StringReader(xml));
    }
}
