package jakarta.persistence;

// jakarta.persistence.PessimisticLockScope (Jakarta Persistence 3.2).
public enum PessimisticLockScope implements FindOption, RefreshOption, LockOption {
    NORMAL,
    EXTENDED
}
