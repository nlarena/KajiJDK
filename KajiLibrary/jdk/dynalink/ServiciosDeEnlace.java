package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.MethodHandleTransformer;
import jdk.dynalink.linker.MethodTypeConversionStrategy;
import jdk.dynalink.linker.support.TypeUtilities;

// Lo que `DynamicLinker` le presta a los enlazadores.
//
// De paquete a proposito: en el JDK esta clase tampoco es publica --se llama `LinkerServicesImpl`--
// porque nadie de afuera la construye. Se llega a ella por `DynamicLinker.getLinkerServices()`.
//
// **Todo lo que decide aca son preguntas sobre tipos, no sobre manijas.** Si un `String` sirve donde
// se pide un `Object`, cual de dos destinos conviene, que fabrica sabe convertir de uno a otro: eso
// es aritmetica sobre `Class` y funciona entero. Lo que necesita construir una manija de metodo
// --`asType`, `getTypeConverter`-- no puede: esta maquina virtual no tiene manijas, y quien las pida
// recibe la `UnsupportedOperationException` de `MethodHandles`, que dice exactamente eso.
//
// La distincion importa porque es la que separa las dos mitades de la interfaz: la que resuelve
// **si** se puede convertir anda, y la que devuelve **con que** convertir no.
final class ServiciosDeEnlace implements LinkerServices {

    private final GuardingDynamicLinker enlazador;
    private final List<GuardingTypeConverterFactory> fabricas;
    private final List<ConversionComparator> comparadores;
    private final MethodTypeConversionStrategy autoConversion;
    private final MethodHandleTransformer filtroInterno;

    ServiciosDeEnlace(final GuardingDynamicLinker enlazador,
            final List<GuardingTypeConverterFactory> fabricas,
            final List<ConversionComparator> comparadores,
            final MethodTypeConversionStrategy autoConversion,
            final MethodHandleTransformer filtroInterno) {
        this.enlazador = enlazador;
        this.fabricas = fabricas;
        this.comparadores = comparadores;
        this.autoConversion = autoConversion;
        this.filtroInterno = filtroInterno;
    }

    /** {@inheritDoc} */
    public MethodHandle asType(final MethodHandle handle, final MethodType fromType) {
        // La conversion de tipos de metodo del lenguaje primero, y despues la del lenguaje que
        // hospeda, si puso una. El orden no es negociable: la estrategia propia es un ultimo
        // recurso para lo que Java no sabe convertir, no un reemplazo de lo que si sabe.
        final MethodHandle base = handle.asType(fromType);
        return this.autoConversion == null ? base : this.autoConversion.asType(base, fromType);
    }

    /** {@inheritDoc} */
    public MethodHandle getTypeConverter(final Class<?> sourceType, final Class<?> targetType) {
        for (int i = 0; i < this.fabricas.size(); i++) {
            final GuardedInvocation g;
            try {
                g = this.fabricas.get(i).convertToType(sourceType, targetType, LOOKUP);
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            if (g != null) {
                return g.getInvocation();
            }
        }
        if (!TypeUtilities.isMethodInvocationConvertible(sourceType, targetType)) {
            return null;
        }
        // Ninguna fabrica la sabe hacer y el lenguaje si: la conversion es un cambio de tipo sobre
        // la identidad. Armarla necesita manijas, y de ahi sale la excepcion.
        return MethodHandles.identity(sourceType).asType(
                MethodType.methodType(targetType, sourceType));
    }

    /** {@inheritDoc} */
    public boolean canConvert(final Class<?> from, final Class<?> to) {
        if (TypeUtilities.isMethodInvocationConvertible(from, to)) {
            return true;
        }
        for (int i = 0; i < this.fabricas.size(); i++) {
            try {
                if (this.fabricas.get(i).convertToType(from, to, LOOKUP) != null) {
                    return true;
                }
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest) throws Exception {
        return this.enlazador.getGuardedInvocation(linkRequest, this);
    }

    /** {@inheritDoc} */
    public ConversionComparator.Comparison compareConversion(final Class<?> sourceType,
            final Class<?> targetType1, final Class<?> targetType2) {
        for (int i = 0; i < this.comparadores.size(); i++) {
            final ConversionComparator.Comparison c =
                    this.comparadores.get(i).compareConversion(sourceType, targetType1,
                            targetType2);
            if (c != ConversionComparator.Comparison.INDETERMINATE) {
                return c;
            }
        }
        return ConversionComparator.Comparison.INDETERMINATE;
    }

    /** {@inheritDoc} */
    public <T> T getWithLookup(final java.util.function.Supplier<T> operation,
            final SecureLookupSupplier lookupSupplier) {
        // El JDK instala el lookup en un lugar del hilo del que `SecureLookupSupplier.getLookup()`
        // lo saca mientras dure la accion, y lo quita al salir. Aca no hay nada que instalar: no
        // existe un lookup que instalar --ver `MethodHandles`-- ni un sitio de donde venga uno. La
        // accion se corre igual, que es lo que el JDK hace cuando el portador es nulo.
        java.util.Objects.requireNonNull(operation);
        java.util.Objects.requireNonNull(lookupSupplier);
        return operation.get();
    }

    /** {@inheritDoc} */
    public MethodHandle filterInternalObjects(final MethodHandle target) {
        return this.filtroInterno == null ? target : this.filtroInterno.transform(target);
    }

    /**
     * El lookup que se les pasa a las fabricas de conversion.
     *
     * <p>El JDK entrega el del sitio que se esta enlazando, para que una conversion pueda alcanzar
     * miembros que solo ese sitio ve. Aca no hay sitio --nada llega a enlazarse-- y ademas
     * {@code MethodHandles.lookup()} no existe en esta maquina virtual, asi que lo que se entrega
     * es un proveedor que tira cuando se lo consulta. Una fabrica que no necesite el lookup no lo
     * nota, que es el caso de casi todas.
     */
    private static final java.util.function.Supplier<MethodHandles.Lookup> LOOKUP =
            new SinLookup();

    /** Ver {@link #LOOKUP}. */
    private static final class SinLookup
            implements java.util.function.Supplier<MethodHandles.Lookup> {

        public MethodHandles.Lookup get() {
            return MethodHandles.lookup();
        }
    }
}
