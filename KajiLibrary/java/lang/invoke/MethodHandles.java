package java.lang.invoke;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.List;

// The factory for method handles, and the home of `Lookup` — the object that carries ACCESS
// RIGHTS. That is the part worth understanding: a handle is not obtained from a class but from a
// lookup, and the lookup remembers which class asked for it. `MethodHandles.lookup()` captures
// the caller, so the handles it produces reach exactly what the caller could reach by writing the
// call directly. Access control therefore happens ONCE, when the handle is created, instead of on
// every invocation — which is what lets a handle be as fast as a direct call.
//
// Capturing the caller means walking the stack, a VM operation, and every factory below has to
// produce a `MethodHandle`, which nothing in the library can build. So this is a declaration with
// honest holes.
//
// The combinators are now declared. The earlier note here argued that "a shape over something
// that cannot exist would add nothing but more surface to be wrong about", and half of that is
// still true — none of them can RUN. What changed the balance is that the combinators are the
// part of this API that carries the design, and the design is checkable independently of any
// implementation: each one states, in its signature, how the argument list of the result is
// derived from the argument lists of its inputs. That relation is the whole content of the
// method-handle calculus, it is what a reader of this package needs, and getting it wrong here
// would be caught by `javap` against the real JDK — which is exactly the check this library runs.
//
// The families, since 40 methods listed flat are unreadable:
//
//   argument plumbing   `dropArguments`, `insertArguments`, `permuteArguments`,
//                       `filterArguments`, `foldArguments`, `collectArguments`
//   return plumbing     `filterReturnValue`, `dropReturn`
//   control flow        `guardWithTest`, `catchException`, `tryFinally`, `tableSwitch`
//   loops               `loop` and its four specialisations (`whileLoop`, `doWhileLoop`,
//                       `countedLoop`, `iteratedLoop`)
//   VarHandle plumbing  the `*Coordinates` family and `filterValue`, which are the same
//                       plumbing applied to a variable's coordinates instead of a call's
//                       arguments
//
// Nothing of the public surface is omitted any more. The last two holes were the pair that takes
// a `VarHandle.AccessMode` and the pair on `Lookup` that takes a `Lookup.ClassOption`; both enums
// now exist — `AccessMode` in `VarHandle.java`, `ClassOption` below.
//
// SPELLING, and it is not cosmetic. A nested type belonging to ANOTHER file cannot be named here
// the way Java says it should: `VarHandle.AccessMode` does not resolve (#101) and an
// `import java.lang.invoke.VarHandle.AccessMode` compiles but emits the descriptor `LAccessMode;`
// — a class that exists in no package (#208). What DOES work is the type's BINARY name,
// `VarHandle$AccessMode`, because our compiler reads `VarHandle$AccessMode.class` off the
// classpath as an ordinary member of the package. That is not a Kaji invention: the reference
// `javac` resolves the flat name of a class file on the classpath the same way — verified —
// which is exactly the build KajiLibrary uses (`javac -cp KajiLibrary`, one file at a time). It
// would NOT resolve in a build that compiled `VarHandle.java` from source in the same
// invocation, so the spelling is a workaround with a real edge, and it comes out when #101 does.
// Every occurrence of `$` in a type name in this package is this and nothing else.
public final class MethodHandles {

    private MethodHandles() {
    }

    public static Lookup lookup() {
        throw new UnsupportedOperationException("a Lookup captures its caller, which needs VM support");
    }

