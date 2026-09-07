package javax.swing.text;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * El contenido mas simple: un arreglo de caracteres que se copia entero en cada edicion.
 *
 * <p>Sirve para documentos chicos y para entender que hace un {@link AbstractDocument.Content}
 * sin la aritmetica del hueco de {@link GapContent}. Cada insercion o borrado mueve todo lo que
 * viene despues, asi que escribir de a un caracter en un documento largo es cuadratico: por eso
 * los documentos de verdad usan {@code GapContent}.
 *
 * <p>Las marcas —lo que hay detras de cada {@link Position}— se guardan en una lista de enteros
 * que se recorre entera en cada edicion. Las que ya no usa nadie se sacan cuando la lista crece,
 * mirando su cuenta de referencias: sin eso, un documento que se edita mucho acumularia marcas
 * muertas para siempre.
 */
public final class StringContent implements AbstractDocument.Content, Serializable {

    private static final char[] empty = new char[0];

    private char[] data;
    private int count;

    /** Las marcas vivas; ver la nota de la clase. */
    transient Vector<PosRec> marks;

    /** Un contenido con lugar para diez caracteres. */
    public StringContent() {
        this(10);
    }

    /** Un contenido con lugar inicial para esa cantidad; siempre arranca con un fin de linea. */
    public StringContent(int initialLength) {
        if (initialLength < 2) {
            initialLength = 2;
        }
        data = new char[initialLength];
        data[0] = '\n';
        count = 1;
    }

    public int length() {
        return count;
    }

    public UndoableEdit insertString(int where, String str) throws BadLocationException {
        if (where > count || where < 0) {
            throw new BadLocationException("Invalid location", count);
        }
        char[] chars = str.toCharArray();
        replace(where, 0, chars, 0, chars.length);
        if (marks != null) {
            updateMarksForInsert(where, str.length());
        }
        return new InsertUndo(where, str.length());
    }

    /** Devuelve una edicion que sabe reponer lo borrado; por eso guarda el texto. */
    public UndoableEdit remove(int where, int nitems) throws BadLocationException {
        if (where + nitems >= count) {
            throw new BadLocationException("Invalid range", count);
        }
        String removedString = getString(where, nitems);
        UndoableEdit edit = new RemoveUndo(where, removedString);
        replace(where, nitems, empty, 0, 0);
        if (marks != null) {
            updateMarksForRemove(where, nitems);
        }
        return edit;
    }

