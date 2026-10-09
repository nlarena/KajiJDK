package java.nio.file.attribute;

import java.security.Principal;

// A user's identity as the filesystem names it.
//
// It is `java.security.Principal` with nothing added: the subtype exists only so
// `FileOwnerAttributeView`'s signatures do not accept any old `Principal`. KajiJDK has nothing with
// which to produce one --there is no native that queries users-- and that is why
// `UserPrincipalLookupService` is left abstract and without an implementation.
public interface UserPrincipal extends Principal {
}
