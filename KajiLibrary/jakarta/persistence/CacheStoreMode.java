package jakarta.persistence;

// jakarta.persistence.CacheStoreMode (Jakarta Persistence 3.2).
public enum CacheStoreMode implements FindOption, RefreshOption {
    USE,
    BYPASS,
    REFRESH
}
