package java.io;

import java.security.Permission;
import java.security.PermissionCollection;

// KajiLibrary's java.io.FilePermission -- el permiso de tocar archivos, con su gramatica de rutas.
//
// **Es logica pura y por eso se puede implementar entera.** Un permiso no abre archivos: contesta
// "¿lo que tengo alcanza para lo que se pide?". Esa cuenta se hace sobre dos cadenas y no toca el
// disco, asi que no depende de nada que a esta VM le falte.
//
// **Nota sobre el estado del modelo**, la misma que `java.security.Permission`: desde JDK 24 el
// `SecurityManager` esta permanentemente deshabilitado, asi que nadie consulta estos permisos en
// tiempo de ejecucion. Se implementan porque son contrato -- estan en firmas que otro codigo nombra
// -- no porque hagan cumplir nada. El JDK 25 la marca deprecada y para remocion, y aca se reproduce
// esa marca en vez de omitirla: quien la use tiene que ver el mismo aviso que veria compilando
// contra el JDK.
//
// <h2>La gramatica de nombres</h2>
//
// <pre>
//   "&lt;&lt;ALL FILES&gt;&gt;"  todo el sistema de archivos
//   "/tmp/x"          exactamente ese archivo
//   "/tmp/*"          los archivos **directamente** dentro de /tmp, no los de sus subdirectorios
//   "/tmp/-"          /tmp y todo lo que cuelgue, a cualquier profundidad
//   "*" / "-"         idem sobre el directorio actual
// </pre>
//
// Dos detalles del contrato que parecen arbitrarios y no lo son:
//
//   - `"/tmp/-"` **no** implica `"/tmp"`. El comodin habla de lo que hay *dentro*; el directorio en
//     si es otro objeto, y borrarlo no es lo mismo que borrar su contenido.
//   - `"/tmp/-"` si implica `"/tmp/*"` -- lo recursivo contiene a lo plano -- pero no al reves.
//
// <h2>Las rutas se normalizan, y no se canonicalizan</h2>
//
// Antes de comparar, la ruta se normaliza: los dos separadores se unifican, los repetidos se
// colapsan, y los segmentos `.` y `..` se resuelven **de forma lexica**. Por eso `"a.txt"` implica
// `"./a.txt"`.
//
// Lo que **no** se hace es resolver la ruta contra el directorio actual ni seguir enlaces: `"/tmp/a"`
// y `"tmp/a"` no se implican, aunque en una sesion dada pudieran nombrar el mismo archivo. Es lo
// mismo que hace el JDK desde la 9 (`jdk.io.permissionsUseCanonicalPath` en falso por defecto), y la
// razon es que canonicalizar toca el disco: el resultado dependeria de que existiera el archivo y de
// donde apuntara un enlace **en el momento de construir el permiso**, con lo cual el mismo permiso
// podria implicar cosas distintas en dos corridas. Un permiso tiene que ser una decision estable.
@Deprecated(since = "24", forRemoval = true)
public final class FilePermission extends Permission implements Serializable {

    private static final int LEER = 1;
    private static final int ESCRIBIR = 2;
    private static final int EJECUTAR = 4;
    private static final int BORRAR = 8;
    private static final int LEER_ENLACE = 16;

    private static final String TODOS_LOS_ARCHIVOS = "<<ALL FILES>>";

    // Las acciones concedidas, como bits. Se guarda la mascara y no la cadena porque la pregunta
    // que se hace mil veces es "¿estan estas incluidas?", que en bits es un `and`.
    private final int mascara;

    private final boolean todosLosArchivos;

    // El comodin de la ruta: `directorio` para `/*`, y ademas `recursivo` para `/-`. Son dos
    // banderas y no un enum de tres porque `recursivo` implica `directorio`, y tenerlas separadas
    // hace que las cuatro combinaciones de `implies` se lean tal cual estan escritas en el contrato.
    private final boolean directorio;
    private final boolean recursivo;

    // La ruta normalizada, ya sin el comodin final: la raiz por un lado (`""`, `"\"`, `"C:\"`) y los
    // segmentos por otro. Partida asi porque las dos preguntas de `implies` son "¿misma raiz?" y
    // "¿es prefijo de segmentos?", y sobre la cadena entera la segunda daria falsos positivos --
    // `/tmpx/a` empieza por `/tmp` como texto y no esta adentro de `/tmp`.
    private final String raiz;
    private final String[] segmentos;

