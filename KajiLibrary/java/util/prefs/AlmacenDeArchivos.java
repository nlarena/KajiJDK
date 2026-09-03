package java.util.prefs;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import jdk.internal.io.Fs;

// El deposito por omision: un directorio por nodo, un archivo por tabla de claves.
//
// POR QUE ESTA FORMA. Un archivo por nodo y no uno solo con todo el arbol, porque dos partes del
// programa que escriben en nodos distintos no se pisan y una escritura fallida arruina un nodo y no
// el arbol entero. Un directorio por nodo y no una ruta codificada dentro del nombre, porque asi
// `childrenNamesSpi` es un `Fs.list` y el borrado de un subarbol es el borrado que ya hace
// {@link AbstractPreferences} de abajo hacia arriba.
//
// EL NOMBRE DEL DIRECTORIO NO ES EL DEL NODO. Un nombre de nodo puede tener cualquier caracter
// menos `/` y distingue mayusculas; un nombre de directorio en Windows **no** distingue mayusculas
// y prohibe `< > : " \ | ? *`, los nombres reservados (`con`, `nul`, `lpt1`...) y el punto final. Si
// se usara el nombre tal cual, los nodos `Datos` y `datos` --que el contrato dice que son
// distintos-- serian el mismo directorio, que es perdida de datos silenciosa. Por eso un nombre que
// no cae en el conjunto seguro `[a-z0-9.-]` se escribe como `_` seguido del hexadecimal de sus
// unidades UTF-16. Los nombres comunes quedan legibles y el resto queda correcto; que lo mangleado
// empiece con `_` y lo no mangleado nunca lo haga es lo que hace la vuelta sin ambiguedad.
//
// CUANDO SE ESCRIBE. No hay hilo de sincronizacion en segundo plano --el JDK tiene uno que corre
// cada treinta segundos-- asi que "eventualmente" no llegaria nunca. En cambio cada mutacion
// escribe **en el acto**, y si esa escritura falla la falla queda anotada y la cuenta `flush()` o
// `sync()`, que son los unicos metodos del contrato que pueden tirar. Asi `put()` sigue sin tirar
// --como manda el contrato-- y aun asi nada se pierde por un corte de luz entre el `put` y el
// `flush` que el programa quiza nunca haga.
final class AlmacenDeArchivos extends AbstractPreferences {

    // El archivo con las claves de este nodo. Empieza con `_`, asi que ningun directorio de hijo
    // mangleado ni sin manglear puede llamarse igual.
    private static final String ARCHIVO = "_prefs";

    private final String dir;

    // `null` mientras el archivo no se leyo. La lectura es perezosa a proposito: materializar un
    // nodo para preguntarle el nombre a un hijo no tiene por que tocar el disco.
    private Map<String, String> valores;

    // Hay cambios en memoria que no estan en disco.
    private boolean sucio;

    // Por que no estan, o `null` si estan. Lo unico que lo lee es `flushSpi`.
    private String falla;

    // El nodo ya se borro del disco; `flushSpi` no debe recrear el directorio.
    private boolean eliminado;

    // La raiz de un arbol, colgada de `dir`.
    AlmacenDeArchivos(String dir) {
        super(null, "");
        this.dir = dir;
    }

    private AlmacenDeArchivos(AlmacenDeArchivos padre, String nombre) {
        super(padre, nombre);
        this.dir = padre.dir + "/" + aDirectorio(nombre);
        // Nuevo si no habia directorio: es lo que decide si sale `childAdded`, y preguntarselo al
        // disco es la unica respuesta que no se inventa.
        this.newNode = !esDirectorio(this.dir);
    }

    // ---- los nueve del Spi -----------------------------------------------------------------

    protected String getSpi(String key) {
        cargar();
        return valores.get(key);
    }

    protected void putSpi(String key, String value) {
        cargar();
        valores.put(key, value);
        sucio = true;
        escribirSinTirar();
    }

    protected void removeSpi(String key) {
        cargar();
        if (valores.remove(key) != null) {
            sucio = true;
            escribirSinTirar();
        }
    }

