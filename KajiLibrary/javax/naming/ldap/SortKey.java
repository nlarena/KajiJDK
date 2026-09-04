package javax.naming.ldap;

/**
 * Por que atributo ordenar, en que sentido y con que regla de comparacion.
 *
 * <h2>Por que hace falta nombrar la regla</h2>
 *
 * <p>Porque comparar dos cadenas no tiene una sola respuesta correcta. En LDAP la comparacion la
 * define una <em>matching rule</em>, y para el mismo atributo puede haber varias: una que distingue
 * mayusculas y otra que no, una que ignora espacios de mas y otra que no.
 *
 * <p>{@code null} en {@link #getMatchingRuleID} deja elegir al servidor la regla por omision del
 * atributo, que es lo razonable casi siempre y lo que hace el constructor de un argumento.
 */
public class SortKey {

    private final String attrID;
    private final boolean reverseOrder;
    private final String matchingRuleID;

    /** Ascendente, con la regla por omision del atributo. */
    public SortKey(String attrID) {
        this.attrID = attrID;
        this.reverseOrder = false;
        this.matchingRuleID = null;
    }

    /**
     * @param ascendingOrder {@code true} para ascendente
     * @param matchingRuleID el OID de la regla, o {@code null} para la del atributo
     */
    public SortKey(String attrID, boolean ascendingOrder, String matchingRuleID) {
        this.attrID = attrID;
        this.reverseOrder = !ascendingOrder;
        this.matchingRuleID = matchingRuleID;
    }

    /** El atributo por el que ordenar. */
    public String getAttributeID() {
        return this.attrID;
    }

    /** Si es ascendente. */
    public boolean isAscending() {
        return !this.reverseOrder;
    }

    /** El OID de la regla de comparacion, o {@code null}. */
    public String getMatchingRuleID() {
        return this.matchingRuleID;
    }
}
