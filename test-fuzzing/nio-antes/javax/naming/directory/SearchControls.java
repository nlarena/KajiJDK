package javax.naming.directory;

import java.io.Serializable;

/**
 * KajiLibrary's javax.naming.directory.SearchControls -- how much to search and what to fetch.
 *
 * <p>Six options, and the first three are the ones that decide whether a search is viable or brings
 * the directory down:
 *
 * <ul>
 *   <li>the <b>scope</b>. {@link #OBJECT_SCOPE} looks at a single entry, {@link #ONELEVEL_SCOPE} at
 *       its direct children, and {@link #SUBTREE_SCOPE} at the whole tree. The third on the root of
 *       a large directory is the classic way to make a request that takes minutes;
 *   <li>the <b>count limit</b>, which cuts off after so many results;
 *   <li>the <b>time limit</b>, in milliseconds.
 * </ul>
 *
 * <p>Both limits at 0 mean <b>no limit</b>, which is the default for both. Worth knowing: this
 * class's defaults are the most permissive, not the safest.
 *
 * <h2>Which attributes come back</h2>
 *
 * <p>{@link #setReturningAttributes} with null fetches <b>all</b>, and with an <b>empty</b> array
 * fetches none. Both are useful and easy to mix up: the empty array serves to ask "which entries
 * match" without fetching their data, which on a remote directory is the difference between a few
 * kilobytes and a few megabytes.
 *
 * <p>{@link #setReturningObjFlag} asks for each entry's <b>object</b> to come too, not just its
 * attributes. It is expensive and that is why it starts off.
 */
public class SearchControls implements Serializable {

    private static final long serialVersionUID = -9138475345988518376L;

    /** Only the named entry. */
    public static final int OBJECT_SCOPE = 0;

    /** Its direct children, without itself. */
    public static final int ONELEVEL_SCOPE = 1;

    /** Itself and its whole subtree. */
    public static final int SUBTREE_SCOPE = 2;

    private int searchScope;

    private int timeLimit;

    private boolean derefLink;

    private boolean returnObj;

    private long countLimit;

    private String[] attributesToReturn;

    /**
     * The defaults: one level, no limits, all attributes, no objects.
     *
     * <p>See the class note: the default limits are none.
     */
    public SearchControls() {
        this.searchScope = ONELEVEL_SCOPE;
        this.timeLimit = 0;
        this.countLimit = 0;
        this.derefLink = false;
        this.returnObj = false;
        this.attributesToReturn = null;
    }

    /**
     * Everything explicit.
     *
     * @param scope one of the three constants
     * @param countlim the maximum number of results; 0 is no limit
     * @param timelim milliseconds; 0 is no limit
     * @param attrs which attributes to fetch; null is all and empty is none
     * @param retobj whether each entry's object comes too
     * @param deref whether links are followed
     */
    public SearchControls(int scope, long countlim, int timelim, String[] attrs, boolean retobj,
                          boolean deref) {
        this.searchScope = scope;
        this.timeLimit = timelim;
        this.derefLink = deref;
        this.returnObj = retobj;
        this.countLimit = countlim;
        this.attributesToReturn = attrs;
    }

    /** The scope. */
    public int getSearchScope() {
        return this.searchScope;
    }

    /** The time limit in milliseconds; 0 is no limit. */
    public int getTimeLimit() {
        return this.timeLimit;
    }

    /** Whether links are followed. */
    public boolean getDerefLinkFlag() {
        return this.derefLink;
    }

    /** Whether each entry's object comes too. */
    public boolean getReturningObjFlag() {
        return this.returnObj;
    }

    /** The count limit; 0 is no limit. */
    public long getCountLimit() {
        return this.countLimit;
    }

    /** Which attributes to fetch. See the class note about null versus empty. */
    public String[] getReturningAttributes() {
        return this.attributesToReturn;
    }

    /** Ver {@link #getSearchScope}. */
    public void setSearchScope(int scope) {
        this.searchScope = scope;
    }

    /** Ver {@link #getTimeLimit}. */
    public void setTimeLimit(int ms) {
        this.timeLimit = ms;
    }

    /** Ver {@link #getDerefLinkFlag}. */
    public void setDerefLinkFlag(boolean on) {
        this.derefLink = on;
    }

    /** Ver {@link #getReturningObjFlag}. */
    public void setReturningObjFlag(boolean on) {
        this.returnObj = on;
    }

    /** Ver {@link #getCountLimit}. */
    public void setCountLimit(long limit) {
        this.countLimit = limit;
    }

    /** Ver {@link #getReturningAttributes}. */
    public void setReturningAttributes(String[] attrs) {
        this.attributesToReturn = attrs;
    }
}
