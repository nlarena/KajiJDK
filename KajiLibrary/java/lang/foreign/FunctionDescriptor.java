package java.lang.foreign;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.FunctionDescriptor -- la **firma** de una funcion nativa: los
 * layouts de sus argumentos y el de su retorno.
 *
 * <p>Como los layouts, es una descripcion y nada mas: no hay ninguna funcion detras. Por eso esta
 * entero aca aunque {@link Linker} --lo unico que sabe hacer algo con el-- no pueda estar.
 *
 * <p>El retorno es **opcional** y eso no es un detalle: una funcion que no devuelve nada es distinta
 * de una que devuelve algo de tamanio cero. `void` no tiene layout, y modelarlo con un layout vacio
 * confundiria las dos cosas.
 */
public interface FunctionDescriptor {

    /** El layout del retorno, o vacio si la funcion es `void`. */
    Optional<MemoryLayout> returnLayout();

    /** Los layouts de los argumentos, en orden. */
    List<MemoryLayout> argumentLayouts();

    /** El mismo descriptor con otro retorno. */
    FunctionDescriptor changeReturnLayout(MemoryLayout newReturn);

    /** El mismo descriptor sin retorno: la funcion pasa a ser `void`. */
    FunctionDescriptor dropReturnLayout();

    /** Con esos argumentos agregados al final. */
    FunctionDescriptor appendArgumentLayouts(MemoryLayout... addedLayouts);

    /**
     * Con esos argumentos insertados en esa posicion.
     *
     * @throws IllegalArgumentException si la posicion esta fuera de rango
     */
    FunctionDescriptor insertArgumentLayouts(int index, MemoryLayout... addedLayouts);

    /**
     * El {@link MethodType} equivalente: los tipos Java que transportan estos layouts.
     *
     * <p>Es el puente entre la descripcion de la memoria y la firma del metodo que la manipula. Un
     * layout compuesto no tiene un tipo Java propio, asi que solo se puede convertir un descriptor
     * hecho de valores.
     *
     * @throws UnsupportedOperationException si algun layout no es un {@link ValueLayout}
     */
    MethodType toMethodType();

    /** Un descriptor con retorno. */
    static FunctionDescriptor of(MemoryLayout resLayout, MemoryLayout... argLayouts) {
        return Descriptor.crear(resLayout, argLayouts);
    }

    /** Un descriptor sin retorno. */
    static FunctionDescriptor ofVoid(MemoryLayout... argLayouts) {
        return Descriptor.crear(null, argLayouts);
    }
}
