package java.security;

// The secret half of an asymmetric pair.
//
// A KajiLibrary subset: in the JDK this interface also extends
// `javax.security.auth.Destroyable`, which contributes `destroy()` and `isDestroyed()`. That
// package does not exist in this library, so the supertype is not declared. The omission shows:
// without `destroy()` there is no standard way of asking an implementation to erase the secret
// material from memory before letting go of it. The day `javax.security.auth` exists, it is added
// and nothing of what is here changes.
public interface PrivateKey extends AsymmetricKey {

    long serialVersionUID = 6034044314589513430L;
}
