package java.text;

/**
 * The register of "which piece of the text is which field" that a formatter fills in as it
 * writes.
 *
 * <p>It exists because the two ways of asking about a field --{@link FieldPosition}, which returns
 * ONE range, and {@link Format#formatToCharacterIterator}, which returns ALL of them-- need exactly
 * the same information, and computing it twice is the classic way for the two answers to contradict
 * each other. The formatter marks once and both come out of here.
 *
 * <p>Each mark carries the field in both nomenclatures: the modern key ({@code
 * java.text.NumberFormat.Field.INTEGER}) and the old {@code int} ({@code
 * NumberFormat.INTEGER_FIELD}, {@code DateFormat.YEAR_FIELD}), with -1 when the field has no number
 * -- which is the separators' case, since they were born with the new API.
 *
 * <p>The keys are kept as {@link AttributedCharacterIterator.Attribute} and not as
 * {@link java.text.Format.Field}, which would be the natural type, because of finding #319: naming
 * {@code Format$Field} in this class's descriptor would break the compilation of every formatter
 * that declares its own Field subclass. The supertype is enough --the only things done with the key
 * are comparing it by identity and using it as an attribute-- so the detour costs nothing.
 *
 * <p>The marks can overlap, and that is not an error: in {@code 1,234} the thousands separator is
 * at once part of the integer field and a field of its own, which is exactly what the JDK
 * reports.
 */
final class FieldMarks {

    private AttributedCharacterIterator.Attribute[] fields;
    private Object[] values;
    private int[] numbers;
    private int[] from;
    private int[] to;
    private int n;

    FieldMarks() {
        this.fields = new AttributedCharacterIterator.Attribute[8];
        this.values = new Object[8];
        this.numbers = new int[8];
        this.from = new int[8];
        this.to = new int[8];
        this.n = 0;
    }

    /**
     * It marks a stretch with a value of its own.
     *
     * <p>Nearly every format field uses the key as the value --the identity says it all-- but
     * {@link MessageFormat} does not: there the value is the argument's NUMBER, because a message can
     * have several and "it is an argument" is not enough to know which.
     */
    void mark(AttributedCharacterIterator.Attribute field, Object value, int number, int d, int h) {
        this.markInternal(field, value, number, d, h);
    }

    void mark(AttributedCharacterIterator.Attribute field, int number, int d, int h) {
        this.markInternal(field, field, number, d, h);
    }

    private void markInternal(AttributedCharacterIterator.Attribute field, Object value,
                               int number, int d, int h) {
        // An empty range is not kept: there is no text to point at, and leaving it would make a
        // FieldPosition report begin == end on a field that was not actually written.
        if (h <= d) {
            return;
        }
        if (this.n == this.fields.length) {
            int raised = this.n * 2;
            AttributedCharacterIterator.Attribute[] c =
                    new AttributedCharacterIterator.Attribute[raised];
            Object[] v = new Object[raised];
            int[] nu = new int[raised];
            int[] a = new int[raised];
            int[] b = new int[raised];
            for (int i = 0; i < this.n; i++) {
                c[i] = this.fields[i];
                v[i] = this.values[i];
                nu[i] = this.numbers[i];
                a[i] = this.from[i];
                b[i] = this.to[i];
            }
            this.fields = c;
            this.values = v;
            this.numbers = nu;
            this.from = a;
            this.to = b;
        }
        this.fields[this.n] = field;
        this.values[this.n] = value;
        this.numbers[this.n] = number;
        this.from[this.n] = d;
        this.to[this.n] = h;
        this.n = this.n + 1;
    }

    /**
     * It writes into the {@code FieldPosition} the range of the first field whose number matches.
     *
     * <p>By number only: while {@code FieldPosition} cannot carry a {@code java.text.Format.Field}
     * (see that class's comment and finding #319), there is no other question to ask it.
     */
    void apply(FieldPosition pos) {
        if (pos == null) {
            return;
        }
        for (int i = 0; i < this.n; i++) {
            if (this.numbers[i] >= 0 && this.numbers[i] == pos.getField()) {
                pos.setBeginIndex(this.from[i]);
                pos.setEndIndex(this.to[i]);
                return;
            }
        }
    }

    /**
     * The text with the marks turned into attributes.
     *
     * <p>Each attribute's default value is the field itself, which is what the JDK does: for a format
     * field the identity is already all the information there is. Only whoever has something more to
     * say --a message's argument number-- passes a value of their own when marking.
     */
    AttributedCharacterIterator iterator(String text) {
        AttributedString as = new AttributedString(text);
        for (int i = 0; i < this.n; i++) {
            as.addAttribute(this.fields[i], this.values[i], this.from[i], this.to[i]);
        }
        return as.getIterator();
    }
}
