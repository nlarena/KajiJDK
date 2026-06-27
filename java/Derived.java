// Subclass whose static initializer reads the inherited `b`: Derived.<clinit> does
// getstatic Base.b, iconst_1, iadd, putstatic d. So d ends up 3 — but ONLY if Base
// was initialized first (super-before-subclass ordering). Otherwise b would be 0.
class Derived extends Base {
    static int d = b + 1;
}
