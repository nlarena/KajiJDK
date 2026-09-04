package jdk.dynalink;

import java.util.Objects;

/**
 * Una {@link Operation} con el nombre del miembro ya fijado en tiempo de enlace.
 *
 * <p>La distincion que importa: `GET:PROPERTY` toma el nombre como argumento en tiempo de
 * ejecucion, mientras que `GET:PROPERTY:x` lo lleva adentro. Eso permite que el enlazador
 * resuelva el miembro **una vez** y deje una invocacion directa, en lugar de una busqueda por
 * llamada.
 *
 * <p>El nombre es un `Object` y no un `String` porque hay lenguajes con claves que no son
 * texto (simbolos, enteros). Solo se le exige `equals`/`hashCode`.
 *
 * @since 9
 */
public final class NamedOperation implements Operation {

    private final Operation baseOperation;
    private final Object name;

    /** @throws IllegalArgumentException si la base ya es un `NamedOperation`. */
    public NamedOperation(final Operation baseOperation, final Object name) {
        if (baseOperation instanceof NamedOperation) {
            throw new IllegalArgumentException("baseOperation is a NamedOperation");
        }
        this.baseOperation = Objects.requireNonNull(baseOperation, "baseOperation is null");
        this.name = Objects.requireNonNull(name, "name is null");
    }

    public Operation getBaseOperation() {
        return baseOperation;
    }

    public Object getName() {
        return name;
    }

    public final NamedOperation changeName(final String newName) {
        return new NamedOperation(baseOperation, newName);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NamedOperation) {
            final NamedOperation other = (NamedOperation) obj;
            return baseOperation.equals(other.baseOperation) && name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return baseOperation.hashCode() + 31 * name.hashCode();
    }

    @Override
    public String toString() {
        return baseOperation.toString() + ":" + name.toString();
    }

    /** La base de `op` si tiene nombre; `op` misma si no. */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation) op).baseOperation : op;
    }

    /**
     * El nombre de `op`, o `null` si `op` no es una operacion con nombre.
     *
     * <p>El `null` es la respuesta correcta y no un valor inventado: "esta operacion no lleva
     * nombre" es exactamente lo que el llamador pregunta, y asi lo especifica el JDK.
     */
    public static Object getName(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation) op).name : null;
    }
}
