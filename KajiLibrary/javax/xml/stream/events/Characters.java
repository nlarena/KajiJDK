package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Characters -- el texto que hay entre las etiquetas.
 *
 * <h2>Un tipo, tres tipos de evento</h2>
 *
 * <p>{@link XMLEvent#getEventType()} de un {@code Characters} puede devolver tres constantes
 * distintas y los tres son este mismo tipo:
 *
 * <ul>
 *   <li>{@link javax.xml.stream.XMLStreamConstants#CHARACTERS}, el texto normal;
 *   <li>{@link javax.xml.stream.XMLStreamConstants#CDATA}, el texto que venia en una seccion
 *       {@code <![CDATA[...]]>};
 *   <li>{@link javax.xml.stream.XMLStreamConstants#SPACE}, el espacio en blanco que el DTD declara
 *       ignorable.
 * </ul>
 *
 * <p>{@link #getData()} devuelve lo mismo en los tres casos --el contenido, ya sin la envoltura de
 * CDATA y con las entidades resueltas--; lo que cambia es de donde salio. La distincion sobrevive
 * porque quien reescribe el documento quiere volver a poner el {@code CDATA} donde estaba: perderlo
 * no cambia el significado pero si el texto, y hay pipelines que comparan textos.
 *
 * <h2>{@link #isWhiteSpace()} y {@link #isIgnorableWhiteSpace()} no son lo mismo</h2>
 *
 * <p>La primera es una pregunta sobre los caracteres: mira el contenido y contesta si son todos
 * espacio. La segunda es una pregunta sobre el <b>esquema</b>: contesta si el DTD dice que en ese
 * lugar solo puede haber elementos, con lo cual el espacio que aparezca es sangria y no datos.
 *
 * <p>La confusion sale cara en el sentido de que solo la segunda autoriza a tirar el evento. Sin
 * DTD no se puede saber si el espacio entre dos elementos es sangria o es el contenido de un campo
 * de texto que quedo en blanco, asi que un parser que no valida --como el de esta biblioteca--
 * contesta siempre false a {@link #isIgnorableWhiteSpace()}: no lo sabe, y decir que si seria
 * autorizar a perder datos.
 */
public interface Characters extends XMLEvent {

    /**
     * El texto, con las referencias a entidad ya resueltas.
     *
     * @return el contenido; nunca null
     */
    String getData();

    /**
     * Si el contenido son todos caracteres de espacio en blanco.
     *
     * <p>Pregunta sobre los caracteres, no sobre el esquema; ver el encabezado.
     *
     * @return true si {@link #getData()} es solo espacio
     */
    boolean isWhiteSpace();

    /**
     * Si venia dentro de una seccion {@code <![CDATA[...]]>}.
     *
     * @return true si era CDATA
     */
    boolean isCData();

    /**
     * Si el esquema declara que este espacio es sangria y se puede descartar.
     *
     * <p>Solo un parser que lee el DTD puede contestar que si; ver el encabezado.
     *
     * @return true si es espacio ignorable segun el esquema
     */
    boolean isIgnorableWhiteSpace();
}
