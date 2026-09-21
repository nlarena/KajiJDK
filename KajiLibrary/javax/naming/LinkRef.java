package javax.naming;

/**
 * JNDI's symbolic link: a binding whose content is **another name**.
 *
 * <p>It is a `Reference` with a single address, of type `"LinkAddress"`, whose content is the
 * target name. Being a `Reference` and not a separate type is no accident: that way any provider
 * that already knows how to store references knows how to store links without changing anything,
 * and the link survives a round trip over the network like the rest.
 *
 * <p>What makes it special is on the resolver's side, not here: `Context.lookup()` **follows**
 * links --it returns the object on the other side-- and `Context.lookupLink()` does not --it
 * returns this object. That pair of methods is the whole difference between "follow the link for
 * me" and "show me the link", and it is the reason `lookupLink` exists.
 *
 * <p>The link name is a URL, or a name resolved relative to the **initial context**, or --when its
 * first character is `.`-- a name relative to the context where the link is bound. (An earlier
 * note said a link always points relative to the initial context; the JDK's `LinkRef` javadoc
 * gives the `.` case too.)
 */
public class LinkRef extends Reference {

    private static final long serialVersionUID = -5386290613498931298L;

    /** The class declared in the reference; `getLinkName` checks against it. */
    static final String linkClassName = LinkRef.class.getName();

    /** The address type under which the target name goes. */
    static final String linkAddrType = "LinkAddress";

    public LinkRef(Name linkName) {
        super(linkClassName, new StringRefAddr(linkAddrType, linkName.toString()));
    }

    public LinkRef(String linkName) {
        super(linkClassName, new StringRefAddr(linkAddrType, linkName));
    }

    /**
     * The target name.
     *
     * <p>It checks instead of trusting because `Reference`'s fields are `protected` and mutable:
     * nothing prevents a `LinkRef` from having its class changed or its address removed, and then
     * there is no name to return. That is why it throws `MalformedLinkException` --which is what
     * "this claims to be a link and is not" means-- instead of `null` or a `NullPointerException`.
     */
    public String getLinkName() throws NamingException {
        if (className != null && className.equals(linkClassName)) {
            RefAddr addr = get(linkAddrType);
            if (addr instanceof StringRefAddr) {
                return (String) addr.getContent();
            }
        }
        throw new MalformedLinkException();
    }
}
