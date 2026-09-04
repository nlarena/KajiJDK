//! **F0 quickening, part 2: the per-call-site cache.** The twin of `objects_operations`'
//! `FieldSite`, for the four `invoke*` opcodes.
//!
//! An `invoke*` at a given `(method, pc)` always names the **same** constant-pool entry, so
//! everything that follows from that entry alone — which method it resolves to, which vtable slot
//! its static type gives, how many operands it pops, whether it is one of the sites the VM
//! intercepts before resolution — is a pure function computed once and reused. Everything that
//! depends on the *receiver* (`vtable_method(runtime_class, slot)`) stays out; that is what an
//! inline cache (Hito F2) would add, and [`SiteKind::Vtable`] is deliberately shaped to leave
//! room for it.
//!
//! What the cache removes, per call, from the uncached path: `class_of(caller).to_string()`, the
//! `HashMap<(String, u16)>` lookup that hashed the whole caller class name **even on a hit**, the
//! two-to-five `to_string()`s of the methodref triple, the `init_states` lookup by `String`, the
//! ~7 string compares against the intrinsic class names (now a [`MetaspaceService::intrinsic`]
//! tag on the callee), and `param_slot_widths`' fresh `Vec` plus descriptor re-parse. A warm site
//! is one indexed atomic load and a handful of shifts.

use crate::jvm::interpreter::metaspace::{MethodId, SignatureId};

/// What a resolved call site dispatches to. The variants split by *how* the target is found, not
/// by opcode — `invokestatic`, `invokespecial` and a nestmate-private `invokevirtual` all land on
/// [`Self::Direct`].
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(super) enum SiteKind {
    /// A statically bound target: the resolved [`MethodId`] *is* the one to run. `invokestatic`,
    /// `invokespecial`, and the JVMS §6.5 rule that a `private` method reached by `invokevirtual`
    /// is its own selected method (nestmate access).
    Direct(MethodId),
    /// A virtual call: the slot comes from the call site's **static** type (cached here); the
    /// method comes from indexing the *receiver's* table at that slot (never cached — the
    /// receiver changes call to call).
    Vtable(usize),
    /// An interface call: no stable slot exists across implementors, so what is cached is the
    /// interned `(name, descriptor)` to search for in the receiver's own table.
    Signature(SignatureId),
    /// An `invokespecial` whose target class can't be loaded — `java.lang.Object.<init>` at the
    /// bottom of every constructor chain. There is no callee: drop the receiver and the arguments
    /// and step past the call.
    NoTarget,
    /// `array.clone()` (JLS §10.7): javac gives the methodref an *array type* owner (`"[I"`),
    /// which is synthetic — no class file, no vtable — so this is intercepted before resolution
    /// can even start.
    ArrayClone,
    /// `MethodHandle.invoke` / `invokeExact` — **signature-polymorphic** (JVMS §2.9.3). The call
    /// site's descriptor is the real one, so this too is intercepted *before* normal resolution,
    /// which would look for the declared `(Object[])Object` and fail.
    MethodHandleInvoke,
    /// `MethodHandle.invokeWithArguments(Object[])` — a regular descriptor, but spreading the
    /// array and dispatching is a VM operation.
    MethodHandleInvokeWithArguments,
    /// A `VarHandle` accessor — **signature-polymorphic** like `MethodHandle.invoke`, and
    /// intercepted for the same reason: the declared `(Object[])Object` is not what the site says.
    ///
    /// The payload is the interned `(name, descriptor)` of the **typed helper** the site maps to
    /// (`get` returning `I` → `VarHandleDeSegmento.leerInt`), found on the receiver's own table
    /// exactly as [`Self::Signature`] does. The trailing arguments — the indices of the path's open
    /// steps — are packed into a `long[]` before the helper is entered.
    VarHandleAccess(SignatureId),
}

impl SiteKind {
    /// The 3-bit discriminant stored in the packed word.
    fn tag(self) -> u64 {
        match self {
            SiteKind::Direct(_) => 0,
            SiteKind::Vtable(_) => 1,
            SiteKind::Signature(_) => 2,
            SiteKind::NoTarget => 3,
            SiteKind::ArrayClone => 4,
            SiteKind::MethodHandleInvoke => 5,
            SiteKind::MethodHandleInvokeWithArguments => 6,
            SiteKind::VarHandleAccess(_) => 7,
        }
    }

