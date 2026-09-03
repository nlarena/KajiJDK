package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.ChoiceCallback -- pide elegir de una lista.
 *
 * <p>Sirve para lo que no se puede escribir a mano: cual de los certificados del almacen usar, con
 * cual de tres dominios autenticarse. La lista la arma el que pregunta, y el que contesta devuelve
 * <b>indices</b>, no textos: asi no hay que volver a buscar cual eligio.
 *
 * <h2>La seleccion multiple se decide al construir</h2>
 *
 * <p>{@code allowMultipleSelections} es final y no un ruego: si es false,
 * {@link #setSelectedIndexes} lanza {@code UnsupportedOperationException} en vez de quedarse con el
 * primero. Quien pregunto dijo "una sola" y quedarse con una de varias seria elegir por el.
 *
 * <p>{@link #setSelectedIndex} en cambio anda siempre -- una sola eleccion cabe en las dos formas --
 * y deja un arreglo de un elemento.
 */
public class ChoiceCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -3975664071579892167L;

    private final String prompt;
    private final String[] choices;
    private final int defaultChoice;
    private final boolean multipleSelectionsAllowed;
    private int[] selections;

    /**
     * @param defaultChoice el indice sugerido; tiene que caer adentro de la lista
     * @throws IllegalArgumentException si el prompt es null o vacio, si la lista esta vacia, si
     *     alguna opcion es null o vacia, o si el default cae fuera
     */
    public ChoiceCallback(String prompt, String[] choices, int defaultChoice,
            boolean multipleSelectionsAllowed) {
        if (prompt == null || prompt.length() == 0 || choices == null || choices.length == 0
                || defaultChoice < 0 || defaultChoice >= choices.length) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        while (i < choices.length) {
            // Una opcion vacia se veria como una linea en blanco en la lista: el usuario no sabria
            // que esta eligiendo.
            if (choices[i] == null || choices[i].length() == 0) {
                throw new IllegalArgumentException();
            }
            i = i + 1;
        }
        this.prompt = prompt;
        this.choices = copy(choices);
        this.defaultChoice = defaultChoice;
        this.multipleSelectionsAllowed = multipleSelectionsAllowed;
    }

    public String getPrompt() {
        return this.prompt;
    }

    /** Las opciones. Copia: tocar lo que sale de aca no cambia la lista que se pregunto. */
    public String[] getChoices() {
        return copy(this.choices);
    }

    public int getDefaultChoice() {
        return this.defaultChoice;
    }

    public boolean allowMultipleSelections() {
        return this.multipleSelectionsAllowed;
    }

    /** Contesta con una sola opcion. Anda con seleccion multiple o sin ella. */
    public void setSelectedIndex(int selection) {
        this.selections = new int[] {selection};
    }

    /**
     * Contesta con varias.
     *
     * @throws UnsupportedOperationException si se construyo con seleccion simple. Ver la nota de la
     *     clase: quedarse con una de varias seria elegir por quien pregunto
     */
    public void setSelectedIndexes(int[] selections) {
        if (!this.multipleSelectionsAllowed) {
            throw new UnsupportedOperationException();
        }
        this.selections = selections == null ? null : copyInts(selections);
    }

    /** Los indices elegidos, o null si todavia nadie contesto. */
    public int[] getSelectedIndexes() {
        return this.selections == null ? null : copyInts(this.selections);
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static int[] copyInts(int[] a) {
        int[] c = new int[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
