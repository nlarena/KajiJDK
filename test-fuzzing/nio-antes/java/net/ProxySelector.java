package java.net;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// The one that decides, for each URI, which proxy to go out through.
//
// The abstraction has two methods and the second is the one people ignore: `connectFailed` is how the
// selector **learns**. If `select` proposed three proxies and the first did not work, whoever tried
// to connect has to say so, and only then can the selector stop proposing it. Without that road back,
// a downed proxy goes on being chosen forever.
//
// ===========================================================================================
// THE DEFAULT SELECTOR IN KajiJDK
// ===========================================================================================
//
// In the real JDK, `getDefault()` returns a selector that reads the system's proxy configuration
// (`http.proxyHost` and company, or the Windows registry). Here it returns one that always answers
// `DIRECT`.
//
// That is not a lie, it is this VM's truth: there are no connections to route, so there is no proxy
// configuration to read, and "you go straight out" is the correct and complete answer. `select`'s
// contract is "tell me where to go out through", and this selector fulfils it. A `select` throwing
// `UnsupportedOperationException` would be a different matter: that would leave its caller stranded.
//
// `setDefault` really works, so whoever wants another policy installs it and it runs.
public abstract class ProxySelector {

    private static volatile ProxySelector theProxySelector = new StaticProxySelector(null);

    public ProxySelector() {
    }

    /** The selector in use, or null if someone installed null. */
    public static ProxySelector getDefault() {
        return theProxySelector;
    }

    /** Installs the selector the whole VM is going to use. */
    public static void setDefault(ProxySelector ps) {
        theProxySelector = ps;
    }

    /**
     * The proxies {@code uri} can be reached through, in order of preference.
     *
     * <p>It never returns an empty list: if there is no proxy, it returns a list holding
     * {@link Proxy#NO_PROXY}.
     */
    public abstract List<Proxy> select(URI uri);

    /**
     * Notice that {@code sa} could not be connected to. See the header: without this the selector
     * does not learn.
     */
    public abstract void connectFailed(URI uri, SocketAddress sa, IOException ioe);

    /**
     * A selector that always proposes the same proxy for http and https, and a direct connection for
     * any other scheme.
     *
     * @param proxyAddress the proxy's address, or null for "always direct"
     */
    public static ProxySelector of(InetSocketAddress proxyAddress) {
        return new StaticProxySelector(proxyAddress);
    }

    // A selector that learns nothing because it has nothing to learn: its answer is constant.
    private static class StaticProxySelector extends ProxySelector {

        private static final List<Proxy> NO_PROXY_LIST =
                Collections.singletonList(Proxy.NO_PROXY);

        private final List<Proxy> list;

        StaticProxySelector(InetSocketAddress address) {
            Proxy p;
            if (address == null) {
                p = Proxy.NO_PROXY;
            } else {
                p = new Proxy(Proxy.Type.HTTP, address);
            }
            this.list = Collections.singletonList(p);
        }

        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
            // There is no state to update.
        }

        public List<Proxy> select(URI uri) {
            if (uri == null) {
                throw new IllegalArgumentException("URI can't be null");
            }
            String scheme = uri.getScheme();
            if (scheme != null) {
                scheme = scheme.toLowerCase();
                if (scheme.equals("http") || scheme.equals("https")) {
                    return this.list;
                }
            }
            return NO_PROXY_LIST;
        }
    }
}
