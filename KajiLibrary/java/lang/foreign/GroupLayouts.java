package java.lang.foreign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// The implementations of the composite layouts: padding, sequence, struct and union.
// Package-private, like the value ones: they are reached through `MemoryLayout`'s factories.

// Space taken up that carries nothing. Its alignment starts at 1 because a constraint on where
// nothing may start constrains nothing.
final class Padding implements PaddingLayout {

    private final long size;
    private final long alignment;
    private final String name;

    Padding(long size, long alignment, String name) {
        this.size = size;
        this.alignment = alignment;
        this.name = name;
    }

    public long byteSize() {
        return this.size;
    }

    public long byteAlignment() {
        return this.alignment;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }

    public PaddingLayout withName(String name) {
        return new Padding(this.size, this.alignment, Layouts.requireName(name));
    }

    public PaddingLayout withoutName() {
        return new Padding(this.size, this.alignment, null);
    }

    public PaddingLayout withByteAlignment(long byteAlignment) {
        return new Padding(this.size, Layouts.requireAlignment(byteAlignment), this.name);
    }

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetByPath(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.selectByPath(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.scaled(this, offset, index);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Padding)) {
            return false;
        }
        Padding other = (Padding) obj;
        boolean sameName = this.name == null ? other.name == null
                : this.name.equals(other.name);
        return this.size == other.size && this.alignment == other.alignment
                && sameName;
    }

    public int hashCode() {
        int h = (int) this.size * 31 + (int) this.alignment;
        return h * 31 + (this.name == null ? 0 : this.name.hashCode());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('x');
        sb.append(this.size);
        Layouts.appendName(sb, this.name);
        return sb.toString();
    }
}

// N copies of a layout, one after the other. The alignment **is the element's**: if each element
// falls aligned, so does the whole sequence.
final class Sequence implements SequenceLayout {

    private final long count;
    private final MemoryLayout element;
    private final long alignment;
    private final String name;

    Sequence(long count, MemoryLayout element, long alignment, String name) {
        this.count = count;
        this.element = element;
        this.alignment = alignment;
        this.name = name;
    }

    public long byteSize() {
        return this.count * this.element.byteSize();
    }

    public long byteAlignment() {
        return this.alignment;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }

    public MemoryLayout elementLayout() {
        return this.element;
    }

    public long elementCount() {
        return this.count;
    }

    public SequenceLayout withElementCount(long elementCount) {
        return Layouts.sequence(elementCount, this.element);
    }

    // Flattening: a sequence of sequences becomes a single one, with the product of the counts. It
    // is correct because in memory they already sit like that -- nesting adds no separation, only
    // structure.
    public SequenceLayout flatten() {
        long total = this.count;
        MemoryLayout leaf = this.element;
        while (leaf instanceof SequenceLayout) {
            SequenceLayout s = (SequenceLayout) leaf;
            total = total * s.elementCount();
            leaf = s.elementLayout();
        }
        return Layouts.sequence(total, leaf);
    }

    /**
     * It spreads the same elements over several dimensions. One may be `-1` and is worked out.
     */
    public SequenceLayout reshape(long... elementCounts) {
        if (elementCounts == null || elementCounts.length == 0) {
            throw new IllegalArgumentException("dimensions are needed");
        }
        int inferred = -1;
        long product = 1L;
        int i = 0;
        while (i < elementCounts.length) {
            long n = elementCounts[i];
            if (n == -1L) {
                if (inferred >= 0) {
                    throw new IllegalArgumentException("only one dimension may be -1");
                }
                inferred = i;
            } else if (n <= 0L) {
                throw new IllegalArgumentException("non-positive dimension: " + n);
            } else {
                product = product * n;
            }
            i = i + 1;
        }
        long[] dims = elementCounts.clone();
        if (inferred >= 0) {
            if (product == 0L || this.count % product != 0L) {
                throw new IllegalArgumentException("the deduced dimension does not come out whole");
            }
            dims[inferred] = this.count / product;
            product = this.count;
        }
        if (product != this.count) {
            throw new IllegalArgumentException(
                    "the product of the dimensions is " + product + " and there are " + this.count
                            + " elements");
        }
        // It is built from the inside out: the last dimension is the one closest to the element.
        MemoryLayout current = this.element;
        int j = dims.length - 1;
        while (j >= 0) {
            current = Layouts.sequence(dims[j], current);
            j = j - 1;
        }
        return (SequenceLayout) current;
    }

    public SequenceLayout withName(String name) {
        return new Sequence(this.count, this.element, this.alignment,
                Layouts.requireName(name));
    }

