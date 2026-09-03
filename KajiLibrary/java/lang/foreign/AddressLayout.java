package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.AddressLayout -- el layout de un **puntero**.
 *
 * <p>Es un {@link ValueLayout} mas una cosa: puede declarar **a que apunta**
 * ({@link #withTargetLayout}). Sin esa declaracion, un puntero es un numero y no se puede seguir;
 * con ella, `PathElement.dereferenceElement()` puede bajar al otro lado.
 *
 * <p>Que el destino sea opcional y no obligatorio es fiel al C que este paquete describe: ahi los
 * punteros a `void` existen, y forzar un destino obligaria a inventar uno.
 */
public interface AddressLayout extends ValueLayout {

    /** El layout al que apunta, si se declaro. */
    Optional<MemoryLayout> targetLayout();

    /**
     * El mismo puntero, declarando a que apunta.
     *
     * @throws IllegalArgumentException si el destino es `null`
     */
    AddressLayout withTargetLayout(MemoryLayout layout);

    /** El mismo puntero, sin declarar destino. */
    AddressLayout withoutTargetLayout();

    AddressLayout withName(String name);

    AddressLayout withoutName();

    AddressLayout withByteAlignment(long byteAlignment);

    AddressLayout withOrder(ByteOrder order);
}
