package java.nio.file.attribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Las utilidades para pasar de `"rwxr-xr-x"` a un `Set<PosixFilePermission>` y volver.
//
// **Es puro texto y por eso esta entera**, aunque el paquete que la rodea no pueda leer permisos del
// disco: convertir la cadena no necesita ningun nativo. Sirve por si sola --por ejemplo para leer
// un modo de un archivo de configuracion o de un tar-- y ademas es lo que hace que
// `asFileAttribute` tenga sentido el dia que haya un nativo que fije permisos al crear.
public final class PosixFilePermissions {

    // Es una clase de utilidades: no hay nada que instanciar.
    private PosixFilePermissions() {
    }

    private static void escribir(StringBuilder sb, Set<PosixFilePermission> perms,
            PosixFilePermission r, PosixFilePermission w, PosixFilePermission x) {
        sb.append(perms.contains(r) ? 'r' : '-');
        sb.append(perms.contains(w) ? 'w' : '-');
        sb.append(perms.contains(x) ? 'x' : '-');
    }

    /**
     * El modo en las nueve letras de `ls -l`, sin el primer caracter de tipo: `"rwxr-x---"`.
     *
     * <p>Siempre nueve caracteres: un permiso ausente es un guion, no una posicion que se saltea.
     */
    public static String toString(Set<PosixFilePermission> perms) {
        StringBuilder sb = new StringBuilder(9);
        escribir(sb, perms, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        escribir(sb, perms, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        escribir(sb, perms, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE);
        return sb.toString();
    }

    private static boolean esPuesto(char c, char esperado) {
        if (c == esperado) {
            return true;
        }
        if (c == '-') {
            return false;
        }
        throw new IllegalArgumentException("Invalid mode");
    }

    /**
     * Lo inverso de `toString`.
     *
     * <p>Se exige la longitud exacta y la letra exacta en cada posicion --`r` solo en la primera de
     * cada terna, `w` en la segunda, `x` en la tercera--. Aceptar `"rwx"` a secas o `"xwrxwrxwr"`
     * seria adivinar que quiso decir quien llamo, y una cadena de modo mal escrita es justo el lugar
     * donde conviene fallar fuerte.
     *
     * @throws IllegalArgumentException si la cadena no tiene nueve caracteres o alguno no
     *     corresponde a su posicion
     */
    public static Set<PosixFilePermission> fromString(String perms) {
        if (perms.length() != 9) {
            throw new IllegalArgumentException("Invalid mode");
        }
        Set<PosixFilePermission> resultado = new HashSet<PosixFilePermission>();
        if (esPuesto(perms.charAt(0), 'r')) {
            resultado.add(PosixFilePermission.OWNER_READ);
        }
        if (esPuesto(perms.charAt(1), 'w')) {
            resultado.add(PosixFilePermission.OWNER_WRITE);
        }
        if (esPuesto(perms.charAt(2), 'x')) {
            resultado.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if (esPuesto(perms.charAt(3), 'r')) {
            resultado.add(PosixFilePermission.GROUP_READ);
        }
        if (esPuesto(perms.charAt(4), 'w')) {
            resultado.add(PosixFilePermission.GROUP_WRITE);
        }
        if (esPuesto(perms.charAt(5), 'x')) {
            resultado.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if (esPuesto(perms.charAt(6), 'r')) {
            resultado.add(PosixFilePermission.OTHERS_READ);
        }
        if (esPuesto(perms.charAt(7), 'w')) {
            resultado.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (esPuesto(perms.charAt(8), 'x')) {
            resultado.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return resultado;
    }

    // El `FileAttribute` que devuelve `asFileAttribute`. Guarda una copia inmutable del conjunto
    // para que el atributo no cambie si quien llamo modifica el suyo despues.
    private static final class AtributoPosix implements FileAttribute<Set<PosixFilePermission>> {

        private final Set<PosixFilePermission> perms;

        AtributoPosix(Set<PosixFilePermission> perms) {
            this.perms = perms;
        }

        public String name() {
            return "posix:permissions";
        }

        public Set<PosixFilePermission> value() {
            return this.perms;
        }
    }

    /**
     * Envuelve los permisos como el `FileAttribute` que reciben `Files.createFile` y compania.
     *
     * <p>El conjunto se **copia y se revisa** aca y no al usarlo: si trae algo que no es un
     * `PosixFilePermission` --posible con un `Set` crudo-- el error tiene que aparecer donde se
     * armo el atributo, no adentro de la creacion del archivo.
     *
     * <p>Ojo: KajiJDK no puede honrar el atributo. `Files.createFile` con un `FileAttribute` no
     * vacio levanta `UnsupportedOperationException`, porque el nativo que crea archivos no toma
     * permisos.
     */
    public static FileAttribute<Set<PosixFilePermission>> asFileAttribute(
            Set<PosixFilePermission> perms) {
        Set<PosixFilePermission> copia = new HashSet<PosixFilePermission>();
        java.util.Iterator<PosixFilePermission> it = perms.iterator();
        while (it.hasNext()) {
            PosixFilePermission p = it.next();
            if (p == null) {
                throw new NullPointerException();
            }
            copia.add(p);
        }
        return new AtributoPosix(Collections.unmodifiableSet(copia));
    }
}
