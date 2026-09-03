package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EntityDeclaration -- un {@code <!ENTITY ...>} del DTD.
 *
 * <h2>Las tres formas de entidad, y como se distinguen aca</h2>
 *
 * <p>Los seis accesores parecen mucho para algo tan chico, pero cubren tres cosas distintas que la
 * sintaxis del DTD junta bajo la misma palabra:
 *
 * <ul>
 *   <li>una entidad <b>interna</b> --{@code <!ENTITY saludo "hola">}-- tiene
 *       {@link #getReplacementText()} y nada mas; los identificadores son null;
 *   <li>una entidad <b>externa analizada</b> --{@code <!ENTITY cap SYSTEM "cap1.xml">}-- tiene
 *       {@link #getSystemId()} y quiza {@link #getPublicId()}, y su texto de reemplazo hay que ir a
 *       buscarlo;
 *   <li>una entidad <b>externa sin analizar</b> --la que termina en {@code NDATA jpeg}-- ademas
 *       tiene {@link #getNotationName()}, que es lo que la marca como datos que XML no va a mirar.
 * </ul>
 *
 * <p>O sea que {@link #getNotationName()} distinto de null es el discriminante duro: esa entidad no
 * se expande nunca, solo se referencia desde un atributo {@code ENTITY}.
 *
 * <p>{@link #getBaseURI()} existe porque un identificador de sistema puede ser relativo, y relativo
 * <b>a donde estaba escrita la declaracion</b>, no al documento: un DTD externo incluido desde otro
 * directorio cambia la base. Sin esto, resolver el URI da un archivo equivocado.
 */
public interface EntityDeclaration extends XMLEvent {

    /**
     * El identificador publico declarado.
     *
     * @return el identificador publico, o null si la entidad no tiene o es interna
     */
    String getPublicId();

    /**
     * El identificador de sistema declarado, quiza relativo a {@link #getBaseURI()}.
     *
     * @return el identificador de sistema, o null si la entidad es interna
     */
    String getSystemId();

    /**
     * El nombre con que se referencia la entidad, sin el {@code &} ni el {@code ;}.
     *
     * @return el nombre; nunca null
     */
    String getName();

    /**
     * La notacion de una entidad externa sin analizar.
     *
     * @return el nombre de la notacion, o null si la entidad se analiza
     */
    String getNotationName();

    /**
     * El texto que reemplaza a la entidad, para las internas.
     *
     * @return el texto de reemplazo, o null si la entidad es externa
     */
    String getReplacementText();

    /**
     * La base contra la que resolver {@link #getSystemId()} si es relativo.
     *
     * @return el URI base, o null si no se conoce
     */
    String getBaseURI();
}
