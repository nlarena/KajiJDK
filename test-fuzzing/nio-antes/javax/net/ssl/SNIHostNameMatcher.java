package javax.net.ssl;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The matcher {@link SNIHostName#createSNIMatcher} returns.
 *
 * <p>Package-private and not public: the JDK does not expose it either. Nobody should be able to
 * build it except through that factory, which is what guarantees the expression was validated.
 */
final class SNIHostNameMatcher extends SNIMatcher {

    private final Pattern pattern;

    SNIHostNameMatcher(String regex) {
        super(StandardConstants.SNI_HOST_NAME);
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Accepts a host name that matches the expression.
     *
     * <p>An {@link SNIServerName} of another type is rejected without looking at it: this matcher
     * only understands host names, and giving an opinion on something else would be inventing.
     */
    public boolean matches(SNIServerName serverName) {
        if (serverName == null) {
            throw new NullPointerException("serverName");
        }
        if (!(serverName instanceof SNIHostName)) {
            if (serverName.getType() != StandardConstants.SNI_HOST_NAME) {
                return false;
            }
            SNIHostName rebuilt = new SNIHostName(serverName.getEncoded());
            return this.pattern.matcher(
                    rebuilt.getAsciiName().toLowerCase(Locale.ENGLISH)).matches();
        }
        SNIHostName h = (SNIHostName) serverName;
        return this.pattern.matcher(h.getAsciiName().toLowerCase(Locale.ENGLISH)).matches();
    }
}
