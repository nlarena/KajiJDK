package javax.naming.directory;

import java.io.Serializable;

/**
 * KajiLibrary's javax.naming.directory.ModificationItem -- a single modification, to apply in a
 * batch.
 *
 * <p>A pair: what to do --add, replace or remove-- and on which attribute. It exists for the
 * version of {@code modifyAttributes} that takes an array, and that version is the one to use when
 * the modifications have to be applied <b>together</b>: the specification asks for all or none to
 * be applied.
 *
 * <p>The other version --the one taking a single code and some {@link Attributes}-- applies the
 * same operation to all, so it does not serve a batch mixing additions and removals.
 *
 * <p>It is immutable: both fields are fixed at construction. That makes sense for something taking
 * part in an atomic operation -- if it could change after building the array, what gets applied
 * would not be what was reviewed.
 */
public class ModificationItem implements Serializable {

    private static final long serialVersionUID = 7573258562534746850L;

    /** One of the three {@link DirContext} constants. */
    private final int mod_op;

    /** On which attribute. */
    private final Attribute attr;

    /**
     * @param mod_op {@link DirContext#ADD_ATTRIBUTE}, {@link DirContext#REPLACE_ATTRIBUTE} or
     *     {@link DirContext#REMOVE_ATTRIBUTE}
     * @param attr the attribute; when removing, its values say <b>which</b> ones to remove
     * @throws IllegalArgumentException if the code is not one of the three, or if the attribute is
     *     null
     */
    public ModificationItem(int mod_op, Attribute attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Must specify non-null attribute for modification");
        }
        if (mod_op != DirContext.ADD_ATTRIBUTE
                && mod_op != DirContext.REPLACE_ATTRIBUTE
                && mod_op != DirContext.REMOVE_ATTRIBUTE) {
            throw new IllegalArgumentException("Invalid modification code " + mod_op);
        }
        this.mod_op = mod_op;
        this.attr = attr;
    }

    /** What to do. */
    public int getModificationOp() {
        return this.mod_op;
    }

    /** On which attribute. */
    public Attribute getAttribute() {
        return this.attr;
    }

    /** The operation in words and the attribute, for a log. */
    public String toString() {
        switch (this.mod_op) {
            case DirContext.ADD_ATTRIBUTE:
                return "Add attribute: " + this.attr.toString();
            case DirContext.REPLACE_ATTRIBUTE:
                return "Replace attribute: " + this.attr.toString();
            default:
                return "Remove attribute: " + this.attr.toString();
        }
    }
}
