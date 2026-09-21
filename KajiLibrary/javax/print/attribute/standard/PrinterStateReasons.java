package javax.print.attribute.standard;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;

/**
 * The conditions the printer has on it, each one with its severity.
 *
 * <p>It is a map from {@link PrinterStateReason} to {@link Severity} and not a set of reasons,
 * because the same condition does not always weigh the same: {@code MEDIA_LOW} is a {@code WARNING}
 * when ten sheets are left and an {@code ERROR} when none is. The severity is decided by the
 * printer and that is why it travels attached to the reason.
 *
 * <p>Like {@link JobStateReasons}, the attribute <b>is</b> the collection --it extends {@link
 * HashMap}-- and therefore it is mutable, unlike the rest of the package.
 *
 * <p>The view by severity ({@link #printerStateReasonSet}) is the only thing with real logic here,
 * and it is a <b>live view</b>, not a copy: it does not walk the map when built but filters while
 * iterating, so it reflects later changes of the map. It is read-only --it inherits from {@link
 * AbstractSet} the {@code add} that throws {@code UnsupportedOperationException}-- because adding a
 * reason to the errors view would have nowhere to keep the severity.
 *
 * <p>That it is lazy has a cost worth knowing: {@code size()} is not O(1) but walks the whole map
 * counting the ones that match.
 */
public final class PrinterStateReasons extends HashMap<PrinterStateReason, Severity>
    implements PrintServiceAttribute {

    private static final long serialVersionUID = -3731791085163619457L;

    public PrinterStateReasons() {
        super();
    }

    public PrinterStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public PrinterStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Copies another map entry by entry --and not with {@link HashMap}'s constructor-- so that each
     * pair goes through {@link #put} and the nulls are rejected.
     */
    public PrinterStateReasons(Map<PrinterStateReason, Severity> map) {
        this();
        for (Map.Entry<PrinterStateReason, Severity> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public Severity put(PrinterStateReason reason, Severity severity) {
        if (reason == null) {
            throw new NullPointerException("reason is null");
        }
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return super.put(reason, severity);
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterStateReasons.class;
    }

    public final String getName() {
        return "printer-state-reasons";
    }

    /** The reasons that have exactly that severity, as a live, read-only view. */
    public Set<PrinterStateReason> printerStateReasonSet(Severity severity) {
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return new PrinterStateReasonSet(severity, this);
    }

    // The view. It keeps no elements: it keeps the criterion and the outer map.
    //
    // It keeps the map and not its entrySet --which is what the JDK does-- so that the view stays
    // live over KajiLibrary's HashMap, whose entrySet() returns a copy and not a view. Asking for
    // it again in each iterator() the result is the same as in the JDK, where the two forms
    // coincide because there the entrySet is a view.
    //
    // Declared `static` and not inner because of the compiler's finding #440: inside an inner class
    // the outer instance argument is not synthesized (it still reproduces with the frozen javac,
    // 2026-09-18). It makes no difference, because they receive the map through the constructor.
    private static class PrinterStateReasonSet extends AbstractSet<PrinterStateReason> {

        private Severity mySeverity;
        private Map<PrinterStateReason, Severity> myMap;

        PrinterStateReasonSet(Severity severity, Map<PrinterStateReason, Severity> map) {
            this.mySeverity = severity;
            this.myMap = map;
        }

        // Counting costs walking, because the filter is not materialized.
        public int size() {
            int result = 0;
            Iterator<PrinterStateReason> iter = iterator();
            while (iter.hasNext()) {
                iter.next();
                ++result;
            }
            return result;
        }

        public Iterator<PrinterStateReason> iterator() {
            return new PrinterStateReasonSetIterator(this.mySeverity,
                                                     this.myMap.entrySet().iterator());
        }
    }

    // The filter. It keeps the next matching entry ready ahead, which is what lets hasNext() answer
    // without consuming anything.
    private static class PrinterStateReasonSetIterator implements Iterator<PrinterStateReason> {

        private Severity mySeverity;
        private Iterator<Map.Entry<PrinterStateReason, Severity>> myIterator;
        private Map.Entry<PrinterStateReason, Severity> myEntry;

        PrinterStateReasonSetIterator(
                Severity severity,
                Iterator<Map.Entry<PrinterStateReason, Severity>> iterator) {
            this.mySeverity = severity;
            this.myIterator = iterator;
            goToNext();
        }

        // The severity is compared by identity and not with equals: they are EnumSyntax singletons.
        private void goToNext() {
            this.myEntry = null;
            while (this.myEntry == null && this.myIterator.hasNext()) {
                this.myEntry = this.myIterator.next();
                if (this.myEntry.getValue() != this.mySeverity) {
                    this.myEntry = null;
                }
            }
        }

        public boolean hasNext() {
            return this.myEntry != null;
        }

        public PrinterStateReason next() {
            if (this.myEntry == null) {
                throw new NoSuchElementException();
            }
            PrinterStateReason result = this.myEntry.getKey();
            goToNext();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
