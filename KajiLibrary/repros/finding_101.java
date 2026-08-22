package repro101;

// Finding #101 — a qualified reference to a nested type, `Outer.Nested`, is not resolved; only the
// simple name `Nested` (in scope, or via a single-type import) works. Reproduces both within the same
// file (self-qualified `finding_101.Flag`) and across files (a sibling naming `Outer.Flag` with Outer
// on the classpath). The JDK resolves all of these.
//
//   Flag[] viaSimple()            -> OK
//   finding_101.Flag[] viaQualified() -> error: no se encuentra el símbolo: finding_101.Flag
//
// Same family as #20 (a qualified `new` name is miscompiled): the compiler's name resolution doesn't
// walk from an enclosing type to its member type through the `Outer.Nested` form. Workaround: import
// the nested type (`import pkg.Outer.Nested;`) and use the simple name.
public class finding_101 {

    enum Flag {
        X,
        Y
    }

    Flag[] viaSimple() {
        return null;                       // OK — simple name in scope
    }

    finding_101.Flag[] viaQualified() {
        return null;                       // FAILS — qualified Outer.Nested reference not resolved
    }
}
