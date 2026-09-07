package java.beans;

// The rule for "how this object is remade". Bean persistence does not store an object's bytes: it
// stores the sequence of calls that builds it again. A PersistenceDelegate is the one that knows,
// for a given type, what those calls are.
//
// The division of labour between the four methods is the only thing to understand, and it is what
// makes the format come out small:
//
//   - `instantiate` gives the expression that CREATES the object —typically `new Foo(...)`.
//   - `initialize` gives the calls that carry it from the freshly created one to the one we have.
//   - `mutatesTo` decides which of the two is needed: if the object the encoder already has built
//     can be *carried* to the one we want with calls alone, only those are emitted; if not, a new
//     one has to be created from scratch.
//   - `writeObject` is the one that orchestrates the above and is hardly ever overridden.
//
// That `mutatesTo` is the reason a bean with twenty properties and a single one differing from the
// default comes out as one line and not as twenty: `initialize` compares against the new object and
// only writes what differs.
public abstract class PersistenceDelegate {

    protected PersistenceDelegate() {
    }

    // The entry point the Encoder uses. If the object already there can be mutated into the one we
    // want, the differences are emitted; if not, it is forgotten and the whole creation is
    // emitted.
    public void writeObject(Object oldInstance, Encoder out) {
        Object newInstance = out.get(oldInstance);
        if (!this.mutatesTo(oldInstance, newInstance)) {
            out.remove(oldInstance);
            out.writeExpression(this.instantiate(oldInstance, out));
        } else {
            this.initialize(oldInstance.getClass(), oldInstance, newInstance, out);
        }
    }

    // By default, two objects are "the same mould" if they are of exactly the same class. It is
    // enough because initialize then takes care of the differences in state.
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        return newInstance != null && oldInstance != null
            && oldInstance.getClass() == newInstance.getClass();
    }

    protected abstract Expression instantiate(Object oldInstance, Encoder out);

    // By default there is nothing to adjust: the delegates that do have state to copy override it
    // and also call super, which climbs the chain of superclasses.
    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            PersistenceDelegate info = out.getPersistenceDelegate(superClass);
            info.initialize(superClass, oldInstance, newInstance, out);
        }
    }
}
