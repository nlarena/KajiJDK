// `Sub` is in the default package — a *different* package from `pkg.Base` it extends.
// `get()` reads the inherited protected `x` through `this` (a `Sub`), which is the
// legal cross-package form and must verify.
class Sub extends pkg.Base {
    int get() {
        return x;
    }
}
