package javax.management.openmbean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The type of a {@link TabularData}: rows of a given {@link CompositeType}, indexed by some of
 * their items.
 *
 * <p>It is a {@code Map} described in a way that can be transmitted: the row type says which
 * columns there are, and the index names say which of those columns make up the key. The
 * consequence worth keeping in mind is that <b>the key comes from the row</b>, it is not passed
 * separately: that is why {@link TabularData#put} takes a single argument and
 * {@link TabularData#calculateIndex} exists.
 *
 * <p>The index names are kept in the <b>order they were passed</b> and that order is part of the
 * type: it is the order the values have to be given in to {@link TabularData#get}. It is the
 * difference from {@link CompositeType}, where the order of the items counts for nothing -- there
 * there is nothing to order, here there is.
 */
public class TabularType extends OpenType<TabularData> {

    private static final long serialVersionUID = 6554071860220659261L;

    private final CompositeType rowType;
    private final List<String> indexNames;

    private transient int hash;

    /**
     * A tabular type with those rows and that key.
     *
     * @throws OpenDataException if some index name is not an item of the row type
     * @throws IllegalArgumentException if the row type or the array are null, if the array is
     *     empty, or if one of its elements is blank
     */
    public TabularType(String typeName, String description, CompositeType rowType,
            String[] indexNames) throws OpenDataException {
        super(TabularData.class.getName(), typeName, description);

        if (rowType == null) {
            throw new IllegalArgumentException("the row type cannot be null");
        }
        if (indexNames == null || indexNames.length == 0) {
            throw new IllegalArgumentException("one or more index names are required");
        }
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < indexNames.length; i++) {
            String n = indexNames[i];
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException(
                        "the index name " + i + " is blank");
            }
            n = n.trim();
            // An index that is not an item of the row type would describe a key no row can have. It
            // is checked here and not when putting the first row because the type has to be valid
            // on its own -- it is what gets transmitted.
            if (!rowType.containsKey(n)) {
                throw new OpenDataException(
                        "the index " + n + " is not an item of the row type");
            }
            names.add(n);
        }
        this.rowType = rowType;
        this.indexNames = Collections.unmodifiableList(names);
    }

    /** The type of the rows. */
    public CompositeType getRowType() {
        return this.rowType;
    }

    /** The items that make up the key, in order and read-only. */
    public List<String> getIndexNames() {
        return this.indexNames;
    }

    /** Whether {@code obj} is a {@link TabularData} whose type is this one. */
    public boolean isValue(Object obj) {
        if (!(obj instanceof TabularData)) {
            return false;
        }
        return this.equals(((TabularData) obj).getTabularType());
    }

    /** Equality by type name, row type and index names <b>in order</b>. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TabularType)) {
            return false;
        }
        TabularType other = (TabularType) obj;
        return this.getTypeName().equals(other.getTypeName())
                && this.rowType.equals(other.rowType)
                && this.indexNames.equals(other.indexNames);
    }

    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getTypeName().hashCode() + this.rowType.hashCode();
            for (int i = 0; i < this.indexNames.size(); i++) {
                h = h + this.indexNames.get(i).hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TabularType.class.getName());
        sb.append("(name=").append(this.getTypeName());
        sb.append(",rowType=").append(this.rowType.toString());
        sb.append(",indexNames=(");
        for (int i = 0; i < this.indexNames.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(this.indexNames.get(i));
        }
        sb.append("))");
        return sb.toString();
    }
}
