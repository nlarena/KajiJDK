package java.io;

import java.io.ObjectInputFilter.FilterInfo;
import java.io.ObjectInputFilter.Status;
import java.util.function.Predicate;

// The three implementations `ObjectInputFilter`'s factories return.
//
// They live here and not inside the interface because **every type nested in an interface is
// public** (JLS 9.5): putting them there would turn them into API with a name of their own, and
// what is promised is an `ObjectInputFilter`, not a particular class. That the name cannot be
// written is what leaves room to change them later.
//
// The `what` of each is in the javadoc of the factory that builds it; here there is only the `how`.
final class Filters {

    private Filters() {
    }

    // `allowFilter` and `rejectFilter` are the same class with the two results swapped over.
    // Keeping them apart would duplicate three lines of logic so that they could differ the day
    // somebody touched only one of them.
    static final class ByPredicate implements ObjectInputFilter {

        private final Predicate<Class<?>> predicate;
        private final Status ifTrue;
        private final Status ifFalse;

        ByPredicate(Predicate<Class<?>> predicate, Status ifTrue, Status ifFalse) {
            this.predicate = predicate;
            this.ifTrue = ifTrue;
            this.ifFalse = ifFalse;
        }

        public Status checkInput(FilterInfo info) {
            Class<?> c = info.serialClass();
            if (c == null) {
                return Status.UNDECIDED;
            }
            return this.predicate.test(c) ? this.ifTrue : this.ifFalse;
        }
    }

    static final class Union implements ObjectInputFilter {

        private final ObjectInputFilter first;
        private final ObjectInputFilter second;

        Union(ObjectInputFilter first, ObjectInputFilter second) {
            this.first = first;
            this.second = second;
        }

        public Status checkInput(FilterInfo info) {
            Status a = this.first.checkInput(info);
            if (a == Status.REJECTED) {
                // It cuts short before consulting the second one: it would not change the result,
                // and a filter may be expensive or have effects (logging the attempt) that should
                // not be triggered once the decision is already made.
                return Status.REJECTED;
            }
            Status b = this.second.checkInput(info);
            if (b == Status.REJECTED) {
                return Status.REJECTED;
            }
            if (a == Status.ALLOWED || b == Status.ALLOWED) {
                return Status.ALLOWED;
            }
            return Status.UNDECIDED;
        }
    }

    static final class RejectUndecided implements ObjectInputFilter {

        private final ObjectInputFilter wrapped;

        RejectUndecided(ObjectInputFilter wrapped) {
            this.wrapped = wrapped;
        }

        public Status checkInput(FilterInfo info) {
            Status s = this.wrapped.checkInput(info);
            if (s == Status.UNDECIDED && info.serialClass() != null) {
                return Status.REJECTED;
            }
            return s;
        }
    }
}
