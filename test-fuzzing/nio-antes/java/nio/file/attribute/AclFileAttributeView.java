package java.nio.file.attribute;

import java.io.IOException;
import java.util.List;

// The `"acl"` view: the NFSv4-style access control list, which is the one Windows uses.
//
// The ACL is a **list** and not a set because the order decides: it is evaluated from the top down
// and the first entry that applies wins, so a `DENY` before an `ALLOW` is not the same as the other
// way round.
//
// Without an implementation in KajiJDK: there is no native that reads or writes ACLs.
public interface AclFileAttributeView extends FileOwnerAttributeView {

    /** Always `"acl"`. */
    String name();

    /** The ACL, in evaluation order. */
    List<AclEntry> getAcl() throws IOException;

    /** It replaces the whole ACL. */
    void setAcl(List<AclEntry> acl) throws IOException;
}
