package javax.sql.rowset.spi;

import java.sql.SQLException;

/**
 * The provider factory could not deliver what was asked of it.
 *
 * <p>It differs from {@link SyncProviderException} in <strong>when</strong> it happens: this one is
 * when getting the provider —it is not registered, the class does not load, the name is wrong— and
 * the other is afterwards, while the provider already obtained synchronizes. One is about
 * configuration, the other about data.
 *
 * @since 1.5
 */
public class SyncFactoryException extends SQLException {

    private static final long serialVersionUID = -4354595476433200352L;

    /** Without detail. */
    public SyncFactoryException() {
        super();
    }

    /**
     * With a message.
     *
     * @param msg the message
     */
    public SyncFactoryException(String msg) {
        super(msg);
    }
}