    /// The 32-bit payload stored alongside the tag (`0` for the payload-less kinds).
    fn payload(self) -> u64 {
        match self {
            SiteKind::Direct(method) => method as u64,
            SiteKind::Vtable(slot) => slot as u64,
            SiteKind::Signature(id) | SiteKind::VarHandleAccess(id) => id as u64,
            _ => 0,
        }
    }
}

/// A resolved `invoke*` site, `Copy` and packable into the cache's single `u64`.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(super) struct CallSite {
    pub kind: SiteKind,
    /// How many operands the call pops **besides** the receiver — read from the *call site's*
    /// descriptor, which for a signature-polymorphic site is the only correct source.
    pub arg_count: usize,
    /// `true` once the callee's declaring class has reached `Done`, after which
    /// `ensure_initialized` is a permanent no-op and the check can be skipped outright. It is
    /// **never** set for `Erroneous` (which must keep throwing `NoClassDefFoundError`) nor for
    /// `InProgress` (still mid-`<clinit>`), so the only state this bit ever skips is the terminal
    /// one. Only `invokestatic` initializes, so only it reads this.
    pub initialized: bool,
}

impl CallSite {
    /// Packs the site into the cache's `u64`:
    /// `[payload: 32 | arg_count: 16 | unused: 11 | initialized: 1 | kind: 3 | present: 1]`.
    /// Bit 0 is always set, so the cache's zero-initialised cell is an unambiguous "unresolved".
    pub fn pack(self) -> u64 {
        1 | (self.kind.tag() << 1)
            | ((self.initialized as u64) << 4)
            | ((self.arg_count as u64 & 0xffff) << 16)
            | (self.kind.payload() << 32)
    }

    /// The inverse of [`Self::pack`] — `None` for the unresolved sentinel (`0`). The whole hot
    /// path: a load, a few shifts, no allocation and no hashing.
    pub fn unpack(bits: u64) -> Option<CallSite> {
        if bits & 1 == 0 {
            return None;
        }
        let payload = (bits >> 32) as u32;
        let kind = match (bits >> 1) & 0b111 {
            0 => SiteKind::Direct(payload as MethodId),
            1 => SiteKind::Vtable(payload as usize),
            2 => SiteKind::Signature(payload as SignatureId),
            3 => SiteKind::NoTarget,
            4 => SiteKind::ArrayClone,
            5 => SiteKind::MethodHandleInvoke,
            6 => SiteKind::MethodHandleInvokeWithArguments,
            _ => SiteKind::VarHandleAccess(payload as SignatureId),
        };
        Some(CallSite {
            kind,
            arg_count: ((bits >> 16) & 0xffff) as usize,
            initialized: bits & 0b1_0000 != 0,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Pack → unpack is the identity for every kind, and the zero cell is "unresolved". This is
    /// the guarantee the whole cache rests on: a site that read back as a *different* site would
    /// silently call the wrong method, with no test anywhere else to catch it.
    #[test]
    fn call_site_packing_round_trips() {
        assert_eq!(CallSite::unpack(0), None, "the zero cell is the unresolved sentinel");

        let kinds = [
            SiteKind::Direct(0),
            SiteKind::Direct(u32::MAX as MethodId),
            SiteKind::Vtable(0),
            SiteKind::Vtable(4095),
            SiteKind::Signature(7),
            SiteKind::NoTarget,
            SiteKind::ArrayClone,
            SiteKind::MethodHandleInvoke,
            SiteKind::MethodHandleInvokeWithArguments,
        ];
        for kind in kinds {
            for arg_count in [0usize, 1, 255, 0xffff] {
                for initialized in [false, true] {
                    let site = CallSite { kind, arg_count, initialized };
                    let packed = site.pack();
                    assert_ne!(packed & 1, 0, "a packed site always sets the present bit");
                    assert_eq!(
                        CallSite::unpack(packed),
                        Some(site),
                        "round-trip of {site:?} through {packed:#018x}"
                    );
                }
            }
        }
    }
}
