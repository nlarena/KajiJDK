package org.xml.sax.ext;

import org.xml.sax.Attributes;

/**
 * KajiLibrary's org.xml.sax.ext.Attributes2Impl -- `AttributesImpl` with the two arrays of flags
 * needed to answer {@link Attributes2}.
 *
 * <p>Inheritance does almost everything: the five fields per attribute, the lookups by name and the
 * growth of the array are in `AttributesImpl`. Here two parallel `boolean[]` are added, one for
 * each new question, and the only real obligation is **keeping them aligned with the list
 * underneath**: every `addAttribute` and every `removeAttribute` has to move the flags just as the
 * superclass moves the data, or attribute `i` ends up answering with the flags of `j`.
 *
 * <p><strong>There is an initialisation trap in the copy constructor and it is set on
 * purpose.</strong> `super(atts)` calls `setAttributes`, which is virtual and therefore runs the
 * version of **this** class while the superclass is still being built: the two arrays are set up
 * from there. That works only because no field here has an initialiser --if it had one, it would
 * run after the `super(...)` and overwrite with `null` what `setAttributes` had just left--. It is
 * the reason why `declared` and `specified` are declared bare and filled in the constructor with no
 * arguments.
 *
 * <p>The default values of `addAttribute` are not filler: `specified` is left at `true` --it is
 * being added, so it was specified-- and `declared` comes from the type, `true` for everything that
 * is not `CDATA`. The latter holds because with no DTD all attributes are `CDATA`, so a type other
 * than `CDATA` can only have come from a declaration. It is a correct deduction, not a guess;
 * whoever wants something else has {@link #setDeclared} and {@link #setSpecified}.
 *
 * <p>And for that same reason `setAttributes` looks at whether the source is an `Attributes2`: if
 * it is, it copies the real flags; if not, it applies that same deduction, which is the only thing
 * that can be known of a list that does not have them.
 */
public class Attributes2Impl extends org.xml.sax.helpers.AttributesImpl
        implements Attributes2 {

    // No initialiser, for what the comment of the class explains.
    private boolean declared[];
    private boolean specified[];

    /** An empty list, ready for `addAttribute`. */
    public Attributes2Impl() {
        declared = new boolean[0];
        specified = new boolean[0];
    }

    /**
     * An independent copy, flags included. The `Attributes` the parser lends in `startElement`
     * stops being valid when the call ends; this copy does not.
     */
    public Attributes2Impl(Attributes atts) {
        super(atts);
    }

    public boolean isDeclared(int index) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        return declared[index];
    }

    public boolean isDeclared(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local name="
                    + localName + ", namespace=" + uri);
        }
        return declared[index];
    }

    public boolean isDeclared(String qName) {
        int index = getIndex(qName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + qName);
        }
        return declared[index];
    }

    public boolean isSpecified(int index) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        return specified[index];
    }

    public boolean isSpecified(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local name="
                    + localName + ", namespace=" + uri);
        }
        return specified[index];
    }

    public boolean isSpecified(String qName) {
        int index = getIndex(qName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + qName);
        }
        return specified[index];
    }

    /**
     * It replaces the contents with a copy of those of `atts`. The arrays are remade at the exact
     * size instead of being reused: the previous list could be longer and leave old flags hanging
     * behind the new one.
     */
    public void setAttributes(Attributes atts) {
        int length = atts.getLength();

        super.setAttributes(atts);
        declared = new boolean[length];
        specified = new boolean[length];

        if (atts instanceof Attributes2) {
            Attributes2 a2 = (Attributes2) atts;
            for (int i = 0; i < length; i++) {
                declared[i] = a2.isDeclared(i);
                specified[i] = a2.isSpecified(i);
            }
        } else {
            for (int i = 0; i < length; i++) {
                declared[i] = !("CDATA".equals(atts.getType(i)));
                specified[i] = true;
            }
        }
    }

    /** It appends with the deduced flags the comment of the class explains. */
    public void addAttribute(String uri, String localName, String qName,
                             String type, String value) {
        super.addAttribute(uri, localName, qName, type, value);
        int length = getLength();

        if (length > declared.length) {
            boolean newFlags[];

            newFlags = new boolean[length];
            System.arraycopy(declared, 0, newFlags, 0, declared.length);
            declared = newFlags;

            newFlags = new boolean[length];
            System.arraycopy(specified, 0, newFlags, 0, specified.length);
            specified = newFlags;
        }

        specified[length - 1] = true;
        declared[length - 1] = !"CDATA".equals(type);
    }

    /**
     * It removes attribute `index`. The arrays of flags shift just like the data of the superclass;
     * otherwise, the ones left behind would answer for the one that went away.
     */
    public void removeAttribute(int index) {
        int origMax = getLength() - 1;

        super.removeAttribute(index);
        if (index != origMax) {
            System.arraycopy(declared, index + 1, declared, index, origMax - index);
            System.arraycopy(specified, index + 1, specified, index, origMax - index);
        }
    }

    public void setDeclared(int index, boolean value) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        declared[index] = value;
    }

    public void setSpecified(int index, boolean value) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        specified[index] = value;
    }
}
