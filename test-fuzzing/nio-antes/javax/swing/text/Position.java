package javax.swing.text;

/**
 * A mark in the document that <strong>moves by itself</strong> when the text changes.
 *
 * <h2>Why an {@code int} is not enough</h2>
 *
 * <p>An offset is a number, and a number knows nothing about the document: if somebody inserts
 * ten characters further up, the number goes on pointing at the same place of the <em>old
 * text</em>, which is no longer where what mattered was. A {@code Position} is maintained by the
 * document and shifts with the insertions and the removals.
 *
 * <p>It is what makes a cursor, a selection or a bookmark survive somebody editing above them.
 * The whole of Swing's text model rests on this distinction.
 */
public interface Position {

    /** Where the mark is now. */
    int getOffset();

    /**
     * Which side of the inserted text a mark stays on.
     *
     * <p>The question is not rhetorical: if something is inserted exactly at the mark's position,
     * there is no obvious answer as to whether the mark ends up before or after what was inserted.
     * Both behaviours are needed -- a cursor wants to end up after what it has just typed, and the
     * end of a highlight wants to end up before.
     */
    public static final class Bias {

        /** The mark ends up <em>after</em> what was inserted. */
        public static final Bias Forward = new Bias("Forward");

        /** The mark ends up <em>before</em> what was inserted. */
        public static final Bias Backward = new Bias("Backward");

        private String name;

        private Bias(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }
}
