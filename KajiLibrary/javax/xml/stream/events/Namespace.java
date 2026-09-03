package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Namespace -- una declaracion {@code xmlns} vista como
 * evento.
 *
 * <h2>Extiende {@link Attribute}, y tiene sentido que asi sea</h2>
 *
 * <p>En el texto del documento una declaracion de espacio de nombres <b>es</b> un atributo:
 * {@code xmlns:a="http://x"} se escribe igual que cualquier otro par nombre-valor. La
 * especificacion de Namespaces la saca despues del conjunto de atributos y le da otro significado,
 * pero la sintaxis es la misma, y de ahi que el tipo herede.
 *
 * <p>La herencia deja los accesores de {@link Attribute} disponibles y con un significado util:
 * {@link Attribute#getValue()} devuelve el URI --lo mismo que {@link #getNamespaceURI()}-- y
 * {@link Attribute#getName()} el nombre tal como estaba escrito, o sea
 * <code>{http://www.w3.org/2000/xmlns/}a</code> para una declaracion con prefijo y
 * {@code xmlns} pelado para la declaracion por omision.
 *
 * <h2>La declaracion por omision y la de anulacion</h2>
 *
 * <p>{@code xmlns="http://x"} pone el espacio de nombres por omision para el elemento y sus
 * descendientes; {@code xmlns=""} lo <b>anula</b>, y vuelve a "sin espacio de nombres". Los dos
 * casos contestan true a {@link #isDefaultNamespaceDeclaration()} y se distinguen porque el segundo
 * tiene {@link #getNamespaceURI()} vacio.
 *
 * <p>{@link #getPrefix()} devuelve la cadena vacia en los dos: el prefijo de la declaracion por
 * omision es, precisamente, ninguno.
 */
public interface Namespace extends Attribute {

    /**
     * El prefijo que se esta declarando, o la cadena vacia para la declaracion por omision.
     *
     * @return el prefijo; nunca null
     */
    String getPrefix();

    /**
     * El URI al que queda asociado el prefijo.
     *
     * @return el espacio de nombres; la cadena vacia si la declaracion anula el de por omision
     */
    String getNamespaceURI();

    /**
     * Si esta declaracion es la del espacio de nombres por omision, o sea {@code xmlns=...}.
     *
     * @return true si no declara un prefijo
     */
    boolean isDefaultNamespaceDeclaration();
}