    protected String[] keysSpi() throws BackingStoreException {
        cargar();
        return valores.keySet().toArray(new String[0]);
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        if (!esDirectorio(dir)) {
            // Un nodo que todavia no se escribio no tiene hijos en disco. No es una falla del
            // deposito y devolver una lista vacia es la respuesta exacta.
            return new String[0];
        }
        String[] entradas = Fs.list(dir);
        if (entradas == null) {
            throw new BackingStoreException("no se pudo listar " + dir);
        }
        ArrayList<String> nombres = new ArrayList<String>();
        for (int i = 0; i < entradas.length; i++) {
            if (!esDirectorio(dir + "/" + entradas[i])) {
                continue; // el archivo de claves, o basura que no pusimos nosotros
            }
            String n = aNombre(entradas[i]);
            if (n != null) {
                nombres.add(n);
            }
        }
        return nombres.toArray(new String[0]);
    }

    protected AbstractPreferences childSpi(String name) {
        return new AlmacenDeArchivos(this, name);
    }

    protected void removeNodeSpi() throws BackingStoreException {
        String archivo = dir + "/" + ARCHIVO;
        if (existe(archivo) && !Fs.delete(archivo)) {
            throw new BackingStoreException("no se pudo borrar " + archivo);
        }
        if (esDirectorio(dir) && !Fs.delete(dir)) {
            throw new BackingStoreException("no se pudo borrar el directorio " + dir);
        }
        valores = new TreeMap<String, String>();
        sucio = false;
        falla = null;
        eliminado = true;
    }

    protected void flushSpi() throws BackingStoreException {
        if (eliminado) {
            return;
        }
        // El directorio se crea aunque no haya ni una clave: es lo que hace que el nodo exista para
        // la proxima VM, y `node("x"); flush();` tiene que dejarlo existiendo.
        escribir();
    }

    protected void syncSpi() throws BackingStoreException {
        if (eliminado) {
            return;
        }
        escribir();
        // Recien ahora se tira lo leido: al reves se perderian los cambios que todavia no bajaron.
        valores = null;
    }

    // ---- disco ------------------------------------------------------------------------------

    private void cargar() {
        if (valores != null) {
            return;
        }
        Map<String, String> m = new TreeMap<String, String>();
        byte[] b = Fs.readAllBytes(dir + "/" + ARCHIVO);
        if (b != null) {
            leer(new String(b, java.nio.charset.StandardCharsets.ISO_8859_1), m);
        }
        valores = m;
    }

    private void escribirSinTirar() {
        try {
            escribir();
        } catch (BackingStoreException e) {
            // No hay por donde contarlo: `put` y `remove` no tiran. Queda anotado para el `flush`.
            falla = e.getMessage();
        }
    }

    private void escribir() throws BackingStoreException {
        if (!esDirectorio(dir)) {
            Fs.mkdir(dir, true);
            if (!esDirectorio(dir)) {
                // Aca es donde se entera el programa de que no hay donde guardar nada: la VM no
                // tiene `user.home` ni `java.io.tmpdir`, asi que si el directorio de trabajo no se
                // deja escribir no queda deposito.
                throw new BackingStoreException("no se pudo crear el directorio " + dir);
            }
        }
        if (!sucio && falla == null) {
            return;
        }
        cargar();
        byte[] b = escribirTabla().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        if (!Fs.writeAllBytes(dir + "/" + ARCHIVO, b, false)) {
            String m = "no se pudo escribir " + dir + "/" + ARCHIVO;
            falla = m;
            throw new BackingStoreException(m);
        }
        sucio = false;
        falla = null;
    }

    private static boolean existe(String ruta) {
        return (Fs.stat(ruta) & Fs.EXISTE) != 0;
    }

    private static boolean esDirectorio(String ruta) {
        return (Fs.stat(ruta) & Fs.ES_DIRECTORIO) != 0;
    }

    // ---- el formato del archivo -------------------------------------------------------------
    //
    // Una linea por entrada, `clave=valor`, con todo lo que no sea ASCII imprimible escrito como
    // `\uXXXX`. Se escapa asi y no se guarda UTF-8 crudo por una razon concreta: el archivo queda
    // en ASCII de siete bits y entonces no depende de con que codificacion lo abra la proxima VM
    // --ni un editor, ni un `type`, ni un `cat`-- que es exactamente el tipo de error que aparece
    // meses despues y en la maquina de otro. Es la misma decision que toma `java.util.Properties`.
    //
    // No es el XML del JDK. Ese formato es el de `exportSubtree`, que es un formato de
    // *intercambio* y esta implementado en {@link Xml}; el de adentro del deposito no lo ve nadie de
    // afuera y no tiene por que pagar un analizador.

