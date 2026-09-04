package jdk.dynalink.linker;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

/**
 * Aporta las conversiones de tipo propias de un lenguaje.
 *
 * <h2>Por que la conversion tambien lleva guarda</h2>
 *
 * <p>Porque casi nunca depende solo de las clases. "Convertir a numero" puede valer para las
 * cadenas que parecen numeros y no para las otras, aunque las dos sean {@code String}. El
 * resultado es un {@link GuardedInvocation}: la conversion mas la condicion bajo la cual es esa y
 * no otra. Cuando la guarda falla, el que invoca prueba la siguiente alternativa.
 *
 * <h2>Por que el lookup llega como un proveedor y no directo</h2>
 *
 * <p>Porque un {@code Lookup} es una credencial y entregarla sin que nadie la pida seria
 * regalarla. El {@link Supplier} obliga a llamarlo para obtenerla, que es donde el control puede
 * ocurrir; un convertidor que no necesita acceso privilegiado simplemente nunca lo invoca.
 *
 * @since 9
 */
public interface GuardingTypeConverterFactory {

    /**
     * Como convertir de un tipo a otro, con la condicion bajo la cual vale.
     *
     * @param sourceType el tipo de partida
     * @param targetType el tipo de llegada
     * @param lookupSupplier el lookup del sitio, si hace falta
     * @return la conversion con su guarda, o {@code null} si esta fabrica no la sabe hacer
     * @throws Exception si la conversion no se puede construir
     */
    GuardedInvocation convertToType(Class<?> sourceType, Class<?> targetType,
            Supplier<MethodHandles.Lookup> lookupSupplier) throws Exception;
}
