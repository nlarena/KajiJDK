package javax.swing;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * It decides which rows are seen and which are not.
 *
 * <h2>Filtering is not deleting</h2>
 *
 * <p>A row the filter leaves out is still in the model: the only thing that changes is that the
 * view does not show it. That is why the filter is given to the row sorter and not to the
 * model, and that is why the view and model indices stop agreeing as soon as there is a filter
 * set.
 *
 * <h2>The entry, not the row</h2>
 *
 * <p>{@link #include} does not receive a row but an {@link Entry}, which is a read-only view of
 * a row: its values, how many there are, and the identifier the model recognizes it by. That
 * way the same filter serves for a table and for a tree, which keep their rows in different
 * ways.
 *
 * <h2>The extra columns</h2>
 *
 * <p>The factories receive an {@code int...} of columns. <strong>With none, they look at them
 * all</strong>, and it is enough for one to match. It is the opposite of what one expects of an
 * empty list, and it is the convenient thing: a table searcher is written with a single call.
 */
public abstract class RowFilter<M, I> {

    /** For the subclasses. */
    protected RowFilter() {
    }

    /**
     * How to compare against the reference value.
     *
     * <p>{@link #BEFORE} and {@link #AFTER} are called that after the dates, but they hold just as
     * well for numbers: they are "less than" and "greater than".
     */
    public enum ComparisonType {

        /** Before, or less than. */
        BEFORE,

        /** After, or greater than. */
        AFTER,

        /** Equal. */
        EQUAL,

        /** Different. */
        NOT_EQUAL;
    }

    /**
     * It lets through the rows where the regular expression finds something.
     *
     * <p>It is {@code find}, not {@code matches}: it is enough for the expression to appear
     * somewhere in the text, it does not have to cover it whole. Hence {@code "ar"} serves to
     * search for "Argentina" with no wildcards.
     *
     * <p>It is not checked that the expression is not null: it is passed to {@code
     * Pattern.compile}, which blows up by itself. It is what the JDK does and the message comes
     * from there.
     *
     * @throws NullPointerException if the expression is null
     * @throws java.util.regex.PatternSyntaxException if it does not compile
     * @throws IllegalArgumentException if some column is negative
     */
    public static <M, I> RowFilter<M, I> regexFilter(String regex, int... indices) {
        return new RegexFilter<M, I>(Pattern.compile(regex), indices);
    }

    /**
     * It lets through the rows whose date compares like that against the reference one.
     *
     * <p>The date is read in the factory, before reaching the filter, so a null date comes out as
     * {@link NullPointerException} and a null type as {@link IllegalArgumentException}. The
     * asymmetry is the JDK's and it is measured.
     *
     * @throws NullPointerException if the date is null
     * @throws IllegalArgumentException if the type is null or some column is negative
     */
    public static <M, I> RowFilter<M, I> dateFilter(ComparisonType type, Date date,
            int... indices) {
        return new DateFilter<M, I>(type, date.getTime(), indices);
    }

    /**
     * It lets through the rows whose number compares like that against the reference one.
     *
     * <p>The comparison is by value and not by type: an {@code Integer} of 3 and a {@code Double}
     * of 3.0 come out equal. Without that, a filter written with an integer literal would find
     * nothing in a column of doubles.
     *
     * @throws IllegalArgumentException if the type or the number are null, or if some column is
     *     negative
     */
    public static <M, I> RowFilter<M, I> numberFilter(ComparisonType type, Number number,
            int... indices) {
        return new NumberFilter<M, I>(type, number, indices);
    }

    /**
     * It lets through whatever passes one of those filters.
     *
     * @throws NullPointerException if the collection is null
     * @throws IllegalArgumentException if it brings a null
     */
    public static <M, I> RowFilter<M, I> orFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new OrFilter<M, I>(filters);
    }

    /**
     * It lets through whatever passes all those filters.
     *
     * @throws NullPointerException if the collection is null
     * @throws IllegalArgumentException if it brings a null
     */
    public static <M, I> RowFilter<M, I> andFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new AndFilter<M, I>(filters);
    }

    /**
     * It turns a filter round.
     *
     * @throws IllegalArgumentException if the filter is null
     */
    public static <M, I> RowFilter<M, I> notFilter(RowFilter<M, I> filter) {
        return new NotFilter<M, I>(filter);
    }

    /** Whether that row is seen. */
    public abstract boolean include(Entry<? extends M, ? extends I> entry);

    /**
     * A row seen from the filter: its values and what the model recognizes it by.
     *
     * <p>It is read-only on purpose. A filter that could touch the row it is evaluating would
     * change what is being filtered while it is being filtered.
     */
    public abstract static class Entry<M, I> {

        /** For the subclasses. */
        public Entry() {
        }

        /** The model this row comes from. */
        public abstract M getModel();

        /** How many values the row has. */
        public abstract int getValueCount();

        /**
         * That column's value.
         *
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public abstract Object getValue(int index);

        /**
         * That column's value as text.
         *
         * <p>Null is converted into the empty string, not into {@code "null"}: a text filter that
         * found the word "null" in the empty cells would be an unpleasant surprise.
         *
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public String getStringValue(int index) {
            Object value = getValue(index);
            return (value == null) ? "" : value.toString();
        }

        /** What the model recognizes this row by. */
        public abstract I getIdentifier();
    }

    /** The part common to the filters that look at certain columns; see the class note. */
    private abstract static class GeneralFilter<M, I> extends RowFilter<M, I> {

        private final int[] columns;

        GeneralFilter(int[] columns) {
            checkIndices(columns);
            this.columns = columns;
        }

        static void checkIndices(int[] columns) {
            if (columns == null) {
                return;
            }
            for (int i = columns.length - 1; i >= 0; i--) {
                if (columns[i] < 0) {
                    throw new IllegalArgumentException("Index must be >= 0");
                }
            }
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            int count = value.getValueCount();
            if (columns.length > 0) {
                for (int i = columns.length - 1; i >= 0; i--) {
                    int index = columns[i];
                    // A requested column the row does not have is skipped, it does not blow up:
                    // different
                                        // rows of a tree may have a different number of values.
                    if (index < count && include(value, index)) {
                        return true;
                    }
                }
            } else {
                while (--count >= 0) {
                    if (include(value, count)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected abstract boolean include(Entry<? extends M, ? extends I> value, int index);
    }

    /** Ver {@link RowFilter#regexFilter}. */
    private static class RegexFilter<M, I> extends GeneralFilter<M, I> {

        private final Matcher matcher;

        RegexFilter(Pattern regex, int[] columns) {
            super(columns);
            matcher = regex.matcher("");
        }

        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            matcher.reset(value.getStringValue(index));
            return matcher.find();
        }
    }

    /** Ver {@link RowFilter#dateFilter}. */
    private static class DateFilter<M, I> extends GeneralFilter<M, I> {

        private final long date;
        private final ComparisonType type;

        DateFilter(ComparisonType type, long date, int[] columns) {
            super(columns);
            if (type == null) {
                throw new IllegalArgumentException("type must be non-null");
            }
            this.type = type;
            this.date = date;
        }

        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);
            if (v instanceof Date) {
                long vDate = ((Date) v).getTime();
                if (type == ComparisonType.BEFORE) {
                    return (vDate < date);
                }
                if (type == ComparisonType.AFTER) {
                    return (vDate > date);
                }
                if (type == ComparisonType.EQUAL) {
                    return (vDate == date);
                }
                if (type == ComparisonType.NOT_EQUAL) {
                    return (vDate != date);
                }
            }
            return false;
        }
    }

    /** Ver {@link RowFilter#numberFilter}. */
    private static class NumberFilter<M, I> extends GeneralFilter<M, I> {

        private final boolean isComparable;
        private final Number number;
        private final ComparisonType type;

        NumberFilter(ComparisonType type, Number number, int[] columns) {
            super(columns);
            if (type == null || number == null) {
                throw new IllegalArgumentException("type and number must be non-null");
            }
            this.type = type;
            this.number = number;
            isComparable = (number instanceof Comparable);
        }

        @SuppressWarnings("unchecked")
        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);
            if (v instanceof Number) {
                boolean compared = true;
                int compareResult;
                Class<?> vClass = v.getClass();
                if (number.getClass() == vClass && isComparable) {
                    compareResult = ((Comparable<Number>) number).compareTo((Number) v);
                } else {
                    // A different type: the values are compared as doubles. See the factory's note.
                    compareResult = compare(number, (Number) v);
                }
                if (compared) {
                    if (type == ComparisonType.BEFORE) {
                        return (compareResult > 0);
                    }
                    if (type == ComparisonType.AFTER) {
                        return (compareResult < 0);
                    }
                    if (type == ComparisonType.EQUAL) {
                        return (compareResult == 0);
                    }
                    if (type == ComparisonType.NOT_EQUAL) {
                        return (compareResult != 0);
                    }
                }
            }
            return false;
        }

        private static int compare(Number a, Number b) {
            double da = a.doubleValue();
            double db = b.doubleValue();
            if (da < db) {
                return -1;
            }
            if (da > db) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * The part common to the filters that combine others.
     *
     * <p><strong>The array is read through {@link #parts} and not directly.</strong> Its natural
     * type is the one that is declared, but this compiler loses the wildcard's lower bound when
     * looking at an <em>inherited</em> field -- finding #516 --, and then the subclasses cannot
     * even copy it to a local variable. The same type returned by an inherited method is
     * substituted properly, so the detour is an accessor and not a change of type nor a generic
     * discard.
     */
    private abstract static class CompoundFilter<M, I> extends RowFilter<M, I> {

        private final RowFilter<? super M, ? super I>[] filters;

        @SuppressWarnings("unchecked")
        CompoundFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            // The null collection is not checked: it blows up by itself on being asked for the
                        // iterator, which is what the JDK does. A null element is, and with a
                        // capital, which is how it is written there.
            List<RowFilter<? super M, ? super I>> l =
                    new ArrayList<RowFilter<? super M, ? super I>>();
            Iterator<? extends RowFilter<? super M, ? super I>> it = filters.iterator();
            while (it.hasNext()) {
                RowFilter<? super M, ? super I> f = it.next();
                if (f == null) {
                    throw new IllegalArgumentException("Filter must be non-null");
                }
                l.add(f);
            }
            this.filters = l.toArray(new RowFilter[l.size()]);
        }

        /** The combined filters; see the class note. */
        RowFilter<? super M, ? super I>[] parts() {
            return filters;
        }
    }

    /** Ver {@link RowFilter#orFilter}. */
    private static class OrFilter<M, I> extends CompoundFilter<M, I> {

        OrFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            super(filters);
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            RowFilter<? super M, ? super I>[] fs = parts();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].include(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Ver {@link RowFilter#andFilter}. */
    private static class AndFilter<M, I> extends CompoundFilter<M, I> {

        AndFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            super(filters);
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            RowFilter<? super M, ? super I>[] fs = parts();
            for (int i = 0; i < fs.length; i++) {
                if (!fs[i].include(value)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Ver {@link RowFilter#notFilter}. */
    private static class NotFilter<M, I> extends RowFilter<M, I> {

        private final RowFilter<M, I> filter;

        NotFilter(RowFilter<M, I> filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must be non-null");
            }
            this.filter = filter;
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            return !filter.include(value);
        }
    }
}
