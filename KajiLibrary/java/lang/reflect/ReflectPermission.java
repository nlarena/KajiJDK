package java.lang.reflect;

/**
 * KajiLibrary's java.lang.reflect.ReflectPermission -- el permiso para saltearse el control de
 * acceso por reflexion.
 *
 * <p>Un solo nombre le importa: `suppressAccessChecks`, que es lo que habilita
 * `AccessibleObject.setAccessible(true)`. Sin ese permiso la reflexion puede **mirar** todo y
 * **tocar** solo lo publico, que es la diferencia entre inspeccionar y romper el encapsulamiento.
 *
 * <p>No tiene acciones -- el permiso es o no es-- y por eso hereda de `BasicPermission`, que ya
 * resuelve el comodin (`*`, `x.*`) y la implicacion por prefijo.
 */
public final class ReflectPermission extends java.security.BasicPermission {

    public ReflectPermission(String name) {
        super(name);
    }

    /** El de arriba; `actions` se ignora, que es lo que hace `BasicPermission`. */
    public ReflectPermission(String name, String actions) {
        super(name, actions);
    }
}
