package jdk.dynalink;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Todo lo que se sabe de un sitio de invocacion dinamico en tiempo de enlace: quien lo escribio
 * (el {@link Lookup}), que quiere hacer (la {@link Operation}) y con que firma
 * (el {@link MethodType}).
 *
 * <p>Es un **tipo de valor**: inmutable, comparable, y con "modificadores" que devuelven una
 * instancia nueva. El par publico/protegido de cada modificador no es duplicacion: el `final`
 * publico ({@link #changeMethodType}) contiene las invariantes, y el `protected`
 * ({@link #changeMethodTypeInternal}) es el que una subclase redefine para preservar sus propios
 * campos. La subclase no puede saltearse el chequeo, y el chequeo no puede impedirle extender.
 *
 * <p>Las invariantes solo se verifican cuando `getClass() != CallSiteDescriptor.class`, es decir,
 * solo contra subclases: la implementacion base las cumple por construccion y pagar el costo en
 * el camino comun no tendria sentido.
 *
 * @since 9
 */
public class CallSiteDescriptor extends SecureLookupSupplier {

    private final Operation operation;
    private final MethodType methodType;

    public CallSiteDescriptor(final Lookup lookup, final Operation operation, final MethodType methodType) {
        super(lookup);
        this.operation = Objects.requireNonNull(operation, "name");
        this.methodType = Objects.requireNonNull(methodType, "methodType");
    }

    public final Operation getOperation() {
        return operation;
    }

    public final MethodType getMethodType() {
        return methodType;
    }

    /**
     * El mismo descriptor con otra firma.
     *
     * @throws AssertionError si una subclase redefinio {@link #changeMethodTypeInternal} de
     *         forma que cambie la clase, el lookup o la operacion.
     */
    public final CallSiteDescriptor changeMethodType(final MethodType newMethodType) {
        final CallSiteDescriptor changed = changeMethodTypeInternal(newMethodType);
        if (getClass() != CallSiteDescriptor.class) {
            assertChangeInvariants(changed, "changeMethodTypeInternal");
            alwaysAssert(operation == changed.operation,
                    () -> "changeMethodTypeInternal must not change the descriptor's operation");
            alwaysAssert(newMethodType == changed.methodType,
                    () -> "changeMethodTypeInternal didn't set the correct new method type");
        }
        return changed;
    }

    /** El punto de extension de {@link #changeMethodType}; una subclase copia aca sus campos. */
    protected CallSiteDescriptor changeMethodTypeInternal(final MethodType newMethodType) {
        return new CallSiteDescriptor(getLookupPrivileged(), operation, newMethodType);
    }

    /**
     * El mismo descriptor con otra operacion.
     *
     * @throws AssertionError si una subclase redefinio {@link #changeOperationInternal} de forma
     *         que cambie la clase, el lookup o la firma.
     */
    public final CallSiteDescriptor changeOperation(final Operation newOperation) {
        getLookup();
        final CallSiteDescriptor changed = changeOperationInternal(newOperation);
        if (getClass() != CallSiteDescriptor.class) {
            assertChangeInvariants(changed, "changeOperationInternal");
            alwaysAssert(methodType == changed.methodType,
                    () -> "changeOperationInternal must not change the descriptor's method type");
            alwaysAssert(newOperation == changed.operation,
                    () -> "changeOperationInternal didn't set the correct new operation");
        }
        return changed;
    }

    /** El punto de extension de {@link #changeOperation}. */
    protected CallSiteDescriptor changeOperationInternal(final Operation newOperation) {
        return new CallSiteDescriptor(getLookupPrivileged(), newOperation, methodType);
    }

    /**
     * Igualdad por valor, con la clase exacta como parte del contrato: un descriptor de una
     * subclase nunca es igual a uno base, porque la subclase puede llevar estado propio.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj.getClass() != getClass()) {
            return false;
        }
        final CallSiteDescriptor other = (CallSiteDescriptor) obj;
        return operation.equals(other.operation)
                && methodType.equals(other.methodType)
                && lookupsEqual(getLookupPrivileged(), other.getLookupPrivileged());
    }

    // Dos lookups son el mismo si dan el mismo acceso desde la misma clase; `Lookup` no define
    // `equals`, asi que la comparacion tiene que ser explicita.
    private static boolean lookupsEqual(final Lookup l1, final Lookup l2) {
        return l1.lookupClass() == l2.lookupClass() && l1.lookupModes() == l2.lookupModes();
    }

    @Override
    public int hashCode() {
        return operation.hashCode() + 31 * methodType.hashCode()
                + 31 * 31 * lookupHashCode(getLookupPrivileged());
    }

    private static int lookupHashCode(final Lookup lookup) {
        return lookup.lookupClass().hashCode() + 31 * lookup.lookupModes();
    }

    @Override
    public String toString() {
        final String mt = methodType.toString();
        final String l = getLookupPrivileged().toString();
        final String o = operation.toString();
        return o + mt + '@' + l;
    }

    private void assertChangeInvariants(final CallSiteDescriptor changed, final String caller) {
        alwaysAssert(changed != null, () -> caller + " must not return null.");
        alwaysAssert(getClass() == changed.getClass(),
                () -> caller + " must not change the descriptor's class");
        alwaysAssert(lookupsEqual(getLookupPrivileged(), changed.getLookupPrivileged()),
                () -> caller + " must not change the descriptor's lookup");
    }

    // `assert` de verdad, no el del `-ea`: estas invariantes protegen a un enlazador de una
    // subclase ajena mal escrita, y desactivarlas convertiria el error en corrupcion silenciosa.
    private static void alwaysAssert(final boolean cond, final Supplier<String> errorMessage) {
        if (!cond) {
            throw new AssertionError(errorMessage.get());
        }
    }
}
