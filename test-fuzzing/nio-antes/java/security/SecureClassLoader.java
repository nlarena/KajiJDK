package java.security;

import java.nio.ByteBuffer;

// A loader that associates with each class the origin it came from.
//
// It is the piece that connects the permission model with the class loading one: `ClassLoader`
// knows how to define a class from bytes, and this subclass adds the `CodeSource`, which is what
// allows somebody afterwards to ask "where did this class come from?" and obtain a verifiable
// answer instead of a name. Without this, all the code of the process would be indistinguishable.
//
// The domains are **cached by origin**: two classes of the same jar share a `ProtectionDomain`, and
// that is not only a saving of memory — it is what makes granting a permission to a jar hold for
// all of its classes without the decision having to be repeated for each one.
public class SecureClassLoader extends ClassLoader {

    private final java.util.HashMap<CodeSource, ProtectionDomain> domains =
        new java.util.HashMap<CodeSource, ProtectionDomain>();

    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
    }

    protected SecureClassLoader() {
        super();
    }

    protected SecureClassLoader(String name, ClassLoader parent) {
        super(name, parent);
    }

    // It defines a class associating it with the given origin.
    protected final Class<?> defineClass(String name, byte[] b, int off, int len,
                                         CodeSource cs) {
        return super.defineClass(name, b, off, len, this.domainFor(cs));
    }

    protected final Class<?> defineClass(String name, ByteBuffer b, CodeSource cs) {
        return super.defineClass(name, b, this.domainFor(cs));
    }

    // The permissions that correspond to that origin.
    //
    // The base one returns an **empty** collection, just as the JDK does, and that does not mean
    // "no permissions": the domain built with it is dynamic, so what it can finally do is decided
    // by the `Policy` at the moment of asking. A subclass that wants to grant something fixed —the
    // classic permission to read its own jar— adds it here.
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return new Permissions();
    }

    // The domain of that origin, creating it the first time.
    private synchronized ProtectionDomain domainFor(CodeSource cs) {
        if (cs == null) {
            return null;
        }
        ProtectionDomain pd = this.domains.get(cs);
        if (pd == null) {
            // Four arguments: a **dynamic** domain, so that a change of policy reaches classes that
            // are loaded already. With the two-argument one they would be frozen with the
            // permissions there were at the moment of defining them.
            pd = new ProtectionDomain(cs, this.getPermissions(cs), this, null);
            this.domains.put(cs, pd);
        }
        return pd;
    }
}
