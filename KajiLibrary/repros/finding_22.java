package repro22;

// Finding #22 — method resolution on `this` (explicit OR implicit receiver) only consults the
// methods DECLARED in the class itself; it ignores every INHERITED method, whether inherited from
// the superclass (java.lang.Object) or from an implemented interface. The JDK resolves all of these.
//
// Verified with the bootstrap Object (with getClass/hashCode/toString) on the classpath:
//   this.getClass()                       -> error: no se encuentra el método: getClass
//   this.hashCode()  (inherited)          -> error: no se encuentra el método: hashCode
//   this.hashCode()  (own override)       -> OK
//   this.toString()  (inherited)          -> error: no se encuentra el método: toString
//   getClass()       (implicit receiver)  -> error: no se encuentra el método: getClass
//   this.id()        (interface method)   -> error: no se encuentra el método: id
//   other.id()       (interface-typed var)-> OK
// The JDK compiles every one of these.
//
// So `this`-receiver resolution walks only the class's own declarations, not the superclass chain or
// the implemented-interface methods. Workaround: rebind `this` to a supertype variable and call
// through it (I self = this; self.id()  /  Object o = this; o.hashCode()), or use `super.m()` for
// superclass methods.

interface I {
    String id();
}

class C {
    // A concrete class: inherited Object methods on `this` also fail to resolve.
    int inheritedObjectMethod() {
        return this.getClass().hashCode();   // FAILS: getClass()/hashCode() inherited from Object
    }
    int ownOverride() {
        return this.hashCode();              // FAILS unless hashCode() is declared here
    }
    public int hashCode() { return 1; }      // with this present, this.hashCode() above would resolve
}

abstract class A implements I {
    String viaThis() {
        return this.id();                    // FAILS: id() inherited from implemented interface I
    }
    String viaParam(I other) {
        return other.id();                   // OK: same method on a variable of the interface type
    }
    String workaround() {
        I self = this;
        return self.id();                    // OK: rebinding `this` to the interface type resolves it
    }
}
