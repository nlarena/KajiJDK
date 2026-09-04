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
// La mitad de PARSEO ya no falta. `parseObject(String, ParsePosition)` es abstracto igual que en el
// JDK: obliga a cada formateador concreto a decir cómo se lee lo que escribe, y la variante de un
// solo argumento es el envoltorio que traduce "no avanzó el cursor" a ParseException.
public abstract class Format implements Serializable, Cloneable {

    /**
     * La clave de atributo con la que un formateador marca los campos del texto que produce.
     *
     * <p>Es una clase vacía a propósito: no agrega comportamiento sobre
     * {@link AttributedCharacterIterator.Attribute}, sólo un nivel de tipo. Ese nivel es lo que
     * permite que {@code FieldPosition} pida "el campo entero" sin poder recibir por error una
     * clave de idioma, y que cada subclase (NumberFormat.Field, DateFormat.Field) cuelgue de un
     * ancestro común.
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
     * Formatea y devuelve el resultado con los campos marcados como atributos.
     *
     * <p>La implementación de base no marca nada: devuelve el texto sin atributos, que es
     * exactamente lo que el contrato manda para un formateador que no informa campos. No es un
     * cuerpo de relleno — es la respuesta correcta para quien no tiene información de campos que
     * dar, y las subclases que sí la tienen lo redefinen.
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return new AttributedString(this.format(obj)).getIterator();
    }

    public abstract Object parseObject(String source, ParsePosition pos);

    /**
     * Parsea desde el principio del texto, y falla con excepción en lugar de con un cursor.
     *
     * <p>El criterio de fracaso es "el cursor no avanzó", no "devolvió null": un formateador puede
     * parsear legítimamente a null, y distinguir los dos casos es justamente para lo que existe
     * ParsePosition.
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
