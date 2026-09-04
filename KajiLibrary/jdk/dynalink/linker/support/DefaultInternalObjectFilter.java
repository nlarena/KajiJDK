package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.dynalink.linker.MethodHandleTransformer;

/**
 * Impide que los objetos internos de un lenguaje se escapen a quien lo hospeda.
 *
 * <h2>El problema</h2>
 *
 * <p>Un lenguaje que corre sobre la JVM casi siempre representa sus valores con clases propias —
 * una cadena de scripting no tiene por que ser un {@code String} de Java. Mientras esos objetos
 * circulan adentro del lenguaje esta bien. El problema aparece en el borde: un metodo Java que
 * recibe {@code Object} y devuelve {@code Object} puede terminar guardando en una coleccion un
 * objeto que solo el lenguaje sabe interpretar.
 *
 * <p>La solucion podria ser acordarse de convertir en cada punto de salida. Nadie se acuerda
 * siempre. Esta clase lo hace de una vez para todos los handles.
 *
 * <h2>Por que solo toca los parametros {@code Object}</h2>
 *
 * <p>Porque los demas ya estan tipados: un parametro declarado {@code String} no puede recibir un
 * objeto interno del lenguaje, el verificador no lo permitiria. {@code Object} es exactamente el
 * lugar por donde algo sin tipo puede pasar, y por eso es el unico que hay que filtrar. Lo mismo
 * del lado del retorno, y lo mismo con {@code Object[]} para el parametro variable.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Las decisiones de que filtrar son reales y ocurren. Aplicarlas necesita
 * {@code MethodHandles.filterArguments}, que todavia no se puede fabricar sin soporte de la VM: si
 * hay algo que filtrar, el metodo termina en {@link UnsupportedOperationException}. Un handle sin
 * nada que filtrar vuelve intacto, sin tocar el fabricante.
 *
 * @since 9
 */
public class DefaultInternalObjectFilter implements MethodHandleTransformer {

    private final MethodHandle parameterFilter;
    private final MethodHandle returnFilter;

    /**
     * Los dos filtros, cualquiera de los dos opcional.
     *
     * @param parameterFilter que aplicar a los argumentos que entran, o {@code null}
     * @param returnFilter que aplicar al valor que sale, o {@code null}
     * @throws IllegalArgumentException si alguno no es de tipo {@code (Object)Object}
     */
    public DefaultInternalObjectFilter(final MethodHandle parameterFilter,
            final MethodHandle returnFilter) {
        this.parameterFilter = revisar(parameterFilter, "parameterFilter");
        this.returnFilter = revisar(returnFilter, "returnFilter");
    }

    /**
     * Un filtro tiene que ser {@code (Object)Object}.
     *
     * <p>La exigencia no es burocratica: el filtro se va a insertar donde habia un {@code Object},
     * asi que si tomara o devolviera otra cosa la firma del handle resultante no cerraria. Es mas
     * util fallar aca, con el nombre del argumento, que en el medio de un enlace.
     */
    private static MethodHandle revisar(final MethodHandle filtro, final String nombre) {
        if (filtro == null) {
            return null;
        }
        final MethodType tipo = filtro.type();
        if (tipo.parameterCount() != 1 || tipo.parameterType(0) != Object.class
                || tipo.returnType() != Object.class) {
            throw new IllegalArgumentException(nombre + " tiene que ser de tipo (Object)Object");
        }
        return filtro;
    }

    /**
     * El handle con los filtros puestos donde hacen falta.
     *
     * @param target el handle original
     * @return el filtrado, o el mismo si no habia nada que filtrar
     */
    public MethodHandle transform(final MethodHandle target) {
        final MethodType tipo = target.type();
        final boolean variable = target.isVarargsCollector();
        // El ultimo parametro de un handle de aridad variable es el arreglo, y se trata aparte:
        // lo que hay que filtrar son sus elementos, no el arreglo.
        final int fijos = tipo.parameterCount() - (variable ? 1 : 0);

        MethodHandle[] filtros = null;
        if (parameterFilter != null) {
            for (int i = 0; i < fijos; i++) {
                if (tipo.parameterType(i) == Object.class) {
                    if (filtros == null) {
                        filtros = new MethodHandle[fijos];
                    }
                    filtros[i] = parameterFilter;
                }
            }
        }

        MethodHandle salida = target;
        if (filtros != null) {
            salida = MethodHandles.filterArguments(target, 0, filtros);
        }
        if (returnFilter != null && tipo.returnType() == Object.class) {
            salida = MethodHandles.filterReturnValue(salida, returnFilter);
        }
        if (variable && salida != target) {
            // filterArguments y filterReturnValue devuelven handles de aridad fija: hay que
            // volver a marcarlo como variable o el sitio dejaria de poder pasarle argumentos
            // sueltos, que es la unica razon por la que era variable.
            salida = salida.asVarargsCollector(tipo.parameterType(fijos));
        }
        return salida;
    }
}
