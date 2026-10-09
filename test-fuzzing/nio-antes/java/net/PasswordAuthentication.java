package java.net;

// A user name and a password, to hand back to an `Authenticator`.
//
// The password is kept as a `char[]` and not as a `String`, and that is not style: a `String` is
// immutable and stays alive until the collector picks it up, that is, the password hangs about in
// memory with nobody able to wipe it. An array can be overwritten.
//
// The constructor **copies** the array --so that whoever passed it can clear their own-- but
// `getPassword()` returns the internal one without copying, so that whoever consumes it can clear it
// when done. The asymmetry is the JDK's and it is deliberate.
//
// Nothing omitted: this is a pair of values.
public final class PasswordAuthentication {

    private final String userName;
    private final char[] password;

    public PasswordAuthentication(String userName, char[] password) {
        this.userName = userName;
        this.password = (char[]) password.clone();
    }

    public String getUserName() {
        return this.userName;
    }

    /** The internal array, not a copy: see the header. */
    public char[] getPassword() {
        return this.password;
    }
}
