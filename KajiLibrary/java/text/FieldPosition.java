package java.text;

// KajiLibrary's java.text.FieldPosition — asks a formatter "and where did you put the X?".
//
// Formatting produces one flat string, but a caller often needs to know which SLICE of it is the
// integer part, or the exponent, so it can be aligned in a column or styled. So `format` takes one
// of these, and writes back the begin/end offsets of the requested field.
//
// FALTAN, y es una elección forzada: los dos constructores que toman `java.text.Format.Field` y
// `getFieldAttribute()`. No es que no se puedan escribir —se escribieron y andaban—, es que
// declararlos pone `java/text/Format$Field` en el pool de constantes de FieldPosition.class, y con
// eso NINGUNA unidad de compilación de java.text que declare una subclase de java.text.Format.Field vuelve a
// compilar: su `super(name)` deja de resolver (finding #319). Como todo formateador nombra
// FieldPosition en su firma, la elección es entre estos tres miembros y las clases
// java.text.NumberFormat.Field / java.text.DateFormat.Field / java.text.MessageFormat.Field enteras.
//
// Se eligieron las Field. Tres miembros que faltan son un subconjunto legal; un FieldPosition
// construido con un atributo que después NINGÚN formateador puede rellenar —porque sin esas clases
// no hay con qué nombrar el campo— no lo es: el llamador recibiría begin == end == 0 y lo leería
// como "el campo salió vacío". Los tres vuelven en cuanto #319 esté arreglado.
public class FieldPosition {

    private Format.Field attribute;
    private final int field;
    private int beginIndex;
    private int endIndex;

    /** El campo que se busca, nombrado por su **atributo** en vez de por un entero. */
    public FieldPosition(Format.Field attribute) {
        this(attribute, -1);
    }

    /** El de arriba, con el entero equivalente para los formateadores viejos. */
    public FieldPosition(Format.Field attribute, int fieldID) {
        this.attribute = attribute;
        this.field = fieldID;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    /** El atributo que se busca, o `null` si se construyo con un entero. */
    public Format.Field getFieldAttribute() {
        return this.attribute;
    }

    public FieldPosition(int field) {
        this.field = field;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    public int getField() {
        return this.field;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public void setBeginIndex(int bi) {
        this.beginIndex = bi;
    }

    public void setEndIndex(int ei) {
        this.endIndex = ei;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldPosition) {
            FieldPosition other = (FieldPosition) obj;
            return this.field == other.field
                    && this.beginIndex == other.beginIndex
                    && this.endIndex == other.endIndex;
        }
        return false;
    }

    public int hashCode() {
        return (this.field << 24) | (this.beginIndex << 16) | this.endIndex;
    }

    public String toString() {
        return "java.text.FieldPosition[field=" + Integer.toString(this.field)
                + ",beginIndex=" + Integer.toString(this.beginIndex)
                + ",endIndex=" + Integer.toString(this.endIndex) + "]";
    }
}
