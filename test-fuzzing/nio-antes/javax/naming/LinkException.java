package javax.naming;

/**
 * A failure following a link, with the link's state **apart** from the context's state.
 *
 * <p>A link (`LinkRef`) is a name bound to another name: resolving `a/b` may lead to a link that
 * says "this is really `x/y/z`", and resolution carries on there. When it fails there are **two**
 * resolutions in play and both matter: the original name's and the link name's. The four fields
 * inherited from `NamingException` tell the first; the four `link*` of this class tell the second.
 *
 * <p>Without that split the diagnosis would be useless: "`z` not found" does not say whether `z`
 * was part of the requested name or of the name the link pointed to.
 *
 * <p>The rest of the hierarchy is explained in `NamingException`.
 */
public class LinkException extends NamingException {

    private static final long serialVersionUID = -7967662604076777712L;

    /** How far **the link name** was resolved. */
    protected Name linkResolvedName;

    /** The object reached by resolving the link name. */
    protected Object linkResolvedObj;

    /** What was left of the link name. */
    protected Name linkRemainingName;

    /** The why, in text, of the link failure. It is the counterpart of `getExplanation()`. */
    protected String linkExplanation;

    public LinkException(String explanation) {
        super(explanation);
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    public LinkException() {
        super();
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    public Name getLinkResolvedName() {
        return this.linkResolvedName;
    }

    public Name getLinkRemainingName() {
        return this.linkRemainingName;
    }

    public Object getLinkResolvedObj() {
        return this.linkResolvedObj;
    }

    public String getLinkExplanation() {
        return this.linkExplanation;
    }

    public void setLinkExplanation(String msg) {
        this.linkExplanation = msg;
    }

    // They clone for the same reason as `NamingException`'s: the provider keeps using its copy of
    // the name after throwing.

    public void setLinkResolvedName(Name name) {
        this.linkResolvedName = (name != null) ? (Name) name.clone() : null;
    }

    public void setLinkRemainingName(Name name) {
        this.linkRemainingName = (name != null) ? (Name) name.clone() : null;
    }

    public void setLinkResolvedObj(Object obj) {
        this.linkResolvedObj = obj;
    }

    @Override
    public String toString() {
        return super.toString() + "; Link Remaining Name: '" + this.linkRemainingName + "'";
    }

    @Override
    public String toString(boolean detail) {
        if (!detail || this.linkResolvedObj == null) {
            return this.toString();
        }
        return this.toString() + "; Link Resolved Object: " + this.linkResolvedObj;
    }
}