    /**
     * Un permiso sobre `path` para `actions`.
     *
     * @throws NullPointerException si `path` es `null`
     * @throws IllegalArgumentException si `actions` es `null`, esta vacio, o nombra algo que no sea
     *     `read`, `write`, `execute`, `delete` o `readlink`. Se rechaza en vez de ignorarse: una
     *     accion mal escrita que se descartara en silencio daria un permiso mas angosto que el que
     *     se quiso escribir, y eso solo se descubre el dia que niega algo.
     */
    public FilePermission(String path, String actions) {
        super(path);
        if (path == null) {
            throw new NullPointerException("name can't be null");
        }
        this.mascara = mascaraDe(actions);

        if (path.equals(TODOS_LOS_ARCHIVOS)) {
            this.todosLosArchivos = true;
            this.directorio = false;
            this.recursivo = false;
            this.raiz = "";
            this.segmentos = new String[0];
            return;
        }
        this.todosLosArchivos = false;

        // El comodin se saca **antes** de normalizar: normalizar primero podria mover el `-` de
        // lugar al resolver un `..` que viniera justo antes.
        String bruto = path;
        boolean dir = false;
        boolean rec = false;
        if (bruto.equals("*")) {
            dir = true;
            bruto = "";
        } else if (bruto.equals("-")) {
            dir = true;
            rec = true;
            bruto = "";
        } else if (bruto.length() >= 2 && esSeparador(bruto.charAt(bruto.length() - 2))) {
            char ultimo = bruto.charAt(bruto.length() - 1);
            if (ultimo == '*') {
                dir = true;
                bruto = bruto.substring(0, bruto.length() - 1);
            } else if (ultimo == '-') {
                dir = true;
                rec = true;
                bruto = bruto.substring(0, bruto.length() - 1);
            }
        }
        this.directorio = dir;
        this.recursivo = rec;

        this.raiz = raizDe(bruto);
        this.segmentos = segmentosDe(bruto.substring(this.raiz.length()));
    }

    /**
     * Si este permiso alcanza para `p`.
     *
     * <p>Son dos preguntas independientes y las dos tienen que dar que si: que las acciones de `p`
     * esten todas incluidas en las de este, y que la ruta de `p` caiga dentro de la de este.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission)) {
            return false;
        }
        FilePermission otro = (FilePermission) p;
        if ((this.mascara & otro.mascara) != otro.mascara) {
            return false;
        }
        return this.cubreLaRutaDe(otro);
    }

    // La mitad de `implies` que mira solo la ruta.
    //
    // Los cuatro casos salen del contrato y estan escritos uno por uno a proposito: colapsarlos en
    // una comparacion de prefijos con un `if` de mas es donde se cuela que `/tmp/-` implique `/tmp`,
    // que es justo lo que no debe pasar.
    private boolean cubreLaRutaDe(FilePermission otro) {
        if (this.todosLosArchivos) {
            return true;
        }
        if (otro.todosLosArchivos) {
            return false;               // solo el comodin universal se implica a si mismo
        }
        if (!this.raiz.equals(otro.raiz)) {
            return false;               // absoluto y relativo no se comparan; ver la nota de clase
        }
        int mios = this.segmentos.length;
        int suyos = otro.segmentos.length;

        if (!this.directorio) {
            // Una ruta pelada implica exactamente a si misma.
            return !otro.directorio && suyos == mios && this.esPrefijoDe(otro);
        }
        if (this.recursivo) {
            if (otro.directorio) {
                // `/tmp/-` cubre a `/tmp/*` y a `/tmp/sub/-`: cualquier comodin de mas abajo.
                return suyos >= mios && this.esPrefijoDe(otro);
            }
            // Estricto: `/tmp/-` habla de lo que hay dentro de /tmp, y /tmp no esta dentro de /tmp.
            return suyos > mios && this.esPrefijoDe(otro);
        }
        if (otro.directorio) {
            // `/tmp/*` solo cubre al mismo `/tmp/*`, y nunca a un `-` que es mas amplio.
            return !otro.recursivo && suyos == mios && this.esPrefijoDe(otro);
        }
        // Un nivel exacto: `/tmp/*` cubre `/tmp/a.txt` y no `/tmp/sub/a.txt`.
        return suyos == mios + 1 && this.esPrefijoDe(otro);
    }

    private boolean esPrefijoDe(FilePermission otro) {
        if (otro.segmentos.length < this.segmentos.length) {
            return false;
        }
        int i = 0;
        while (i < this.segmentos.length) {
            if (!this.segmentos[i].equals(otro.segmentos[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * Las acciones en orden canonico: `read,write,execute,delete,readlink`.
     *
     * <p>Canonico y no el orden en que se escribieron, para que dos permisos iguales se vean
     * iguales: `"write,read"` y `"read,write"` conceden lo mismo y tienen que imprimirse igual, si
     * no `equals` y `toString` contarian historias distintas.
     */
    public String getActions() {
        StringBuilder sb = new StringBuilder();
        if ((this.mascara & LEER) != 0) {
            sb.append("read");
        }
        if ((this.mascara & ESCRIBIR) != 0) {
            coma(sb);
            sb.append("write");
        }
        if ((this.mascara & EJECUTAR) != 0) {
            coma(sb);
            sb.append("execute");
        }
        if ((this.mascara & BORRAR) != 0) {
            coma(sb);
            sb.append("delete");
        }
        if ((this.mascara & LEER_ENLACE) != 0) {
            coma(sb);
            sb.append("readlink");
        }
        return sb.toString();
    }

