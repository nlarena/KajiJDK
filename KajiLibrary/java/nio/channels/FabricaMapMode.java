package java.nio.channels;

/**
 * Fabrica de {@link FileChannel.MapMode} para los modos que viven **fuera** de `java.nio.channels`.
 *
 * <p>No es una clase del JDK: es andamiaje nuestro, del mismo tipo que `KajiFileChannel`. Existe
 * porque `MapMode` no es un `enum` --la lista de modos queda abierta a proposito-- pero su
 * constructor no es publico, asi que `jdk.nio.mapmode.ExtendedMapMode` no tiene forma de fabricar
 * sus dos constantes desde otro paquete. En el JDK real ese puente lo hace
 * `jdk.internal.access.SharedSecrets` con un `MethodHandle` a un constructor privado; aca alcanza
 * con un metodo de paquete, que es el mismo puente sin la maquinaria.
 *
 * <p>Que este en `java.nio.channels` es justamente el punto: es el unico lugar desde el que se ve
 * el constructor de `MapMode`.
 */
public final class FabricaMapMode {

    private FabricaMapMode() {
    }

    /**
     * Un modo de mapeo nuevo con el nombre dado.
     *
     * <p>El nombre es lo unico que distingue a un `MapMode` de otro: `MapMode` no lleva
     * comportamiento, es una etiqueta que `map()` interpreta.
     *
     * @param nombre el nombre del modo, el que devuelve {@code toString()}
     * @return un `MapMode` nuevo, distinto de cualquier otro
     * @throws NullPointerException si `nombre` es nulo
     */
    public static FileChannel.MapMode nuevo(String nombre) {
        if (nombre == null) {
            throw new NullPointerException("nombre");
        }
        return new FileChannel.MapMode(nombre);
    }
}
