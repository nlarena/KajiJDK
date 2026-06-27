// Base class with a static initializer → compiles to Base.<clinit> (iconst_2,
// putstatic b). Must run before Derived's <clinit>, which reads b.
class Base {
    static int b = 2;
}