    public static Lookup publicLookup() {
        throw new UnsupportedOperationException("a Lookup captures its caller, which needs VM support");
    }

    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayLength(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayElementGetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayElementSetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle exactInvoker(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle invoker(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The same two invokers, for a `VarHandle` access instead of a `MethodHandle` call. They take
    // an `AccessMode` because a `VarHandle` is not one callable thing but 31 of them, so naming
    // the access is what a `MethodType` alone cannot do here. `varHandleExactInvoker` demands the
    // exact type; `varHandleInvoker` allows the asType conversions.
    public static MethodHandle varHandleExactInvoker(VarHandle$AccessMode accessMode, MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle varHandleInvoker(VarHandle$AccessMode accessMode, MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle constant(Class<?> type, Object value) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle identity(Class<?> type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle zero(Class<?> type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle empty(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle throwException(Class<?> returnType, Class<?> exType) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The public shortcut for cracking a handle open, for a handle that was created under a
    // PUBLIC lookup and therefore needs no rights to inspect. The private form lives on `Lookup`,
    // where the rights are.
    //
    // MODELLED RAW, the same deliberate trick as `TypeDescriptor`: the JDK declares this
    // `<T extends Member> T reflectAs(Class<T>, MethodHandle)`, and our compiler erases a bounded
    // type variable to `Object` rather than to its bound (#100), which would emit
    // `…)Ljava/lang/Object;`. Returning the bound outright emits exactly the descriptor the JDK
    // emits, because returning the bound is what the JDK's own erasure amounts to. The source
    // loses the `Class<T>`/`T` correspondence; the binary is faithful.
    public static Member reflectAs(Class<?> expected, MethodHandle target) {
        throw new UnsupportedOperationException("no method handle cracking without VM support");
    }

    // ---- lookups over somebody else's class ----

    // A lookup on `targetClass` with the CALLER's private rights, granted only when the caller
    // and the target are in the same module or nestmate relationship. It is the supported
    // replacement for `setAccessible(true)`: the permission is decided once, against a lookup the
    // caller already legitimately holds, instead of being asserted per member.
    public static Lookup privateLookupIn(Class<?> targetClass, Lookup caller)
            throws IllegalAccessException {
        throw new UnsupportedOperationException("a Lookup captures its caller, which needs VM support");
    }

    // The extra argument a hidden class was defined with. `defineHiddenClassWithClassData` stores
    // it beside the class, and this is how the class reads it back — a constant that is not in
    // the constant pool, which is what lets a hidden class be shared while still being specialised.
    //
    // The type variable is UNBOUNDED, so it erases to `Object` in both compilers and the emitted
    // descriptor is the JDK's either way — which is why these two can be written the JDK's way
    // while `reflectAs` below cannot. That is the whole of the difference: #100 misplaces the
    // erasure of a BOUNDED variable only.
    public static <T> T classData(Lookup caller, String name, Class<T> type)
            throws IllegalAccessException {
        throw new UnsupportedOperationException("no hidden classes without VM support");
    }

    public static <T> T classDataAt(Lookup caller, String name, Class<T> type, int index)
            throws IllegalAccessException {
        throw new UnsupportedOperationException("no hidden classes without VM support");
    }

    // ---- argument plumbing ----
    //
    // Read each of these as "what argument list does the RESULT take, given the target's". That
    // relation, not the invocation, is what the combinator is.

    // Result takes `valueTypes` extra arguments at `pos` and throws them away. The direction that
    // confuses everyone: this makes the handle accept MORE than the target, because the dropped
    // arguments are the ones the caller supplies and the target never sees.
    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>[] valueTypes) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Drops whatever it takes to make `target` accept `newTypes` — the form used when two handles
    // must be given a common type before `guardWithTest` or `tryFinally` can combine them.
    public static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip,
            List<Class<?>> newTypes, int pos) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The mirror of `dropArguments`: the values are supplied NOW, so the result takes fewer
    // arguments than the target. This is currying.
    public static MethodHandle insertArguments(MethodHandle target, int pos, Object[] values) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Reorders and/or duplicates arguments. `reorder[i]` is the index, in the RESULT's argument
    // list, of the value the target's parameter `i` receives — so the array is read as a mapping
    // from target position to caller position, which also lets one caller argument feed several
    // target parameters, and lets one be left unused.
    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int[] reorder) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Preprocesses arguments `pos…pos+filters.length` one by one. Each filter is unary; a `null`
    // entry leaves that argument alone.
    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle[] filters) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Runs `combiner` over a prefix of the arguments and PREPENDS its result to the target's
    // arguments — the arguments the combiner consumed are still passed through. That "and" is the
    // difference from `collectArguments`, which replaces them.
    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Replaces the target's parameter at `pos` with the whole argument list of `filter`. A
    // `void` filter degenerates to dropping the position, which is how one combinator covers
    // both spreading and collecting.
    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // ---- return plumbing ----

    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // Same arguments, `void` return. Useful precisely where a value would have to be discarded
    // anyway — a loop body, a `tryFinally` cleanup.
    public static MethodHandle dropReturn(MethodHandle target) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // ---- control flow ----
    //
    // The point of this family is that branching, catching and looping become VALUES: a
    // conditional is not a bytecode here, it is a handle built out of three other handles.

    // `test` must return `boolean` and take a PREFIX of the target's arguments — it may look at
    // fewer arguments than the branches do, never more.
    public static MethodHandle guardWithTest(MethodHandle test, MethodHandle target,
            MethodHandle fallback) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // `handler` receives the thrown exception FIRST, then the original arguments — so it can
    // retry, not merely report.
    public static MethodHandle catchException(MethodHandle target, Class<?> exType,
            MethodHandle handler) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // `cleanup` receives the thrown `Throwable` (null on the normal path) and the result, then
    // the original arguments — enough to observe both outcomes and to substitute a result.
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // A `tableswitch` as a value: the leading `int` argument selects a case, out-of-range falls
    // to `fallback`. All the cases and the fallback must already share one type.
    public static MethodHandle tableSwitch(MethodHandle fallback, MethodHandle[] targets) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // ---- loops ----
    //
    // `loop` is the general form and the other four are its specialisations. Each clause is an
    // array `{init, step, pred, fini}` describing ONE loop variable; the clauses run in lockstep,
    // the first `pred` that fails ends the loop, and the matching `fini` produces the result. A
    // shorter clause array is padded with nulls, which is why the four convenience forms below
    // can all be expressed as one or two clauses.
    public static MethodHandle loop(MethodHandle[][] clauses) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The same, with the test AFTER the body — so the body always runs at least once.
    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle countedLoop(MethodHandle count, MethodHandle init, MethodHandle body) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init,
            MethodHandle body) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The for-each: `iterator` yields the `Iterator`, `body` receives each element.
    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init,
            MethodHandle body) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // ---- VarHandle plumbing ----
    //
    // A `VarHandle`'s COORDINATES are what a method handle's arguments are: the values that
    // locate the variable. These combinators are therefore the same plumbing as above, applied to
    // the coordinate list — which is why the names rhyme with the method-handle family.

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget,
            MethodHandle filterFromTarget) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle[] filters) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object[] values) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates,
            int[] reorder) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>[] valueTypes) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    // ---- VarHandle factories ----

    public static VarHandle arrayElementVarHandle(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    // A view that reads a WIDER primitive out of a `byte[]` — the supported way to do what used
    // to require `Unsafe`. The `ByteOrder` is a parameter and not a property of the platform
    // because the whole reason to reach for this is that the bytes came from somewhere with its
    // own convention.
    public static VarHandle byteArrayViewVarHandle(Class<?> viewArrayClass, ByteOrder byteOrder) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    public static VarHandle byteBufferViewVarHandle(Class<?> viewArrayClass, ByteOrder byteOrder) {
        throw new UnsupportedOperationException("no VarHandle factory without VM support");
    }

    // The access-rights token. Nested, as in the JDK, because its identity is inseparable from the
    // factory that hands it out.
    public static final class Lookup {

        // Which accesses this lookup may perform. A lookup can be NARROWED but never widened,
        // which is what makes it safe to hand one to somebody else.
        public static final int PUBLIC = 1;
        public static final int PRIVATE = 2;
        public static final int PROTECTED = 4;
        public static final int PACKAGE = 8;
        public static final int MODULE = 16;
        public static final int UNCONDITIONAL = 32;
        public static final int ORIGINAL = 64;

        Lookup() {
        }

        public Class<?> lookupClass() {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public int lookupModes() {
            return 0;
        }

        // Set when this lookup was produced by `privateLookupIn` — it remembers where the rights
        // came from, so a lookup handed across a module boundary can still be told apart from one
        // that was minted there.
        public Class<?> previousLookupClass() {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // Narrowing, and only ever narrowing. `in` retargets the lookup at another class and
        // `dropLookupMode` removes bits; there is deliberately no widening operation, which is
        // what makes a `Lookup` safe to pass to code you do not trust — it can never grow back
        // the rights you took off it.
        public Lookup in(Class<?> requestedLookupClass) {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public Lookup dropLookupMode(int modeToDrop) {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public boolean hasPrivateAccess() {
            return false;
        }

        public boolean hasFullPrivilegeAccess() {
            return false;
        }

        public MethodHandle findStatic(Class<?> refc, String name, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findConstructor(Class<?> refc, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findGetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findSetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // `findSpecial` is the one that is not "find the method": it is the `invokespecial`
        // form — the one a `super.m()` compiles to — so it bypasses virtual dispatch, and
        // `specialCaller` is the class from whose position the call is being made. That extra
        // parameter is not decoration: allowing a non-virtual call into somebody else's
        // hierarchy would let a caller skip an override, so it must be proved.
        public MethodHandle findSpecial(Class<?> refc, String name, MethodType type,
                Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // Finds a virtual method AND binds the receiver in one step — `findVirtual(...).bindTo(r)`
        // with the access check done against the receiver's actual class.
        public MethodHandle bind(Object receiver, String name, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // ---- variables ----

        public VarHandle findVarHandle(Class<?> recv, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no VarHandle without VM support");
        }

        public VarHandle findStaticVarHandle(Class<?> decl, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no VarHandle without VM support");
        }

        // ---- the reflection bridge ----
        //
        // `unreflect*` converts a `java.lang.reflect` member into a handle. The direction matters:
        // a `Method` carries no access decision — it is checked on every `invoke` — while a handle
        // carries the decision made here, once. So this is where the cost moves, and it is also
        // where `setAccessible(true)` is honoured: an already-accessible `Method` produces a
        // handle without a further check.

        public MethodHandle unreflect(Method m) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle unreflectSpecial(Method m, Class<?> specialCaller)
                throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle unreflectConstructor(Constructor<?> c) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle unreflectGetter(Field f) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle unreflectSetter(Field f) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public VarHandle unreflectVarHandle(Field f) throws IllegalAccessException {
            throw new UnsupportedOperationException("no VarHandle without VM support");
        }

        // The inverse of the whole package: given a DIRECT handle, recover the member it names.
        // It only works for direct handles and only for a lookup that could have created the
        // handle itself — otherwise cracking one open would launder the access check that making
        // it required.
        public MethodHandleInfo revealDirect(MethodHandle target) {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // ---- classes ----

        // Loads by name with THIS lookup's loader and access rights, which is the difference from
        // `Class.forName`: the answer is a class this lookup may actually use.
        public Class<?> findClass(String targetName)
                throws ClassNotFoundException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // Checks access to an already-loaded class and hands it back unchanged — the check on its
        // own, for code that wants to fail early rather than at the first member lookup.
        public Class<?> accessClass(Class<?> targetClass) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // Forces `<clinit>` to have run. Ordinary code cannot ask for this — initialisation is a
        // side effect of a first active use (JVMS §5.5) — so a lookup that is about to hand out
        // handles needs a way to say it explicitly.
        public Class<?> ensureInitialized(Class<?> targetClass) throws IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        // Defines a class in this lookup's package with this lookup's loader. It is the supported
        // successor to `ClassLoader.defineClass`, and it needs PACKAGE access precisely because
        // the new class lands inside the lookup's own package and can see its internals.
        public Class<?> defineClass(byte[] bytes) throws IllegalAccessException {
            throw new UnsupportedOperationException("no class definition without VM support");
        }

        // How a hidden class relates to the lookup class that defines it. Two independent
        // choices, encoded as an enum rather than as booleans because the JDK reserves the room
        // to add more.
        //
        //   NESTMATE  the new class joins the lookup class's NEST, so the two can reach each
        //             other's private members — which is the whole point when the hidden class is
        //             a generated accessor or a lambda body.
        //   STRONG    the new class is strongly reachable from its defining loader, so it lives
        //             and dies with the loader. Without it a hidden class may be unloaded as soon
        //             as nothing refers to it, which is what makes hidden classes cheap enough to
        //             spin one per lambda call site.
        public enum ClassOption {
            NESTMATE,
            STRONG
        }

        // Defines a class that is NOT discoverable by name: no loader can find it, `Class.forName`
        // cannot reach it, and the only reference to it is the `Lookup` returned here. That is the
        // supported mechanism behind `LambdaMetafactory` — the class a lambda links to is spun
        // this way — and it is why a lambda's class has a name with a `/` in it that no source
        // could spell.
        //
        // `initialize` decides whether `<clinit>` runs now or on first active use, which matters
        // because a hidden class often exists precisely to hold a constant computed at link time.
        public Lookup defineHiddenClass(byte[] bytes, boolean initialize, ClassOption[] options)
                throws IllegalAccessException {
            throw new UnsupportedOperationException("no class definition without VM support");
        }

        // The same, plus a value handed to the new class out of band — retrievable from inside it
        // with `MethodHandles.classData`. It exists because a hidden class has no name, so there
        // is no way to pass it anything through a static field set from outside: the data has to
        // travel with the definition.
        public Lookup defineHiddenClassWithClassData(byte[] bytes, Object classData,
                boolean initialize, ClassOption[] options) throws IllegalAccessException {
            throw new UnsupportedOperationException("no class definition without VM support");
        }

        public String toString() {
            return "Lookup";
        }
    }
}
