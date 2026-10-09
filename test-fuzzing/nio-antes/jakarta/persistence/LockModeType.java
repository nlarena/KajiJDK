package jakarta.persistence;

// jakarta.persistence.LockModeType (Jakarta Persistence 3.2).
public enum LockModeType implements FindOption, RefreshOption {
    READ,
    WRITE,
    OPTIMISTIC,
    OPTIMISTIC_FORCE_INCREMENT,
    PESSIMISTIC_READ,
    PESSIMISTIC_WRITE,
    PESSIMISTIC_FORCE_INCREMENT,
    NONE
}
