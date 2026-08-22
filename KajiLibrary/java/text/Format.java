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
// A KajiLibrary subset: the PARSING half (`parseObject` in both forms) is omitted rather than
// declared abstract — nothing in this package parses yet, and declaring it would force every
// concrete formatter to ship a method that only throws. `formatToCharacterIterator` needs
// AttributedCharacterIterator, whose API is built on the nested type `Attribute` (finding #101),
// and `clone()` is omitted because it would depend on Object.clone().
public abstract class Format implements Serializable, Cloneable {

    protected Format() {
    }

    public final String format(Object obj) {
        StringBuffer buf = new StringBuffer();
        StringBuffer result = this.format(obj, buf, new FieldPosition(0));
        return result.toString();
    }

    public abstract StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos);
}
