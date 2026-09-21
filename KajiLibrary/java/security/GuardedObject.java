package java.security;

import java.io.Serializable;

// An object behind a guard: to have it one has to go through `getObject()`.
//
// It is the opposite pattern to checking the permission before handing over the reference. There
// the check happens once, when it is handed over; here it happens **every time somebody unwraps
// it**, and that matters because the reference can travel: it can be kept, serialised and sent
// elsewhere, and the guard goes on being stuck to it.
public class GuardedObject implements Serializable {

    private final Object object;
    private final Guard guard;

    public GuardedObject(Object object, Guard guard) {
        this.object = object;
        this.guard = guard;
    }

    // The object, if the guard allows it.
    public Object getObject() throws SecurityException {
        if (this.guard != null) {
            this.guard.checkGuard(this.object);
        }
        return this.object;
    }
}
