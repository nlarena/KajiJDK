package java.awt.font;

/**
 * Un lugar del texto donde se puede poner el cursor: **entre** dos caracteres.
 *
 * <p>Un punto de inserción no es un carácter, es una frontera, y hay dos maneras de nombrar la misma
 * frontera: el borde de entrada del carácter que sigue o el de salida del que viene. Esta clase
 * guarda cuál de las dos, y ésa es toda su razón de ser.
 *
 * <p>Parece una distinción sin diferencia hasta que el renglón mezcla direcciones. En un texto que
 * va de izquierda a derecha con una palabra en árabe adentro, la frontera lógica entre dos
 * caracteres cae en **dos lugares distintos de la pantalla** según de qué lado se venga, y sin
 * decirlo no hay forma de saber dónde dibujar el cursor.
 */
public final class TextHitInfo {

    private final int charIndex;
    private final boolean isLeadingEdge;

    /** Con el carácter y de qué lado. */
    private TextHitInfo(int charIndex, boolean isLeadingEdge) {
        this.charIndex = charIndex;
        this.isLeadingEdge = isLeadingEdge;
    }

    /** El carácter al que se refiere. */
    public int getCharIndex() {
        return this.charIndex;
    }

    /** Si es el borde de entrada de ese carácter. */
    public boolean isLeadingEdge() {
        return this.isLeadingEdge;
    }

    /**
     * La posición de inserción, contada en caracteres.
     *
     * <p>Es donde entraría un carácter nuevo. Dos `TextHitInfo` distintos pueden dar la misma:
     * el borde de salida del carácter `n` y el de entrada del `n+1` son la misma frontera.
     */
    public int getInsertionIndex() {
        if (this.isLeadingEdge) {
            return this.charIndex;
        }
        return this.charIndex + 1;
    }

    public int hashCode() {
        return this.charIndex;
    }

    /** Igualdad por carácter y por lado. */
    public boolean equals(Object obj) {
        return obj instanceof TextHitInfo && this.equals((TextHitInfo) obj);
    }

    /**
     * Lo mismo, con el tipo ya conocido.
     *
     * <p>Dos que apunten a la misma frontera desde lados distintos **no** son iguales: la clase
     * guarda de qué lado se llegó, y eso es lo que la distingue.
     */
    public boolean equals(TextHitInfo hitInfo) {
        return hitInfo != null && this.charIndex == hitInfo.charIndex
                && this.isLeadingEdge == hitInfo.isLeadingEdge;
    }

    public String toString() {
        return "TextHitInfo[" + this.charIndex + (this.isLeadingEdge ? "L" : "T") + "]";
    }

    /** El borde de entrada de ese carácter. */
    public static TextHitInfo leading(int charIndex) {
        return new TextHitInfo(charIndex, true);
    }

    /** El borde de salida de ese carácter. */
    public static TextHitInfo trailing(int charIndex) {
        return new TextHitInfo(charIndex, false);
    }

    /** La frontera de antes de esa posición, nombrada desde el carácter anterior. */
    public static TextHitInfo beforeOffset(int offset) {
        return new TextHitInfo(offset - 1, false);
    }

    /** La frontera de después de esa posición, nombrada desde el carácter siguiente. */
    public static TextHitInfo afterOffset(int offset) {
        return new TextHitInfo(offset, true);
    }

    /** La otra manera de nombrar la misma frontera. */
    public TextHitInfo getOtherHit() {
        if (this.isLeadingEdge) {
            return trailing(this.charIndex - 1);
        }
        return leading(this.charIndex + 1);
    }

    /** El mismo lado, tantos caracteres más allá. */
    public TextHitInfo getOffsetHit(int delta) {
        return new TextHitInfo(this.charIndex + delta, this.isLeadingEdge);
    }
}
