package java.util.jar;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El manifiesto de un JAR: una seccion principal de atributos, y despues cero o mas secciones por
 * entrada.
 *
 * <p>Esta clase es lo unico que un JAR tiene y un ZIP no, y por eso es donde esta todo el trabajo del
 * paquete. El resto --`JarFile`, `JarEntry`, los dos flujos-- se apoya en `java.util.zip`, que ya
 * estaba entero.
 *
 * <h2>El formato, en la parte que se puede hacer mal</h2>
 *
 * <p><b>Las lineas se cortan a 72 bytes</b> y la continuacion arranca con un espacio. El corte es
 * por **bytes UTF-8**, no por caracteres: la primera linea lleva 72 bytes de contenido y cada
 * continuacion lleva el espacio mas 71.
 *
 * <p>Y aca esta lo que hay que mirar antes de escribir el codigo, porque la intuicion dice lo
 * contrario: <b>el JDK parte los caracteres multibyte al medio</b>. Se verifico contra el JDK 25 con
 * un valor de 60 `n` con virgulilla --dos bytes cada una-- y la primera linea termina en un `c3` suelto que la
 * continuacion completa con su `b1`. Eso es correcto, y la razon es el lector: la continuacion se une
 * a nivel de **bytes** y recien despues se decodifica UTF-8, asi que el caracter se rearma antes de
 * que nadie lo mire. Un escritor que se negara a partir caracteres tambien seria legible, pero no
 * daria los mismos bytes que el JDK; se eligio dar los mismos.
 *
 * <p>La contrapartida obliga: <b>el lector de aca une bytes, nunca `String`s</b>. Decodificar cada
 * linea fisica por separado y concatenar los textos romperia todo valor con un caracter partido, y
 * lo haria en silencio --con un `?` en el medio--. Por eso {@link #read} trabaja sobre `byte[]`.
 *
 * <p>La otra regla es la de las secciones: una linea en blanco cierra la principal, y cada seccion
 * siguiente tiene que empezar con `Name: `. Una seccion sin `Name` es un error, no una seccion
 * anonima.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p>Nada de la superficie publica. De los miembros que el JDK declara y aca no estan, todos son
 * **de paquete** --los dos constructores internos, `getTrustedAttributes`, `getErrorPosition`--; por
 * la regla del contrato, lo interno es libre. `getTrustedAttributes` en particular solo tiene sentido
 * con verificacion de firmas, que este paquete no hace (ver la cabecera de {@link JarFile}).
 */
public class Manifest implements Cloneable {

    private final Attributes attr = new Attributes();

    // `LinkedHashMap` y no `HashMap` como el JDK: el orden de las secciones no es contrato, pero
    // que escribir dos veces el mismo manifiesto de los mismos bytes si lo es de hecho, y con un
    // `HashMap` no lo era.
    private final Map<String, Attributes> entries = new LinkedHashMap<String, Attributes>();

    /** Un manifiesto vacio. */
    public Manifest() {
    }

    /**
     * Un manifiesto leido de ese flujo.
     *
     * @throws IOException si el flujo no tiene un manifiesto bien formado
     */
    public Manifest(InputStream is) throws IOException {
        read(is);
    }

    /** Una copia de `man`. */
    public Manifest(Manifest man) {
        this.attr.putAll(man.getMainAttributes());
        for (Map.Entry<String, Attributes> e : man.getEntries().entrySet()) {
            this.entries.put(e.getKey(), new Attributes(e.getValue()));
        }
    }

    /** Los atributos de la seccion principal. */
    public Attributes getMainAttributes() {
        return this.attr;
    }

    /**
     * Las secciones por entrada, indexadas por el nombre que dice su linea `Name`.
     *
     * <p>Es el mapa vivo: modificarlo modifica el manifiesto.
     */
    public Map<String, Attributes> getEntries() {
        return this.entries;
    }

    /** Los atributos de esa entrada, o `null` si el manifiesto no tiene una seccion para ella. */
    public Attributes getAttributes(String name) {
        return getEntries().get(name);
    }

    /** Vacia la seccion principal y todas las secciones por entrada. */
    public void clear() {
        this.attr.clear();
        this.entries.clear();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Manifest)) {
            return false;
        }
        Manifest m = (Manifest) o;
        return this.attr.equals(m.attr) && this.entries.equals(m.entries);
    }

    public int hashCode() {
        return this.attr.hashCode() + this.entries.hashCode();
    }

    /** Una copia. */
    public Object clone() {
        return new Manifest(this);
    }

    // ---- escritura ------------------------------------------------------------------------------

    /**
     * Escribe el manifiesto en el formato que lee cualquier herramienta de JAR.
     *
     * <p>Si la seccion principal no tiene ni `Manifest-Version` ni `Signature-Version` no se escribe
     * ningun atributo: ver la nota en `Attributes.writeMain`, donde esta el motivo.
     */
    public void write(OutputStream out) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        this.attr.writeMain(dos);
        for (Map.Entry<String, Attributes> e : this.entries.entrySet()) {
            println72(dos, "Name: " + e.getKey());
            e.getValue().write(dos);
        }
        dos.flush();
    }

    /**
     * Escribe una linea logica plegada a 72 bytes.
     *
     * <p>El primer byte se escribe suelto y despues van bloques de 71: asi la primera linea queda en
     * 1 + 71 = 72 bytes y cada continuacion en 1 (el espacio) + 71 = 72. Es exactamente el reparto
     * del JDK, incluido que un caracter multibyte se pueda partir en el corte.
     */
    static void println72(OutputStream out, String line) throws IOException {
        if (!line.isEmpty()) {
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            int largo = bytes.length;
            out.write(bytes[0]);
            int pos = 1;
            while (largo - pos > 71) {
                out.write(bytes, pos, 71);
                pos = pos + 71;
                println(out);
                out.write(' ');
            }
            out.write(bytes, pos, largo - pos);
        }
        println(out);
    }

    /** El fin de linea del formato, que es CRLF y no el del sistema. */
    static void println(OutputStream out) throws IOException {
        out.write('\r');
        out.write('\n');
    }

    // ---- lectura --------------------------------------------------------------------------------

    /**
     * Lee un manifiesto de ese flujo.
     *
     * <p>Se lee el flujo entero a memoria antes de parsear. Es la misma decision que tomo `ZipFile`
     * en esta biblioteca y por el mismo motivo: un manifiesto no llega a los megabytes, y a cambio no
     * queda ningun estado a medio consumir si el parseo falla.
     *
     * <p>Lo leido se **mezcla** con lo que el manifiesto ya tuviera, que es lo que dice el javadoc
     * del JDK. No reemplaza.
     */
    public void read(InputStream is) throws IOException {
        byte[] datos = leerTodo(is);
        List<byte[]> logicas = new ArrayList<byte[]>();
        List<Integer> numeros = new ArrayList<Integer>();
        plegar(datos, logicas, numeros);

        // No se limpia: el contrato del JDK dice que lo leido se **mezcla** con lo que ya habia.
        int i = 0;
        // La seccion principal: hasta la primera linea en blanco.
        while (i < logicas.size() && logicas.get(i).length != 0) {
            leerCabecera(this.attr, logicas.get(i), numeros.get(i).intValue());
            i = i + 1;
        }
        // Y despues, una seccion por entrada. Las lineas en blanco de sobra se ignoran.
        while (i < logicas.size()) {
            if (logicas.get(i).length == 0) {
                i = i + 1;
                continue;
            }
            int nro = numeros.get(i).intValue();
            String[] par = partirCabecera(logicas.get(i), nro);
            if (!par[0].equalsIgnoreCase("Name")) {
                throw new IOException("invalid manifest format (line " + nro + ")");
            }
            String nombre = par[1];
            Attributes seccion = this.entries.get(nombre);
            if (seccion == null) {
                seccion = new Attributes();
                this.entries.put(nombre, seccion);
            }
            i = i + 1;
            while (i < logicas.size() && logicas.get(i).length != 0) {
                leerCabecera(seccion, logicas.get(i), numeros.get(i).intValue());
                i = i + 1;
            }
        }
    }

    /**
     * Parte el contenido en lineas **logicas**: cada linea fisica que empieza con un espacio se pega
     * a la anterior, sin ese espacio.
     *
     * <p>Se pega a nivel de bytes. Ver la cabecera de la clase: es lo que permite que el escritor
     * parta caracteres UTF-8 en el corte de 72.
     *
     * <p>Los tres finales de linea --CRLF, LF y CR solo-- valen, porque el JDK acepta los tres.
     */
    private static void plegar(byte[] datos, List<byte[]> logicas, List<Integer> numeros)
            throws IOException {
        int pos = 0;
        int nro = 0;
        while (pos < datos.length) {
            nro = nro + 1;
            int fin = pos;
            while (fin < datos.length && datos[fin] != '\n' && datos[fin] != '\r') {
                fin = fin + 1;
            }
            int siguiente = fin;
            if (siguiente < datos.length) {
                if (datos[siguiente] == '\r' && siguiente + 1 < datos.length
                        && datos[siguiente + 1] == '\n') {
                    siguiente = siguiente + 2;
                } else {
                    siguiente = siguiente + 1;
                }
            }
            int largo = fin - pos;
            if (largo > 0 && datos[pos] == ' ') {
                // Una continuacion. Tiene que haber una linea logica no vacia adelante: si no, el
                // manifiesto arranca con un espacio y no hay a que pegarla.
                if (logicas.isEmpty() || logicas.get(logicas.size() - 1).length == 0) {
                    throw new IOException("misplaced continuation line (line " + nro + ")");
                }
                byte[] previa = logicas.get(logicas.size() - 1);
                byte[] junta = new byte[previa.length + largo - 1];
                System.arraycopy(previa, 0, junta, 0, previa.length);
                System.arraycopy(datos, pos + 1, junta, previa.length, largo - 1);
                logicas.set(logicas.size() - 1, junta);
            } else {
                byte[] linea = new byte[largo];
                System.arraycopy(datos, pos, linea, 0, largo);
                logicas.add(linea);
                numeros.add(Integer.valueOf(nro));
            }
            pos = siguiente;
        }
    }

    /** Parte `nombre: valor` en sus dos mitades, con los errores que da el JDK. */
    private static String[] partirCabecera(byte[] linea, int nro) throws IOException {
        int i = 0;
        while (i < linea.length && linea[i] != ':') {
            i = i + 1;
        }
        // Hace falta el `:` **y** el espacio que va detras: `A:uno` es invalido para el JDK.
        if (i >= linea.length || i + 1 >= linea.length || linea[i + 1] != ' ') {
            throw new IOException("invalid header field (line " + nro + ")");
        }
        String nombre = new String(linea, 0, i, StandardCharsets.UTF_8);
        String valor = new String(linea, i + 2, linea.length - i - 2, StandardCharsets.UTF_8);
        return new String[] { nombre, valor };
    }

    private static void leerCabecera(Attributes destino, byte[] linea, int nro) throws IOException {
        String[] par = partirCabecera(linea, nro);
        try {
            destino.putValue(par[0], par[1]);
        } catch (IllegalArgumentException e) {
            throw new IOException("invalid header field name: " + par[0] + " (line " + nro + ")");
        }
    }

    private static byte[] leerTodo(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("is");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n = is.read(buf, 0, buf.length);
        while (n > 0) {
            out.write(buf, 0, n);
            n = is.read(buf, 0, buf.length);
        }
        return out.toByteArray();
    }
}
