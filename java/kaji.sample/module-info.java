// Fixture for the module descriptor reader (Fase J / J0): exercises all five
// directives, both modifiers of `requires`, and the qualified form of exports/opens.
open module kaji.sample {
    requires java.base;
    requires transitive java.logging;
    requires static java.compiler;

    exports com.kaji.api;
    exports com.kaji.internal to kaji.friend, kaji.other;

    opens com.kaji.reflect;
    opens com.kaji.deep to kaji.friend;

    uses com.kaji.api.Service;
    provides com.kaji.api.Service with com.kaji.internal.Impl, com.kaji.internal.Alt;
}