    public String getString(int where, int len) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid range", count);
        }
        return new String(data, where, len);
    }

    /**
     * El texto en el segmento.
     *
     * <p>Apunta al arreglo propio, sin copiar: quien lo reciba no debe escribirlo. Es el trato de
     * {@link Segment}, y lo que hace que dibujar texto no reserve memoria.
     */
    public void getChars(int where, int len, Segment chars) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid location", count);
        }
        chars.array = data;
        chars.offset = where;
        chars.count = len;
    }

    /** Una marca en esa posicion; dos posiciones iguales comparten la marca. */
    public Position createPosition(int offset) throws BadLocationException {
        if (marks == null) {
            marks = new Vector<PosRec>();
        }
        return new StickyPosition(offset);
    }

    /** Reemplaza un tramo por otro, agrandando el arreglo si hace falta. */
    void replace(int offset, int length, char[] replArray, int replOffset, int replLength) {
        int delta = replLength - length;
        int src = offset + length;
        int nmove = count - src;
        int dest = src + delta;
        if ((count + delta) > data.length) {
            resize(count + delta);
        }
        System.arraycopy(data, src, data, dest, nmove);
        System.arraycopy(replArray, replOffset, data, offset, replLength);
        count = count + delta;
    }

    /** Agranda el arreglo al doble o a lo pedido, lo que sea mayor. */
    void resize(int ncount) {
        char[] ndata = new char[ncount * 2 + 1];
        System.arraycopy(data, 0, ndata, 0, count);
        data = ndata;
    }

    synchronized void updateMarksForInsert(int offset, int length) {
        if (offset == 0) {
            // Una marca en cero no se corre: es el principio del documento.
            offset = 1;
        }
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= offset) {
                mark.offset = mark.offset + length;
            }
        }
    }

    synchronized void updateMarksForRemove(int offset, int length) {
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= (offset + length)) {
                mark.offset = mark.offset - length;
            } else if (mark.offset >= offset) {
                // Una marca dentro de lo borrado se pega al principio del hueco.
                mark.offset = offset;
            }
        }
    }

    /** Las marcas que caen en ese rango, para que una edicion pueda reponerlas al deshacer. */
    protected Vector<UndoPosRef> getPositionsInRange(Vector<UndoPosRef> v, int offset, int length) {
        int n = marks.size();
        int end = offset + length;
        Vector<UndoPosRef> placeIn = (v == null) ? new Vector<UndoPosRef>() : v;
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= offset && mark.offset <= end) {
                placeIn.addElement(new UndoPosRef(mark));
            }
        }
        return placeIn;
    }

    /** Vuelve a poner cada marca donde estaba; lo llama una edicion al deshacerse. */
    protected void updateUndoPositions(Vector<UndoPosRef> positions) {
        for (int counter = positions.size() - 1; counter >= 0; counter--) {
            UndoPosRef ref = positions.elementAt(counter);
            ref.resetLocation();
        }
    }

    /**
     * Una marca compartida: el entero al que apuntan una o mas {@link Position}.
     *
     * <p>Estatica y no interna: no necesita el contenido, y nuestro javac todavia no pasa la
     * instancia externa implicita cuando una clase interna crea a una hermana (#508).
     */
    static final class PosRec {

        PosRec(int offset) {
            this.offset = offset;
        }

        int offset;
        boolean unused;
    }

    /**
     * Una posicion que se pega al texto.
     *
     * <p>Al recolectarse marca su registro como no usado, y la proxima edicion lo saca de la
     * lista. Es la unica forma de no crecer sin limite sin obligar al usuario a soltar posiciones
     * a mano.
     */
    final class StickyPosition implements Position {

        StickyPosition(int offset) {
            rec = new PosRec(offset);
            marks.addElement(rec);
        }

        public int getOffset() {
            return rec.offset;
        }

        protected void finalize() throws Throwable {
            rec.unused = true;
        }

        public String toString() {
            return Integer.toString(getOffset());
        }

        PosRec rec;
    }

    /** Donde estaba una marca antes de una edicion, para reponerla al deshacer; estatica por #508. */
    static final class UndoPosRef {

        UndoPosRef(PosRec rec) {
            this.rec = rec;
            this.undoLocation = rec.offset;
        }

        protected void resetLocation() {
            rec.offset = undoLocation;
        }

        protected PosRec rec;
        protected int undoLocation;
    }

    /** Deshacer una insercion es borrar lo insertado; rehacerla, volver a ponerlo. */
    class InsertUndo extends AbstractUndoableEdit {

        protected InsertUndo(int offset, int length) {
            super();
            this.offset = offset;
            this.length = length;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized (StringContent.this) {
                    if (marks != null) {
                        posRefs = getPositionsInRange(null, offset, length);
                    }
                    string = getString(offset, length);
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized (StringContent.this) {
                    insertString(offset, string);
                    string = null;
                    if (posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
                }
            } catch (BadLocationException bl) {
                throw new CannotRedoException();
            }
        }

        protected int offset;
        protected int length;
        protected String string;
        protected Vector<UndoPosRef> posRefs;
    }

    /** Deshacer un borrado es reponer el texto, y con el las marcas que quedaron adentro. */
    class RemoveUndo extends AbstractUndoableEdit {

        protected RemoveUndo(int offset, String string) {
            super();
            this.offset = offset;
            this.string = string;
            this.length = string.length();
            if (marks != null) {
                posRefs = getPositionsInRange(null, offset, length);
            }
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized (StringContent.this) {
                    insertString(offset, string);
                    if (posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
                    string = null;
                }
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized (StringContent.this) {
                    string = getString(offset, length);
                    if (marks != null) {
                        posRefs = getPositionsInRange(null, offset, length);
                    }
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
                throw new CannotRedoException();
            }
        }

        protected int offset;
        protected int length;
        protected String string;
        protected Vector<UndoPosRef> posRefs;
    }
}
