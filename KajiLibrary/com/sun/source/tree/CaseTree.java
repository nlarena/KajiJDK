package com.sun.source.tree;

import java.util.List;

/**
 * Un `case` de un {@link SwitchTree} o de un {@link SwitchExpressionTree}.
 *
 * <h2>El nodo con mas historia acumulada del paquete</h2>
 *
 * <p>Se le nota en los metodos: hay tres formas de preguntarle por sus etiquetas, y las tres estan
 * porque el `switch` cambio tres veces sin poder romper a quien ya lo recorria.
 *
 * <ul>
 * <li>{@link #getExpression} — la etiqueta unica del `switch` original. {@code null} en el
 *     `default`, y {@code null} tambien cuando hay mas de una.</li>
 * <li>{@link #getExpressions} — la lista, desde que un `case` pudo llevar varias constantes
 *     separadas por coma.</li>
 * <li>{@link #getLabels} — la lista de {@link CaseLabelTree}, desde que una etiqueta pudo ser un
 *     patron y ya no alcanzaba con expresiones.</li>
 * </ul>
 *
 * <p>La ultima es la unica que representa todo lo que hoy se puede escribir; las dos primeras
 * quedan por compatibilidad y contestan lo que pueden. Recorrer un arbol moderno con
 * {@link #getExpression} no da error: da {@code null}, que es peor, y es la trampa de este nodo.
 *
 * <h2>Los dos cuerpos</h2>
 *
 * <p>{@link #getCaseKind} dice si el `case` se escribio con `:` o con `->`, y de eso depende cual de
 * los dos accesos al cuerpo sirve: {@link #getStatements} en la forma vieja, {@link #getBody} en la
 * nueva. El otro da {@code null} — de nuevo, sin avisar.
 */
public interface CaseTree extends Tree {

    /**
     * Como se escribio el `case`.
     *
     * <p>La diferencia no es tipografica: con `:` los `case` caen en cascada al siguiente y con
     * `->` no, que era el problema que la flecha vino a resolver.
     */
    enum CaseKind {

        /** `case X:`, con caida al siguiente. */
        STATEMENT,
        /** `case X ->`, sin caida. */
        RULE
    }

    /** La etiqueta unica, o {@code null} si es el `default` o si hay mas de una. */
    ExpressionTree getExpression();

    /** Las etiquetas como expresiones; vacia en el `default`. Ver la nota de la clase. */
    List<? extends ExpressionTree> getExpressions();

    /** Las etiquetas, que es la forma que representa todo lo que hoy se puede escribir. */
    List<? extends CaseLabelTree> getLabels();

    /** La guarda del `when`, o {@code null} si no tiene. */
    ExpressionTree getGuard();

    /** Las sentencias, en la forma con `:`; {@code null} en la forma con `->`. */
    List<? extends StatementTree> getStatements();

    /** El cuerpo, en la forma con `->`; {@code null} en la forma con `:`. */
    default Tree getBody() {
        return null;
    }

    /** Si el `case` usa `:` o `->`. */
    default CaseKind getCaseKind() {
        return CaseKind.STATEMENT;
    }
}
