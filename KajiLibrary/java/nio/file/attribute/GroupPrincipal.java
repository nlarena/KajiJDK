package java.nio.file.attribute;

// A group's identity. That it extends `UserPrincipal` --rather than the two being siblings-- is the
// spec's, and it is what allows handing a group to `setOwner` on the systems where that makes
// sense.
public interface GroupPrincipal extends UserPrincipal {
}
