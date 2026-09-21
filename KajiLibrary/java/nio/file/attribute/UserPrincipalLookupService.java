package java.nio.file.attribute;

import java.io.IOException;

// The service that turns a name into a `UserPrincipal` or a `GroupPrincipal`.
//
// Abstract and **without an implementation in KajiJDK**: there is no native that queries the system's
// user database. That is why `KajiFileSystem`'s `FileSystem.getUserPrincipalLookupService()` throws
// `UnsupportedOperationException` --which is what the spec already provides for a filesystem that
// does not support principals-- rather than return a service that answers with invented names.
public abstract class UserPrincipalLookupService {

    /** For the subclasses. */
    protected UserPrincipalLookupService() {
    }

    /**
     * It looks a user up by name.
     *
     * @throws UserPrincipalNotFoundException if it does not exist
     */
    public abstract UserPrincipal lookupPrincipalByName(String name) throws IOException;

    /**
     * It looks a group up by name.
     *
     * @throws UserPrincipalNotFoundException if it does not exist
     */
    public abstract GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException;
}
