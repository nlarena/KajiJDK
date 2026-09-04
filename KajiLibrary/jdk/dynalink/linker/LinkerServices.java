package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import jdk.dynalink.SecureLookupSupplier;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * Lo que un enlazador puede pedirle al que lo hospeda mientras enlaza.
 *
 * <h2>Por que un enlazador necesita algo de afuera</h2>
 *
 * <p>Porque no esta solo. Un enlazador de objetos Java tiene que convertir un argumento al tipo
 * del parametro, y esa conversion puede ser de <strong>otro</strong> lenguaje que corre en el
 * mismo sitio: pasarle una funcion de un lenguaje de scripting a un metodo que espera un
 * {@code Runnable}, por ejemplo. El unico que conoce la cadena entera es el que la compuso, y
 * esta interfaz es como la presta.
 *
 * <p>Por eso {@link #getGuardedInvocation} esta aca: un enlazador puede delegar un pedido a la
 * cadena completa, incluyendose a si mismo, sin conocerla.
 *
 * @since 9
 */
public interface LinkerServices {

    /**
     * Adapta un metodo a otra firma, usando ademas las conversiones de los lenguajes.
     *
     * <p>Es el reemplazo de {@code MethodHandle.asType}, que solo hace las de Java.
     *
     * @param handle el metodo
     * @param fromType la firma pedida
     * @return el metodo adaptado
     */
    MethodHandle asType(MethodHandle handle, MethodType fromType);

    /**
     * Como {@link #asType}, pero sin degradar el valor de retorno.
     *
     * <p>La diferencia importa en una cadena de invocaciones. Si el metodo devuelve {@code long} y
     * el sitio declara {@code int}, {@code asType} trunca — y trunca <strong>antes</strong> de que
     * nadie pueda mirar el valor. Este metodo, en cambio, deja el retorno mas ancho cuando la
     * conversion perderia informacion, y el recorte queda para el final del todo, donde el que
     * invoca puede decidir otra cosa.
     *
     * @param handle el metodo
     * @param fromType la firma pedida
     * @return el metodo adaptado, quiza con un retorno mas ancho que el pedido
     */
    default MethodHandle asTypeLosslessReturn(final MethodHandle handle, final MethodType fromType) {
        final Class<?> retorno = handle.type().returnType();
        return asType(handle, TypeUtilities.isConvertibleWithoutLoss(retorno, fromType.returnType())
                ? fromType : fromType.changeReturnType(retorno));
    }

    /**
     * El metodo que convierte de un tipo a otro, o {@code null} si no hay ninguno.
     *
     * @param sourceType el tipo de partida
     * @param targetType el tipo de llegada
     * @return el convertidor, o {@code null}
     */
    MethodHandle getTypeConverter(Class<?> sourceType, Class<?> targetType);

    /**
     * Si existe alguna conversion de un tipo a otro.
     *
     * <p>No es lo mismo que {@link #getTypeConverter} distinto de {@code null}: la conversion
     * puede existir y estar guardada, de modo que solo se sepa con el valor en la mano.
     *
     * @param from el tipo de partida
     * @param to el tipo de llegada
     * @return si la conversion es posible
     */
    boolean canConvert(Class<?> from, Class<?> to);

    /**
     * Delega un pedido a la cadena completa de enlazadores.
     *
     * @param linkRequest el pedido
     * @return la invocacion enlazada, o {@code null} si nadie supo
     * @throws Exception si el enlace falla
     */
    GuardedInvocation getGuardedInvocation(LinkRequest linkRequest) throws Exception;

    /**
     * Consulta a los {@link ConversionComparator} de la cadena cual destino conviene.
     *
     * @param sourceType el tipo del valor
     * @param targetType1 el primer destino
     * @param targetType2 el segundo destino
     * @return la preferencia, o {@code INDETERMINATE} si ninguno opina
     */
    ConversionComparator.Comparison compareConversion(Class<?> sourceType, Class<?> targetType1,
            Class<?> targetType2);

    /**
     * Aplica el filtro de objetos internos configurado por quien hospeda.
     *
     * <p>Sirve para que los valores propios de un lenguaje no se escapen a otro. Si no hay filtro
     * configurado devuelve el metodo tal cual.
     *
     * @param target el metodo
     * @return el metodo filtrado
     */
    MethodHandle filterInternalObjects(MethodHandle target);

    /**
     * Corre algo con el {@code Lookup} de un sitio disponible.
     *
     * <p>Es la forma de que un convertidor de tipos alcance miembros privados de la clase que hizo
     * la llamada: en vez de recibir la credencial —que podria guardarse— recibe una ventana de
     * tiempo durante la cual esta puesta. Fuera de esa ventana no la tiene.
     *
     * @param <T> lo que devuelve la accion
     * @param operation la accion
     * @param lookupSupplier el portador del lookup
     * @return lo que devolvio la accion
     */
    <T> T getWithLookup(Supplier<T> operation, SecureLookupSupplier lookupSupplier);
}
