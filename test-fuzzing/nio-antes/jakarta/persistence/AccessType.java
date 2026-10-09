package jakarta.persistence;

// Whether the persistence provider accesses an entity's state through its fields or
// through its property accessors.
public enum AccessType {
    FIELD,
    PROPERTY
}
