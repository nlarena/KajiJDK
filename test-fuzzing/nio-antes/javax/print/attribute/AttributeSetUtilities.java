package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.AttributeSetUtilities -- the package's three checks and the
 * ten view factories.
 *
 * <h2>Why a whole class of statics exists</h2>
 *
 * <p>It does two jobs that do not look alike, and it is as well to see them separately.
 *
 * <p><b>The checks.</b> {@link #verifyAttributeCategory} and {@link #verifyAttributeValue} are the
 * **single** place where the package decides whether something is a valid category or a valid
 * value. {@link HashAttributeSet} calls them on each operation instead of writing the check, and
 * that is why the four restricted subclasses do not need to override a single method: passing their
 * interface to the constructor is enough. All of the package's category restriction goes through
 * these two lines.
 *
 * <p>Both are odd on purpose. {@code verifyAttributeCategory} takes an {@code Object} and not a
 * {@code Class} --it does the downcast itself-- and returns the argument instead of a boolean, so
 * that one can write {@code map.get(verify(...))} in a single expression. And neither puts a
 * message in the exception.
 *
 * <p><b>The views.</b> {@link #unmodifiableView} and {@link #synchronizedView}, each in five
 * overloads --one per set interface-- because the wrapper's static type has to stay that of the
 * wrapped one: a read-only view of a {@code DocAttributeSet} has to remain a {@code
 * DocAttributeSet} or it cannot be passed where one is asked for. They are ten methods that do the
 * same, and the ten inner classes supporting them are a wrapper and four empty subclasses, twice.
 * (The note said nine.)
 *
 * <h2>The detail that looks odd: the read-only view does not restrict category</h2>
 *
 * <p>{@code UnmodifiableDocAttributeSet} extends the generic wrapper and implements {@code
 * DocAttributeSet} without adding anything. It can afford that because **every** modification
 * throws {@link UnmodifiableSetException}: there is no way in for an attribute of the wrong type.
 * The set underneath still has the restriction.
 *
 * <p>The synchronized view does delegate the {@code add}s, and that is why the restriction works
 * too: the wrapped set applies it when the call reaches it.
 *
 * <h2>What was left out</h2>
 *
 * <p>Nothing of the public surface. The inner classes are {@code private} and do not count; they
 * were written all the same because they are the body of the ten methods.
 */
public final class AttributeSetUtilities {

    // It is not instantiated: it is a box of statics.
    private AttributeSetUtilities() {
    }

    // The read-only view. Everything that queries delegates; everything that modifies throws.
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

        // The `synchronized` on this one is the JDK's and has no explanation: the other three
        // modifiers do not carry it and none of the four touches state. It is replicated all the
        // same because the modifier is observable through reflection.
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

        // It delegates to the wrapped one, so a view is equal to the set it wraps. Note that the
        // relation is not symmetric in general: `set.equals(view)` goes through the set's equals,
        // which compares through the AttributeSet interface and also gives true.
        public boolean equals(Object o) {
            return this.attrset.equals(o);
        }

        public int hashCode() {
            return this.attrset.hashCode();
        }
    }

    // The four empty subclasses. They only exist to keep the static type; see the header.
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

    /** Read-only view. NullPointerException if the set is null. */
    public static AttributeSet unmodifiableView(AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableAttributeSet(attributeSet);
    }

    /** Read-only view that is still a DocAttributeSet. */
    public static DocAttributeSet unmodifiableView(DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableDocAttributeSet(attributeSet);
    }

    /** Read-only view that is still a PrintRequestAttributeSet. */
    public static PrintRequestAttributeSet unmodifiableView(
            PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintRequestAttributeSet(attributeSet);
    }

    /** Read-only view that is still a PrintJobAttributeSet. */
    public static PrintJobAttributeSet unmodifiableView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintJobAttributeSet(attributeSet);
    }

    /** Read-only view that is still a PrintServiceAttributeSet. */
    public static PrintServiceAttributeSet unmodifiableView(
            PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintServiceAttributeSet(attributeSet);
    }

    // The synchronized view. It delegates everything, but with the wrapper's monitor held.
    //
    // It is the simplest synchronization there is and it has the usual hole: it protects each call,
    // not a sequence. An `if (!set.containsKey(c)) set.add(a)` on a synchronized view still has a
    // race, because they are two calls. For that the view's monitor has to be taken from outside.
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

    /** Synchronized view. NullPointerException if the set is null. */
    public static AttributeSet synchronizedView(AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedAttributeSet(attributeSet);
    }

    /** Synchronized view that is still a DocAttributeSet. */
    public static DocAttributeSet synchronizedView(DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedDocAttributeSet(attributeSet);
    }

    /** Synchronized view that is still a PrintRequestAttributeSet. */
    public static PrintRequestAttributeSet synchronizedView(
            PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintRequestAttributeSet(attributeSet);
    }

    /** Synchronized view that is still a PrintJobAttributeSet. */
    public static PrintJobAttributeSet synchronizedView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintJobAttributeSet(attributeSet);
    }

    /** Synchronized view that is still a PrintServiceAttributeSet. */
    public static PrintServiceAttributeSet synchronizedView(
            PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintServiceAttributeSet(attributeSet);
    }

    /**
     * That `object` is a {@code Class} implementing `interfaceName`, and returning it already cast.
     *
     * <p>Two different exceptions for two different reasons: {@code ClassCastException} if it is
     * not a {@code Class} --the cast on the first line-- and also if it is a {@code Class} but does
     * not implement the interface. {@code NullPointerException} if it is null, which comes from
     * calling {@code isAssignableFrom} on the interface with null.
     */
    public static Class<?> verifyAttributeCategory(Object object, Class<?> interfaceName) {
        Class<?> result = (Class<?>) object;
        // The JDK does not write this check: the null reaches `isAssignableFrom` and the
        // NullPointerException comes from there. Here it has to be written because on this VM
        // `Class.isAssignableFrom(null)` **brings the process down** instead of throwing --minimal
        // repro: `Object.class.isAssignableFrom(null)`, a panic in src/jvm/interpreter/natives.rs
        // "Class: no hay ninguna clase en este mirror" (still so on 2026-09-18)--, and without the
        // guard `attributeSet.get(null)` would kill the VM instead of throwing. The observable
        // behaviour stays identical to the JDK's: same exception, same point. `isInstance(null)`
        // does not have the problem and returns false properly, which is why verifyAttributeValue
        // carries nothing similar.
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
     * That `object` is an instance of `interfaceName`, and returning it already cast to
     * {@link Attribute}.
     *
     * <p>This one does check null explicitly, unlike {@link #verifyAttributeCategory}.
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
     * That the given category is **exactly** the given attribute's; otherwise
     * {@code IllegalArgumentException}.
     *
     * <p>Nobody in this package uses it: it is for the supported-value tables of {@code
     * javax.print}, where an entry maps a category to the permitted values and one has to check
     * that the value answers the question it claims to answer.
     */
    public static void verifyCategoryForValue(Class<?> category, Attribute attribute) {
        if (!category.equals(attribute.getCategory())) {
            throw new IllegalArgumentException();
        }
    }
}
