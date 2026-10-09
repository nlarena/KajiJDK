package javax.sql.rowset.spi;

import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;

/**
 * Whoever knows how to carry the data back and forth between a disconnected {@code RowSet} and its
 * source.
 *
 * <h2>The problem it solves</h2>
 *
 * <p>A {@code CachedRowSet} fills up, disconnects, travels, is modified and comes back. Minutes can
 * pass between reading and writing, and in that while somebody else may have changed the same rows.
 * Somebody has to decide what to do about that, and that somebody is the synchronization provider.
 *
 * <h2>The grades, which are a scale of how much is controlled</h2>
 *
 * <p>{@link #GRADE_NONE} checks nothing: what is written overwrites whatever there is. The two
 * {@code GRADE_CHECK_*} compare when writing —only the modified rows, or all— and fail if something
 * changed. The two {@code GRADE_LOCK_*} outright prevent it from changing, by taking locks in the
 * source.
 *
 * <p>The scale goes from less to more safety and also from less to more cost, and that is the
 * decision behind it: locks give the strongest guarantee and are the ones that scale worst, because
 * they keep the source locked while the {@code RowSet} wanders around disconnected. That is why the
 * default grade of the providers is a check and not a lock.
 *
 * <h2>Why a {@code RowSet} does not talk to the database directly</h2>
 *
 * <p>Because that way the same {@code RowSet} serves for an SQL database, an XML file or whatever:
 * changing the provider changes the source without touching the code that uses the rows. The reader
 * and the writer returned by {@link #getRowSetReader} and {@link #getRowSetWriter} are the two
 * halves of that coupling.
 *
 * @since 1.5
 */
public abstract class SyncProvider {

    /** Nothing is checked when writing. */
    public static final int GRADE_NONE = 1;

    /** When writing, it is checked that the modified rows have not changed in the source. */
    public static final int GRADE_CHECK_MODIFIED_AT_COMMIT = 2;

    /** When writing, all the rows are checked, not only the modified ones. */
    public static final int GRADE_CHECK_ALL_AT_COMMIT = 3;

    /** A lock is taken on the rows when modifying them. */
    public static final int GRADE_LOCK_WHEN_MODIFIED = 4;

    /** A lock is taken on the rows when loading them. */
    public static final int GRADE_LOCK_WHEN_LOADED = 5;

    /** No lock is taken in the source. */
    public static final int DATASOURCE_NO_LOCK = 1;

    /** Row locks are taken. */
    public static final int DATASOURCE_ROW_LOCK = 2;

    /** Table locks are taken. */
    public static final int DATASOURCE_TABLE_LOCK = 3;

    /** A lock is taken on the whole database. */
    public static final int DATASOURCE_DB_LOCK = 4;

    /** The provider can synchronize against an updatable view. */
    public static final int UPDATABLE_VIEW_SYNC = 5;

    /** The provider cannot synchronize against a view. */
    public static final int NONUPDATABLE_VIEW_SYNC = 6;

    /** For the subclasses. */
    public SyncProvider() {
    }

    /**
     * The unique identifier of this provider, in the form of a reversed package name.
     *
     * <p>It is what is passed to {@link SyncFactory#getInstance} to ask for it, so it has to be
     * unique among all the installed providers.
     *
     * @return the identifier
     */
    public abstract String getProviderID();

    /**
     * The reader that fills the {@code RowSet} from the source.
     *
     * @return the reader
     */
    public abstract RowSetReader getRowSetReader();

    /**
     * The writer that returns the changes to the source.
     *
     * @return the writer
     */
    public abstract RowSetWriter getRowSetWriter();

    /**
     * The synchronization grade this provider offers.
     *
     * @return one of the {@code GRADE_} constants
     */
    public abstract int getProviderGrade();

    /**
     * Asks for a lock level in the source.
     *
     * <p>It is a request, not an order: a provider that cannot take that lock has to fail and not
     * drop silently to a weaker one. Dropping without warning would give the caller a guarantee
     * they think they have and do not.
     *
     * @param datasourceLock one of the {@code DATASOURCE_} constants
     * @throws SyncProviderException if the provider does not support that level
     */
    public abstract void setDataSourceLock(int datasourceLock) throws SyncProviderException;

    /**
     * The lock level being used.
     *
     * @return one of the {@code DATASOURCE_} constants
     * @throws SyncProviderException if it could not be found out
     */
    public abstract int getDataSourceLock() throws SyncProviderException;

    /**
     * Whether it can synchronize against a view.
     *
     * @return {@link #UPDATABLE_VIEW_SYNC} or {@link #NONUPDATABLE_VIEW_SYNC}
     */
    public abstract int supportsUpdatableView();

    /**
     * The version of this provider.
     *
     * @return the version
     */
    public abstract String getVersion();

    /**
     * Who made it.
     *
     * @return the vendor's name
     */
    public abstract String getVendor();
}
