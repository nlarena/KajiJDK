package javax.xml.stream.events;

import java.util.List;

/**
 * KajiLibrary's javax.xml.stream.events.DTD -- la declaracion de tipo de documento, entera.
 *
 * <h2>Un evento con dos caras</h2>
 *
 * <p>{@link #getDocumentTypeDeclaration()} devuelve el texto crudo: todo lo que hay entre
 * {@code <!DOCTYPE} y el {@code >} que lo cierra, subconjunto interno incluido. Es lo que hace
 * falta para reescribir el documento sin perder nada, y es lo unico que un parser que no interpreta
 * el DTD puede dar honestamente.
 *
 * <p>{@link #getEntities()} y {@link #getNotations()} son la otra cara: el DTD ya <b>interpretado</b>,
 * como listas de {@link EntityDeclaration} y {@link NotationDeclaration}. Un parser que no lee el
 * subconjunto interno no puede armarlas, y la especificacion lo previo: en ese caso los dos
 * devuelven null.
 *
 * <p>{@link #getProcessedDTD()} es el escape para las implementaciones que tienen una
 * representacion propia del DTD --un grafo de modelos de contenido, digamos-- y quieren
 * exponerla. Devuelve {@link Object} porque no hay ningun tipo comun que prometer, asi que solo
 * sirve a quien sabe con que implementacion esta hablando; devolver null es la respuesta correcta
 * para las demas.
 */
public interface DTD extends XMLEvent {

    /**
     * El texto completo de la declaracion, tal como estaba escrita.
     *
     * @return la declaracion cruda; nunca null
     */
    String getDocumentTypeDeclaration();

    /**
     * El DTD en la representacion interna de la implementacion, si la hay.
     *
     * @return la representacion propia, o null si la implementacion no expone ninguna
     */
    Object getProcessedDTD();

    /**
     * Las entidades declaradas en el subconjunto interno.
     *
     * @return la lista, o null si la implementacion no interpreta el DTD
     */
    List<EntityDeclaration> getEntities();

    /**
     * Las notaciones declaradas en el subconjunto interno.
     *
     * @return la lista, o null si la implementacion no interpreta el DTD
     */
    List<NotationDeclaration> getNotations();
}