    public SequenceLayout withoutName() {
        return new Sequence(this.count, this.element, this.alignment, null);
    }

    public SequenceLayout withByteAlignment(long byteAlignment) {
        return new Sequence(this.count, this.element,
                Layouts.requireAlignment(byteAlignment), this.name);
    }

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetByPath(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.selectByPath(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.scaled(this, offset, index);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Sequence)) {
            return false;
        }
        Sequence other = (Sequence) obj;
        boolean sameName = this.name == null ? other.name == null
                : this.name.equals(other.name);
        return this.count == other.count && this.element.equals(other.element)
                && this.alignment == other.alignment && sameName;
    }

    public int hashCode() {
        int h = (int) this.count * 31 + this.element.hashCode();
        h = h * 31 + (int) this.alignment;
        return h * 31 + (this.name == null ? 0 : this.name.hashCode());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(this.count);
        sb.append(':');
        sb.append(this.element.toString());
        sb.append(']');
        Layouts.appendName(sb, this.name);
        return sb.toString();
    }
}

// The base of struct and union: the list of members and everything that does not depend on whether
// they stack or overlay. The only thing telling them apart --each member's offset-- lives in the
// subclasses.
abstract class Group implements GroupLayout {

    private final List<MemoryLayout> members;
    private final long alignment;
    private final String name;

    Group(List<MemoryLayout> members, long alignment, String name) {
        this.members = members;
        this.alignment = alignment;
        this.name = name;
    }

    public List<MemoryLayout> memberLayouts() {
        return Collections.unmodifiableList(this.members);
    }

    List<MemoryLayout> rawMembers() {
        return this.members;
    }

    public long byteAlignment() {
        return this.alignment;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }

    String rawName() {
        return this.name;
    }

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetByPath(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.selectByPath(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.scaled(this, offset, index);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Group other = (Group) obj;
        boolean sameName = this.name == null ? other.name == null
                : this.name.equals(other.name);
        return this.members.equals(other.members) && this.alignment == other.alignment
                && sameName;
    }

    public int hashCode() {
        int h = this.members.hashCode() * 31 + (int) this.alignment;
        return h * 31 + (this.name == null ? 0 : this.name.hashCode());
    }

    // `[m1m2]` for a struct, `[m1|m2]` for a union: the separator says which is which.
    String render(String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (i < this.members.size()) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(this.members.get(i).toString());
            i = i + 1;
        }
        sb.append(']');
        Layouts.appendName(sb, this.name);
        return sb.toString();
    }
}

final class Struct extends Group implements StructLayout {

    Struct(List<MemoryLayout> members, long alignment, String name) {
        super(members, alignment, name);
    }

    // The sum of the members. There is no implicit padding: if any were needed, the construction
    // has already failed.
    public long byteSize() {
        long total = 0L;
        List<MemoryLayout> ms = this.rawMembers();
        int i = 0;
        while (i < ms.size()) {
            total = total + ms.get(i).byteSize();
            i = i + 1;
        }
        return total;
    }

    public StructLayout withName(String name) {
        return new Struct(this.rawMembers(), this.byteAlignment(),
                Layouts.requireName(name));
    }

    public StructLayout withoutName() {
        return new Struct(this.rawMembers(), this.byteAlignment(), null);
    }

    public StructLayout withByteAlignment(long byteAlignment) {
        return new Struct(this.rawMembers(), Layouts.requireAlignment(byteAlignment),
                this.rawName());
    }

    public String toString() {
        return this.render("");
    }
}

final class Union extends Group implements UnionLayout {

    Union(List<MemoryLayout> members, long alignment, String name) {
        super(members, alignment, name);
    }

    // The largest one. They all start at zero, so the size is that of the one taking up most.
    public long byteSize() {
        long max = 0L;
        List<MemoryLayout> ms = this.rawMembers();
        int i = 0;
        while (i < ms.size()) {
            long n = ms.get(i).byteSize();
            if (n > max) {
                max = n;
            }
            i = i + 1;
        }
        return max;
    }

    public UnionLayout withName(String name) {
        return new Union(this.rawMembers(), this.byteAlignment(), Layouts.requireName(name));
    }

    public UnionLayout withoutName() {
        return new Union(this.rawMembers(), this.byteAlignment(), null);
    }

    public UnionLayout withByteAlignment(long byteAlignment) {
        return new Union(this.rawMembers(), Layouts.requireAlignment(byteAlignment),
                this.rawName());
    }

    public String toString() {
        return this.render("|");
    }
}
