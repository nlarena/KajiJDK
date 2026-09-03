package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EntityReference -- una {@code &entidad;} que quedo sin
 * expandir.
 *
 * <h2>Cuando aparece, que es casi nunca</h2>
 *
 * <p>Por omision un lector de StAX expande las entidades: el {@code &saludo;} se convierte en el
 * texto que la entidad declaraba y llega como {@link Characters}. Este evento existe solo si se
 * apago esa expansion con {@link javax.xml.stream.XMLInputFactory#IS_REPLACING_ENTITY_REFERENCES}.
 *
 * <p>Apagarla sirve para dos cosas concretas: reescribir un documento conservando las referencias
 * tal cual estaban --expandirlas es una perdida irreversible-- y no expandir entidades de
 * documentos que no son de confianza, que es de donde vienen los ataques de expansion
 * exponencial.
 *
 * <p>Las cinco entidades predefinidas de XML --{@code &lt;}, {@code &gt;}, {@code &amp;},
 * {@code &quot;}, {@code &apos;}-- se resuelven <b>siempre</b> y nunca llegan como este evento: no
 * son entidades declaradas sino sintaxis del lenguaje.
 *
 * <p>{@link #getDeclaration()} devuelve la declaracion que le corresponde, lo que solo es posible
 * si el parser leyo el DTD. Sin DTD no hay declaracion que devolver.
 */
public interface EntityReference extends XMLEvent {

    /**
     * La declaracion de la entidad referenciada.
     *
     * @return la declaracion, o null si el parser no leyo el DTD que la declara
     */
    EntityDeclaration getDeclaration();

    /**
     * El nombre de la entidad, sin el {@code &} ni el {@code ;}.
     *
     * @return el nombre; nunca null
     */
    String getName();
}
