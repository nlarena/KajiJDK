// The module descriptor of KajiLibrary's `java.base` — the base module every other one
// implicitly requires (JLS §7.7.3), which is why it declares no `requires` of its own.
//
// It exports exactly the packages the library actually has today; jlink resolves and
// packages against *this*, not against the JDK's. A smaller java.base just means a
// smaller image — completeness is not what linking needs, a well-formed descriptor is.
module java.base {
    exports java.io;
    exports java.lang;
    exports java.lang.annotation;
    exports java.lang.constant;
    exports java.lang.ref;
    exports java.lang.reflect;
    exports java.time;
    exports java.time.chrono;
    exports java.time.format;
    exports java.time.temporal;
    exports java.util;
    exports java.util.concurrent;
    exports java.util.concurrent.atomic;
    exports java.util.concurrent.locks;
    exports java.util.function;
    exports java.util.regex;
    exports java.util.stream;
}