    private String escribirTabla() {
        StringBuilder sb = new StringBuilder();
        sb.append("# KajiJDK java.util.prefs -- ").append(absolutePath()).append('\n');
        for (Map.Entry<String, String> e : valores.entrySet()) {
            escapar(sb, e.getKey());
            sb.append('=');
            escapar(sb, e.getValue());
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void escapar(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '=') {
                sb.append("\\=");
            } else if (c == '#') {
                // Solo molesta al principio de la linea, pero escaparlo siempre evita tener que
                // saber en que posicion estamos.
                sb.append("\\#");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c >= 0x20 && c <= 0x7e) {
                sb.append(c);
            } else {
                sb.append("\\u");
                for (int d = 12; d >= 0; d -= 4) {
                    sb.append(HEX[(c >> d) & 0xf]);
                }
            }
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static void leer(String texto, Map<String, String> m) {
        int i = 0;
        int n = texto.length();
        while (i < n) {
            int fin = texto.indexOf('\n', i);
            if (fin < 0) {
                fin = n;
            }
            String linea = texto.substring(i, fin);
            i = fin + 1;
            if (linea.length() == 0 || linea.charAt(0) == '#') {
                continue;
            }
            int corte = separador(linea);
            if (corte < 0) {
                continue; // linea rota: se saltea y se conserva lo demas
            }
            m.put(desescapar(linea.substring(0, corte)), desescapar(linea.substring(corte + 1)));
        }
    }

    // La posicion del primer `=` que no venga escapado.
    private static int separador(String linea) {
        boolean escape = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= s.length()) {
                break;
            }
            char e = s.charAt(i);
            if (e == 'n') {
                sb.append('\n');
            } else if (e == 'r') {
                sb.append('\r');
            } else if (e == 'u' && i + 4 < s.length()) {
                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                sb.append(e);
            }
        }
        return sb.toString();
    }

    // ---- el nombre del directorio -----------------------------------------------------------

    // Los nombres que Windows le reserva a los dispositivos. Un directorio que se llame asi no se
    // puede crear, y el error que da no se parece en nada a la causa.
    private static final String[] RESERVADOS = {
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    };

    static String aDirectorio(String nombre) {
        if (seguro(nombre)) {
            return nombre;
        }
        StringBuilder sb = new StringBuilder(1 + nombre.length() * 4);
        sb.append('_');
        for (int i = 0; i < nombre.length(); i++) {
            char c = nombre.charAt(i);
            for (int d = 12; d >= 0; d -= 4) {
                sb.append(HEX[(c >> d) & 0xf]);
            }
        }
        return sb.toString();
    }

    // `null` si la entrada no es un directorio que hayamos escrito nosotros.
    static String aNombre(String dir) {
        if (dir.length() == 0) {
            return null;
        }
        if (dir.charAt(0) != '_') {
            return seguro(dir) ? dir : null;
        }
        String hex = dir.substring(1);
        if (hex.length() == 0 || hex.length() % 4 != 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(hex.length() / 4);
        for (int i = 0; i < hex.length(); i += 4) {
            int v = 0;
            for (int j = 0; j < 4; j++) {
                int d = Character.digit(hex.charAt(i + j), 16);
                if (d < 0) {
                    return null;
                }
                v = (v << 4) | d;
            }
            sb.append((char) v);
        }
        return sb.toString();
    }

    private static boolean seguro(String nombre) {
        int n = nombre.length();
        if (n == 0 || nombre.charAt(0) == '_' || nombre.charAt(n - 1) == '.') {
            return false;
        }
        for (int i = 0; i < n; i++) {
            char c = nombre.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-';
            // Ojo con lo que NO esta: las mayusculas (Windows no las distingue en un directorio)
            // y el `_` (que es la marca de "esto viene mangleado").
            if (!ok) {
                return false;
            }
        }
        int punto = nombre.indexOf('.');
        String base = punto < 0 ? nombre : nombre.substring(0, punto);
        if (base.length() == 0) {
            return false; // "." y ".." entran por aca
        }
        for (int i = 0; i < RESERVADOS.length; i++) {
            if (base.equals(RESERVADOS[i])) {
                return false;
            }
        }
        return true;
    }
}
