package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.AttributeSetUtilities -- las tres verificaciones del paquete
 * y las diez fabricas de vistas.
 *
 * <h2>Por que existe una clase entera de estaticos</h2>
 *
 * <p>Hace dos trabajos que no se parecen, y conviene verlos separados.
 *
 * <p><b>Las verificaciones.</b> {@link #verifyAttributeCategory} y {@link #verifyAttributeValue}
 * son el lugar **unico** donde el paquete decide si algo es una categoria valida o un valor
 * valido. {@link HashAttributeSet} las llama en cada operacion en vez de escribir el chequeo, y por
 * eso las cuatro subclases restringidas no necesitan redefinir ni un metodo: les alcanza con pasar
 * su interfaz al constructor. Toda la restriccion de categoria del paquete pasa por estas dos
 * lineas.
 *
 * <p>Las dos son raras a proposito. {@code verifyAttributeCategory} toma un {@code Object} y no un
 * {@code Class} --hace el downcast ella-- y devuelve el argumento en vez de devolver un booleano,
 * para poder escribir {@code map.get(verify(...))} en una sola expresion. Y ninguna de las dos
 * pone mensaje en la excepcion.
 *
 * <p><b>Las vistas.</b> {@link #unmodifiableView} y {@link #synchronizedView}, cada una en cinco
 * sobrecargas --una por interfaz de conjunto-- porque el tipo estatico del envoltorio tiene que
 * seguir siendo el del envuelto: una vista de solo lectura de un {@code DocAttributeSet} tiene que
 * seguir siendo un {@code DocAttributeSet} o no sirve para pasarla a donde piden uno. Son diez
 * metodos que hacen lo mismo, y las nueve clases internas que los sostienen son un envoltorio y
 * cuatro subclases vacias, dos veces.
 *
 * <h2>El detalle que se ve raro: la vista de solo lectura no restringe categoria</h2>
 *
 * <p>{@code UnmodifiableDocAttributeSet} extiende al envoltorio generico e implementa
 * {@code DocAttributeSet} sin agregar nada. Puede permitirselo porque **toda** modificacion tira
 * {@link UnmodifiableSetException}: no hay por donde entrar un atributo del tipo equivocado. La
 * restriccion la sigue teniendo el conjunto de abajo.
 *
 * <p>La vista sincronizada si delega los {@code add}, y por eso la restriccion tambien funciona:
 * la aplica el conjunto envuelto cuando le llega la llamada.
 *
 * <h2>Lo que quedo afuera</h2>
 *
 * <p>Nada de la superficie publica. Las nueve clases internas son {@code private} y no cuentan; se
 * escribieron igual porque son el cuerpo de los diez metodos.
 */
public final class AttributeSetUtilities {

    // No se instancia: es una caja de estaticos.
    private AttributeSetUtilities() {
    }

    // La vista de solo lectura. Todo lo que consulta delega; todo lo que modifica tira.
    private static class UnmodifiableAttributeSet implements AttributeSet, Serializable {

        private static final long serialVersionUID = -6131802583863447813L;

        private AttributeSet attrset;

        public UnmodifiableAttributeSet(AttributeSet attributeSet) {
            this.attrset = attributeSet;
        }

        public Attribute get(Class<?> key) {
            return this.attrset.get(key);
        }

        public boolean add(Attribute attribute) {
            throw new UnmodifiableSetException();
        }

        // El `synchronized` de este es del JDK y no tiene explicacion: los otros tres
        // modificadores no lo llevan y ninguno de los cuatro toca estado. Se replica igual porque
        // el modificador es observable con reflexion.
        public synchronized boolean remove(Class<?> category) {
            throw new UnmodifiableSetException();
        }

        public boolean remove(Attribute attribute) {
            throw new UnmodifiableSetException();
        }

        public boolean containsKey(Class<?> category) {
            return this.attrset.containsKey(category);
        }

        public boolean containsValue(Attribute attribute) {
            return this.attrset.containsValue(attribute);
        }

        public boolean addAll(AttributeSet attributes) {
            throw new UnmodifiableSetException();
        }

        public int size() {
            return this.attrset.size();
        }

        public Attribute[] toArray() {
            return this.attrset.toArray();
        }

        public void clear() {
            throw new UnmodifiableSetException();
        }

        public boolean isEmpty() {
            return this.attrset.isEmpty();
        }

        // Delega en el envuelto, asi que una vista es igual al conjunto que envuelve. Notar que la
        // relacion no es simetrica en general: `conjunto.equals(vista)` pasa por el equals del
        // conjunto, que compara por la interfaz AttributeSet y tambien da true.
        public boolean equals(Object o) {
            return this.attrset.equals(o);
        }

        public int hashCode() {
            return this.attrset.hashCode();
        }
    }

    // Las cuatro subclases vacias. Solo existen para conservar el tipo estatico; ver la cabecera.
    private static class UnmodifiableDocAttributeSet extends UnmodifiableAttributeSet
            implements DocAttributeSet, Serializable {

        private static final long serialVersionUID = -6349408326066898956L;

        public UnmodifiableDocAttributeSet(DocAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class UnmodifiablePrintRequestAttributeSet extends UnmodifiableAttributeSet
            implements PrintRequestAttributeSet, Serializable {

        private static final long serialVersionUID = 7799373532614825073L;

        public UnmodifiablePrintRequestAttributeSet(PrintRequestAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class UnmodifiablePrintJobAttributeSet extends UnmodifiableAttributeSet
            implements PrintJobAttributeSet, Serializable {

        private static final long serialVersionUID = -8002245296274522112L;

        public UnmodifiablePrintJobAttributeSet(PrintJobAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class UnmodifiablePrintServiceAttributeSet extends UnmodifiableAttributeSet
            implements PrintServiceAttributeSet, Serializable {

        private static final long serialVersionUID = -7112165137107826819L;

        public UnmodifiablePrintServiceAttributeSet(PrintServiceAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /** Vista de solo lectura. NullPointerException si el conjunto es null. */
    public static AttributeSet unmodifiableView(AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableAttributeSet(attributeSet);
    }

    /** Vista de solo lectura que sigue siendo un DocAttributeSet. */
    public static DocAttributeSet unmodifiableView(DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableDocAttributeSet(attributeSet);
    }

    /** Vista de solo lectura que sigue siendo un PrintRequestAttributeSet. */
    public static PrintRequestAttributeSet unmodifiableView(
            PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintRequestAttributeSet(attributeSet);
    }

    /** Vista de solo lectura que sigue siendo un PrintJobAttributeSet. */
    public static PrintJobAttributeSet unmodifiableView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintJobAttributeSet(attributeSet);
    }

    /** Vista de solo lectura que sigue siendo un PrintServiceAttributeSet. */
    public static PrintServiceAttributeSet unmodifiableView(
            PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintServiceAttributeSet(attributeSet);
    }

    // La vista sincronizada. Delega todo, pero con el monitor del envoltorio tomado.
    //
    // Es la sincronizacion mas simple que existe y tiene el agujero de siempre: protege cada
    // llamada, no una secuencia. Un `if (!set.containsKey(c)) set.add(a)` sobre una vista
    // sincronizada sigue teniendo carrera, porque son dos llamadas. Para eso hay que tomar el
    // monitor de la vista desde afuera.
    private static class SynchronizedAttributeSet implements AttributeSet, Serializable {

        private static final long serialVersionUID = 8365731020128564925L;

        private AttributeSet attrset;

        public SynchronizedAttributeSet(AttributeSet attributeSet) {
            this.attrset = attributeSet;
        }

        public synchronized Attribute get(Class<?> category) {
            return this.attrset.get(category);
        }

        public synchronized boolean add(Attribute attribute) {
            return this.attrset.add(attribute);
        }

        public synchronized boolean remove(Class<?> category) {
            return this.attrset.remove(category);
        }

        public synchronized boolean remove(Attribute attribute) {
            return this.attrset.remove(attribute);
        }

        public synchronized boolean containsKey(Class<?> category) {
            return this.attrset.containsKey(category);
        }

        public synchronized boolean containsValue(Attribute attribute) {
            return this.attrset.containsValue(attribute);
        }

        public synchronized boolean addAll(AttributeSet attributes) {
            return this.attrset.addAll(attributes);
        }

        public synchronized int size() {
            return this.attrset.size();
        }

        public synchronized Attribute[] toArray() {
            return this.attrset.toArray();
        }

        public synchronized void clear() {
            this.attrset.clear();
        }

        public synchronized boolean isEmpty() {
            return this.attrset.isEmpty();
        }

        public synchronized boolean equals(Object o) {
            return this.attrset.equals(o);
        }

        public synchronized int hashCode() {
            return this.attrset.hashCode();
        }
    }

    private static class SynchronizedDocAttributeSet extends SynchronizedAttributeSet
            implements DocAttributeSet, Serializable {

        private static final long serialVersionUID = 6455869095246629354L;

        public SynchronizedDocAttributeSet(DocAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class SynchronizedPrintRequestAttributeSet extends SynchronizedAttributeSet
            implements PrintRequestAttributeSet, Serializable {

        private static final long serialVersionUID = 5671237023971169027L;

        public SynchronizedPrintRequestAttributeSet(PrintRequestAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class SynchronizedPrintJobAttributeSet extends SynchronizedAttributeSet
            implements PrintJobAttributeSet, Serializable {

        private static final long serialVersionUID = 2117188707856965749L;

        public SynchronizedPrintJobAttributeSet(PrintJobAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    private static class SynchronizedPrintServiceAttributeSet extends SynchronizedAttributeSet
            implements PrintServiceAttributeSet, Serializable {

        private static final long serialVersionUID = -2830705374001675073L;

        public SynchronizedPrintServiceAttributeSet(PrintServiceAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /** Vista sincronizada. NullPointerException si el conjunto es null. */
    public static AttributeSet synchronizedView(AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedAttributeSet(attributeSet);
    }

    /** Vista sincronizada que sigue siendo un DocAttributeSet. */
    public static DocAttributeSet synchronizedView(DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedDocAttributeSet(attributeSet);
    }

    /** Vista sincronizada que sigue siendo un PrintRequestAttributeSet. */
    public static PrintRequestAttributeSet synchronizedView(
            PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintRequestAttributeSet(attributeSet);
    }

    /** Vista sincronizada que sigue siendo un PrintJobAttributeSet. */
    public static PrintJobAttributeSet synchronizedView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintJobAttributeSet(attributeSet);
    }

    /** Vista sincronizada que sigue siendo un PrintServiceAttributeSet. */
    public static PrintServiceAttributeSet synchronizedView(
            PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintServiceAttributeSet(attributeSet);
    }

    /**
     * Que `object` sea un {@code Class} que implementa `interfaceName`, y devolverlo ya casteado.
     *
     * <p>Dos excepciones distintas por dos motivos distintos: {@code ClassCastException} si no es
     * un {@code Class} --el cast de la primera linea-- y tambien si es un {@code Class} pero no
     * implementa la interfaz. {@code NullPointerException} si es null, que sale de llamarle
     * {@code isAssignableFrom} a la interfaz con null.
     */
    public static Class<?> verifyAttributeCategory(Object object, Class<?> interfaceName) {
        Class<?> result = (Class<?>) object;
        // El JDK no escribe este chequeo: le llega el null a `isAssignableFrom` y la
        // NullPointerException sale de ahi. Aca hay que escribirlo porque en esta VM
        // `Class.isAssignableFrom(null)` **voltea el proceso** en vez de tirar --repro minimo:
        // `Object.class.isAssignableFrom(null)`, panic en src/jvm/interpreter/natives.rs "Class: no
        // hay ninguna clase en este mirror"--, y sin el guardia `attributeSet.get(null)` mataria la
        // VM en vez de tirar. El comportamiento observable queda identico al del JDK: misma
        // excepcion, mismo punto. `isInstance(null)` no tiene el problema y devuelve false bien,
        // por eso verifyAttributeValue no lleva nada parecido.
        if (result == null) {
            throw new NullPointerException();
        }
        if (interfaceName.isAssignableFrom(result)) {
            return result;
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * Que `object` sea una instancia de `interfaceName`, y devolverlo ya casteado a
     * {@link Attribute}.
     *
     * <p>Esta si chequea null explicitamente, a diferencia de {@link #verifyAttributeCategory}.
     */
    public static Attribute verifyAttributeValue(Object object, Class<?> interfaceName) {
        if (object == null) {
            throw new NullPointerException();
        } else if (interfaceName.isInstance(object)) {
            return (Attribute) object;
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * Que la categoria dada sea **exactamente** la del atributo dado; si no,
     * {@code IllegalArgumentException}.
     *
     * <p>No la usa nadie de este paquete: es para las tablas de valores soportados de
     * {@code javax.print}, donde una entrada mapea una categoria a los valores permitidos y hay
     * que verificar que el valor conteste la pregunta que dice contestar.
     */
    public static void verifyCategoryForValue(Class<?> category, Attribute attribute) {
        if (!category.equals(attribute.getCategory())) {
            throw new IllegalArgumentException();
        }
    }
}
