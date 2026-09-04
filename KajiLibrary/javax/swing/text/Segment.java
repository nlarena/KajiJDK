package javax.swing.text;

import java.text.CharacterIterator;

/**
 * Un pedazo de texto <strong>prestado</strong>: un arreglo ajeno, un desplazamiento y un largo.
 *
 * <h2>Para que existe</h2>
 *
 * <p>Para no copiar. Pedirle a un documento un fragmento como {@link String} aloca y copia; un
 * {@code Segment} apunta al arreglo que el documento ya tiene. En un editor eso pasa en cada
 * repintado, asi que la diferencia se nota.
 *
 * <p>El precio esta en la palabra <em>prestado</em>: el arreglo <strong>no es propio</strong> y el
 * documento puede cambiarlo. Un {@code Segment} vale hasta la proxima edicion, y guardarlo mas alla
 * de eso es leer memoria que ya significa otra cosa.
 */
public class Segment implements Cloneable, CharacterIterator, CharSequence {

    /** El arreglo, que es de otro. */
    public char[] array;

    /** Donde empieza el pedazo. */
    public int offset;

    /** Cuantos caracteres tiene. */
    public int count;

    private boolean partialReturn;
    private int pos;

    /** Un segmento vacio, sin arreglo. */
    public Segment() {
        this(null, 0, 0);
    }

    /** Un segmento sobre {@code array}. */
    public Segment(char[] array, int offset, int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;
        this.partialReturn = false;
    }

    /**
     * Si se acepta que el documento devuelva menos de lo pedido.
     *
     * <p>Un documento puede tener el texto partido en varios arreglos. Con esto en {@code true}
     * entrega el primer tramo contiguo en vez de juntar todo en uno nuevo — que es justamente la
     * copia que esta clase vino a evitar. Quien lo prende tiene que estar dispuesto a llamar de
     * nuevo por lo que falta.
     */
    public void setPartialReturn(boolean p) {
        this.partialReturn = p;
    }

    /** Si se acepta una devolucion parcial. */
    public boolean isPartialReturn() {
        return this.partialReturn;
    }

    public String toString() {
        if (this.array != null) {
            return new String(this.array, this.offset, this.count);
        }
        return "";
    }

    public char first() {
        this.pos = this.offset;
        if (this.count != 0) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char last() {
        this.pos = this.offset + this.count;
        if (this.count != 0) {
            this.pos = this.pos - 1;
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char current() {
        if (this.count != 0 && this.pos < this.offset + this.count) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char next() {
        this.pos = this.pos + 1;
        int fin = this.offset + this.count;
        if (this.pos >= fin) {
            this.pos = fin;
            return CharacterIterator.DONE;
        }
        return current();
    }

    public char previous() {
        if (this.pos == this.offset) {
            return CharacterIterator.DONE;
        }
        this.pos = this.pos - 1;
        return current();
    }

    public char setIndex(int position) {
        int fin = this.offset + this.count;
        if (position < this.offset || position > fin) {
            throw new IllegalArgumentException("posicion fuera de rango");
        }
        this.pos = position;
        if (this.pos != fin && this.count != 0) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public int getBeginIndex() {
        return this.offset;
    }

    public int getEndIndex() {
        return this.offset + this.count;
    }

    public int getIndex() {
        return this.pos;
    }

    public char charAt(int index) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return this.array[this.offset + index];
    }

    public int length() {
        return this.count;
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end > this.count || start > end) {
            throw new StringIndexOutOfBoundsException("subSequence fuera de rango");
        }
        Segment s = new Segment();
        s.array = this.array;
        s.offset = this.offset + start;
        s.count = end - start;
        return s;
    }

    /**
     * Una copia superficial: comparte el arreglo.
     *
     * <p>Y tiene que compartirlo. Copiar el arreglo seria exactamente lo que esta clase evita, y
     * ademas romperia la relacion con el documento, que es de donde el arreglo saca su sentido.
     */
    public Object clone() {
        Object copia = null;
        try {
            copia = super.clone();
        } catch (CloneNotSupportedException e) {
            // No puede pasar: esta clase implementa Cloneable.
        }
        return copia;
    }
}
