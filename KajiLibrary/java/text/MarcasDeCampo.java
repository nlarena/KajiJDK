package java.text;

/**
 * El registro de "qué pedazo del texto es qué campo" que un formateador va llenando mientras
 * escribe.
 *
 * <p>Existe porque las dos formas de preguntar por un campo —{@link FieldPosition}, que devuelve UN
 * rango, y {@link Format#formatToCharacterIterator}, que devuelve TODOS— necesitan exactamente la
 * misma información, y calcularla dos veces es la manera clásica de que las dos respuestas se
 * contradigan. El formateador marca una vez y de acá salen las dos.
 *
 * <p>Cada marca lleva el campo en las dos nomenclaturas: la clave moderna ({@code
 * java.text.NumberFormat.Field.INTEGER}) y el {@code int} viejo ({@code NumberFormat.INTEGER_FIELD},
 * {@code DateFormat.YEAR_FIELD}), con -1 cuando el campo no tiene número — que es el caso de los
 * separadores, que nacieron con la API nueva.
 *
 * <p>Las claves se guardan como {@link AttributedCharacterIterator.Attribute} y no como
 * {@link java.text.Format.Field}, que sería el tipo natural, por el finding #319: nombrar {@code Format$Field}
 * en el descriptor de esta clase rompería la compilación de todo formateador que declare su propia
 * subclase de Field. El supertipo alcanza —lo único que se hace con la clave es compararla por
 * identidad y usarla de atributo— así que el rodeo no cuesta nada.
 *
 * <p>Las marcas pueden solaparse, y eso no es un error: en {@code 1,234} el separador de miles es a
 * la vez parte del campo entero y un campo propio, que es exactamente lo que informa el JDK.
 */
final class MarcasDeCampo {

    private AttributedCharacterIterator.Attribute[] campos;
    private Object[] valores;
    private int[] numeros;
    private int[] desde;
    private int[] hasta;
    private int n;

    MarcasDeCampo() {
        this.campos = new AttributedCharacterIterator.Attribute[8];
        this.valores = new Object[8];
        this.numeros = new int[8];
        this.desde = new int[8];
        this.hasta = new int[8];
        this.n = 0;
    }

    /**
     * Marca un tramo con un valor propio.
     *
     * <p>Casi todos los campos de formato usan la clave como valor —la identidad ya lo dice todo—,
     * pero {@link MessageFormat} no: ahí el valor es el NÚMERO de argumento, porque un mensaje
     * puede tener varios y "es un argumento" no alcanza para saber cuál.
     */
    void marcar(AttributedCharacterIterator.Attribute campo, Object valor, int numero, int d, int h) {
        this.marcarInterno(campo, valor, numero, d, h);
    }

    void marcar(AttributedCharacterIterator.Attribute campo, int numero, int d, int h) {
        this.marcarInterno(campo, campo, numero, d, h);
    }

    private void marcarInterno(AttributedCharacterIterator.Attribute campo, Object valor,
                               int numero, int d, int h) {
        // Un rango vacío no se guarda: no hay texto que señalar, y dejarlo haría que un
        // FieldPosition informara begin == end en un campo que en realidad no se escribió.
        if (h <= d) {
            return;
        }
        if (this.n == this.campos.length) {
            int nuevo = this.n * 2;
            AttributedCharacterIterator.Attribute[] c =
                    new AttributedCharacterIterator.Attribute[nuevo];
            Object[] v = new Object[nuevo];
            int[] nu = new int[nuevo];
            int[] a = new int[nuevo];
            int[] b = new int[nuevo];
            for (int i = 0; i < this.n; i++) {
                c[i] = this.campos[i];
                v[i] = this.valores[i];
                nu[i] = this.numeros[i];
                a[i] = this.desde[i];
                b[i] = this.hasta[i];
            }
            this.campos = c;
            this.valores = v;
            this.numeros = nu;
            this.desde = a;
            this.hasta = b;
        }
        this.campos[this.n] = campo;
        this.valores[this.n] = valor;
        this.numeros[this.n] = numero;
        this.desde[this.n] = d;
        this.hasta[this.n] = h;
        this.n = this.n + 1;
    }

    /**
     * Escribe en el {@code FieldPosition} el rango del primer campo cuyo número coincida.
     *
     * <p>Sólo por número: mientras {@code FieldPosition} no pueda llevar un {@code java.text.Format.Field}
     * (ver el comentario de esa clase y el finding #319), no hay otra pregunta que hacerle.
     */
    void aplicar(FieldPosition pos) {
        if (pos == null) {
            return;
        }
        for (int i = 0; i < this.n; i++) {
            if (this.numeros[i] >= 0 && this.numeros[i] == pos.getField()) {
                pos.setBeginIndex(this.desde[i]);
                pos.setEndIndex(this.hasta[i]);
                return;
            }
        }
    }

    /**
     * El texto con las marcas convertidas en atributos.
     *
     * <p>El valor por omisión de cada atributo es el propio campo, que es lo que hace el JDK: para
     * un campo de formato la identidad ya es toda la información. Sólo quien tenga algo más que
     * decir —el número de argumento de un mensaje— pasa un valor propio al marcar.
     */
    AttributedCharacterIterator iterador(String texto) {
        AttributedString as = new AttributedString(texto);
        for (int i = 0; i < this.n; i++) {
            as.addAttribute(this.campos[i], this.valores[i], this.desde[i], this.hasta[i]);
        }
        return as.getIterator();
    }
}
