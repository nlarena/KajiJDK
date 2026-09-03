package javax.management.openmbean;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// Las restricciones de un parametro abierto --valor por omision, valores legales, minimo y maximo--
// con su validacion y su comportamiento.
//
// Existe porque `OpenMBeanParameterInfoSupport` y `OpenMBeanAttributeInfoSupport` necesitan
// exactamente lo mismo y **no pueden compartir una superclase**: cada uno hereda de su
// `MBeanXxxInfo` de `javax.management`, y Java no tiene herencia multiple. Delegar en un objeto es
// la unica forma de que la regla viva escrita una sola vez.
//
// De paquete a proposito: es un detalle de como estan implementados esos dos, no parte del contrato.
final class Constraints {

    private final OpenType<?> openType;
    private final Object defaultValue;
    private final Set<?> legalValues;
    private final Comparable<?> minValue;
    private final Comparable<?> maxValue;

    Constraints(OpenType<?> openType, Object defaultValue, Object[] legalValues,
            Comparable<?> minValue, Comparable<?> maxValue) throws OpenDataException {
        if (openType == null) {
            throw new IllegalArgumentException("el tipo abierto no puede ser nulo");
        }
        this.openType = openType;

        // Las dos formas de restringir se excluyen: una lista de valores legales ya dice cuales
        // valen, y un rango sobre esa lista o es redundante o la contradice. Aceptar las dos
        // obligaria a decidir cual gana, y cualquier eleccion sorprenderia a alguien.
        boolean hasLegalList = legalValues != null && legalValues.length > 0;
        boolean hasRange = minValue != null || maxValue != null;
        if (hasLegalList && hasRange) {
            throw new OpenDataException(
                    "no se pueden dar valores legales y un rango a la vez");
        }
        // Un `ArrayType` y un `TabularType` no admiten NINGUNA de las cuatro restricciones. Un
        // `CompositeType` si las admite, y ese reparto sorprende: uno esperaria que el compuesto
        // fuera el mas restringido de los tres. Esta comprobado contra el JDK 25 --la primera
        // version de esta clase lo tenia justo al reves-- y la razon es que un valor compuesto es
        // un valor con identidad por contenido, mientras que un arreglo o una tabla no se comparan
        // de forma util con `equals`.
        if (openType instanceof ArrayType || openType instanceof TabularType) {
            if (hasLegalList || hasRange || defaultValue != null) {
                throw new OpenDataException(
                        "un " + openType.getClass().getSimpleName()
                                + " no admite valor por omision ni restricciones");
            }
        }

        if (defaultValue != null && !openType.isValue(defaultValue)) {
            throw new OpenDataException("el valor por omision no es de tipo "
                    + openType.getTypeName());
        }

        Set<Object> ls = null;
        if (hasLegalList) {
            // `LinkedHashSet` y no `HashSet`: `getLegalValues` se imprime en `toString` y una
            // salida que cambia de orden entre corridas es un dolor para comparar contra el JDK.
            ls = new LinkedHashSet<Object>();
            for (int i = 0; i < legalValues.length; i++) {
                Object v = legalValues[i];
                if (v == null) {
                    throw new OpenDataException("un valor legal es nulo");
                }
                if (!openType.isValue(v)) {
                    throw new OpenDataException("el valor legal " + v + " no es de tipo "
                            + openType.getTypeName());
                }
                ls.add(v);
            }
            if (defaultValue != null && !ls.contains(defaultValue)) {
                throw new OpenDataException(
                        "el valor por omision no esta entre los valores legales");
            }
        }

        if (minValue != null && !openType.isValue(minValue)) {
            throw new OpenDataException("el minimo no es de tipo " + openType.getTypeName());
        }
        if (maxValue != null && !openType.isValue(maxValue)) {
            throw new OpenDataException("el maximo no es de tipo " + openType.getTypeName());
        }
        if (minValue != null && maxValue != null && compare(minValue, maxValue) > 0) {
            throw new OpenDataException("el minimo es mayor que el maximo");
        }
        if (defaultValue != null && minValue != null
                && compare(minValue, defaultValue) > 0) {
            throw new OpenDataException("el valor por omision es menor que el minimo");
        }
        if (defaultValue != null && maxValue != null
                && compare(maxValue, defaultValue) < 0) {
            throw new OpenDataException("el valor por omision es mayor que el maximo");
        }

        this.defaultValue = defaultValue;
        this.legalValues = ls == null ? null : Collections.unmodifiableSet(ls);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // El `unchecked` esta acotado a este metodo: los dos valores ya pasaron por `isValue` del mismo
    // tipo abierto, asi que son de la misma clase y esa clase es comparable -- todos los tipos
    // simples lo son. El comodin de `Comparable<?>` es lo que impide decirlo sin el cast.
    @SuppressWarnings("unchecked")
    static int compare(Object a, Object b) {
        Comparable<Object> c = (Comparable<Object>) a;
        return c.compareTo(b);
    }

    OpenType<?> getOpenType() {
        return this.openType;
    }

    Object getDefaultValue() {
        return this.defaultValue;
    }

    Set<?> getLegalValues() {
        return this.legalValues;
    }

    Comparable<?> getMinValue() {
        return this.minValue;
    }

    Comparable<?> getMaxValue() {
        return this.maxValue;
    }

    boolean hasDefaultValue() {
        return this.defaultValue != null;
    }

    boolean hasLegalValues() {
        return this.legalValues != null;
    }

    boolean hasMinValue() {
        return this.minValue != null;
    }

    boolean hasMaxValue() {
        return this.maxValue != null;
    }

    /** Si el valor es del tipo abierto **y** cumple las restricciones. */
    boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!this.openType.isValue(obj)) {
            return false;
        }
        if (this.legalValues != null && !this.legalValues.contains(obj)) {
            return false;
        }
        if (this.minValue != null && compare(this.minValue, obj) > 0) {
            return false;
        }
        if (this.maxValue != null && compare(this.maxValue, obj) < 0) {
            return false;
        }
        return true;
    }

    /** La igualdad que los dos `Support` comparten: tipo, omision, legales, minimo y maximo. */
    boolean sameAs(Constraints other) {
        return sameValue(this.openType, other.openType)
                && sameValue(this.defaultValue, other.defaultValue)
                && sameValue(this.legalValues, other.legalValues)
                && sameValue(this.minValue, other.minValue)
                && sameValue(this.maxValue, other.maxValue);
    }

    static boolean sameValue(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    static int hash(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    int partialHash() {
        return hash(this.openType) + hash(this.defaultValue) + hash(this.legalValues)
                + hash(this.minValue) + hash(this.maxValue);
    }

    /** El tramo de `toString` que describe las restricciones. */
    void describe(StringBuilder sb) {
        sb.append(",openType=").append(this.openType.toString());
        // El orden es el del JDK 25 --omision, minimo, maximo, legales--, comprobado contra su
        // salida. Un `toString` es texto para una persona y nadie deberia parsearlo, pero coincidir
        // con el original hace que comparar las dos corridas sea leer una diferencia y no traducir.
        sb.append(",default=").append(String.valueOf(this.defaultValue));
        sb.append(",minValue=").append(String.valueOf(this.minValue));
        sb.append(",maxValue=").append(String.valueOf(this.maxValue));
        sb.append(",legalValues=").append(String.valueOf(this.legalValues));
    }
}
