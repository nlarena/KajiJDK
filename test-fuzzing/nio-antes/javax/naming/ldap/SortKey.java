package javax.naming.ldap;

/**
 * Which attribute to sort by, in which direction and with which comparison rule.
 *
 * <h2>Why the rule has to be named</h2>
 *
 * <p>Because comparing two strings has no single right answer. In LDAP comparison is defined by a
 * <em>matching rule</em>, and there may be several for the same attribute: one that is
 * case-sensitive and one that is not, one that ignores extra spaces and one that does not.
 *
 * <p>{@code null} in {@link #getMatchingRuleID} lets the server pick the attribute's default rule,
 * which is almost always sensible and what the one-argument constructor does.
 */
public class SortKey {

    private final String attrID;
    private final boolean reverseOrder;
    private final String matchingRuleID;

    /** Ascending, with the attribute's default rule. */
    public SortKey(String attrID) {
        this.attrID = attrID;
        this.reverseOrder = false;
        this.matchingRuleID = null;
    }

    /**
     * @param ascendingOrder {@code true} for ascending
     * @param matchingRuleID the rule's OID, or {@code null} for the attribute's
     */
    public SortKey(String attrID, boolean ascendingOrder, String matchingRuleID) {
        this.attrID = attrID;
        this.reverseOrder = !ascendingOrder;
        this.matchingRuleID = matchingRuleID;
    }

    /** The attribute to sort by. */
    public String getAttributeID() {
        return this.attrID;
    }

    /** Whether it is ascending. */
    public boolean isAscending() {
        return !this.reverseOrder;
    }

    /** The OID of the comparison rule, or {@code null}. */
    public String getMatchingRuleID() {
        return this.matchingRuleID;
    }
}
