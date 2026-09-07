package javax.swing.text;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * El contenido de siempre: un arreglo con un hueco movil.
 *
 * <h2>El hueco</h2>
 *
 * <p>El arreglo tiene un agujero adentro, y el texto son los dos pedazos de los costados. Insertar
 * donde esta el hueco es escribir un caracter y achicar el hueco: no se mueve nada. Insertar en
 * otro lado es mover el hueco hasta ahi primero, y eso si copia, pero solo una vez por lugar. Como
 * quien escribe lo hace en un lugar durante un rato, en la practica casi nunca se copia.
 *
 * <p>De ahi el nombre de cada campo: {@code g0} y {@code g1} son los bordes del hueco. Todo indice
 * que da esta clase hacia afuera es <em>logico</em> —cuenta caracteres, no posiciones del
 * arreglo—, y {@link #shiftGap} es lo unico que traduce.
 *
 * <h2>Las marcas</h2>
 *
 * <p>Las {@link Position} se guardan en una lista ordenada de marcas, cada una con su indice
 * <em>del arreglo</em>. Guardarlas asi es lo que hace que mover el hueco no tenga que tocarlas:
 * solo se acomodan las que quedan dentro del tramo movido. Las que ya no usa nadie se descartan
 * cuando la lista crece.
 */
public class GapContent implements AbstractDocument.Content, Serializable {

    /** Cuanto crece el arreglo cuando se llena. */
    static final int GROWTH_SIZE = 1024 * 512;

    private char[] array;
    private int g0;
    private int g1;

    private transient Vector<MarkData> marks;
    private transient MarkVector unusedMarks;

    /** Un contenido con lugar para diez caracteres. */
    public GapContent() {
        this(10);
    }

    /** Un contenido con ese lugar inicial; siempre arranca con un fin de linea. */
    public GapContent(int initialLength) {
        if (initialLength < 2) {
            initialLength = 2;
        }
        array = (char[]) allocateArray(initialLength);
        g0 = 1;
        g1 = initialLength;
        array[0] = '\n';
        marks = new Vector<MarkData>();
    }

    /** El arreglo donde se guardan los caracteres; una subclase podria usar otro tipo. */
    protected Object allocateArray(int len) {
        return new char[len];
    }

    protected int getArrayLength() {
        return array.length;
    }

    /** Agranda el arreglo a ese tamano, dejando el hueco donde estaba. */
    void resize(int nsize) {
        char[] narray = new char[nsize];
        int part1 = g0;
        int part2 = array.length - g1;
        System.arraycopy(array, 0, narray, 0, part1);
        System.arraycopy(array, g1, narray, nsize - part2, part2);
        g1 = nsize - part2;
        array = narray;
    }

    /** El largo logico: el arreglo menos el hueco. */
    public int length() {
        return array.length - (g1 - g0);
    }

    public UndoableEdit insertString(int where, String str) throws BadLocationException {
        if (where > length() || where < 0) {
            throw new BadLocationException("Invalid insert", length());
        }
        char[] chars = str.toCharArray();
        replace(where, 0, chars, chars.length);
        return new InsertUndo(where, str.length());
    }

    public UndoableEdit remove(int where, int nitems) throws BadLocationException {
        if (where + nitems >= length()) {
            throw new BadLocationException("Invalid remove", length() + 1);
        }
        String removedString = getString(where, nitems);
        UndoableEdit edit = new RemoveUndo(where, removedString);
        replace(where, nitems, null, 0);
        return edit;
    }

    public String getString(int where, int len) throws BadLocationException {
        Segment s = new Segment();
        getChars(where, len, s);
        return new String(s.array, s.offset, s.count);
    }

    /**
     * El texto en el segmento, sin copiar si se puede.
     *
     * <p>Si el tramo pedido cae entero de un lado del hueco, el segmento apunta al arreglo. Si lo
     * cruza, hay que copiar: no hay forma de ver dos pedazos separados como uno solo.
     */
    public void getChars(int where, int len, Segment chars) throws BadLocationException {
        int end = where + len;
        if (where < 0 || end < 0) {
            throw new BadLocationException("Invalid location", -1);
        }
        if (end > length() || where > length()) {
            throw new BadLocationException("Invalid location", length() + 1);
        }
        if ((where + len) <= g0) {
            chars.array = array;
            chars.offset = where;
        } else if (where >= g0) {
            chars.array = array;
            chars.offset = g1 + where - g0;
        } else {
            int before = g0 - where;
            char[] copia = new char[len];
            System.arraycopy(array, where, copia, 0, before);
            System.arraycopy(array, g1, copia, before, len - before);
            chars.array = copia;
            chars.offset = 0;
        }
        chars.count = len;
    }

    public Position createPosition(int offset) throws BadLocationException {
        if (offset < 0 || offset > length()) {
            throw new BadLocationException("Invalid position offset", offset);
        }
        int g0 = this.g0;
        int g1 = this.g1;
        int index = (offset < g0) ? offset : (offset + (g1 - g0));
        removeUnusedMarks();
        int sortIndex = findSortIndex(index);
        MarkData m = new MarkData(index);
        marks.insertElementAt(m, sortIndex);
        return new StickyPosition(m);
    }

    /** Reemplaza un tramo por otro; es la unica escritura, y la que mueve el hueco. */
    void replace(int position, int rmSize, char[] addItems, int addSize) {
        if (rmSize > 0) {
            borrar(position, rmSize);
        }
        if (addSize > 0) {
            insertar(position, addItems, addSize);
        }
    }

    /**
     * Mete caracteres en esa posicion.
     *
     * <p>Lleva el hueco hasta ahi y escribe adentro. Una marca que estaba justo en la posicion se
     * queda antes o pasa despues segun de que lado del hueco tenga su indice, que es como esta
     * clase guarda "esta marca se pega a la izquierda o a la derecha".
     */
    private void insertar(int where, char[] chars, int len) {
        if (len > (g1 - g0)) {
            shiftEnd(getNewArraySize(array.length + len - (g1 - g0)));
        }
        shiftGap(where);
        System.arraycopy(chars, 0, array, g0, len);
        g0 = g0 + len;
    }

    /** Saca caracteres: lleva el hueco hasta ahi y lo agranda por encima de ellos. */
    private void borrar(int where, int nitems) {
        shiftGap(where);
        shiftGapEndUp(g1 + nitems);
    }

    /** Cuanto pedir cuando hay que agrandar: el doble, o el salto grande si ya es grande. */
    int getNewArraySize(int reqSize) {
        if (reqSize < GROWTH_SIZE) {
            return Math.max(2 * reqSize, 4);
        }
        return reqSize + GROWTH_SIZE;
    }

    /** Agranda el arreglo; las marcas de despues del hueco se corren con el. */
    protected void shiftEnd(int newSize) {
        int oldGapEnd = g1;
        resize(newSize);
        int dg = g1 - oldGapEnd;
        int adjustIndex = findMarkAdjustIndex(oldGapEnd);
        int n = marks.size();
        for (int i = adjustIndex; i < n; i++) {
            marks.elementAt(i).index = marks.elementAt(i).index + dg;
        }
    }

    /**
     * Mueve el hueco a esa posicion logica.
     *
     * <p>Es la unica copia de caracteres que hace esta clase, y la razon de que insertar en el
     * mismo lugar muchas veces seguidas sea barato: la primera mueve el hueco y las demas no.
     */
    protected void shiftGap(int newGapStart) {
        if (newGapStart == g0) {
            return;
        }
        int oldGapStart = g0;
        int dg = newGapStart - oldGapStart;
        int oldGapEnd = g1;
        int newGapEnd = oldGapEnd + dg;
        int gapSize = oldGapEnd - oldGapStart;

        if (dg > 0) {
            System.arraycopy(array, oldGapEnd, array, oldGapStart, dg);
        } else {
            System.arraycopy(array, newGapStart, array, newGapEnd, -dg);
        }
        g0 = newGapStart;
        g1 = newGapEnd;

        // Solo se tocan las marcas del tramo que el hueco cruzo: las demas siguen valiendo.
        if (dg > 0) {
            int adjustIndex = findMarkAdjustIndex(oldGapStart);
            int n = marks.size();
            for (int i = adjustIndex; i < n; i++) {
                MarkData mark = marks.elementAt(i);
                if (mark.index >= newGapEnd) {
                    break;
                }
                mark.index = mark.index - gapSize;
            }
        } else if (dg < 0) {
            int adjustIndex = findMarkAdjustIndex(newGapStart);
            int n = marks.size();
            for (int i = adjustIndex; i < n; i++) {
                MarkData mark = marks.elementAt(i);
                if (mark.index >= oldGapEnd) {
                    break;
                }
                mark.index = mark.index + gapSize;
            }
        }
        resetMarksAtZero();
    }

    /**
     * Las marcas del principio se pegan a la izquierda.
     *
     * <p>Con el hueco al principio, toda marca que caiga dentro esta logicamente en cero, y hay
     * que fijarla ahi: si no, insertar al principio del documento empujaria la marca de la
     * posicion cero, que por definicion no se mueve.
     */
    protected void resetMarksAtZero() {
        if (marks == null || g0 != 0) {
            return;
        }
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.index <= g1) {
                mark.index = 0;
            }
        }
    }

    protected void shiftGapStartDown(int newGapStart) {
        shiftGap(newGapStart);
    }

    /** Agranda el hueco hacia adelante; las marcas que se come quedan en su borde. */
    protected void shiftGapEndUp(int newGapEnd) {
        int adjustIndex = findMarkAdjustIndex(g1);
        int n = marks.size();
        for (int i = adjustIndex; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.index >= newGapEnd) {
                break;
            }
            mark.index = newGapEnd;
        }
        g1 = newGapEnd;
    }

    /** Compara dos marcas por su indice del arreglo. */
    final int compare(MarkData o1, MarkData o2) {
        if (o1.index < o2.index) {
            return -1;
        } else if (o1.index > o2.index) {
            return 1;
        }
        return 0;
    }

    /** Donde arranca el tramo de marcas que hay que acomodar a partir de ese indice. */
    final int findMarkAdjustIndex(int searchIndex) {
        int index = findSortIndex(searchIndex);
        // Todas las marcas con el mismo indice tienen que quedar del mismo lado.
        for (int i = index - 1; i >= 0; i--) {
            if (marks.elementAt(i).index != searchIndex) {
                break;
            }
            index = i;
        }
        return index;
    }

    /** El lugar donde va una marca con ese indice, por busqueda binaria. */
    final int findSortIndex(int index) {
        int lower = 0;
        int upper = marks.size() - 1;
        int mid = 0;

        if (upper == -1) {
            return 0;
        }

        while (lower <= upper) {
            mid = lower + ((upper - lower) / 2);
            int i = marks.elementAt(mid).index;
            if (i == index) {
                return mid;
            } else if (i < index) {
                lower = mid + 1;
            } else {
                upper = mid - 1;
            }
        }
        if (marks.elementAt(mid).index < index) {
            mid = mid + 1;
        }
        return mid;
    }

    /** Saca de la lista las marcas que ya no usa nadie. */
    final void removeUnusedMarks() {
        int n = marks.size();
        MarkVector cleaned = new MarkVector(n);
        for (int i = 0; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.refCount > 0) {
                cleaned.addElement(mark);
            }
        }
        if (cleaned.size() != n) {
            marks = cleaned;
        }
    }

    /** Las marcas que caen en ese rango, para reponerlas al deshacer. */
    protected Vector<UndoPosRef> getPositionsInRange(Vector<UndoPosRef> v, int offset,
            int length) {
        int endOffset = offset + length;
        int startIndex;
        int endIndex;
        int g0 = this.g0;
        int g1 = this.g1;

        if (offset < g0) {
            if (offset == 0) {
                startIndex = 0;
            } else {
                startIndex = findMarkAdjustIndex(offset);
            }
            if (endOffset >= g0) {
                endIndex = findMarkAdjustIndex(endOffset + (g1 - g0));
            } else {
                endIndex = findMarkAdjustIndex(endOffset);
            }
        } else {
            startIndex = findMarkAdjustIndex(offset + (g1 - g0));
            endIndex = findMarkAdjustIndex(endOffset + (g1 - g0));
        }

        Vector<UndoPosRef> placeIn = (v == null) ? new Vector<UndoPosRef>(Math.max(1,
                endIndex - startIndex)) : v;
        for (int counter = startIndex; counter < endIndex; counter++) {
            placeIn.addElement(new UndoPosRef(marks.elementAt(counter)));
        }
        return placeIn;
    }

    /** Vuelve a poner cada marca donde estaba. */
    protected void updateUndoPositions(Vector<UndoPosRef> positions, int offset, int length) {
        for (int counter = positions.size() - 1; counter >= 0; counter--) {
            UndoPosRef ref = positions.elementAt(counter);
            ref.resetLocation(offset, length);
        }
    }

    /** Traduce un indice del arreglo a uno logico. */
    private int indiceLogico(int arrayIndex) {
        if (arrayIndex < g0) {
            return arrayIndex;
        }
        return arrayIndex - (g1 - g0);
    }

    /** Una lista de marcas; existe para poder crearla con capacidad. */
    static class MarkVector extends Vector<MarkData> {

        MarkVector(int size) {
            super(size);
        }
    }

    /**
     * Una marca: un indice del arreglo con cuenta de referencias.
     *
     * <p>Estatica y no interna por el hallazgo #508; el contenido va por parametro donde hace
     * falta.
     */
    static final class MarkData {

        MarkData(int index) {
            this.index = index;
        }

        int index;
        int refCount;
    }

    /** Una posicion que sigue al texto; traduce el indice del arreglo a uno logico. */
    final class StickyPosition implements Position {

        StickyPosition(MarkData mark) {
            this.mark = mark;
            mark.refCount = mark.refCount + 1;
        }

        public int getOffset() {
            return indiceLogico(mark.index);
        }

        protected void finalize() throws Throwable {
            mark.refCount = mark.refCount - 1;
        }

        public String toString() {
            return Integer.toString(getOffset());
        }

        MarkData mark;
    }

    /** Donde estaba una marca antes de una edicion; estatica por #508. */
    static final class UndoPosRef {

        UndoPosRef(MarkData rec) {
            this.rec = rec;
            this.undoLocation = rec.index;
        }

        protected void resetLocation(int startOffset, int length) {
            rec.index = undoLocation;
        }

        protected MarkData rec;
        protected int undoLocation;
    }

    /** Deshacer una insercion es borrar lo insertado. */
    class InsertUndo extends AbstractUndoableEdit {

        protected InsertUndo(int offset, int length) {
            super();
            this.offset = offset;
            this.length = length;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                string = getString(offset, length);
                posRefs = getPositionsInRange(null, offset, length);
                remove(offset, length);
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                insertString(offset, string);
                string = null;
                if (posRefs != null) {
                    updateUndoPositions(posRefs, offset, length);
                    posRefs = null;
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

    /** Deshacer un borrado es reponer el texto y las marcas que quedaron adentro. */
    class RemoveUndo extends AbstractUndoableEdit {

        protected RemoveUndo(int offset, String string) {
            super();
            this.offset = offset;
            this.string = string;
            this.length = string.length();
            posRefs = getPositionsInRange(null, offset, length);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                insertString(offset, string);
                if (posRefs != null) {
                    updateUndoPositions(posRefs, offset, length);
                    posRefs = null;
                }
                string = null;
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                string = getString(offset, length);
                posRefs = getPositionsInRange(null, offset, length);
                remove(offset, length);
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
