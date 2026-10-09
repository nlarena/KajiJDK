package java.text;

import java.io.Serializable;

// KajiLibrary's java.text.Format — the root of the formatting hierarchy.
//
// The shape of this class IS the design of java.text. Every formatter — numbers, messages, dates —
// answers the same question, "render this object into text", and the signature that expresses it is
//
//     StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
//
// Three things are deliberate in that one line:
//   - it APPENDS to a caller-owned buffer instead of returning a fresh String, so composing
//     formatters (a message containing a number containing a currency) does not build a new string
//     at every level;
//   - it takes a FieldPosition, so the caller can learn where a particular field landed in the
//     output — the information a plain String return would destroy;
//   - it takes Object, which is what lets a MessageFormat hold a heterogeneous list of formatters
//     and drive them all through one call.
//
// The convenience `format(Object)` is final precisely because it is the trivial wrapper: subclasses
// override the three-argument form, and every entry point funnels there.
//
// The PARSING half is no longer missing. `parseObject(String, ParsePosition)` is abstract just as
// in the JDK: it forces every concrete formatter to say how what it writes is read back, and the
// one-argument variant is the wrapper translating "the cursor did not advance" into a
// ParseException.
public abstract class Format implements Serializable, Cloneable {

    /**
     * The attribute key a formatter marks the fields of the text it produces with.
     *
     * <p>It is an empty class on purpose: it adds no behaviour over
     * {@link AttributedCharacterIterator.Attribute}, only a level of type. That level is what lets
     * {@code FieldPosition} ask for "the integer field" without being able to receive a language key
     * by mistake, and lets every subclass (NumberFormat.Field, DateFormat.Field) hang off a common
     * ancestor.
     */
    public static class Field extends AttributedCharacterIterator.Attribute {

        protected Field(String name) {
            super(name);
        }
    }

    protected Format() {
    }

    public final String format(Object obj) {
        StringBuffer buf = new StringBuffer();
        StringBuffer result = this.format(obj, buf, new FieldPosition(0));
        return result.toString();
    }

    public abstract StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos);

    /**
     * It formats and returns the result with the fields marked as attributes.
     *
     * <p>The base implementation marks nothing: it returns the text without attributes, which is
     * exactly what the contract demands of a formatter that reports no fields. It is not a filler
     * body -- it is the right answer for one that has no field information to give, and the
     * subclasses that do have it override it.
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return new AttributedString(this.format(obj)).getIterator();
    }

    public abstract Object parseObject(String source, ParsePosition pos);

    /**
     * It parses from the beginning of the text, and fails with an exception instead of with a
     * cursor.
     *
     * <p>The failure criterion is "the cursor did not advance", not "it returned null": a formatter
     * can legitimately parse to null, and telling the two cases apart is precisely what ParsePosition
     * exists for.
     */
    public Object parseObject(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object result = this.parseObject(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Format.parseObject(String) failed", pos.getErrorIndex());
        }
        return result;
    }
}
