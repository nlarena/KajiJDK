package javax.naming;

/**
 * An address that is text: a URL, a host name, an identifier.
 *
 * <p>It is the common case and adds nothing but the content. `equals`, `hashCode` and `toString`
 * come as they are from `RefAddr`, and work fine there because `String`'s `equals` is the right one
 * --which is exactly what does not happen with `BinaryRefAddr`, see there.
 *
 * <p>The content may be `null`: some addresses are identified only by their type.
 */
public class StringRefAddr extends RefAddr {

    private static final long serialVersionUID = -8913762495138505527L;

    private String contents;

    public StringRefAddr(String addrType, String addr) {
        super(addrType);
        contents = addr;
    }

    @Override
    public Object getContent() {
        return contents;
    }
}