    /**
     * Dos permisos son iguales si conceden lo mismo sobre la misma ruta.
     *
     * <p>Se compara la ruta **normalizada** y no el nombre tal como se escribio, por lo mismo que
     * `implies`: `"/tmp/./a"` y `"/tmp/a"` son el mismo permiso, y si no fueran iguales una
     * coleccion guardaria los dos y contestaria dos veces lo mismo.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilePermission)) {
            return false;
        }
        FilePermission otro = (FilePermission) obj;
        if (this.mascara != otro.mascara
                || this.todosLosArchivos != otro.todosLosArchivos
                || this.directorio != otro.directorio
                || this.recursivo != otro.recursivo
                || !this.raiz.equals(otro.raiz)
                || this.segmentos.length != otro.segmentos.length) {
            return false;
        }
        return this.esPrefijoDe(otro);
    }

    public int hashCode() {
        int h = this.raiz.hashCode();
        int i = 0;
        while (i < this.segmentos.length) {
            h = h * 31 + this.segmentos[i].hashCode();
            i = i + 1;
        }
        h = h * 31 + this.mascara;
        h = h * 31 + (this.directorio ? 2 : 0) + (this.recursivo ? 1 : 0);
        return h * 31 + (this.todosLosArchivos ? 1 : 0);
    }

    /**
     * Una coleccion para juntar permisos de archivo.
     *
     * <p>No indexa por nombre como hace `BasicPermission`, y no es una omision: ahi el comodin cae
     * siempre en un lugar previsible (`a.b.*`) y se pueden probar los cuatro candidatos; aca
     * `"/a/-"` puede cubrir a `"/a/b/c/d"` a cualquier profundidad, asi que no hay un conjunto
     * chico de claves que consultar. Se recorre.
     */
    public PermissionCollection newPermissionCollection() {
        return new FilePermissionCollection();
    }

    // ---- partido y normalizado de rutas ------------------------------------------------------

    private static boolean esSeparador(char c) {
        return c == '/' || c == '\\';
    }

    // La raiz: `""` (relativa), `"\"` (absoluta), `"C:"` (relativa a un disco) o `"C:\"`.
    //
    // La distincion entre `"C:"` y `"C:\"` se conserva porque son cosas distintas en Windows: la
    // primera es relativa al directorio actual **de ese disco**. Fundirlas haria que un permiso
    // sobre una implicara a la otra.
    private static String raizDe(String p) {
        int i = 0;
        int n = p.length();
        if (n >= 2 && p.charAt(1) == ':' && esLetra(p.charAt(0))) {
            i = 2;
        }
        StringBuilder sb = new StringBuilder(p.substring(0, i));
        if (i < n && esSeparador(p.charAt(i))) {
            sb.append(File.separatorChar);
            while (i < n && esSeparador(p.charAt(i))) {
                i = i + 1;
            }
        }
        return sb.toString();
    }

    private static boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // Los segmentos de la parte sin raiz, con `.` descartado y `..` resuelto lexicamente.
    private static String[] segmentosDe(String resto) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int i = 0;
        int n = resto.length();
        while (i < n) {
            int j = i;
            while (j < n && !esSeparador(resto.charAt(j))) {
                j = j + 1;
            }
            if (j > i) {
                String seg = resto.substring(i, j);
                if (seg.equals(".")) {
                    // nada: `.` es "aca mismo"
                } else if (seg.equals("..")) {
                    // Si no hay a quien subir, el `..` se conserva: descartarlo convertiria
                    // `../secreto` en `secreto`, o sea en otro archivo.
                    if (!out.isEmpty() && !out.get(out.size() - 1).equals("..")) {
                        out.remove(out.size() - 1);
                    } else {
                        out.add("..");
                    }
                } else {
                    out.add(seg);
                }
            }
            i = j + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    // ---- acciones ----------------------------------------------------------------------------

    private static void coma(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(',');
        }
    }

    private static int mascaraDe(String actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions can't be null");
        }
        int m = 0;
        int i = 0;
        int n = actions.length();
        while (i <= n) {
            int j = i;
            while (j < n && actions.charAt(j) != ',') {
                j = j + 1;
            }
            String pieza = actions.substring(i, j).trim();
            if (pieza.length() > 0) {
                m = m | unaAccion(pieza);
            } else if (n > 0) {
                // `"read,,write"` o `"read,"`: una coma sin accion es un error de escritura, y
                // aceptarla escondería el que de verdad importa -- una accion mal tipeada al lado.
                throw new IllegalArgumentException("invalid actions: " + actions);
            }
            i = j + 1;
        }
        if (m == 0) {
            throw new IllegalArgumentException("invalid actions: " + actions);
        }
        return m;
    }

    private static int unaAccion(String s) {
        String a = s.toLowerCase();
        if (a.equals("read")) {
            return LEER;
        }
        if (a.equals("write")) {
            return ESCRIBIR;
        }
        if (a.equals("execute")) {
            return EJECUTAR;
        }
        if (a.equals("delete")) {
            return BORRAR;
        }
        if (a.equals("readlink")) {
            return LEER_ENLACE;
        }
        throw new IllegalArgumentException("invalid actions: " + s);
    }
}
