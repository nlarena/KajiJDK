package pkg;

// A base class in package `pkg` with a protected field — the setup for the JVMS
// §4.10.1.8 protected-access rule: a subclass in another package may touch `x`, but
// only on a receiver of its own type (or a subtype), never on a bare `pkg.Base`.
public class Base {
    protected int x = 7;
}
