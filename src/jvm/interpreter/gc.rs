//! The garbage collector — **first cut: mark-only**.
//!
//! No memory is reclaimed yet (sweep/compact come later). The point of this pass
//! is to make reachability *visible*: starting from the GC roots, set the mark bit
//! on every object the running program can still reach, so the visualizer can paint
//! live objects apart from garbage (allocated but unreachable).
//!
//! We use a **tracing** collector — the canonical mark phase:
//!   1. clear every object's mark bit;
//!   2. seed a worklist with the *roots* (the references reachable without going
//!      through another object): the operand stacks + locals of every active frame,
//!      and the `Class<…>` mirrors (with the references held in their statics);
//!   3. drain the worklist, marking each object and following its outgoing
//!      references, until nothing new is reachable.
//!
//! Step 3's "follow its outgoing references" is [`reference_slots`] — the heart of
//! the trace, left to implement (it needs each object's field layout).

use std::collections::{HashMap, HashSet};

use crate::jvm::class_file::ClassFile;

use super::bytecode_interpreter::objects_operations::{place_field, HEADER_SIZE, SLOT_SIZE};
use super::bytecode_interpreter::GreenThread;
use super::frame::Value;
use super::heap::{Allocation, Gen, HeapService};
use super::metaspace::MetaspaceService;

/// The outcome of a mark phase, for the visualizer: which allocated objects came
/// out **live** (reachable) and which are **garbage** (unreachable), by start offset.
pub struct MarkReport {
    pub live: Vec<usize>,
    pub garbage: Vec<usize>,
}

/// Runs the **mark** phase and reports what's live vs garbage. Mark-only: it sets
/// mark bits but frees nothing — sweeping/compacting is a later step.
pub fn mark(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &[GreenThread],
    condy_roots: &[usize],
) -> MarkReport {
    mark_with(metaspace, heap, threads, condy_roots, SoftPolicy::Retain)
}

/// [`mark`], with an explicit **soft-reference** policy ([`SoftPolicy`]): under `Retain`
/// a `SoftReference`'s referent is traced as an ordinary strong edge (so it survives);
/// under `Clear` it is traced weakly, exactly like a `WeakReference`'s.
fn mark_with(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &[GreenThread],
    condy_roots: &[usize],
    soft: SoftPolicy,
) -> MarkReport {
    // 1. Start from a clean slate — last pass's marks are stale.
    heap.clear_all_marks();

    // 2. Seed the worklist with the roots, and trace transitively. `seen` guards
    //    against cycles (object A → B → A) and re-visiting shared objects.
    let mut worklist = roots(metaspace, heap, threads, condy_roots);
    let mut seen: HashSet<usize> = HashSet::new();
    while let Some(offset) = worklist.pop() {
        if offset == 0 || !seen.insert(offset) {
            continue; // `null`, or an object we've already marked
        }
        heap.set_mark(offset);
        // Follow each outgoing *strong* reference: `strong_reference_slots` gives the
        // heap addresses of this object's reference words, **minus** the weak `referent`
        // of a `java.lang.ref.Reference` — so a weakly-referenced object isn't kept alive.
        for slot in strong_reference_slots(metaspace, heap, offset, soft) {
            worklist.push(heap.read_u32(slot) as usize);
        }
    }

    // 3. Partition the allocation log into the marked (live) and the rest (garbage).
    let (live, garbage) = heap
        .allocations()
        .iter()
        .map(|a| a.offset)
        .partition(|&off| heap.is_marked(off));
    MarkReport { live, garbage }
}

/// The **major** mark-and-sweep: mark, then reclaim the dead **Old** objects to the
/// Old free list ([`HeapService::free`], which coalesces). Young garbage is left to the minor
/// collector — freeing a young object here would put a young-range hole on the Old free
/// list. Returns the mark report.
pub fn sweep(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &[GreenThread],
    condy_roots: &[usize],
) -> MarkReport {
    sweep_with(metaspace, heap, threads, condy_roots, SoftPolicy::Retain)
}

/// [`sweep`] for a heap **under memory pressure** — the collection that is allowed to
/// give soft caches back. Identical to `sweep` except that `SoftReference` referents are
/// traced weakly, so an otherwise-unreachable one is cleared, enqueued and reclaimed just
/// like a weak referent. See [`SoftPolicy`] for which trigger picks which.
pub fn sweep_under_pressure(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &[GreenThread],
    condy_roots: &[usize],
) -> MarkReport {
    sweep_with(metaspace, heap, threads, condy_roots, SoftPolicy::Clear)
}

/// The shared body of [`sweep`] / [`sweep_under_pressure`], parameterised by the
/// soft-reference policy.
fn sweep_with(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &[GreenThread],
    condy_roots: &[usize],
    soft: SoftPolicy,
) -> MarkReport {
    let report = mark_with(metaspace, heap, threads, condy_roots, soft);
    // Clear weakly-reachable referents and enqueue their references — *before* freeing,
    // while the dead referents are still identifiable.
    process_weak_references(metaspace, heap);
    for &offset in &report.garbage {
        if heap.gen_of(offset) == Gen::Old {
            heap.free(offset); // dead Old → free list, reusable by a later Old alloc
        }
    }
    rebuild_remembered(metaspace, heap); // freed Old holders must leave the set
    report
}

// --- Minor GC: the young generation's copying collector -------------------------
//
// New objects are born in Eden. Most die young, so collecting just the young
// generation — Eden + the from-survivor — is frequent and cheap: we **copy** the few
// survivors out (to the to-survivor, or promoted to Old once they're old enough),
// then recycle Eden and the from-survivor wholesale. Copying is the natural fit for a
// sparse live set (work is proportional to the *survivors*, not the garbage), and it
// compacts for free. References are rewritten through a forwarding map, exactly as the
// major compactor does.

/// Default tenuring threshold: survive this many minor collections and you're promoted
/// to Old. Env: `JVM_GC_TENURE`. (A survivor space filling up also forces promotion.)
const DEFAULT_TENURE: u8 = 3;

/// What a [`minor`] collection did, for the visualizer / stats.
pub struct MinorReport {
    /// Survivors copied to the to-survivor space (still young).
    pub copied: usize,
    /// Survivors **promoted** to Old (reached the tenuring age, or the survivor filled).
    pub promoted: usize,
    /// Young bytes reclaimed — the dead that weren't copied.
    pub reclaimed: usize,
    /// `old offset → new offset` for every object this collection moved — so callers can
    /// fix offset-keyed state the GC doesn't own (e.g. the object-monitor map).
    pub relocations: HashMap<usize, usize>,
}

/// The mutable state of one minor collection — the heap being evacuated, the
/// forwarding map (old young address → new address), and the Cheney scan queue.
struct Minor<'a> {
    metaspace: &'a MetaspaceService,
    heap: &'a mut HeapService,
    /// Pre-collection `(size, age)` of every young object — what [`Minor::evacuate`]
    /// needs to copy and re-age one without consulting the (still-old) log.
    young_info: HashMap<usize, (usize, u8)>,
    /// Forwarding pointers: a young object's old address → where it was copied to.
    forward: HashMap<usize, usize>,
    /// The new log entries for the evacuated objects (survivors + promotions).
    new_objects: Vec<Allocation>,
    /// Cheney scan queue: evacuated objects (at their *new* address) whose own
    /// reference slots still need following.
    scan: Vec<usize>,
    tenure: u8,
    copied: usize,
    promoted: usize,
}

impl Minor<'_> {
    /// Evacuates a young object out of the collection set: copy it to the to-survivor
    /// (or promote to Old if it's old enough or the survivor is full), record the
    /// forwarding pointer, and queue it for scanning. Idempotent via `forward`.
    fn evacuate(&mut self, obj: usize) -> usize {
        if let Some(&new) = self.forward.get(&obj) {
            return new;
        }
        let (size, age) = self.young_info[&obj];
        let new_age = age.saturating_add(1);
        let (dest, promoted) = self.heap.alloc_evacuation(size, new_age >= self.tenure);
        self.heap.evacuate_block(obj, dest, size);
        self.forward.insert(obj, dest);
        let gen = if promoted { Gen::Old } else { Gen::Young };
        self.new_objects.push(Allocation { offset: dest, size, gen, age: new_age });
        self.scan.push(dest);
        if promoted {
            self.promoted += 1;
        } else {
            self.copied += 1;
        }
        dest
    }

    /// Follows one reference slot: if it points into the collection set, evacuate the
    /// target and rewrite the slot to the survivor's new address.
    fn process_slot(&mut self, slot: usize) {
        let target = self.heap.read_u32(slot) as usize;
        if target != 0 && self.heap.in_collection_set(target) {
            let new = self.evacuate(target);
            self.heap.write_u32(slot, new as u32);
        }
    }
}

/// A **minor** collection (JVMS doesn't mandate the algorithm; this is the textbook
/// copying young collector). Evacuates the live objects of Eden + the from-survivor —
/// reachable from the frame roots and from any **Old → young** pointer — into the
/// to-survivor (or Old, by tenuring), rewrites every reference through the forwarding
/// map, then recycles Eden and the from-survivor and swaps the survivor roles.
///
/// Old→young roots are found here by scanning **all** Old objects (and the mirrors).
/// That's correct but not yet cheap — a write barrier + remembered set (next phase)
/// will narrow it to just the Old objects that actually hold young pointers.
pub fn minor(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &mut [GreenThread],
    tenure: u8,
    condy_roots: &mut [usize],
) -> MinorReport {
    // Snapshot the pre-collection log: the young objects (the collection set) and the
    // Old objects (kept as-is, and scanned as roots).
    let young_info: HashMap<usize, (usize, u8)> = heap
        .allocations()
        .iter()
        .filter(|a| a.gen == Gen::Young)
        .map(|a| (a.offset, (a.size, a.age)))
        .collect();
    let young_total: usize = young_info.values().map(|&(size, _)| size).sum();
    let old_objects: Vec<Allocation> =
        heap.allocations().iter().filter(|a| a.gen == Gen::Old).copied().collect();
    // Old→young roots come from two precise sources now: the mirrors (always — their
    // statics may point young) and the remembered set (the write barrier's record of
    // Old objects holding young pointers). No more scanning *all* of Old.
    let mirrors: Vec<usize> =
        metaspace.class_object_offsets().iter().map(|&(_, _, off)| off).collect();
    let old_remembered: Vec<usize> = heap.remembered();

    let mut m = Minor {
        metaspace,
        heap,
        young_info,
        forward: HashMap::new(),
        new_objects: Vec::new(),
        scan: Vec::new(),
        tenure,
        copied: 0,
        promoted: 0,
    };

    // 1. Old → young roots: the mirrors' statics and the remembered Old holders —
    //    their reference slots into the collection set, evacuated and rewritten in place.
    for holder in mirrors.iter().chain(&old_remembered) {
        for slot in reference_slots(m.metaspace, m.heap, *holder) {
            m.process_slot(slot);
        }
    }

    // 2. Frame roots (across every thread): evacuate their collection-set targets.
    for frame in threads.iter().flat_map(|t| t.frames.iter()) {
        for value in frame.stack().iter().chain(frame.locals()) {
            if let Value::Reference(off) = value {
                if *off != 0 && m.heap.in_collection_set(*off) {
                    m.evacuate(*off);
                }
            }
        }
    }
    // 2b. Each thread's own `Thread` object is a root too — the `main` thread holds it
    //     *only* here (its entry frame has no `Thread` receiver), so without this it would
    //     be collected out from under `currentThread()`.
    for t in threads.iter() {
        if t.thread_obj != 0 && m.heap.in_collection_set(t.thread_obj) {
            m.evacuate(t.thread_obj);
        }
    }
    // 2c. VM-held roots the frames don't cover — the condy cache's resolved constants. Evacuate
    //     each so it survives; the caller's copies are rewritten to their new homes below.
    for &r in condy_roots.iter() {
        if r != 0 && m.heap.in_collection_set(r) {
            m.evacuate(r);
        }
    }

    // 3. Cheney scan: copy reachable young transitively, fixing each copied object's
    //    own reference slots as it's scanned at its new address.
    while let Some(obj) = m.scan.pop() {
        for slot in reference_slots(m.metaspace, m.heap, obj) {
            m.process_slot(slot);
        }
    }

    // 4. Rewrite frame references (every thread) through the now-complete forwarding map.
    let forward = std::mem::take(&mut m.forward);
    for frame in threads.iter_mut().flat_map(|t| t.frames.iter_mut()) {
        frame.remap_references(|off| forward.get(&off).copied().unwrap_or(off));
    }
    // ...and the caller's condy roots (in place), so the condy cache points at the new homes.
    for r in condy_roots.iter_mut() {
        *r = forward.get(r).copied().unwrap_or(*r);
    }
    // A thread parked in `wait()` remembers the monitor object to re-acquire — move it too.
    for t in threads.iter_mut() {
        if let Some((obj, count)) = t.wait_reacquire {
            t.wait_reacquire = Some((forward.get(&obj).copied().unwrap_or(obj), count));
        }
        // ...and its `Thread` object (a root, evacuated above) may have moved.
        if t.thread_obj != 0 {
            t.thread_obj = forward.get(&t.thread_obj).copied().unwrap_or(t.thread_obj);
        }
    }


    // 5. Rebuild the remembered set for the next cycle: a holder is kept iff it still
    //    points into the young generation (its targets survived as survivors). The
    //    candidates are the previously-remembered holders plus anything **promoted**
    //    this cycle (now Old, possibly pointing at a survivor). Their young pointers
    //    have all been rewritten to survivor addresses by now, so a `Young` slot is a
    //    real live edge — never a stale one.
    let promoted_offsets: Vec<usize> =
        m.new_objects.iter().filter(|a| a.gen == Gen::Old).map(|a| a.offset).collect();
    let mut remembered: HashSet<usize> = HashSet::new();
    for holder in old_remembered.into_iter().chain(promoted_offsets) {
        let points_young = reference_slots(m.metaspace, m.heap, holder).into_iter().any(|slot| {
            let target = m.heap.read_u32(slot) as usize;
            target != 0 && m.heap.gen_of(target) == Gen::Young
        });
        if points_young {
            remembered.insert(holder);
        }
    }
    m.heap.set_remembered(remembered);

    // 6. Install the new log (kept Old + evacuated) and recycle Eden + from-survivor.
    let (copied, promoted) = (m.copied, m.promoted);
    let survived: usize = m.new_objects.iter().map(|a| a.size).sum();
    let mut new_log = old_objects;
    new_log.append(&mut m.new_objects);
    m.heap.reset_after_minor(new_log);

    MinorReport { copied, promoted, reclaimed: young_total.saturating_sub(survived), relocations: forward }
}

/// **Post-GC heap verifier** (opt-in, `JVM_GC_VERIFY`). Asserts the invariant a moving collector
/// must preserve: *every* reference the program can still reach — in any thread's frame (operand
/// stack + locals) and in any live object's reference slots — points at a live allocation, a
/// `Class` mirror, or null. A reference outside that set is a **dangling pointer**: an object the
/// collector moved or freed without rewriting this holder. Panicking here catches the exact
/// collection that broke the invariant, with the holder and target named — instead of a later,
/// far-removed "could not resolve the receiver" when the stale pointer is finally dereferenced.
///
/// Must run with every thread's stack in its slot (i.e. inside [`Exec::parked`]), so `threads`
/// spans all live frames. O(live set) — hence gated off by default.
pub fn verify_heap(metaspace: &MetaspaceService, heap: &HeapService, threads: &[GreenThread]) {
    let mut live: HashSet<usize> = heap.allocations().iter().map(|a| a.offset).collect();
    for (_uuid, _name, off) in metaspace.class_object_offsets() {
        live.insert(off);
    }
    let describe = |off: usize| {
        // Bounds-checked read: `off` may be out of range (a truly wild pointer), so never a raw read.
        let class = heap
            .try_read_u32(off)
            .and_then(|header| metaspace.class_name_at_mirror(header as usize))
            .unwrap_or("<unresolved>");
        format!("{off} (region={:?}, header→{class})", heap.region_of(off))
    };
    // Frame roots — the receivers/locals a stale value would crash on.
    for (ti, t) in threads.iter().enumerate() {
        for (fi, frame) in t.frames.iter().enumerate() {
            for value in frame.stack().iter().chain(frame.locals()) {
                if let Value::Reference(off) = value {
                    assert!(
                        *off == 0 || live.contains(off),
                        "verify_heap: DANGLING frame reference {} in thread[{ti}] frame#{fi}",
                        describe(*off)
                    );
                }
            }
        }
    }
    // Object reference slots — a missed old→young remap or an un-rewritten field.
    for holder in heap.allocations().iter().map(|a| a.offset).collect::<Vec<_>>() {
        for slot in reference_slots(metaspace, heap, holder) {
            let target = heap.read_u32(slot) as usize;
            assert!(
                target == 0 || live.contains(&target),
                "verify_heap: DANGLING field {}->{} @slot {slot}",
                describe(holder),
                describe(target)
            );
        }
    }
}

/// Recomputes the remembered set from scratch by scanning every Old object for a young
/// pointer. The minor maintains the set incrementally, but a **major** collection moves
/// and frees Old objects, so it rebuilds afterwards. O(Old) — fine off the minor hot
/// path. Mirrors are excluded (they're always scanned as roots, never remembered).
fn rebuild_remembered(metaspace: &MetaspaceService, heap: &mut HeapService) {
    let mirrors: HashSet<usize> =
        metaspace.class_object_offsets().iter().map(|&(_, _, off)| off).collect();
    let old: Vec<usize> = heap
        .allocations()
        .iter()
        .filter(|a| a.gen == Gen::Old && !mirrors.contains(&a.offset))
        .map(|a| a.offset)
        .collect();
    let mut remembered = HashSet::new();
    for holder in old {
        let points_young = reference_slots(metaspace, heap, holder).into_iter().any(|slot| {
            let target = heap.read_u32(slot) as usize;
            target != 0 && heap.gen_of(target) == Gen::Young
        });
        if points_young {
            remembered.insert(holder);
        }
    }
    heap.set_remembered(remembered);
}

// --- Compaction policy: when is the heap fragmented enough to defragment? --------
//
// Not every free hole is fragmentation: a *big* hole gets reused by the next
// `malloc`. Only holes **too small to hold any object** are dead space — bytes no
// allocation can ever reclaim. We track those, and compact (slide live objects
// together to coalesce all the holes) once that waste crosses a budget.
//
// The three knobs are *application variables*: tunable at runtime via environment
// variables (the way a real allocator reads `MALLOC_*` or a JVM its `-XX:` flags),
// falling back to these defaults when unset.

/// A free hole **smaller than this** counts as a fragment. Default `HEADER_SIZE`
/// (8): the smallest allocation is an object header, so a hole under 8 bytes can
/// never be reused. Our heap is 4-byte aligned, so in practice only 4-byte slivers
/// (a `split` leftover) fall here. Env: `JVM_GC_FRAGMENT_MIN_SIZE`.
const DEFAULT_FRAGMENT_THRESHOLD: usize = 8;

/// Compact once the fragmented bytes exceed this **absolute** budget. Env:
/// `JVM_GC_FRAGMENT_BYTE_LIMIT`.
const DEFAULT_FRAGMENT_BYTES_LIMIT: usize = 64;

/// …**or** this fraction of the total heap size (`0.10` = 10%); either limit trips
/// it on its own (OR). Env: `JVM_GC_FRAGMENT_RATIO_LIMIT`.
const DEFAULT_FRAGMENT_RATIO_LIMIT: f64 = 0.10;

// --- When does a collection run? The automatic triggers (all polled at a safepoint).
//
// A real GC's collections are kicked off by the allocator — reactively (no space) or
// proactively (occupancy / allocation-rate), plus the explicit `System.gc()`. None of
// them fire mid-instruction: they fire when the VM reaches a **safepoint**. We poll
// one between opcodes (single-threaded, so every opcode boundary is a safepoint).
//
// All of this is **off by default** (`JVM_GC_AUTO`): an automatic collection runs the
// GC over an arbitrary program state, which is only correct once `reference_slots`
// makes the mark transitive. Enable it to experiment on graphs without inter-object
// references.

/// Why a collection was triggered — the four causes, in priority order.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GcCause {
    /// `System.gc()` — an explicit request from the program.
    Explicit,
    /// Allocation pushed past the heap **capacity** (the reactive, allocation-failure
    /// trigger: collect, then the allocator can carry on reusing reclaimed space).
    OutOfSpace,
    /// Crossed the **occupancy** threshold — proactive, like G1's IHOP.
    Occupancy,
    /// Projected to fill before the **horizon** at the current allocation **rate** —
    /// predictive, like ZGC's rate-driven scheduling.
    AllocationRate,
}

const DEFAULT_AUTO: bool = false;
const DEFAULT_CAPACITY: usize = 1024;
const DEFAULT_OCCUPANCY_RATIO: f64 = 0.75;
const DEFAULT_RATE_HORIZON: usize = 64;

/// The (tunable) GC policy. Built with [`GcPolicy::from_env`] at startup so every
/// knob can be set per-run without recompiling.
#[derive(Debug, Clone, Copy)]
pub struct GcPolicy {
    /// A free hole strictly smaller than this is a fragment (dead, unreusable space).
    pub fragment_threshold: usize,
    /// Compact once total fragmented bytes exceed this absolute count.
    pub fragment_bytes_limit: usize,
    /// …or once they exceed this fraction of the heap's size. (OR with the above.)
    pub fragment_ratio_limit: f64,
    /// Master switch for *automatic* collection (the four triggers below). Off by
    /// default — see the module note on the correctness gate.
    pub auto: bool,
    /// Soft heap capacity in bytes: the line the occupancy/out-of-space triggers
    /// measure against.
    pub capacity: usize,
    /// Collect when used memory reaches this fraction of `capacity` (proactive).
    pub occupancy_ratio: f64,
    /// How many opcodes (our logical clock) the rate trigger looks ahead.
    pub rate_horizon: usize,
    /// Tenuring threshold: a young object promoted to Old after surviving this many
    /// minor collections.
    pub tenure: u8,
    /// **Post-GC heap verification** (`JVM_GC_VERIFY`, off by default). When set, every collection
    /// ends with a full scan asserting that no reference — in any frame or any live object — dangles
    /// (points outside the live set). A full-heap walk per GC, so it's for stress/CI, not production;
    /// it turns a rare, deferred "stale receiver" crash into an immediate, diagnosed failure *at* the
    /// offending collection.
    pub verify: bool,
}

impl Default for GcPolicy {
    fn default() -> Self {
        GcPolicy {
            fragment_threshold: DEFAULT_FRAGMENT_THRESHOLD,
            fragment_bytes_limit: DEFAULT_FRAGMENT_BYTES_LIMIT,
            fragment_ratio_limit: DEFAULT_FRAGMENT_RATIO_LIMIT,
            auto: DEFAULT_AUTO,
            capacity: DEFAULT_CAPACITY,
            occupancy_ratio: DEFAULT_OCCUPANCY_RATIO,
            rate_horizon: DEFAULT_RATE_HORIZON,
            tenure: DEFAULT_TENURE,
            verify: false,
        }
    }
}

impl GcPolicy {
    /// Reads the policy from the environment, falling back to the defaults for any
    /// variable that's unset or unparseable. Read once at VM startup.
    pub fn from_env() -> Self {
        let d = GcPolicy::default();
        GcPolicy {
            fragment_threshold: env_usize("JVM_GC_FRAGMENT_MIN_SIZE", d.fragment_threshold),
            fragment_bytes_limit: env_usize("JVM_GC_FRAGMENT_BYTE_LIMIT", d.fragment_bytes_limit),
            fragment_ratio_limit: env_f64("JVM_GC_FRAGMENT_RATIO_LIMIT", d.fragment_ratio_limit),
            auto: env_bool("JVM_GC_AUTO", d.auto),
            capacity: env_usize("JVM_GC_CAPACITY", d.capacity),
            occupancy_ratio: env_f64("JVM_GC_OCCUPANCY", d.occupancy_ratio),
            rate_horizon: env_usize("JVM_GC_RATE_HORIZON", d.rate_horizon),
            tenure: env_usize("JVM_GC_TENURE", d.tenure as usize) as u8,
            verify: env_bool("JVM_GC_VERIFY", d.verify),
        }
    }

    /// Which automatic trigger (if any) fires now, given the heap's `used` bytes, the
    /// logical clock `step`, and the `used`/`step` snapshot from the **last** GC. The
    /// size-based triggers only fire if memory *grew* since the last GC, so a
    /// collection that can't free anything doesn't re-fire every opcode.
    ///
    /// Returns `None` when automatic GC is off or nothing is warranted. (`Explicit`
    /// — `System.gc()` — is handled by the caller, not here.)
    pub fn auto_cause(
        &self,
        used: usize,
        step: usize,
        last_gc_used: usize,
        last_gc_step: usize,
    ) -> Option<GcCause> {
        if !self.auto || used <= last_gc_used {
            return None; // off, or no new allocation since the last collection
        }
        if used > self.capacity {
            return Some(GcCause::OutOfSpace);
        }
        if used as f64 >= self.occupancy_ratio * self.capacity as f64 {
            return Some(GcCause::Occupancy);
        }
        // Predictive: extrapolate the allocation rate (bytes per opcode) to the
        // horizon; if we'd blow capacity by then, collect now.
        if step > last_gc_step {
            let rate = used.saturating_sub(last_gc_used) as f64 / (step - last_gc_step) as f64;
            let projected = used as f64 + rate * self.rate_horizon as f64;
            if projected >= self.capacity as f64 {
                return Some(GcCause::AllocationRate);
            }
        }
        None
    }
}

/// Reads `key` from the environment as a `usize`, or returns `default` if it's
/// unset or doesn't parse.
fn env_usize(key: &str, default: usize) -> usize {
    std::env::var(key).ok().and_then(|v| v.trim().parse().ok()).unwrap_or(default)
}

/// Reads `key` from the environment as an `f64`, or returns `default` if it's unset
/// or doesn't parse.
fn env_f64(key: &str, default: f64) -> f64 {
    std::env::var(key).ok().and_then(|v| v.trim().parse().ok()).unwrap_or(default)
}

/// Reads `key` as a boolean — `1`/`true`/`yes`/`on` (any case) are true, anything
/// else parseable is false; unset returns `default`.
fn env_bool(key: &str, default: bool) -> bool {
    match std::env::var(key) {
        Ok(v) => matches!(v.trim().to_ascii_lowercase().as_str(), "1" | "true" | "yes" | "on"),
        Err(_) => default,
    }
}

/// Total bytes locked up in **fragments** — free holes too small to hold any object
/// (under `policy.fragment_threshold`), so no `malloc` will ever reuse them. The
/// waste a compaction recovers (a big, reusable hole is *not* counted).
pub fn fragmented_bytes(heap: &HeapService, policy: &GcPolicy) -> usize {
    heap.free_blocks()
        .iter()
        .filter(|&&(_, size)| size < policy.fragment_threshold)
        .map(|&(_, size)| size)
        .sum()
}

/// Whether the heap is fragmented enough to warrant a **compaction**, per `policy`.
/// Trips when the fragmented bytes blow **either** budget — the absolute byte count
/// *or* its share of the heap — whichever hits first. (Compares `frag > ratio *
/// size` instead of dividing, to sidestep integer rounding and a zero-size heap.)
pub fn should_compact(heap: &HeapService, policy: &GcPolicy) -> bool {
    let frag = fragmented_bytes(heap, policy);
    frag > policy.fragment_bytes_limit
        || (frag as f64) > policy.fragment_ratio_limit * heap.size() as f64
}

/// What a [`compact`] pass did, for the visualizer.
pub struct CompactReport {
    /// How many live objects were relocated (slid to a new address).
    pub moved: usize,
    /// Bytes the high-water mark dropped by — the contiguous space handed back.
    pub reclaimed: usize,
    /// `old offset → new offset` for every relocated object — so callers can fix
    /// offset-keyed state the GC doesn't own (e.g. the object-monitor map).
    pub relocations: HashMap<usize, usize>,
}

/// **Mark-compact**: mark the live set, then slide every live object down into one
/// contiguous run, squeezing out the holes, and fix every reference to its new
/// address. Unlike sweep (which leaves reusable holes), this hands back a clean
/// contiguous region — the heavy hammer [`should_compact`] calls for.
///
/// `Class<…>` mirrors are **pinned** (never moved): they're roots and every object's
/// `class_id` points at its mirror, so keeping them put avoids rewriting class
/// headers and the metaspace mirror map. Only instances relocate.
///
/// **The warning that used to be here was stale, and it was worse than no warning.** It said
/// inter-object references were "left until the slot walk exists" and that this was therefore
/// "safe for object graphs without such references (e.g. the demos)". [`reference_slots`] has
/// existed for a long time and step 3(b) below walks every object's reference words with it, young
/// and old alike. A caveat that has outlived its cause does not merely fail to inform: it invites
/// a reader chasing a stale-pointer bug to write this path off as known-broken and go look
/// somewhere else, which is the opposite of what a warning is for.
///
/// What the pass actually rewrites before moving a byte, and each because something would dangle
/// otherwise: the **frame roots** of every thread (precise — `Value` is tagged), the caller's
/// **condy** roots, a parked thread's `wait_reacquire` monitor and its `Thread` object, and then
/// **every reference slot of every object**. The object-monitor map is keyed by offset and is not
/// the GC's to touch, so it comes back in [`CompactReport::relocations`] for the caller to remap.
///
/// The one thing that is genuinely conditional is [`reference_slots`] itself: an object whose
/// header does not resolve to a known mirror yields **no** slots, so its references would be
/// invisible here. That is unreachable if the class-id invariant holds — but it fails *silently*
/// and towards "this object has no references", which is the wrong direction for a collector to
/// be wrong in.
pub fn compact(
    metaspace: &MetaspaceService,
    heap: &mut HeapService,
    threads: &mut [GreenThread],
    condy_roots: &mut [usize],
) -> CompactReport {
    // 1. Mark — only the live get relocated — then process weak references (clear dead
    //    referents + enqueue) before anything moves.
    mark(metaspace, heap, &*threads, condy_roots);
    process_weak_references(metaspace, heap);

    // Pinned set: the mirror offsets and the **string pool** (both stay put).
    //
    // A literal has to be pinned for a reason the mirrors do not share. Step 3(b) below rewrites
    // every reference slot, so an ordinary moved object is fine — but a literal's whole contract is
    // its **identity**, and the pool is keyed by content, not by offset. Move one and the map still
    // names the old address: the next `ldc` of that literal hands back a pointer into whatever now
    // occupies it, and `"a" == "a"` starts answering about someone else's object.
    let pinned: HashSet<usize> = metaspace
        .class_object_offsets()
        .iter()
        .map(|&(_, _, off)| off)
        .chain(metaspace.interned_offsets())
        .collect();
    let before = heap.used();

    // 2. Forwarding addresses for the **Old** generation only (young is copy-collected
    //    by the minor). Walk live Old objects in address order; pinned mirrors stay,
    //    the rest pack down from the Old floor into one contiguous run.
    let mut live_old: Vec<Allocation> = heap
        .allocations()
        .iter()
        .copied()
        .filter(|a| a.gen == Gen::Old && heap.is_marked(a.offset))
        .collect();
    live_old.sort_by_key(|a| a.offset);
    // Young objects ride along unchanged — a major collection doesn't move them.
    let young: Vec<Allocation> =
        heap.allocations().iter().copied().filter(|a| a.gen == Gen::Young).collect();

    let mut forward: HashMap<usize, usize> = HashMap::new();
    let mut new_old: Vec<Allocation> = Vec::with_capacity(live_old.len());
    let mut dest = heap.floor();
    for a in &live_old {
        let to = if pinned.contains(&a.offset) {
            dest = dest.max(a.offset + a.size); // resume relocations past the pinned block
            a.offset
        } else {
            let to = dest;
            dest += a.size;
            to
        };
        if to != a.offset {
            forward.insert(a.offset, to);
        }
        new_old.push(Allocation { offset: to, size: a.size, gen: a.gen, age: a.age });
    }

    // 3. Rewrite references to the moved Old objects — *before* moving, while they're
    //    still at their old locations.
    //    (a) frame roots (every thread): precise, since `Value` is tagged.
    for frame in threads.iter_mut().flat_map(|t| t.frames.iter_mut()) {
        frame.remap_references(|off| *forward.get(&off).unwrap_or(&off));
    }
    //    (a'') the caller's condy roots (in place), so the condy cache follows moved Old constants.
    for r in condy_roots.iter_mut() {
        *r = *forward.get(r).unwrap_or(r);
    }
    //    (a') a thread parked in `wait()` remembers its monitor object — move that too.
    //         Its `Thread` object is a root as well (see the minor collector).
    for t in threads.iter_mut() {
        if let Some((obj, count)) = t.wait_reacquire {
            t.wait_reacquire = Some((*forward.get(&obj).unwrap_or(&obj), count));
        }
        if t.thread_obj != 0 {
            t.thread_obj = *forward.get(&t.thread_obj).unwrap_or(&t.thread_obj);
        }
    }
    //    (b) inter-object references: a pointer to a moved Old object can live in *any*
    //        object's slot — young or old — so rewrite every object's reference words.
    let all: Vec<usize> = heap.allocations().iter().map(|a| a.offset).collect();
    for obj in all {
        for slot in reference_slots(metaspace, heap, obj) {
            let old = heap.read_u32(slot) as usize;
            if let Some(&new) = forward.get(&old) {
                heap.write_u32(slot, new as u32);
            }
        }
    }

    // 4. Move the bytes (address order keeps the slides non-clobbering).
    for a in &live_old {
        if let Some(&to) = forward.get(&a.offset) {
            heap.relocate(a.offset, to, a.size);
        }
    }

    // 5. Install the new layout (young unchanged + relocated Old) and the new Old
    //    high-water; clear the (now-moved) mark bits.
    let new_old_cursor = new_old.iter().map(|a| a.offset + a.size).max().unwrap_or(heap.floor());
    let moved = forward.len();
    let mut new_layout = young;
    new_layout.extend(new_old);
    heap.reset_after_compaction(new_layout, new_old_cursor);
    heap.clear_all_marks();
    rebuild_remembered(metaspace, heap); // Old objects moved → recompute the set
    CompactReport { moved, reclaimed: before.saturating_sub(heap.used()), relocations: forward }
}

/// Gathers the **GC roots** — the references the program reaches *directly*, without
/// dereferencing another object. Two sources (the ones we chose for this first cut):
///  - every **frame** (of **every thread**) — its operand stack and local variables;
///  - the **`Class<…>` mirrors** themselves (always live while their class is
///    loaded). The references held in their *static* slots are reached by tracing
///    the mirror, the same way an object's instance fields are — see
///    [`reference_slots`].
fn roots(
    metaspace: &MetaspaceService,
    _heap: &HeapService,
    threads: &[GreenThread],
    extra: &[usize],
) -> Vec<usize> {
    let mut roots = Vec::new();

    // Resolved dynamic constants (the condy cache) and any other VM-held references the caller
    // passes are roots: they live for the program (e.g. a pattern-`switch`'s `EnumDesc` labels)
    // but are reachable only from the VM's own tables, not from any frame or mirror.
    roots.extend(extra.iter().copied().filter(|&r| r != 0));

    // Stacks + locals of every frame on every thread's call stack. `Value` is tagged,
    // so this is *precise*: we add exactly the references, never an int that looks like one.
    for frame in threads.iter().flat_map(|t| t.frames.iter()) {
        for value in frame.stack().iter().chain(frame.locals()) {
            if let Value::Reference(offset) = value {
                roots.push(*offset);
            }
        }
    }

    // The **string pool** is a root (JLS §3.10.5, FZ-008). Between two `ldc`s of the same
    // literal nothing else refers to it — that is exactly what a pool is for — so without this the
    // first collection frees it and the next `ldc` hands back a dead offset. Nothing is traced
    // *from* a pooled String: this VM lays the text inline, so it has no reference slots.
    roots.extend(metaspace.interned_offsets());

    // The mirrors are roots: a loaded class's statics outlive any object, so the
    // mirror (and, transitively, what its statics point at) is always reachable.
    for (_uuid, _name, offset) in metaspace.class_object_offsets() {
        roots.push(offset);
    }

    // Each live thread's own `Thread` object is a root — `main` holds it only in its slot,
    // so the mark-sweep would otherwise reclaim it (see the same handling in the minor).
    for t in threads.iter() {
        if t.thread_obj != 0 {
            roots.push(t.thread_obj);
        }
    }

    roots
}

// --- weak references (`java.lang.ref`) ------------------------------------------

/// Whether a collection may clear **soft** references — our memory-pressure policy for
/// `java.lang.ref.SoftReference`.
///
/// A soft referent is supposed to survive "while there is room" and be dropped only when
/// the heap is tight. Rather than imitate HotSpot's clock/free-space heuristic, we make
/// the rule deterministic and observable: the *cause* of the collection decides.
///
///  - [`SoftPolicy::Retain`] — the mark traces a soft referent as an ordinary **strong**
///    edge. It is therefore marked, so [`process_weak_references`] skips it and the sweep
///    never frees it: a soft referent always survives.
///  - [`SoftPolicy::Clear`] — the mark traces it **weakly**, exactly like a
///    `WeakReference`'s, so an otherwise-unreachable referent is cleared, enqueued and
///    reclaimed in the same pass.
///
/// The wiring lives in the interpreter's `collect`: a collection caused by memory pressure
/// ([`GcCause::OutOfSpace`] / [`GcCause::Occupancy`] / [`GcCause::AllocationRate`]) clears;
/// an explicit [`GcCause::Explicit`] `System.gc()` retains — as does a standalone
/// [`compact`], which runs after the sweep in the same cycle and must not second-guess it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SoftPolicy {
    /// There is room: soft referents are kept alive.
    Retain,
    /// The heap is under pressure: soft referents die like weak ones.
    Clear,
}

/// The strong outgoing references of an object — [`reference_slots`] minus the **weak**
/// `referent` of a `java.lang.ref.Reference`. The major **mark** uses this so an object
/// isn't kept alive merely because a weak reference points at it. (Compaction and the
/// minor still use the full slot set: compaction must relocate a *surviving* referent,
/// and the minor deliberately keeps young referents alive — see the module note.)
fn strong_reference_slots(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    offset: usize,
    soft: SoftPolicy,
) -> Vec<usize> {
    let mut slots = reference_slots(metaspace, heap, offset);
    if let Some(referent) = weak_referent_slot(metaspace, heap, offset, soft) {
        slots.retain(|&s| s != referent);
    }
    slots
}

/// The referent slot this collection treats as **weak**: every `Reference`'s — except a
/// `SoftReference`'s while the policy is [`SoftPolicy::Retain`], which stays a strong edge.
/// That single exception *is* the soft-reference policy; everything downstream (clearing,
/// enqueueing, freeing) then follows from the mark bits.
fn weak_referent_slot(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    offset: usize,
    soft: SoftPolicy,
) -> Option<usize> {
    if soft == SoftPolicy::Retain && is_soft_reference(metaspace, heap, offset) {
        return None;
    }
    referent_slot(metaspace, heap, offset)
}

/// Whether the object at `offset` is a `java.lang.ref.SoftReference` (or a subclass).
fn is_soft_reference(metaspace: &MetaspaceService, heap: &HeapService, offset: usize) -> bool {
    let Some(class) = metaspace.class_name_at_mirror(heap.read_u32(offset) as usize) else {
        return false;
    };
    let class = class.to_string();
    is_subclass_of(metaspace, &class, "java/lang/ref/SoftReference")
}

/// The heap address of the `referent` field, if `offset` is a `java.lang.ref.Reference`
/// instance — otherwise `None`. The one slot the mark treats as weak.
fn referent_slot(metaspace: &MetaspaceService, heap: &HeapService, offset: usize) -> Option<usize> {
    let class = metaspace.class_name_at_mirror(heap.read_u32(offset) as usize)?.to_string();
    if !is_reference_subclass(metaspace, &class) {
        return None;
    }
    Some(offset + field_byte_offset(metaspace, &class, "referent")?)
}

/// Whether `class` is `java.lang.ref.Reference` or a subclass — i.e. whether its
/// `referent` field is the GC-managed one.
fn is_reference_subclass(metaspace: &MetaspaceService, class: &str) -> bool {
    is_subclass_of(metaspace, class, "java/lang/ref/Reference")
}

/// Whether `class` is `ancestor` or a subclass of it — walking the superclass chain
/// (immutably).
fn is_subclass_of(metaspace: &MetaspaceService, class: &str, ancestor: &str) -> bool {
    let mut current = Some(class.to_string());
    while let Some(name) = current {
        if name == ancestor {
            return true;
        }
        current = metaspace.get(&name).and_then(|cf| cf.class_name(cf.super_class).map(str::to_string));
    }
    false
}

/// The byte offset of a named instance field within an object, by the same width-aware,
/// super-first layout [`instance_reference_slots`] walks. `None` if not found.
fn field_byte_offset(metaspace: &MetaspaceService, class: &str, field: &str) -> Option<usize> {
    let mut chain = Vec::new();
    let mut current = Some(class.to_string());
    while let Some(name) = current.take() {
        match metaspace.get(&name) {
            Some(cf) => {
                current = cf.class_name(cf.super_class).map(str::to_string);
                chain.push(name);
            }
            None => break,
        }
    }
    chain.reverse();
    let mut index = 0;
    for name in &chain {
        let cf = metaspace.get(name)?;
        for f in cf.fields.iter().filter(|f| !f.is_static()) {
            let (start, next) = place_field(index, cf.utf8(f.descriptor_index).unwrap_or(""));
            if cf.utf8(f.name_index) == Some(field) {
                return Some(HEADER_SIZE + start * SLOT_SIZE);
            }
            index = next;
        }
    }
    None
}

/// Processes the weak references after a major mark: for each **live** `Reference`
/// whose `referent` is now unreachable (unmarked), **clear** the referent (so `get()`
/// returns `null`) and, if it was constructed with a `ReferenceQueue`, **enqueue** it
/// (push onto the queue's `head` list through the reference's `next` field). Run once
/// per death — a referent already cleared (0) or still live is skipped.
fn process_weak_references(metaspace: &MetaspaceService, heap: &mut HeapService) {
    let refs: Vec<usize> = heap
        .allocations()
        .iter()
        .map(|a| a.offset)
        .filter(|&off| heap.is_marked(off) && referent_slot(metaspace, heap, off).is_some())
        .collect();
    let head_off = field_byte_offset(metaspace, "java/lang/ref/ReferenceQueue", "head");
    for r in refs {
        let referent_off = referent_slot(metaspace, heap, r).unwrap();
        let referent = heap.read_u32(referent_off) as usize;
        if referent == 0 || heap.is_marked(referent) {
            continue; // already cleared, or the referent is still strongly reachable
        }
        heap.write_u32(referent_off, 0); // clear → get() now returns null
        let class = metaspace.class_name_at_mirror(heap.read_u32(r) as usize).map(str::to_string);
        let queue_off = class.as_deref().and_then(|c| field_byte_offset(metaspace, c, "queue"));
        let next_off = class.as_deref().and_then(|c| field_byte_offset(metaspace, c, "next"));
        if let (Some(q_off), Some(n_off), Some(h_off)) = (queue_off, next_off, head_off) {
            let queue = heap.read_u32(r + q_off) as usize;
            if queue != 0 {
                let head = heap.read_u32(queue + h_off); // push r onto the queue's list
                heap.write_u32(r + n_off, head);
                heap.write_u32(queue + h_off, r as u32);
            }
        }
    }
}

/// The **heap addresses of an object's reference words** — the locations holding the
/// outgoing edges of the object graph. This single function powers both halves of
/// the GC: the **mark** reads each slot to follow the target; the **compactor**
/// rewrites each slot to the target's new address. (It returns the slot *addresses*,
/// not the targets, precisely so the compactor can write through them.)
///
/// Three shapes, told apart from the header: a `Class<…>` **mirror** (its statics),
/// a **reference array** (its elements), or a plain **instance** (its fields). The
/// `class_id` header word is skipped — it points at the (pinned) mirror, so it never
/// needs following or rewriting. Primitive fields/arrays have no outgoing references.
fn reference_slots(metaspace: &MetaspaceService, heap: &HeapService, offset: usize) -> Vec<usize> {
    // A mirror is also "an instance of Class", so check it first: its reference words
    // are the static reference fields of the class it mirrors.
    let mirrored = metaspace
        .class_object_offsets()
        .iter()
        .find(|&&(_, _, off)| off == offset)
        .map(|&(_, name, _)| name.to_string());
    if let Some(class) = mirrored {
        return static_reference_slots(metaspace, &class, offset);
    }

    // Otherwise the runtime class comes from the header's `class_id` (mirror offset).
    let class = match metaspace.class_name_at_mirror(heap.read_u32(offset) as usize) {
        Some(name) => name.to_string(),
        None => return Vec::new(),
    };
    if class.starts_with('[') {
        array_reference_slots(heap, offset, &class)
    } else {
        instance_reference_slots(metaspace, &class, offset)
    }
}

/// Reference fields of a plain instance. Walks the non-static fields with superclass
/// fields **first** (matching the object layout `field_offset` produces), indexing
/// each into a 4-byte slot; the reference-typed ones yield their slot addresses.
fn instance_reference_slots(metaspace: &MetaspaceService, class: &str, offset: usize) -> Vec<usize> {
    // The superclass chain, root-first — supers are laid out before subclass fields.
    let mut chain = Vec::new();
    let mut current = Some(class.to_string());
    while let Some(name) = current.take() {
        match metaspace.get(&name) {
            Some(cf) => {
                current = cf.class_name(cf.super_class).map(str::to_string);
                chain.push(name);
            }
            None => break, // reached Object (not on our classpath) — chain ends
        }
    }
    chain.reverse();

    let mut slots = Vec::new();
    let mut index = 0;
    for name in &chain {
        let Some(cf) = metaspace.get(name) else { continue };
        for field in cf.fields.iter().filter(|f| !f.is_static()) {
            let (start, next) = place_field(index, cf.utf8(field.descriptor_index).unwrap_or(""));
            if is_reference_descriptor(cf, field.descriptor_index) {
                slots.push(offset + HEADER_SIZE + start * SLOT_SIZE);
            }
            // Width-aware + 8-aligned: a `long`/`double` field consumes two slots (and may pad
            // to an even slot), so a reference after it lands correspondingly further along.
            index = next;
        }
    }
    slots
}

/// Element slots of a **reference** array (`[L…;` / `[[…`). Primitive arrays
/// (`[I`, …) hold no references, so they yield none.
fn array_reference_slots(heap: &HeapService, offset: usize, class: &str) -> Vec<usize> {
    if !matches!(class.as_bytes().get(1), Some(b'L') | Some(b'[')) {
        return Vec::new();
    }
    let length = heap.read_u32(offset + HEADER_SIZE) as usize; // the length word
    let elements = offset + HEADER_SIZE + SLOT_SIZE; // past [class_id | mark | length]
    (0..length).map(|i| elements + i * SLOT_SIZE).collect()
}

/// Static reference fields of a class, located in its mirror. Each class keeps its
/// own statics (no superclass flattening), in declaration order — matching the
/// layout `static_slot` uses.
fn static_reference_slots(metaspace: &MetaspaceService, class: &str, mirror: usize) -> Vec<usize> {
    let Some(cf) = metaspace.get(class) else { return Vec::new() };
    let mut slots = Vec::new();
    let mut index = 0;
    for f in cf.fields.iter().filter(|f| f.is_static()) {
        let (start, next) = place_field(index, cf.utf8(f.descriptor_index).unwrap_or(""));
        if is_reference_descriptor(cf, f.descriptor_index) {
            slots.push(mirror + HEADER_SIZE + start * SLOT_SIZE);
        }
        // Width-aware + 8-aligned: a `long`/`double` static consumes two slots (and may pad).
        index = next;
    }
    slots
}

/// Whether a field's descriptor names a *reference* type (`L…;` or `[…`) rather than
/// a primitive (`I`, `J`, …) — the same first-byte test `getfield`/`getstatic` use.
fn is_reference_descriptor(cf: &ClassFile, descriptor_index: u16) -> bool {
    matches!(
        cf.utf8(descriptor_index).and_then(|d| d.as_bytes().first()),
        Some(b'L') | Some(b'[')
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    // The verifier (`JVM_GC_VERIFY`) must actually *fire* on a dangling pointer — a no-op check is
    // worse than none. A thread holds a reference to an offset nothing ever allocated (empty heap):
    // `verify_heap` must catch it. This is the shape of the real os-parallel bug — a stale frame
    // reference — reduced to its essence, so the safety net is proven to work.
    #[test]
    #[should_panic(expected = "DANGLING frame reference")]
    fn verify_heap_catches_a_dangling_frame_reference() {
        use super::super::bytecode_interpreter::ThreadStatus;
        use super::super::frame::Frame;
        let metaspace = MetaspaceService::new(vec![], vec![]);
        let heap = HeapService::new();
        let thread = GreenThread {
            id: 0,
            status: ThreadStatus::Runnable,
            frames: vec![Frame::new(0, 1, vec![Value::Reference(0x9999)])],
            thread_obj: 0,
            wait_reacquire: None,
            joining_on: None,
            sleep_until: None,
            interrupt_pending: false,
            block_call_pc: 0,
            os_handle: None,
            os_spawned: false,
            at_safepoint: false,
            park_permit: false,
            parked: false,
        };
        verify_heap(&metaspace, &heap, &[thread]);
    }

    #[test]
    fn only_sub_object_holes_count_as_fragments() {
        // Fragmentation is an Old-generation concern (it has the free list), so these
        // exercise `malloc_old`.
        let policy = GcPolicy::default();
        let mut heap = HeapService::new();
        let a = heap.malloc_old(16);
        let _b = heap.malloc_old(16);

        // A 16-byte hole is reusable, so it is NOT fragmentation.
        heap.free(a);
        assert_eq!(fragmented_bytes(&heap, &policy), 0);

        // Reusing it for 12 bytes leaves a 4-byte sliver — too small for any object,
        // so that one IS a fragment.
        let _c = heap.malloc_old(12);
        assert_eq!(fragmented_bytes(&heap, &policy), 4);
    }

    #[test]
    fn should_compact_trips_on_the_absolute_byte_budget() {
        let policy = GcPolicy::default();
        let mut heap = HeapService::new();
        assert!(!should_compact(&heap, &policy)); // empty heap: nothing fragmented

        // Build 20 *isolated* 4-byte slivers: each `tmp` is reused for 12 bytes
        // (leaving a 4-byte tail), fenced by a never-freed block so the slivers stay
        // apart and don't coalesce back into a usable hole.
        for _ in 0..20 {
            let _fence = heap.malloc_old(16); // stays allocated — keeps slivers isolated
            let tmp = heap.malloc_old(16);
            heap.free(tmp);
            let _reuse = heap.malloc_old(12); // 4-byte sliver, fenced by live blocks
        }
        // 20 × 4 B = 80 B > 64 B absolute budget → compaction is warranted (the
        // percentage limit, 10% of the 1 KiB heap ≈ 102 B, isn't reached — the OR
        // trips on the byte budget alone).
        assert_eq!(fragmented_bytes(&heap, &policy), 80);
        assert!(should_compact(&heap, &policy));
    }

    #[test]
    fn fragment_threshold_is_configurable() {
        let mut heap = HeapService::new();
        let a = heap.malloc_old(16);
        heap.free(a);
        let _b = heap.malloc_old(12); // 16-byte hole reused → leftover 4-byte hole

        // Under the default threshold (8) the leftover 4-byte hole is the only
        // fragment, and the 12-byte live block is not free at all.
        let lax = GcPolicy { fragment_threshold: 8, ..GcPolicy::default() };
        assert_eq!(fragmented_bytes(&heap, &lax), 4);

        // Raise the threshold and *more* holes count as fragments: with the knob at
        // 5, the 4-byte hole still counts; with it at 4, the same hole no longer does
        // (the bound is strict: `size < threshold`). This is the app-variable knob.
        let strict = GcPolicy { fragment_threshold: 4, ..GcPolicy::default() };
        assert_eq!(fragmented_bytes(&heap, &strict), 0);
    }

    #[test]
    fn major_sweep_reclaims_old_garbage_but_leaves_young() {
        use std::path::PathBuf;
        // An empty metaspace: no classes/mirrors, so `reference_slots` is empty and a
        // header-only object has no outgoing edges.
        let metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let mut heap = HeapService::new();
        let young = heap.malloc(8); // Eden
        let old = heap.malloc_old(8); // Old
        heap.write_u32(young, 0);
        heap.write_u32(old, 0);

        // No roots (no frames) → both are unreachable. The major sweep reclaims the
        // **Old** garbage and leaves the young object to the minor collector.
        heap.commit_pending(); // W2b: the real GC entry (`parked`) flushes pending Eden first
        sweep(&metaspace, &mut heap, &[], &[]);
        assert!(heap.allocations().iter().any(|a| a.offset == young), "young kept");
        assert!(!heap.allocations().iter().any(|a| a.offset == old), "old reclaimed");
    }

    /// **A `Class<…>` mirror does not move when the Old generation is compacted.**
    ///
    /// This is the fact — and until now the *untested* fact — that a whole family of baked-in
    /// immediates rests on: the F3 JIT compiles `checkcast`, `instanceof` and `ldc Foo.class` to a
    /// comparison against, or a materialisation of, a mirror's **heap offset**, and `getstatic` to
    /// an absolute address inside one. Every one of those is a constant in the instruction stream
    /// and can never be re-read, so a mirror that relocated would leave compiled code comparing
    /// against, and handing out, an address that now belongs to some other object. The same
    /// assumption is why `class_id` header words are never rewritten by the compactor.
    ///
    /// It is asserted as a *contrast*: an ordinary Old object of the same size, allocated right
    /// behind the mirror, must slide down into the hole in front of it. Without that half the test
    /// would pass on a compactor that simply moved nothing.

    /// **FZ-008, as a regression test.** The string pool is a GC root: without it the first
    /// collection frees a literal nothing else refers to — which is every literal, between two
    /// `ldc`s of it — and the next `ldc` hands back a dead offset.
    ///
    /// Verified by sabotage: dropping `metaspace.interned_offsets()` from [`roots`] makes this
    /// fail, and it is the only test that notices.
    #[test]
    fn a_pooled_literal_survives_a_collection_nothing_else_references() {
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("KajiLibrary")], vec![PathBuf::from("java")]);
        let mut heap = HeapService::new();

        let literal = crate::jvm::interpreter::strings::intern(&mut metaspace, &mut heap, "kaji");
        // Not on any stack, not in any field, not in a mirror's static: the pool is its only
        // referrer, which is exactly the situation a literal is in between two uses of it.
        let report = sweep(&metaspace, &mut heap, &[], &[]);

        assert!(
            !report.garbage.contains(&literal),
            "the pooled literal at {literal} was collected — the next `ldc` of it would hand back \
             a dead offset, and `\"kaji\" == \"kaji\"` would start answering about whatever \
             took its place"
        );
        assert_eq!(
            metaspace.interned_string(&"kaji".encode_utf16().collect::<Vec<_>>()),
            Some(literal),
            "and the pool still names it"
        );
    }

    /// The other half of FZ-008: a pooled literal is **pinned**, so a compaction cannot move it.
    ///
    /// The mirrors are pinned for a reason literals do not share — every object's header points at
    /// its mirror. A literal is pinned for its own: the pool is keyed by **content**, so a moved
    /// literal leaves the map naming an address something else now occupies, and identity is the
    /// whole contract of a literal.
    #[test]
    fn a_pooled_literal_does_not_move_when_the_heap_compacts() {
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("KajiLibrary")], vec![PathBuf::from("java")]);
        let mut heap = HeapService::new();

        // **The order is the test.** A first `intern` is done only to force `java/lang/String`
        // and its mirror into Old, because a mirror is pinned too — leave it in front of the hole
        // and it plugs it, everything behind stays where it is, and the test passes whether the
        // literal is pinned or not. That is not a hypothetical: the first version of this test was
        // written that way and **the sabotage did not fail it**.
        let earlier = crate::jvm::interpreter::strings::intern(&mut metaspace, &mut heap, "antes");
        let hole = heap.malloc_old(128);
        let literal = crate::jvm::interpreter::strings::intern(&mut metaspace, &mut heap, "kaji");
        heap.free(hole);
        assert!(literal > hole, "the literal has to sit *behind* the hole to have anywhere to go");

        let report = compact(&metaspace, &mut heap, &mut [], &mut []);
        assert!(
            !report.relocations.contains_key(&earlier),
            "the earlier literal moved as well, so this test is measuring the wrong thing"
        );

        assert!(
            !report.relocations.contains_key(&literal),
            "the literal at {literal} moved to {:?} — the pool still names the old address",
            report.relocations.get(&literal)
        );
        assert_eq!(
            metaspace.interned_string(&"kaji".encode_utf16().collect::<Vec<_>>()),
            Some(literal),
            "and the pool agrees with where it actually is"
        );
    }

    /// The half of JLS §3.10.5 that is about **not** sharing: a String the program computes is a
    /// distinct object even when its contents equal a literal.
    ///
    /// Worth its own test because the two are one edit apart — `allocate` and `intern` differ by a
    /// map lookup — and getting it backwards makes `new String("a") == "a"` answer `true`, which is
    /// as wrong as the bug this pool was added to fix. It happened once already, in the first cut
    /// of this fix: `String.rawValueOf` was left calling the pooled entry point.
    #[test]
    fn a_computed_string_is_never_the_pooled_instance() {
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("KajiLibrary")], vec![PathBuf::from("java")]);
        let mut heap = HeapService::new();

        let literal = crate::jvm::interpreter::strings::intern(&mut metaspace, &mut heap, "kaji");
        let again = crate::jvm::interpreter::strings::intern(&mut metaspace, &mut heap, "kaji");
        let computed = crate::jvm::interpreter::strings::allocate(&mut metaspace, &mut heap, "kaji");

        assert_eq!(literal, again, "two `ldc`s of one literal are the same object (JLS 3.10.5)");
        assert_ne!(computed, literal, "a computed String is a distinct object");
        assert_eq!(
            crate::jvm::interpreter::strings::read(&heap, computed),
            "kaji",
            "distinct, and still the same text"
        );
    }

    #[test]
    fn a_class_mirror_is_pinned_across_a_compaction() {
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let mut heap = HeapService::new();

        // Old, in address order: a block that becomes the hole to compact into, an ordinary object,
        // and the mirror behind it. The order matters — a pinned block stops everything behind it
        // from sliding past, so the object that must move has to sit *in front* of the mirror. Both
        // survivors are header-only, so `reference_slots` is empty and neither has an outgoing edge
        // to rewrite.
        let hole = heap.malloc_old(64);
        let plain = heap.malloc_old(16);
        let mirror = heap.malloc_old(16);
        heap.write_u32(mirror, 0);
        heap.write_u32(plain, 0);
        heap.free(hole);

        let uuid = metaspace.class_id("Pinned").to_string();
        metaspace.set_class_object(&uuid, mirror);

        // The mirror is a root by virtue of being one; `plain` is passed as a VM-held root so that
        // it is live and therefore a candidate for relocation.
        let mut extra_roots = [plain];
        let report = compact(&metaspace, &mut heap, &mut [], &mut extra_roots);

        assert!(
            !report.relocations.contains_key(&mirror),
            "the mirror at {mirror} was relocated to {:?} — every baked-in mirror offset in              compiled code is now stale",
            report.relocations.get(&mirror)
        );
        assert!(
            heap.allocations().iter().any(|a| a.offset == mirror),
            "the mirror is still allocated at its original offset"
        );
        assert_eq!(
            metaspace.class_object(&uuid),
            Some(mirror),
            "and the metaspace's mirror index still names it"
        );
        // The contrast: an unpinned neighbour of the same size *does* slide into the hole.
        assert_eq!(
            report.relocations.get(&plain),
            Some(&heap.floor()),
            "the ordinary object should have been packed down to the Old floor"
        );
    }

    #[test]
    fn minor_gc_preserves_survivors_through_a_full_run() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;

        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Genny.class").expect("load Genny");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("Genny.run");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());

        // Genny.run allocates ~200 short-lived objects (overflowing Eden, forcing ~13
        // minor GCs) while keeping `keep` alive and writing an Old→young `keep.next`
        // pointer. The result 19900 + 7 + 199 = 20106 is correct only if every
        // survivor and reference survived evacuation and the forwarding rewrite.
        assert_eq!(execute(metaspace, frame), Some(Value::Int(20106)));
    }

    /// Runs `Class.run()` to completion and returns its int result (the execute harness).
    /// Like [`run_int`] but for a named `()I` method: a fixture that probes several shapes of
    /// one defect exposes a method per shape instead of a class per shape.
    fn run_int_method(class_file: &str, method: &str) -> i32 {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, method, "()I").expect("the ()I method");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("expected an int result from {method}, got {other:?}"),
        }
    }

    /// Runs `method` with **KajiLibrary ahead of `boot/`** on the bootclasspath — the
    /// configuration `run-headless` uses, and the one the library sessions actually develop
    /// against. Returns the raw result, so a test can assert on a thread that ended *without*
    /// one. See COMPILER_FINDINGS #246: the two libraries diverge, and the VM must not assume
    /// the shape of whichever one it was written against.
    fn run_with_kajilibrary(class_file: &str, method: &str) -> Option<Value> {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![PathBuf::from("java")],
        );
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, method, "()I").expect("the ()I method");
        let max_locals = metaspace.max_locals(entry);
        execute(metaspace, Frame::new(entry, max_locals, Vec::new()))
    }

    /// A `Throwable` is not required to declare the `backtrace` field the VM writes its captured
    /// stack trace into — that field is **our own convention**, and
    /// `KajiLibrary/java/lang/Throwable` declares only `message` and `cause`
    /// (COMPILER_FINDINGS #227). The VM assumed `boot/`'s shape, so **any** uncaught exception
    /// took the whole VM down with `field_offset: field not found in the class or its
    /// superclasses` — before printing the report it exists to print.
    ///
    /// The thread ends with no value (JVMS §2.10); what this pins is that the VM survives to say
    /// so, with or without a trace to show.
    #[test]
    fn an_uncaught_exception_survives_a_throwable_with_no_backtrace_field() {
        assert_eq!(run_with_kajilibrary("java/VmProbe.class", "p227"), None);
    }

    /// `invokeinterface` with a **`String`** receiver (COMPILER_FINDINGS #225). `CharSequence cs
    /// = "abc"; cs.length()` blew the interpreter up with an index-out-of-bounds — and since
    /// `Matcher` works through `CharSequence`, it made all of `java.util.regex` unrunnable.
    ///
    /// Needs KajiLibrary on the bootclasspath: `boot/`'s `String` implements **no** interfaces at
    /// all, so it cannot even express the call. Only the library that is actually developed
    /// declares `implements Comparable<String>, CharSequence` — the #246 divergence again.
    #[test]
    fn invokeinterface_dispatches_on_a_string_receiver() {
        assert_eq!(run_with_kajilibrary("java/VmProbe.class", "p225"), Some(Value::Int(3)));
    }

    /// `invokevirtual` through an **abstract** declaration (COMPILER_FINDINGS #230). Two
    /// independent library sessions hit this; the A/B is in the fixture: the same body, the same
    /// class, the same static type — the only variable is whether the method overrides an
    /// `abstract` one. `build_vtable` skips any method `resolve_method` cannot resolve, and
    /// `resolve_method` gives up on a member with no `Code`, so an abstract declaration never took
    /// a slot — and the call site, which reads the slot off the **static** type, found nothing.
    #[test]
    fn invokevirtual_dispatches_through_an_abstract_declaration() {
        assert_eq!(run_int("java/AbsProbe.class"), 7); // por el tipo abstracto
        assert_eq!(run_int_method("java/AbsProbe.class", "viaConcrete"), 7); // control
        assert_eq!(run_int_method("java/AbsProbe.class", "viaExact"), 7); // control
    }

    /// A `String` is **UTF-16** (COMPILER_FINDINGS #229). The VM laid the UTF-8 bytes inline and
    /// called the byte count `length`, so every non-ASCII literal was wrong in both directions:
    /// `"ñ".length()` answered 2 and `charAt(0)` handed back `0xC3`, the first byte of the
    /// encoding, instead of the character.
    ///
    /// The fixture is compiled with the **real** javac (`-encoding UTF-8`): its `CONSTANT_Utf8`
    /// carries proper modified UTF-8, including the surrogate pair for the astral character —
    /// which our class reader already decodes correctly. The loss happened afterwards, on the way
    /// into the heap.
    #[test]
    fn a_string_is_measured_in_utf16_code_units() {
        let at = |m| run_int_method("java/Utf16Probe.class", m);
        assert_eq!(at("lenAscii"), 3); // control: ASCII ya andaba
        assert_eq!(at("len1"), 1); // U+00F1: 1 unidad, 2 bytes UTF-8
        assert_eq!(at("charAt1"), 0x00F1);
        assert_eq!(at("len3"), 1); // U+20AC: 1 unidad, 3 bytes UTF-8
        assert_eq!(at("charAt3"), 0x20AC);
        assert_eq!(at("lenMixed"), 3); // "añb"
        assert_eq!(at("charAtMixed"), 'b' as i32);
        assert_eq!(at("lenAstral"), 2); // U+1D160: par subrogado = 2 unidades
        assert_eq!(at("charAtAstral"), 0xD834); // la mitad alta
    }

    /// `new String(...)` works, though no String constructor assigns anything.
    ///
    /// A String is sized when it is allocated, because its characters sit inline; the `new`
    /// opcode sizes an instance from its declared fields and `String` declares none. So `new`
    /// hands the constructor an object with room for nothing, and a heap block does not grow.
    /// Each constructor therefore builds a separate String and publishes it
    /// (`Exec::string_publish`); the `return` that ends it rewrites the caller's references
    /// from the object it was handed to the one it built.
    ///
    /// The six constructors that take a `byte[]` and a charset are NOT covered here. They pass
    /// against the JDK, and they are blocked on this VM by #110 exactly as the whole of
    /// java.nio.charset is -- decoding reads `StandardCharsets.UTF_8`, a cross-unit static.
    ///
    /// `java/CtorTest.java` is the same source the JDK 25 runs to 0, so these counts are the
    /// reference answers and not this VM agreeing with itself. Each group is asserted on its
    /// own: a single total would say "something broke" without saying what.
    #[test]
    fn a_string_constructor_is_rewritten_into_a_factory_call() {
        let at = |m| run_with_kajilibrary("java/CtorTest.class", m);
        assert_eq!(at("caracteres"), Some(Value::Int(0))); // char[], entero y rebanado, y ES copia
        assert_eq!(at("vacio"), Some(Value::Int(0))); // String()
        assert_eq!(at("noAscii"), Some(Value::Int(0))); // lo que el layout inline puede perder
        assert_eq!(at("puntosDeCodigo"), Some(Value::Int(0))); // int[]: 3 entran, 4 chars salen
        assert_eq!(at("deOtros"), Some(Value::Int(0))); // String/StringBuilder/StringBuffer
        assert_eq!(at("altoByte"), Some(Value::Int(0))); // las dos formas deprecadas
        assert_eq!(at("fueraDeRango"), Some(Value::Int(0))); // la rebanada mala tira, no recorta
        assert_eq!(at("esUnStringDeVerdad"), Some(Value::Int(0))); // trim/substring/concat/hash
    }

    fn run_int(class_file: &str) -> i32 {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("expected an int result, got {other:?}"),
        }
    }

    #[test]
    fn call_java_runs_a_method_and_hands_back_its_result() {
        // The capability that stops intrinsics from being terminal: the VM invokes a Java
        // method from the outside and gets the value back. Class initialization has
        // always done the frame-pushing half; what is new is capturing a result, which a
        // `void` `<clinit>` never exercises.
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::JVM;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;

        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Lambdas.class").expect("load Lambdas");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run");
        let twice = metaspace.resolve_method(&name, "twice", "(I)I").expect("twice");
        let max_locals = metaspace.max_locals(entry);

        // A JVM parked on `run`, which never executes: `call_java` drives `twice` on top
        // of it and unwinds back, leaving the caller exactly as it was.
        let mut jvm = JVM::new(metaspace, Frame::new(entry, max_locals, Vec::new()));
        assert_eq!(jvm.exec().call_java(twice, vec![Value::Int(21)], &[1]), Some(Value::Int(42)));

        // And a second call is independent — the nested loop leaves no residue.
        assert_eq!(jvm.exec().call_java(twice, vec![Value::Int(-3)], &[1]), Some(Value::Int(-6)));
    }

    #[test]
    fn concatenating_an_object_asks_it_for_its_text() {
        // `"x" + obj`. javac emits `String.valueOf(Object)` *before* the concatenation
        // call site, so the indy only ever sees Strings — which means the interesting
        // work isn't in the opcode at all. `valueOf` has to call the object's own
        // `toString()`, a virtual call back into user bytecode, so it is intercepted
        // ahead of the native bridge rather than being a leaf native.
        assert_eq!(run_int("java/ObjConcat.class"), 42);
    }

    #[test]
    fn record_with_a_reference_component_asks_the_component() {
        // `RecStr(String, int)`. Everything here depends on asking the component itself:
        // the two `"bob"` literals are distinct heap objects (nothing is interned), so
        // comparing the *references* — which is what this did before — answered `false`
        // where Java answers `true`. The hash likewise folds the String's own, and the
        // text comes from its `toString`. The expected hash (3029228) was read off the
        // real `java` rather than derived, and `java` returns 42 on the same class file.
        assert_eq!(run_int("java/RecStrOps.class"), 42);
    }

    #[test]
    fn enum_pattern_switch_resolves_its_dynamic_labels() {
        // The whole of D4 in one demo. Each case label is a *dynamic constant* whose
        // value is produced by `ConstantBootstraps.invoke` — which means the VM has to
        // run Java (`ClassDesc.of`, then `Enum$EnumDesc.of`) just to know what the label
        // *is*. The two labels share one `ClassDesc` condy, so the cache is part of the
        // design rather than an optimisation. Real `java` returns 42 on the same file.
        assert_eq!(run_int("java/EnumSwitch.class"), 42);
    }

    #[test]
    fn interrupt_wakes_a_sleeping_thread() {
        // A worker sleeps 100000; main interrupts it; the worker catches
        // InterruptedException out of sleep(). The demo also checks the throw *cleared* the
        // interrupt flag (isInterrupted() false in the handler), per JLS. Real `java` returns
        // 42 on the same class files.
        assert_eq!(run_int("java/InterruptSleep.class"), 42);
    }

    #[test]
    fn interrupt_wakes_a_joining_thread() {
        // A joiner blocked in join() on a long spinner is interrupted and catches
        // InterruptedException out of join(). Real `java` returns 42.
        assert_eq!(run_int("java/InterruptJoin.class"), 42);
    }

    #[test]
    fn interrupt_wakes_a_waiting_thread_holding_its_lock() {
        // A thread in `wait()` is interrupted. The catch must run **holding the monitor
        // again** (JLS: wait re-acquires the lock before the InterruptedException is seen) —
        // the demo asserts `Thread.holdsLock(lock)` in the handler. This is the notify/
        // interrupt race path, resolved by the GIL serialising the two. Real `java` → 42.
        assert_eq!(run_int("java/InterruptWait.class"), 42);
    }

    #[test]
    fn thread_interrupt_flag_set_read_and_clear() {
        // The flag half of interruption (not the waking half): `interrupt()` sets it,
        // `isInterrupted()` reads without clearing, `interrupted()` (static) reads and
        // clears the current thread's. The flag lives on the *object*, so a NEW thread can
        // be interrupted before it starts — verified against real `java`, which is what
        // forced that placement. Pure Java on top of currentThread(); no new native.
        assert_eq!(run_int("java/ThreadInterruptFlag.class"), 42);
    }

    #[test]
    fn thread_identity_current_name_and_id() {
        // The identity wiring: `currentThread()` from **main** returns a real Thread object
        // named "main" (fabricated on first ask, kept alive because `thread_obj` is now a
        // GC root), and it's the *same* object each call. A spawned thread gets a default
        // "Thread-N" name and a distinct id; `setName` sticks. Real `java` returns 42 too
        // — the demo avoids asserting main's exact id, which is implementation-defined.
        assert_eq!(run_int("java/ThreadIdentity.class"), 42);
    }

    #[test]
    fn thread_wiring_runnable_isalive_and_double_start() {
        // The H1 wiring layer: `new Thread(() -> ...)` runs the lambda target (proving the
        // Runnable path *and* that a lambda satisfies Runnable), `isAlive()` reads false
        // before start and after termination (pure Java on getState()), and a second
        // `start()` on the finished thread throws IllegalThreadStateException. Real `java`
        // returns 42 on the same class files.
        assert_eq!(run_int("java/ThreadWiring.class"), 42);
    }

    #[test]
    fn thread_get_state_maps_the_scheduler_state() {
        // `Thread.getState()` reads the scheduler's authoritative state and returns the
        // matching `Thread.State` constant — the *same object* the enum holds, so
        // `getState() == Thread.State.NEW` works. The demo pins NEW (created, not started
        // → no scheduler slot), TERMINATED (after join), and that the constants carry real
        // enum behaviour (ordinal/name). Real `java` returns 42 on the same class files.
        assert_eq!(run_int("java/ThreadState.class"), 42);
    }

    #[test]
    fn enum_constants_carry_name_and_ordinal() {
        // Enums ran even before `java.lang.Enum` existed: the constants are created by
        // the class's `<clinit>`, and the unresolvable superclass `<init>` no-opped (the
        // same escalón the exception hierarchy uses), so identity comparison was already
        // right. With a real `java.lang.Enum` in `boot/` they also carry state — the
        // `name()`/`ordinal()` checks fail unless its constructor genuinely runs, which
        // is what distinguishes "the superclass exists" from "the call no-ops".
        assert_eq!(run_int("java/EnumProbe.class"), 42);
    }

    #[test]
    fn a_captured_reference_survives_a_collection() {
        // A lambda capturing a String, with a `System.gc()` in between. The capture lives
        // in a synthetic class the VM mints — no class file, so no field descriptors for
        // the collector to walk. It is visible only because the class **declares its
        // reference layout** to the metaspace, which is where `reference_slots` looks.
        //
        // Without that the test fails two different ways: the String may be collected out
        // from under the lambda, and a moving collection leaves the capture pointing at
        // the old address. Real `java` returns 42 too.
        assert_eq!(run_int("java/LambdaRef.class"), 42);
    }

    #[test]
    fn lambda_capture_reaches_the_implementation() {
        // The smallest capturing lambda: `int n = 5; a -> a + n` called with 10. The
        // capture becomes the implementation's leading parameter, so `lambda$run$0(5, 10)`
        // must run and give 15.
        assert_eq!(run_int("java/L2.class"), 15);
    }

    #[test]
    fn lambdas_and_method_references_run() {
        // `Lambdas.run()` covers a lambda with no capture, a method reference, and a
        // capture (which becomes the implementation's *leading* parameter). The decisive
        // case is `adder(1)` vs `adder(2)`: one call site, two objects, two captures —
        // which is why the captured values live in each object while the shape (the
        // implementation method) is shared by the site. Real `java` returns 42 too.
        assert_eq!(run_int("java/Lambdas.class"), 42);
    }

    #[test]
    fn record_methods_run_from_one_bootstrap() {
        // `RecordOps.run()` drives the `Point` record. Its `equals`/`hashCode`/`toString`
        // all come from a *single* BootstrapMethods entry — `ObjectMethods.bootstrap` —
        // and are told apart only by the call site's **name**, which is why discarding
        // that name would have collapsed the three into one. The demo pins value
        // equality (distinct objects, equal components), rejection of null and of a
        // different class, the exact `31*acc + h` folding, and the
        // `Point[x=1, y=2]` layout. Real `java` returns 42 on the same class files.
        assert_eq!(run_int("java/RecordOps.class"), 42);
    }

    #[test]
    fn type_switch_selects_the_matching_case() {
        // `TypeSwitch.run()` drives a pattern `switch` — an `invokedynamic` bootstrapped
        // by `SwitchBootstraps.typeSwitch`, whose call site answers *which case to run*
        // as an index the `tableswitch` consumes. The demo pins all three outcomes of
        // the contract: null → -1, the index of the first matching label, and
        // labels.length → default. It also checks that a subclass matches a superclass
        // label, so the match walks the hierarchy instead of comparing identity.
        // Real `java` returns 42 on the same class file.
        assert_eq!(run_int("java/TypeSwitch.class"), 42);
    }

    #[test]
    fn ldc_of_a_class_literal_pushes_the_mirror() {
        // `ClassLit.run()` does six `ldc`s of Class constants. The demo checks the two
        // properties that matter: the mirror is **cached by Class ID**, so the same
        // literal evaluated twice is the *same* reference (`Foo.class == Foo.class`),
        // and distinct classes never collapse onto one mirror. It then feeds the mirror
        // to `Class.isInstance`, proving it's a real object the natives can use. Real
        // `java` returns 42 on the same class file.
        assert_eq!(run_int("java/ClassLit.class"), 42);
    }

    #[test]
    fn invokedynamic_renders_floats_like_java() {
        // The call site descriptor is `(DF)`, so the double and float arrive raw and the
        // VM renders them itself. Java prints `1.0` where Rust's `Display` prints `1`,
        // so the concatenation goes through `float_to_decimal` — the same Java-faithful
        // formatter that makes `javap` byte-identical — instead of `to_string()`. Real
        // `java` returns 42 on this same class file.
        assert_eq!(run_int("java/ConcatFloat.class"), 42);
    }

    #[test]
    fn invokedynamic_runs_string_concatenation() {
        // `Concat.run()` is eight `invokedynamic` call sites, all bootstrapped by
        // StringConcatFactory.makeConcatWithConstants — which is what every `+` on
        // strings has compiled to since Java 9. The demo pins the cases where the
        // *descriptor* decides the rendering rather than the `Value`: a `char` must
        // print as 'A' and not 65, a `boolean` as `true` and not `1`. It also covers a
        // String argument read back out of the heap, a category-2 `long`, several
        // arguments spliced by one call site, and a null rendering as "null". Each
        // failure returns its own negative code; 42 means all of them held, and the
        // real `java` of JDK 25 agrees on the same class file.
        assert_eq!(run_int("java/Concat.class"), 42);
    }

    #[test]
    fn multianewarray_builds_every_level() {
        // `MultiArray.run()` makes javac emit three `multianewarray`s: `[[I` (2 dims),
        // `[[[I` (3 dims) and `[[B` (2 dims). The demo checks the shape of each level,
        // that the rows are *distinct objects* (the classic bug is allocating one child
        // and storing it N times), that the recursion reaches the third dimension, and
        // that a `byte[][]` row is one byte per element rather than four — a wrong
        // element width would make the rows overlap. Every failure mode returns its own
        // negative code; 42 means all of them held. The real `java` of JDK 25 agrees.
        assert_eq!(run_int("java/MultiArray.class"), 42);
    }

    #[test]
    fn wide_prefix_addresses_locals_past_slot_255() {
        // `WideLocals.run()` declares 300 int locals, so `javac` *must* use the `wide`
        // prefix to reach the last one: the tail compiles to `istore_w 299`,
        // `iinc_w 299, 35` (6 bytes) and `iload_w 299` (4 bytes). Without the 0xc4
        // handler this hits the `todo!()`; with a wrong instruction length the pc
        // desynchronises and the method decodes garbage. 7 + 35 = 42, and the real
        // `java` of JDK 25 agrees on the same class file.
        assert_eq!(run_int("java/WideLocals.class"), 42);
    }

    /// Like `run_int` but forces the **os-gil** substrate (real `std::thread`s + GIL),
    /// bypassing the `JVM_THREADS` env so parallel tests don't race on a global.
    fn run_int_os(class_file: &str) -> i32 {
        use crate::jvm::interpreter::bytecode_interpreter::execute_os_gil;
        run_int_with(class_file, execute_os_gil)
    }

    /// Like `run_int_os` but forces the **os** substrate (real `std::thread`s, GIL-free —
    /// H3, still sharing the os-gil engine for now). Proves the new mode runs and agrees
    /// with the green/os-gil oracle.
    fn run_int_os_parallel(class_file: &str) -> i32 {
        use crate::jvm::interpreter::bytecode_interpreter::execute_os_parallel;
        run_int_with(class_file, execute_os_parallel)
    }

    /// Shared body of the OS-mode test harnesses: load the class, run its `run()I` on the
    /// given engine, and unwrap the int result. `engine` is `execute_os_gil` or
    /// `execute_os_parallel` (same signature — the mode differs only in the tag today).
    fn run_int_with(
        class_file: &str,
        engine: fn(MetaspaceService, crate::jvm::interpreter::frame::Frame) -> Option<Value>,
    ) -> i32 {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match engine(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("expected an int result, got {other:?}"),
        }
    }

    #[test]
    fn os_threads_monitor_exclusion() {
        // The same demos as the green tests, but each java.lang.Thread is a real
        // std::thread serialised by the GIL. Mutual exclusion via the intrinsic monitor
        // (block form and synchronized-method form) still holds → exactly 200.
        assert_eq!(run_int_os("java/Sync.class"), 200);
        assert_eq!(run_int_os("java/SyncMethod.class"), 200);
    }

    #[test]
    fn os_threads_spawn_and_spin() {
        // main spawns two workers (real OS threads) and spin-waits on shared statics → 100.
        assert_eq!(run_int_os("java/Threads.class"), 100);
    }

    #[test]
    fn os_threads_wait_notify_and_join() {
        // wait/notify (park/unpark across OS threads) and join + sleep (real wall time in
        // OS mode) coordinate exactly as in green mode.
        assert_eq!(run_int_os("java/WaitNotify.class"), 42);
        assert_eq!(run_int_os("java/Joiner.class"), 30);
        // Timed wait(50) expires by real time in OS mode → 7.
        assert_eq!(run_int_os("java/WaitTimeout.class"), 7);
    }

    #[test]
    fn os_threads_illegal_monitor_state() {
        // notify() without the monitor still throws IllegalMonitorStateException → 99.
        assert_eq!(run_int_os("java/Imse.class"), 99);
    }

    #[test]
    fn atomic_integer_cas() {
        // H5: AtomicInteger built on the native compareAndSet — direct CAS (success + failure),
        // get, incrementAndGet, getAndAdd, addAndGet. → 30, confirmed vs real java.
        assert_eq!(run_int("java/Cas.class"), 30);
    }

    #[test]
    fn count_down_latch_releases_waiters() {
        // H6: three worker threads block on CountDownLatch.await(); main counts the latch to zero,
        // releasing them all, and each increments a shared AtomicInteger → 3. Oracle green ≡ os-gil
        // ≡ os (the latch is monitor-based: wait/notifyAll over real threads).
        assert_eq!(run_int("java/CdlTest.class"), 3); // green
        assert_eq!(run_int_os("java/CdlTest.class"), 3); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CdlTest.class"), 3); // os (parallel)
        }
    }

    #[test]
    fn aqs_lock_serialises() {
        // H6: a lock built on AbstractQueuedSynchronizer (state CAS + a park/unpark waiter queue).
        // Three workers, 100 guarded non-atomic increments each → 300 iff acquire/release serialise.
        // In os (real parallelism) a broken AQS loses updates (<300) or hangs; green/os-gil agree.
        assert_eq!(run_int("java/AqsLockTest.class"), 300); // green
        assert_eq!(run_int_os("java/AqsLockTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/AqsLockTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn lock_support_park_unpark() {
        // H6 (AQS foundation): LockSupport.park/unpark with permit semantics. Three workers
        // park-loop until a flag is set; main sets it and unparks each → 3. The permit makes an
        // unpark-before-park not get lost, so it's correct under any ordering (green/os-gil/os).
        assert_eq!(run_int("java/ParkTest.class"), 3); // green
        assert_eq!(run_int_os("java/ParkTest.class"), 3); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/ParkTest.class"), 3); // os (parallel)
        }
    }

    #[test]
    fn condition_await_and_signal() {
        // H6: Condition on a ReentrantLock. Three workers `await()` (releasing the lock, blocking)
        // until main sets `ready` and `signalAll()`s; each re-acquires the lock, sees ready, and
        // increments → 3. Exercises the fully-release / re-acquire dance across real threads.
        assert_eq!(run_int("java/CondTest.class"), 3); // green
        assert_eq!(run_int_os("java/CondTest.class"), 3); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CondTest.class"), 3); // os (parallel)
        }
    }

    #[test]
    fn cyclic_barrier_holds_all_parties() {
        // H6: CyclicBarrier(3) — each worker increments `before`, waits, then checks before==3;
        // that holds for all three only if the barrier held everyone until the last arrived → 3.
        assert_eq!(run_int("java/BarrierTest.class"), 3); // green
        assert_eq!(run_int_os("java/BarrierTest.class"), 3); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/BarrierTest.class"), 3); // os (parallel)
        }
    }

    #[test]
    fn array_blocking_queue_producer_consumer() {
        // H6 volume: ArrayBlockingQueue(4) — three producers put 100 tokens each, main takes all
        // 300 and sums → 300 iff the bounded queue blocks/wakes correctly (notFull/notEmpty over a
        // ReentrantLock). Small capacity forces real put-while-full / take-while-empty blocking.
        assert_eq!(run_int("java/AbqTest.class"), 300); // green
        assert_eq!(run_int_os("java/AbqTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/AbqTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn delay_queue_releases_in_expiration_order() {
        // H6 volume: DelayQueue — 10 items put in reverse (largest delay first); take() releases each
        // only once its delay elapsed, earliest-expiration first, via a timed wait. Out in ascending
        // id (0..9) → 10.
        assert_eq!(run_int("java/DqTest.class"), 10); // green
        assert_eq!(run_int_os("java/DqTest.class"), 10); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/DqTest.class"), 10); // os (parallel)
        }
    }

    #[test]
    fn priority_blocking_queue_orders_across_producers() {
        // H6 volume: PriorityBlockingQueue — 3 producers put 300 distinct-priority items concurrently;
        // after they join, main takes all 300 and they come out in strictly ascending order (the
        // min-heap orders regardless of insertion order/thread) → 300.
        assert_eq!(run_int("java/PqTest.class"), 300); // green
        assert_eq!(run_int_os("java/PqTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/PqTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn completable_future_then_compose_flattens() {
        // H6 volume: CompletableFuture.thenCompose — the mapping function returns another future
        // (async double on the pool); thenCompose flattens CF<CF<Box>> → CF<Box>, so get() yields
        // the doubled value 42, not a nested future.
        assert_eq!(run_int("java/CcComposeTest.class"), 42); // green
        assert_eq!(run_int_os("java/CcComposeTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CcComposeTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn completable_future_supply_then_apply_chain() {
        // H6 volume: CompletableFuture — supplyAsync (on a ThreadPoolExecutor) → thenApply → get().
        // The supplier produces Box(21), thenApply doubles it to Box(42), get() blocks for the chain
        // → 42. Exercises complete()/dependent firing + the blocking get() handshake.
        assert_eq!(run_int("java/CfTest.class"), 42); // green
        assert_eq!(run_int_os("java/CfTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CfTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn completable_future_exceptionally_recovers_pool_failure() {
        // H6 volume: CompletableFuture.exceptionally — the supplier throws on the pool, so supplyAsync
        // completes the future EXCEPTIONALLY (captures the Throwable); exceptionally() recovers it to
        // 99, so get() returns the recovered value instead of throwing. Exercises the failure-capture
        // path and downstream propagation → recovery.
        assert_eq!(run_int("java/CeTest.class"), 99); // green
        assert_eq!(run_int_os("java/CeTest.class"), 99); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CeTest.class"), 99); // os (parallel)
        }
    }

    #[test]
    fn class_is_annotation_present_reads_runtime_visible_annotations() {
        // A7 #9: runtime annotations (JSR 175) — Class.isAnnotationPresent. @AnMark is declared
        // @Retention(RUNTIME), so javac writes a RuntimeVisibleAnnotations attribute (§4.7.16)
        // holding "LAnMark;" into AnMarked.class; the native reads that attribute off the mirror's
        // class file and matches descriptors. Score 20 (present on AnMarked) + 12 (absent on the
        // unannotated AnPlain) + 10 (absent on int.class, a mirror with no class file) = 42.
        assert_eq!(run_int("java/AnTest.class"), 42); // green
        assert_eq!(run_int_os("java/AnTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/AnTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn class_get_name_reports_dotted_names() {
        // A7 #8: Class.getName()/getSimpleName() — minimal reflection. getClass() hands back the
        // mirror; getName() reads the class name off it in JDK format (dotted binary name), so the
        // unpackaged test class reports "GnTest" and a `String.class` literal reports
        // "java.lang.String"; getSimpleName() yields the trailing segment. Score
        // 10 (getName on own class) + 12 (getName on String.class) + 20 (getSimpleName) = 42.
        assert_eq!(run_int("java/GnTest.class"), 42); // green
        assert_eq!(run_int_os("java/GnTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/GnTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn completable_future_then_combine_merges_two_futures() {
        // H6 volume: CompletableFuture.thenCombine — two futures run independently on the pool (20 and
        // 22); thenCombine waits for BOTH and merges them with a summing BiFunction → 42. The internal
        // AtomicInteger gate makes the merge run exactly once, when the second future completes.
        assert_eq!(run_int("java/CkTest.class"), 42); // green
        assert_eq!(run_int_os("java/CkTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CkTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn autoboxing_wrappers_box_unbox_and_cache_identity() {
        // A7 #5 (JLS §5.1.7, JSR 201): autoboxing. javac lowers `Integer a = 5` to
        // Integer.valueOf and `int b = a` to intValue(); our boot wrappers supply those
        // methods plus the mandated valueOf caches, so boxing 100 twice yields the SAME
        // object (== true, +10) while 200 boxes fresh (== false, +10). Also: box/unbox
        // round-trip (+5), equals against a reboxed literal (+5), Boolean's canonical
        // TRUE (+4), Character's ASCII cache (+4), Long round-trip (+4) → 42. Before the
        // wrappers existed, `Integer a = 5` died with NoSuchMethodError on valueOf.
        //
        // NOTE: BxTest triggers Long.<clinit> FIRST (fresh heap) on purpose. Initializing
        // Long at a near-full Eden reproduces an OPEN os-parallel-only GC bug at ~50%: a
        // spurious ArithmeticException out of bytecode that contains no division (green
        // and os-gil are unaffected, and JVM_GC_VERIFY stays silent — control-flow state,
        // not heap refs, gets corrupted). Deterministic-ish reproducers are kept in
        // java/BxDbgT.java and java/BxDbgY.java (Long section last).
        assert_eq!(run_int("java/BxTest.class"), 42); // green
        assert_eq!(run_int_os("java/BxTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/BxTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn throwable_carries_message_and_stack_trace() {
        // A6 loose ends: a RuntimeException thrown several frames deep carries a detail message
        // (getMessage → "boom"), renders "java.lang.RuntimeException: boom" (toString reads the
        // runtime class name), and the VM captured a backtrace at throw time so printStackTrace()
        // runs without faulting. Score 10 (message) + 20 (toString) = 30. If any piece were broken
        // — null message, bad field offset, unwritten backtrace — the run would panic, not return 30.
        assert_eq!(run_int("java/ExcTest.class"), 30); // green
        assert_eq!(run_int_os("java/ExcTest.class"), 30); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/ExcTest.class"), 30); // os (parallel)
        }
    }

    #[test]
    fn aastore_throws_array_store_exception_on_covariant_mismatch() {
        // A7 item 3 (JVMS §6.5): `aastore`'s dynamic assignability check. An AsDog[] held
        // through an AsAnimal[] variable (array covariance) accepts an AsDog (+10), rejects
        // an AsCat with ArrayStoreException caught by the test (+20), and always accepts
        // null (+12) → 42. Before the fix the bad store silently corrupted the array.
        assert_eq!(run_int("java/AsTest.class"), 42); // green
        assert_eq!(run_int_os("java/AsTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/AsTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn phantom_and_soft_references_follow_their_strengths() {
        // The two reference strengths above `WeakReference`. A `PhantomReference`'s `get()`
        // is null **always** — even while the referent is strongly reachable — and the
        // reference lands on its `ReferenceQueue` only once the referent dies. A
        // `SoftReference`'s referent, by contrast, **survives** an ordinary `System.gc()`:
        // our policy (`SoftPolicy`) only clears soft referents on a collection the heap
        // itself asked for (occupancy / out-of-space / allocation rate). 42 = all six
        // observations held.
        assert_eq!(run_int("java/RfTest.class"), 42);
        assert_eq!(run_int_os("java/RfTest.class"), 42);
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/RfTest.class"), 42);
        }
    }

    #[test]
    fn interface_default_methods_resolve_and_dispatch() {
        // A7 (JSR 335): default methods. DefA inherits f/g as defaults (DefSub's f shadows DefI's —
        // maximally-specific); DefB's class override beats both. Dispatched via invokeinterface
        // (interface-typed receiver), invokevirtual (class-typed receiver), plus a static interface
        // method. 2 + 10 + 3 + 100 = 115. Before the fix: NoSuchMethodError (the vtable never
        // folded superinterface defaults).
        assert_eq!(run_int("java/DefProbe.class"), 115); // green
        assert_eq!(run_int_os("java/DefProbe.class"), 115); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/DefProbe.class"), 115); // os (parallel)
        }
    }

    #[test]
    fn stack_overflow_is_a_catchable_error() {
        // A7 #4 (JVMS §6.3): infinite recursion (`deep(n + 1)` with no base case) must not blow up
        // the process — when the frame stack hits MAX_FRAMES, the invoke throws a catchable
        // java.lang.StackOverflowError instead of pushing. The test catches it and returns 42.
        // Before the fix: `Vec<Frame>` grew without bound until the host process died.
        assert_eq!(run_int("java/SoTest.class"), 42); // green
        assert_eq!(run_int_os("java/SoTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/SoTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn system_exit_terminates_the_vm_with_its_status() {
        // A7 #11 (JLS §12.8): `System.exit(42)` ends the VM *at the call*. It is not a return
        // and not a throw, so nothing unwinds: the `return 1` after it, the enclosing `finally`
        // (which would set ExMarker.value = 7), and the caller's `100 + value + ExMarker.value`
        // are all dead code. The program's result is the exit status itself — 42, not 108.
        // `Runtime.getRuntime().availableProcessors()` guards the call, so the singleton and its
        // native are exercised on the way in. Before: no `exit` at all (NoSuchMethodError).
        assert_eq!(run_int("java/ExTest.class"), 42); // green
        assert_eq!(run_int_os("java/ExTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/ExTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn uncaught_exception_terminates_thread_not_vm() {
        // A7 #2 (JVMS §2.10): a worker throws a RuntimeException nobody catches. The VM prints
        // `Exception in thread "Thread-N" ...` + the captured trace to the console and terminates
        // just that thread (joiners wake, main keeps running) — before the fix the whole VM
        // panicked on any uncaught exception. TERMINATED check (40) + main alive (2) = 42.
        assert_eq!(run_int("java/UncTest.class"), 42); // green
        assert_eq!(run_int_os("java/UncTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/UncTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn object_clone_copies_fields_and_honors_cloneable_opt_in() {
        // A7 #6 (JLS §10.7): Object.clone() + Cloneable. A CnPoint (implements Cloneable)
        // clones to a distinct object carrying its fields; mutating the clone leaves the
        // original untouched (7 + 7 + 8 + 2 + 5 = 29). A CnPlain (no Cloneable) gets
        // CloneNotSupportedException, caught (+2 = 31). A CnVec override delegates to
        // super.clone() — the invokespecial path — and the copied field comes through
        // (+4 = 35). An int[] clones to a distinct array with the elements copied —
        // arrays are implicitly Cloneable (+7 = 42). Before the fix, `clone` existed
        // nowhere: resolution died with NoSuchMethodError.
        assert_eq!(run_int("java/CnTest.class"), 42); // green
        assert_eq!(run_int_os("java/CnTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/CnTest.class"), 42); // os (parallel)
        }
    }

    /// The **measurement workloads** (`java/Bm*.java`), each stressing one dimension of the
    /// interpreter, with the result the real `java` of JDK 25 gives for the same class file.
    /// Shared by the cheap correctness check below and by `bench_baseline` at the end of the
    /// module, so a benchmark can never drift from the value it is supposed to compute.
    ///
    /// **These runs respect `JVM_JIT`**, which is on by default, so most of the five are no longer
    /// pure interpreter measurements: `BmLoop` has been compiled since F3 step 3 and `BmVirtual`
    /// since step 5 (its `f` overrides are `aload_0; getfield; …; ireturn`). `bench_baseline` says
    /// which engine it measured in its own header and marks the workloads native code took part
    /// of, so the table no longer has to be read with that caveat in mind — but the caveat is the
    /// reason it says so. `burst::jit_tests::bench_jit` is the harness that measures the two arms
    /// against each other on purpose.
    const BENCH_WORKLOADS: [(&str, i32, &str); 5] = [
        ("java/BmLoop.class", 161265, "frame-local arithmetic + branches"),
        ("java/BmInvoke.class", 252624, "invokestatic (recursive fib)"),
        ("java/BmField.class", 973376, "new + getfield/putfield (allocates → GC)"),
        ("java/BmArray.class", 615180, "iaload/iastore over one array"),
        ("java/BmVirtual.class", 861237, "invokevirtual, polymorphic receiver"),
    ];

    #[test]
    fn benchmark_workloads_return_their_expected_values() {
        // The guard that keeps the baseline honest. `bench_baseline` is `#[ignore]`, so nothing
        // in a normal run would notice if one of these workloads started throwing, returning
        // early, or computing something else entirely — it would still report a tidy ns/opcode,
        // just for a different program. This runs all five in green and pins their results.
        for (class_file, expected, dimension) in BENCH_WORKLOADS {
            assert_eq!(run_int(class_file), expected, "{class_file} ({dimension})");
        }
    }

    #[test]
    fn reference_workloads_agree_across_every_substrate() {
        // The F3 step-5 workloads through the **three-substrate oracle**, which is the strongest
        // differential test the project has and costs nothing extra here: the JIT is on in `green`
        // and `os-gil` and off in `os` (parallel), so an agreement between the three is an
        // agreement between compiled and interpreted execution of the same program — and these two
        // programs are the ones whose compiled code holds *references* in native frames.
        //
        // `os-gil` matters in its own right: it is the substrate where a compiled frame coexists
        // with sibling OS threads, and the whole no-stack-maps argument rests on the claim that the
        // one global lock is held across the native call, so no sibling can collect while it runs.
        // `JmDead` (F3 step 9) joins them for a reason of its own, and it is a **collector** reason
        // rather than a type-system one. Its three hot methods all carry a local slot the type map
        // calls `Kind::Conflict` — two edges, two kinds, no answer — and the write-back's response
        // to one of those is to write *nothing*, leaving the interpreter's frame holding a value
        // native code may have overwritten. That frame is a GC root the instant it is interpreted
        // again, the workload allocates 1 500-odd objects to make sure the collector looks at it,
        // and one of its deopts lands at a pc where the conflicted slot holds a stale reference
        // from the previous iteration. If "a conflicted slot is dead" were wrong, or if the stale
        // value were anything but a well-typed `Value`, this is where it would show.
        //
        // `JxRich` (group 2) is here for the **`os-gil`** arm specifically. Its `volatile` accesses
        // are compiled to plain `mov`s, and the entire licence for that is that no other thread
        // runs a Java opcode while a native frame is on this stack — which is a claim about the
        // substrate, and `os-gil` is the only substrate that has sibling OS threads *and* the JIT
        // on. Its own differential (`burst::jit_tests`) runs on `green` alone, where the claim is
        // trivially true and therefore untested. VOLATILE-REVISIT-OS-PARALLEL.
        for (class_file, expected) in
            [
                ("java/JrRef.class", 604164),
                ("java/JrPoll.class", 977804),
                ("java/JmDead.class", 854257),
                ("java/JxRich.class", 353090),
            ]
        {
            assert_eq!(run_int(class_file), expected, "{class_file} (green)");
            assert_eq!(run_int_os(class_file), expected, "{class_file} (os-gil)");
            for _ in 0..5 {
                assert_eq!(run_int_os_parallel(class_file), expected, "{class_file} (os)");
            }
        }
    }

    #[test]
    fn nested_array_stores_survive_minor_gc() {
        // Regression for two latent bugs the new aastore check exposed: (1) synthetic array-class
        // mirrors were Eden-allocated — a minor GC moved (or collected) them while the metaspace
        // index and array headers kept the stale offset, so a later lookup resolved a *different*
        // class and a valid `holder[i] = new long[8]` store threw a spurious ArrayStoreException;
        // (2) `anewarray` named a nested array's class `[L[J;` instead of `[[J`. The test churns
        // garbage between stores of `[J` into `[[J` to force minor GCs → all 64 stores succeed.
        assert_eq!(run_int("java/AsGcProbe.class"), 64); // green
        assert_eq!(run_int_os("java/AsGcProbe.class"), 64); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/AsGcProbe.class"), 64); // os (parallel)
        }
    }

    #[test]
    fn uncaught_exception_handler_runs_in_java() {
        // A7 #12: Thread.setUncaughtExceptionHandler. An exception escaping run() is no longer
        // just printed — the VM calls the handler back **in Java**, on the dying thread, with the
        // stack already unwound (the last frame is kept alive one moment longer precisely so the
        // exception has a GC root while that Java call allocates). Handler ran exactly once (10)
        // with the right Thread (8) and the very Throwable that escaped (8), message intact (6),
        // thread still TERMINATED afterwards (4), ThreadGroup inherited from the creator (3), and
        // sleep(long,int)/onSpinWait not faulting (3) = 42 — the same 42 a real JDK returns for
        // this file, which is what pins the semantics.
        assert_eq!(run_int("java/UhTest.class"), 42); // green
        assert_eq!(run_int_os("java/UhTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/UhTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn thread_local_isolates_values_per_thread() {
        // A6 loose ends: Thread peripherals. Four OS threads each store id*7 into the SAME
        // ThreadLocal, interleave, then read it back — each sees only its own value (per-thread
        // isolation via a list hanging off currentThread()). Sum 0+7+14+21 = 42. Also validates
        // priority/daemon getters/setters + setPriority range check (sabotages to -1 on misbehavior).
        assert_eq!(run_int("java/TlTest.class"), 42); // green
        assert_eq!(run_int_os("java/TlTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/TlTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn heap_exhaustion_throws_catchable_out_of_memory_error() {
        // A7 (JVMS §6.3): OutOfMemoryError. Each recursion frame roots a 512 KiB long[] in a
        // local (frame locals are GC roots), so no collection can reclaim anything and the
        // 16 MiB max heap truly fills after ~31 frames; the failing `newarray` then throws a
        // *catchable* OutOfMemoryError (`try_malloc` → Err → throw_exception) instead of
        // panicking the VM ("heap exhausted"), and run()'s catch returns 42. Single-threaded
        // → deterministic in all three modes.
        assert_eq!(run_int("java/OmTest.class"), 42); // green
        assert_eq!(run_int_os("java/OmTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/OmTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn class_init_pulls_in_only_default_declaring_superinterfaces() {
        // A7 #13 (JVMS §5.5): implementing an interface is not, by itself, an active use of it.
        // Initializing a class runs the `<clinit>` of its superinterfaces — direct *and* indirect —
        // that declare a **default** method (+8 direct, +4 indirect: their code can run on the
        // instance), and of no others (+8: a constants-and-abstracts interface stays untouched).
        // Initializing an *interface* runs its own `<clinit>` (+4) but never a superinterface's,
        // not even one declaring a default (+8). The skipped ones are merely deferred, not broken:
        // reading their own non-constant static field initializes them on the spot (+5 +5) = 42.
        // Before the fix `ensure_initialized` walked only `superclass_name`, so a default-method
        // interface's `<clinit>` never ran (the measured score was 30).
        assert_eq!(run_int("java/IiTest.class"), 42); // green
        assert_eq!(run_int_os("java/IiTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/IiTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn clinit_failure_wraps_in_exception_in_initializer_error() {
        // A6 loose end (JVMS §5.5): a static initializer that throws. The failure now (a) propagates
        // to the triggering code — before the fix it was silently swallowed and the read returned 0 —
        // (b) is wrapped in ExceptionInInitializerError (the thrown RuntimeException is not an Error),
        // and (c) leaves the class erroneous, so a second use throws NoClassDefFoundError. 3 + 5 = 8.
        assert_eq!(run_int("java/ClinitProbe.class"), 8); // green
        assert_eq!(run_int_os("java/ClinitProbe.class"), 8); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/ClinitProbe.class"), 8); // os (parallel)
        }
    }

    #[test]
    fn arraycopy_uses_the_real_element_width_and_survives_overlap() {
        // #269. El nativo suponia cuatro bytes por elemento para TODO array, asi que un `char[]`
        // se copiaba con el doble de paso: nada caia donde debia y la lectura se salia del array
        // por el final -- el origen de los panicos "range end index N out of range". Y el
        // solapamiento no se contemplaba, que es justo lo que hace un `delete` de StringBuilder.
        //
        // Siete propiedades, un bit cada una, para que una falla parcial se nombre sola:
        // char[] solapado hacia abajo (1), hacia arriba (2), disjunto (4), byte[] (8), long[]
        // (16), int[] (32, el ancho que el codigo viejo suponia) y referencias por el write
        // barrier (64). El JDK 25 corriendo la misma fuente da 127.
        assert_eq!(run_int("java/CopyProbe.class"), 127);
    }

    #[test]
    fn scheduled_executor_one_shot_delays() {
        // H6 volume: ScheduledThreadPoolExecutor.schedule() — 5 delayed one-shot tasks each increment
        // a guarded counter and count down a latch; main awaits the latch (all 5 provably ran) → 5.
        assert_eq!(run_int("java/SchedTest.class"), 5); // green
        assert_eq!(run_int_os("java/SchedTest.class"), 5); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/SchedTest.class"), 5); // os (parallel)
        }
    }

    #[test]
    fn scheduled_executor_fixed_rate_periodic() {
        // H6 volume: scheduleAtFixedRate() — a periodic task fires repeatedly, incrementing a counter
        // capped at 10 (deterministic) and releasing a latch at 10; main awaits, then shuts down → 10.
        assert_eq!(run_int("java/SchedRateTest.class"), 10); // green
        assert_eq!(run_int_os("java/SchedRateTest.class"), 10); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/SchedRateTest.class"), 10); // os (parallel)
        }
    }

    #[test]
    fn linked_blocking_queue_two_lock_producer_consumer() {
        // H6 volume: LinkedBlockingQueue (two-lock: putLock at the tail, takeLock at the head, so
        // producers and the consumer run in parallel). Three producers put 100 tokens each; main
        // takes all 300 and counts → 300 iff no node is lost/duplicated and notEmpty wakes correctly.
        assert_eq!(run_int("java/LbqTest.class"), 300); // green
        assert_eq!(run_int_os("java/LbqTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/LbqTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn concurrent_hash_map_concurrent_puts_and_value_lookup() {
        // H6 volume: ConcurrentHashMap — 3 workers put 100 distinct keys each (300 entries across
        // the lock stripes) with no lost entry, then a value-based get() with a rebuilt key finds
        // its value (dispatching key.hashCode()/equals()). size(300) + found(1) = 301.
        assert_eq!(run_int("java/ChmTest.class"), 301); // green
        assert_eq!(run_int_os("java/ChmTest.class"), 301); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/ChmTest.class"), 301); // os (parallel)
        }
    }

    #[test]
    fn thread_pool_executor_runs_all_tasks() {
        // H6 volume: ThreadPoolExecutor(4) runs 300 guarded-increment tasks — the pool must run each
        // exactly once on its worker threads (poison-pill shutdown drains the queue first) and the
        // ReentrantLock serialises them → exactly 300.
        assert_eq!(run_int("java/PoolTest.class"), 300); // green
        assert_eq!(run_int_os("java/PoolTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/PoolTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn executor_submit_future_get() {
        // H6 volume: submit() returns a Future; get() blocks until the pool ran the task (which sets
        // 42) → 42. Exercises the FutureTask completion handshake (run() notifyAll, get() waits).
        assert_eq!(run_int("java/PoolFutureTest.class"), 42); // green
        assert_eq!(run_int_os("java/PoolFutureTest.class"), 42); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/PoolFutureTest.class"), 42); // os (parallel)
        }
    }

    #[test]
    fn reentrant_read_write_lock_readers_and_writers() {
        // H6 volume: ReentrantReadWriteLock — three writers do 100 guarded increments each while
        // two readers read concurrently under the read lock. The write lock's mutual exclusion
        // gives an exact 300 (no lost update); readers exercise the shared path without deadlock.
        assert_eq!(run_int("java/RwLockTest.class"), 300); // green
        assert_eq!(run_int_os("java/RwLockTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/RwLockTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn reentrant_lock_serialises_and_reenters() {
        // H6: ReentrantLock taken reentrantly (lock/lock, unlock/unlock). Three workers, 100
        // guarded non-atomic increments each → 300 iff the lock both serialises and re-enters.
        assert_eq!(run_int("java/LockTest.class"), 300); // green
        assert_eq!(run_int_os("java/LockTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/LockTest.class"), 300); // os (parallel)
        }
    }

    /// JVMS §5.4.2 Preparation: a `static` field carrying a `ConstantValue` attribute starts at
    /// that constant, not at its type's default. The real `javac` folds every compile-time
    /// constant into its use sites, so a JDK class never depends on this — **ours does not fold**,
    /// so it emits a `getstatic` against a field whose value lives only in the attribute
    /// (COMPILER_FINDINGS #216/#112). Every `static final int` read back 0, which is how
    /// `FutureTask`'s four state constants were all 0 and `get()` waited forever.
    ///
    /// The fixture is compiled with **our** javac on purpose (`java/KProbe.java`): with the real
    /// one the constant folds to `bipush 7` and the test would pass without ever reaching the
    /// code path.
    ///
    /// All five shapes §4.7.2 allows, because a width bug is invisible from `int` alone: the
    /// `long` is deliberately 5_000_000_000 (above 2^32, so a 32-bit write loses it), the
    /// `double` needs the same 8 bytes, and the `String` exercises the interning path — the one
    /// that forced this to run *after* the mirror is registered.
    #[test]
    fn a_static_final_constant_is_applied_at_preparation() {
        assert_eq!(run_int("java/KProbe.class"), 7); // int
        assert_eq!(run_int_method("java/KProbe.class", "runLong"), 1); // long, 64-bit
        assert_eq!(run_int_method("java/KProbe.class", "runDouble"), 1); // double, 64-bit
        assert_eq!(run_int_method("java/KProbe.class", "runString"), 3); // String, interned
        assert_eq!(run_int_method("java/KProbe.class", "runBool"), 1); // boolean
    }

    // Deterministic repro of the `RunningCtx::pending_exception` hole — single-threaded, `green`,
    // no race involved. A record component's `equals` throws; the `ObjectMethods` bootstrap calls
    // it through `call_virtual`, which returns `None` with the exception parked in
    // `pending_exception`, and `record_equals`'s `None => false` arm reads that as "the component
    // has no equals" and carries on. Nobody in Java-land saw a throw, but the VM is now carrying
    // an exception that the next opcode with a `take_pending_throw()` delivers somewhere unrelated.
    //
    // Real `java` (JDK 25) answers **110**: the component's equals propagates out of the record's
    // equals (10), and the later `new` is clean (100). Anything else is the bug — 1001 means the
    // exception was swallowed and resurfaced at the `new`.
    //
    // `pending_exception` is also invisible to the collector (`gc::roots` walks `threads[*].frames`
    // and `Exec::parked` syncs only `frames`), so an exception parked across a moving collection is
    // a stale reference on top of being delivered in the wrong place.
    #[test]
    fn pending_exception_is_not_swallowed_by_record_equals() {
        assert_eq!(run_int("java/PeStale.class"), 110); // green — igual que el `java` real
    }

    // The same defect in its second shape — and the test that originally exposed the *stale
    // reference*, not just the misplaced delivery. With the exception parked, an allocation storm
    // ran on top of it (`newarray`/`arraylength` do not call `take_pending_throw`, so nothing
    // reclaimed it), the minor collection recycled a throwable no root was holding, and the later
    // `new` panicked in `athrow.rs` with "cannot resolve the thrown object's class".
    //
    // It no longer reaches the storm: the component's exception now propagates out of the record's
    // equals, so the method returns at the first `catch` — which is exactly what real `java` (JDK
    // 25) does, and it answers **110** too. What this guards now is that propagation. The GC-root
    // property itself is guarded directly by `a_parked_exception_survives_a_moving_collection`
    // in `bytecode_interpreter.rs`, because with every consumer taking the exception before the
    // frame runs another instruction, no Java program can hold one across a safepoint any more.
    #[test]
    fn a_throwing_record_component_propagates_out_of_equals() {
        assert_eq!(run_int("java/PeGcStale.class"), 110); // green — igual que el `java` real
    }

    // RELIABLE reproduction of the os-parallel stale-reference bug (see the project memory
    // `os-parallel-gc-stale-ref-heisenbug`). 12 workers run an allocation storm (constant minor GCs)
    // while `main` holds every worker reference across the storm, then dispatches `join` on each. In
    // `os` (real parallelism) a worker/array reference in a frame is occasionally left un-remapped by
    // a minor collection, so a later `invokevirtual` resolves the receiver to the wrong (reused)
    // offset → NoSuchMethodError / IllegalThreadStateException / "could not resolve the receiver".
    //
    // **`green` and `os-gil` always pass; only `os` crashes**, ~every run. Kept `#[ignore]` because
    // it currently fails: it is the standing repro for the unresolved bug, not a passing check. Run
    // it with `cargo test --release -- --ignored gc_race_stress` (add `JVM_GC_VERIFY=1` to catch a
    // botched remap at the offending collection). The green/os-gil asserts confirm the oracle result.
    #[test]
    #[ignore = "reliable repro of the unresolved os-parallel stale-ref bug; os path crashes"]
    fn gc_race_stress() {
        assert_eq!(run_int("java/GcRace.class"), 12); // green
        assert_eq!(run_int_os("java/GcRace.class"), 12); // os-gil
        for _ in 0..40 {
            assert_eq!(run_int_os_parallel("java/GcRace.class"), 12); // os (parallel)
        }
    }

    #[test]
    fn semaphore_gives_mutual_exclusion() {
        // H6: a binary Semaphore(1) as a mutex. Three workers each do 100 NON-atomic increments of
        // a shared int under the permit — no update is lost only if the permit truly serializes
        // them → 300. In os (real parallelism) an unguarded increment would race and fall short.
        assert_eq!(run_int("java/SemTest.class"), 300); // green
        assert_eq!(run_int_os("java/SemTest.class"), 300); // os-gil
        for _ in 0..10 {
            assert_eq!(run_int_os_parallel("java/SemTest.class"), 300); // os (parallel)
        }
    }

    #[test]
    fn atomic_long_and_reference_cas() {
        // H5: AtomicLong (64-bit CAS) + AtomicReference (identity CAS, store through the write
        // barrier). → 201 + "b".length() = 202, confirmed vs real java.
        assert_eq!(run_int("java/AtomicMix.class"), 202);
    }

    #[test]
    fn atomic_integer_cas_under_contention() {
        // H5: three threads each `incrementAndGet` a shared AtomicInteger 1000× — the retry loop
        // over compareAndSet must lose no updates → 3000. Oracle green ≡ os-gil ≡ os (the CAS is
        // serialized on the write lock in os), then hammer os to signal races/deadlocks.
        assert_eq!(run_int("java/CasStress.class"), 3000); // green (reference)
        assert_eq!(run_int_os("java/CasStress.class"), 3000); // os-gil (serialized)
        for _ in 0..20 {
            assert_eq!(run_int_os_parallel("java/CasStress.class"), 3000); // os (parallel)
        }
    }

    #[test]
    fn os_parallel_stress() {
        // Real parallelism (H3 1d) + the widened fast-path set (int/long arith, shifts,
        // conversions, refs, if_acmp). Three workers run a lock-free frame-local compute loop
        // interleaved with heap-pressuring allocation, so the GC stop-the-world handshake fires
        // while siblings are mid lock-free run. Value confirmed against real `java` (68126370).
        //
        // Cross-substrate oracle first — green ≡ os-gil ≡ os validates the fast-path arms
        // functionally (a mis-transcribed arm makes os disagree). Then hammer os: a data race
        // shows as a wrong result, a deadlock as a hang (the harness caps wall time). The hammer
        // is a *signal*, not a proof, of race/deadlock freedom (the serialized oracle can't see it).
        assert_eq!(run_int("java/ParallelStress.class"), 68126370); // green (reference)
        assert_eq!(run_int_os("java/ParallelStress.class"), 68126370); // os-gil (serialized)
        for _ in 0..20 {
            assert_eq!(run_int_os_parallel("java/ParallelStress.class"), 68126370); // os (parallel)
        }
    }

    #[test]
    fn method_handle_find_static_and_invoke() {
        // 0xba / java.lang.invoke object model: `MethodHandles.lookup().findStatic(...)` builds a
        // MethodHandle, and signature-polymorphic `invoke` calls it. `id("hello")` round-trips the
        // string through the handle; `.length()` = 5. Confirmed against real `java`.
        assert_eq!(run_int("java/MHInvoke.class"), 5);
    }

    #[test]
    fn method_handle_with_primitive_int_type() {
        // MH-d (primitives): `int.class` → `Integer.TYPE` (a primitive Class mirror via
        // `Class.getPrimitiveClass`); `methodType(int.class, int.class)` → "(I)I"; the handle
        // invokes `twice(21)` with a plain int (call site `(I)I`, no boxing). → 42, vs real `java`.
        assert_eq!(run_int("java/MHInt.class"), 42);
    }

    #[test]
    fn method_handle_virtual_dispatch() {
        // MH-d (kind 5, invokeVirtual): `findVirtual(String, "length", ()I).invoke("hello")`
        // dispatches on the receiver → 5. Confirmed vs real `java`.
        assert_eq!(run_int("java/MHVirtual.class"), 5);
    }

    #[test]
    fn method_handle_constructor() {
        // MH-d (kind 8, newInvokeSpecial): `findConstructor(Box, (int)void).invoke(42)` allocates a
        // Box, runs its `<init>`, and hands back the new object; `b.get()` → 42. Vs real `java`.
        assert_eq!(run_int("java/MHCtor.class"), 42);
    }

    #[test]
    fn method_handle_invoke_with_arguments_spreads() {
        // `MethodHandle.invokeWithArguments(Object[])` spreads the array into the handle — the VM
        // primitive that a Java `ConstantBootstraps.invoke` is built on. `id("spread!")` → length 7.
        assert_eq!(run_int("java/MHSpread.class"), 7);
    }

    #[test]
    fn ldc_of_method_type_and_method_handle_constants() {
        // MH-b: `ldc` of `CONSTANT_MethodType` / `CONSTANT_MethodHandle` — constants `javac` never
        // emits, so this is the first thing exercised only through **hand-written class files**
        // built with the `.class` writer (`crate::javac::class_writer`). The class does:
        //   ldc MethodType "(I)I"; .descriptorString().length()   → 4
        //   ldc MethodHandle(invokeStatic id:(String)String); .invoke("hello").length()  → 5
        //   iadd → 9
        // Loading + running it proves both `ldc` materialisers and, transitively, `invoke`.
        use crate::javac::class_writer::{ClassFile, MethodInfo};
        let be = |v: u16| [(v >> 8) as u8, v as u8];

        let mut cf = ClassFile::new();
        cf.access_flags = 0x0021; // ACC_PUBLIC | ACC_SUPER
        cf.this_class = cf.pool.class("MHLdc");
        cf.super_class = cf.pool.class("java/lang/Object");

        let mt = cf.pool.method_type("(I)I");
        let mt_desc = cf.pool.methodref(
            "java/lang/invoke/MethodType",
            "descriptorString",
            "()Ljava/lang/String;",
        );
        let len = cf.pool.methodref("java/lang/String", "length", "()I");
        let mh = cf.pool.method_handle(6, "MHLdc", "id", "(Ljava/lang/String;)Ljava/lang/String;");
        let hello = cf.pool.string("hello");
        let invoke = cf.pool.methodref(
            "java/lang/invoke/MethodHandle",
            "invoke",
            "(Ljava/lang/String;)Ljava/lang/String;",
        );
        let run_name = cf.pool.utf8("run");
        let run_desc = cf.pool.utf8("()I");
        let id_name = cf.pool.utf8("id");
        let id_desc = cf.pool.utf8("(Ljava/lang/String;)Ljava/lang/String;");

        let mut code = Vec::new();
        code.push(0x13); // ldc_w MethodType "(I)I"
        code.extend(be(mt));
        code.push(0xb6); // invokevirtual MethodType.descriptorString ()String
        code.extend(be(mt_desc));
        code.push(0xb6); // invokevirtual String.length ()I  → 4
        code.extend(be(len));
        code.push(0x13); // ldc_w MethodHandle(invokeStatic MHLdc.id)
        code.extend(be(mh));
        code.push(0x13); // ldc_w "hello"
        code.extend(be(hello));
        code.push(0xb6); // invokevirtual MethodHandle.invoke (String)String  (signature-poly) → "hello"
        code.extend(be(invoke));
        code.push(0xb6); // invokevirtual String.length ()I  → 5
        code.extend(be(len));
        code.push(0x60); // iadd  → 9
        code.push(0xac); // ireturn

        cf.methods.push(MethodInfo {
            access_flags: 0x0009, // ACC_PUBLIC | ACC_STATIC
            name_index: run_name,
            descriptor_index: run_desc,
            max_stack: 3,
            max_locals: 0,
            code,
            stack_map: None,
            exceptions: Vec::new(),
            ..Default::default() // hand-built class file: only `Code`
        });
        // static String id(String s) { return s; }  — the MethodHandle's target.
        cf.methods.push(MethodInfo {
            access_flags: 0x0009,
            name_index: id_name,
            descriptor_index: id_desc,
            max_stack: 1,
            max_locals: 1,
            code: vec![0x2a, 0xb0], // aload_0; areturn
            stack_map: None,
            exceptions: Vec::new(),
            ..Default::default() // hand-built class file: only `Code`
        });

        // Write the hand-built class to a temp file and run it (loads MethodType/MethodHandle from boot/).
        let path = std::env::temp_dir().join("kaji_mh_ldc_MHLdc.class");
        std::fs::write(&path, cf.to_bytes()).expect("write hand-built class");
        assert_eq!(run_int(path.to_str().unwrap()), 9);
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn os_parallel_volatile_publication() {
        // H4 (JMM): a publisher writes a payload then a *volatile* flag; a reader spins on the flag
        // (volatile → Acquire) and then reads the payload — which the Release/Acquire handoff makes
        // visible, never the stale defaults. Exercises the H4-e field paths end-to-end: volatile
        // `putfield`/`getfield` (Release/Acquire, on both an int and a `long` → a real `AtomicU64`,
        // no tearing) and non-volatile lock-free field access. Result 42 + 1000 + 777 = 1819,
        // confirmed against real `java`.
        //
        // Oracle: green ≡ os-gil ≡ os. In os (real parallelism) the reader's spin *must* terminate —
        // that only happens if the flag write becomes visible — and every run must still read the
        // published payload, not a default. A wrong result or a hang would surface a broken order.
        assert_eq!(run_int("java/VolatilePublish.class"), 1819); // green (reference)
        assert_eq!(run_int_os("java/VolatilePublish.class"), 1819); // os-gil (serialized)
        for _ in 0..20 {
            assert_eq!(run_int_os_parallel("java/VolatilePublish.class"), 1819); // os (parallel)
        }
    }

    #[test]
    fn os_parallel_matches_oracle() {
        // `JVM_THREADS=os` — the GIL-free parallel substrate (H3, in progress). Today it
        // shares the os-gil engine, so it must produce byte-for-byte the same results as
        // green/os-gil on the whole concurrency demo set: monitors, spawn/spin, wait/notify,
        // join, timed wait, and IMSE. This is the oracle that guards every lock-shrinking step.
        assert_eq!(run_int_os_parallel("java/Sync.class"), 200);
        assert_eq!(run_int_os_parallel("java/SyncMethod.class"), 200);
        assert_eq!(run_int_os_parallel("java/Threads.class"), 100);
        assert_eq!(run_int_os_parallel("java/WaitNotify.class"), 42);
        assert_eq!(run_int_os_parallel("java/Joiner.class"), 30);
        assert_eq!(run_int_os_parallel("java/WaitTimeout.class"), 7);
        assert_eq!(run_int_os_parallel("java/Imse.class"), 99);
    }

    #[test]
    fn synchronized_gives_mutual_exclusion() {
        // Two threads each add 100 to a shared counter inside `synchronized` → the
        // critical section is mutually exclusive, so no updates are lost: exactly 200.
        assert_eq!(run_int("java/Sync.class"), 200);
    }

    #[test]
    fn notify_without_monitor_throws_illegal_monitor_state() {
        // Calling notify() without holding the object's monitor must throw
        // IllegalMonitorStateException (JLS 17.2). The demo catches it and returns 99 —
        // proving both the throw and that it flows through the exception machinery.
        assert_eq!(run_int("java/Imse.class"), 99);
    }

    #[test]
    fn synchronized_method_gives_mutual_exclusion() {
        // Same exclusion, but the critical section is a `synchronized` *method*
        // (`ACC_SYNCHRONIZED`, no monitorenter/monitorexit opcodes): the VM takes the
        // receiver's monitor on frame entry and releases it on return. Two threads call
        // `bump()` 100× each → exactly 200 iff the lock serialized the read-modify-writes.
        assert_eq!(run_int("java/SyncMethod.class"), 200);
    }

    #[test]
    fn join_and_sleep_coordinate_threads() {
        // Each worker sleeps briefly then sets its value; `main` join()s both (no
        // spin-wait), so the result is a deterministic 30 once both have finished.
        assert_eq!(run_int("java/Joiner.class"), 30);
    }

    #[test]
    fn wait_notify_handshake() {
        // A worker waits inside `synchronized` until the producer sets a value and
        // notifies; it then reads 42. Exercises wait (release + park), notify (move the
        // waiter to the blocked-set), and the re-acquire on wake.
        assert_eq!(run_int("java/WaitNotify.class"), 42);
    }

    #[test]
    fn wait_timeout_returns_after_deadline() {
        // A timed wait(50) with no notifier returns once the deadline passes, re-acquires
        // the monitor, and the program continues → 7. (Green mode: opcode-clock deadline.)
        assert_eq!(run_int("java/WaitTimeout.class"), 7);
    }

    #[test]
    fn monitor_survives_gc_relocation() {
        // Inside `synchronized(lock)`, System.gc() runs a minor that evacuates `lock` to a
        // new address. The monitor map (keyed by offset) must follow the move, else the
        // closing monitorexit throws IllegalMonitorStateException. 5 = the monitor survived.
        assert_eq!(run_int("java/GcMonitor.class"), 5);
        assert_eq!(run_int_os("java/GcMonitor.class"), 5); // also under OS-threads + GIL
    }

    #[test]
    fn unsynchronized_loses_updates() {
        // The control: the same increments WITHOUT the lock race, so the total is well
        // under 200 (with our per-opcode scheduler, updates are heavily lost). This is
        // what proves the monitor actually provides exclusion — not a no-op.
        let racy = run_int("java/Racy.class");
        assert!((1..200).contains(&racy), "expected lost updates (1..200), got {racy}");
    }

    #[test]
    fn green_threads_run_concurrently() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;

        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Threads.class").expect("load Threads");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("Threads.run");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());

        // `main` spawns two worker threads and spin-waits; the cooperative scheduler
        // interleaves all three. Each worker writes 50 to its own flag → 100.
        assert_eq!(execute(metaspace, frame), Some(Value::Int(100)));
    }

    #[test]
    fn weak_reference_is_cleared_and_enqueued_when_referent_dies() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;

        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Weak.class").expect("load Weak");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("Weak.run");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());

        // The referent is held only by a WeakReference; after `System.gc()` the major
        // collector clears it (`get()` → null) and enqueues the reference. 11 = both.
        assert_eq!(execute(metaspace, frame), Some(Value::Int(11)));
    }

    #[test]
    fn write_barrier_keeps_an_old_to_young_pointer_alive() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;

        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Barrier.class").expect("load Barrier");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("Barrier.run");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());

        // `keep` is tenured to Old, then pointed at a fresh young object held *only*
        // through `keep.next` — no frame root. It survives the ensuing minors solely
        // because the write barrier remembered `keep`. Result 99 proves the path.
        assert_eq!(execute(metaspace, frame), Some(Value::Int(99)));
    }

    #[test]
    fn auto_cause_picks_the_right_trigger() {
        let base = GcPolicy {
            auto: true,
            capacity: 100,
            occupancy_ratio: 0.75,
            rate_horizon: 10,
            ..GcPolicy::default()
        };
        // Off → never fires, even way past capacity.
        assert_eq!(GcPolicy { auto: false, ..base }.auto_cause(200, 5, 0, 0), None);
        // No growth since the last GC → don't re-fire.
        assert_eq!(base.auto_cause(50, 5, 50, 0), None);
        // Over capacity → OutOfSpace (highest priority).
        assert_eq!(base.auto_cause(120, 5, 0, 0), Some(GcCause::OutOfSpace));
        // Over the 75% occupancy line but under capacity → Occupancy.
        assert_eq!(base.auto_cause(80, 5, 0, 0), Some(GcCause::Occupancy));
        // Under occupancy, but the rate (5 B/step) projects to 100 ≥ capacity by the
        // 10-step horizon → AllocationRate.
        assert_eq!(base.auto_cause(50, 10, 0, 0), Some(GcCause::AllocationRate));
        // Low and slow → nothing warranted.
        assert_eq!(base.auto_cause(10, 100, 0, 0), None);
    }

    /// Loads a workload class and builds the frame for its `run()I` — everything that is
    /// *not* execution, so the benchmark's clock never measures class loading or parsing.
    fn bench_setup(class_file: &str) -> (MetaspaceService, crate::jvm::interpreter::frame::Frame) {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::frame::Frame;
        use std::path::PathBuf;
        let mut metaspace =
            MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        (metaspace, Frame::new(entry, max_locals, Vec::new()))
    }

    /// One timed **green** run: the workload's result, the number of opcodes it executed, and
    /// the wall time of the execution alone.
    fn bench_green(class_file: &str) -> (i32, usize, std::time::Duration) {
        use crate::jvm::interpreter::bytecode_interpreter::execute_counting;
        let (metaspace, frame) = bench_setup(class_file);
        let start = std::time::Instant::now();
        let (value, steps) = execute_counting(metaspace, frame);
        let elapsed = start.elapsed();
        match value {
            Some(Value::Int(v)) => (v, steps, elapsed),
            other => panic!("expected an int result, got {other:?}"),
        }
    }

    /// **How many opcodes this workload executes with the JIT forced off** — the reference the
    /// timed runs' counts are read against, and the whole of what makes the `ns/opcode` column
    /// honest.
    ///
    /// Untimed on purpose: it is not a measurement, it is the denominator's alibi. A timed run that
    /// counts *fewer* opcodes than this did part of its work in native code, and dividing its wall
    /// time by what the interpreter had left to do is the artefact `BmLoop` produced when it
    /// reported ~6624 ns/op over 620 opcodes. The JIT is switched **programmatically** (the same
    /// reason the differential tests do: `cargo test` shares one process, so touching `JVM_JIT`
    /// here would touch it for every test running at that moment), which also means this reference
    /// is right whatever the environment says.
    fn bench_interpreted_opcodes(class_file: &str) -> usize {
        use crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned;
        let (metaspace, frame) = bench_setup(class_file);
        let (_, steps, stats) = execute_counting_tuned(metaspace, frame, Some(false), None, |_| {});
        assert_eq!(stats, crate::burst::code_cache::JitStats::default(), "the reference run must compile nothing");
        steps
    }

    /// The same run on the **os-gil** substrate (real OS threads + GIL). No opcode count comes
    /// back from that engine, and none is needed: these workloads are single-threaded and
    /// deterministic, so they execute exactly the opcodes green counted — only the per-opcode
    /// overhead (lock + unlock around every instruction) differs, which is the point.
    fn bench_os_gil(class_file: &str) -> (i32, std::time::Duration) {
        use crate::jvm::interpreter::bytecode_interpreter::execute_os_gil;
        let (metaspace, frame) = bench_setup(class_file);
        let start = std::time::Instant::now();
        let value = execute_os_gil(metaspace, frame);
        let elapsed = start.elapsed();
        match value {
            Some(Value::Int(v)) => (v, elapsed),
            other => panic!("expected an int result, got {other:?}"),
        }
    }

    /// The median of a set of samples (they are pre-sorted by the caller).
    fn median(sorted: &[std::time::Duration]) -> std::time::Duration {
        sorted[sorted.len() / 2]
    }

    // The **baseline harness** for the optimisation track (quickening → superinstructions →
    // inline caching → JIT). It measures; it optimises nothing. Five workloads, each stressing
    // one dimension (see `BENCH_WORKLOADS` and `java/Bm*.java`), are run 6× each: the first run
    // is discarded as warm-up and the median of the other 5 is reported.
    //
    // The column that matters is **ns/opcode** — time divided by the opcodes actually executed
    // (`SharedVm::steps`, handed back by `execute_counting`). Absolute times only say how big a
    // workload is; ns/opcode says how expensive the engine is, and stays comparable when a
    // workload is resized. **It is a metric of the interpreter and of nothing else**, which is why
    // it is now printed only for a workload no native code touched — see the note above the test.
    // `green` is the measurement substrate: it is single-threaded and
    // deterministic, so the opcode count is identical run to run and there is no scheduling
    // noise. (`gil_overhead_bench`, below, contrasts the same five against `os-gil`; it lives in
    // its own test because the GIL makes that an order of magnitude slower to collect.)
    //
    // `#[ignore]` because it is a measurement, not a check — several seconds of pure CPU, and a
    // timing assert would be a flaky test on shared hardware. The *correctness* of these same
    // workloads is checked, cheaply and unconditionally, by
    // `benchmark_workloads_return_their_expected_values`. Run it with:
    //
    //     cargo test --release --lib bench_baseline -- --ignored --nocapture
    //
    // ---------------------------------------------------------------------------------------
    // **How to read this table — the measurement protocol.** Learned the hard way on the F
    // track, and it applies to every optimisation this harness is used to judge.
    //
    // The dominant noise here is **code layout**, not the program. Any edit relinks the crate
    // and reshuffles function addresses, alignment and branch-predictor aliasing; on this
    // machine the resulting swing is **±3–12% per workload** — larger than the honest effect of
    // most changes worth making. It is not subtle: adding a `HashMap` field that was never read
    // moved a *control* workload by +3.4%. So a number from this table is only evidence when it
    // was collected like this:
    //
    //  1. **Never compare a single binary before and after.** That difference is the change and
    //     the relayout added together, and you cannot tell which one you are looking at.
    //  2. **Latin square.** Keep *both* binaries built and run them interleaved in mirrored
    //     orders (A B B A / B A A B …), so run position, thermal drift and background load fall
    //     on both arms equally. Same binaries in every position — never rebuild mid-experiment.
    //  3. **Zero-effect controls.** `BmLoop` and `BmArray` execute no `invoke` at all, so any
    //     change to the call path *must* leave them flat. When they move with the target, what
    //     moved was the layout: subtract their shift from the target's and report **both** the
    //     raw and the normalised figure, never the raw one alone.
    //  4. **Medians, and enough of them.** Report the median across invocations (each of which
    //     is already a median of 5 runs), and quote the minimum too: an interrupted run can only
    //     be slower, so the minimum is the least-perturbed sample. If the spread within one arm
    //     is wider than the effect you are claiming, you have not measured the effect yet.
    // ---------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------
    // **Two things this table used to lie about**, both fixed in place rather than annotated.
    //
    //  1. *The header.* It said "interpreter baseline" unconditionally, and the JIT's default
    //     became **on** several steps ago — so the words were wrong for every run anybody had
    //     made since. The engine is now read from `JitCache::enabled_by_env`, i.e. from the same
    //     line `from_env` reads, and the header says which one it measured and names the flag
    //     that changes it.
    //  2. *The `ns/opcode` column.* Dividing wall time by "opcodes the interpreter executed" is
    //     the right metric for an interpreter and an **artefact** for anything else: once a
    //     workload's loop is compiled, the numerator is the whole run and the denominator is only
    //     what was left over. `BmLoop` reported ~6624 ns/op over 620 opcodes that way — a number
    //     three orders of magnitude off, printed with two decimal places. So each workload is
    //     also run once with the JIT forced **off** (`bench_interpreted_opcodes`, untimed), and a
    //     timed run that counts fewer opcodes than that reference is one native code did part of:
    //     the column prints `jit` and the `native` column says how much.
    // ---------------------------------------------------------------------------------------
    #[test]
    #[ignore = "benchmark: prints the green baseline table, asserts no timing"]
    fn bench_baseline() {
        const RUNS: usize = 6; // 1 warm-up (discarded) + 5 measured

        let jit = crate::burst::code_cache::JitCache::enabled_by_env();
        let engine = match jit {
            true => "JIT on (the default) — set JVM_JIT=0 for the interpreter baseline",
            false => "interpreter only (JVM_JIT=0) — unset it for the JIT",
        };

        eprintln!();
        eprintln!("green, median of {} runs (1 warm-up discarded) — {engine}", RUNS - 1);
        eprintln!(
            "{:<10} {:>10} {:>12} {:>14} {:>8} {:>11}  dimension",
            "workload", "value", "median", "opcodes", "native", "ns/opcode"
        );
        eprintln!("{}", "-".repeat(105));

        for (class_file, expected, dimension) in BENCH_WORKLOADS {
            let short = class_file.trim_start_matches("java/").trim_end_matches(".class");
            // The denominator's alibi, collected before the clock starts and never timed.
            let interpreted = bench_interpreted_opcodes(class_file);
            let mut times = Vec::with_capacity(RUNS - 1);
            let (mut value, mut opcodes) = (0, 0);
            for run in 0..RUNS {
                let (v, steps, elapsed) = bench_green(class_file);
                assert_eq!(v, expected, "{short}: wrong result");
                if run > 0 {
                    // The warm-up run pays for first-touch page faults and CPU frequency ramp-up.
                    assert_eq!(steps, opcodes, "{short}: opcode count is not deterministic");
                    times.push(elapsed);
                }
                (value, opcodes) = (v, steps);
            }
            times.sort();
            let green_median = median(&times);
            // Fewer opcodes than the interpreter needed for the same program means native code did
            // the difference, and the ratio is what makes the `—` in the last column readable
            // rather than merely cautious.
            let compiled_away = interpreted.saturating_sub(opcodes);
            let (native, per_opcode) = match compiled_away {
                0 => ("—".to_string(), format!("{:>11.2}", green_median.as_nanos() as f64 / opcodes as f64)),
                n => (
                    format!("{:.0}%", 100.0 * n as f64 / interpreted.max(1) as f64),
                    format!("{:>11}", "jit"),
                ),
            };
            eprintln!(
                "{:<10} {:>10} {:>11.1?} {:>14} {:>8} {}  {}",
                short, value, green_median, opcodes, native, per_opcode, dimension
            );
        }
        eprintln!();
        eprintln!(
            "  native: share of this workload's interpreted opcodes that native code took over."
        );
        eprintln!(
            "  ns/opcode is printed only when it means something — i.e. when the whole run was"
        );
        eprintln!(
            "  interpreted. Where native code did part of the work, wall time over the opcodes it"
        );
        eprintln!("  left behind is an artefact, not a per-opcode cost, so the column says `jit`.");
        eprintln!();
    }

    // The **GIL tax**: the same five workloads on `green` and on `os-gil`, side by side. Both
    // run the identical single-threaded program, so the opcode count is the same on both and
    // green's count is used for each — the whole difference is the per-opcode cost of taking
    // and releasing the one `Mutex<SharedVm>` around every instruction, which the ratio column
    // states directly. Separate from `bench_baseline` (and named so its filter doesn't pick this
    // up) because os-gil is ~an order of magnitude slower, i.e. minutes rather than seconds:
    //
    //     cargo test --release --lib gil_overhead_bench -- --ignored --nocapture
    #[test]
    #[ignore = "benchmark: green vs os-gil per-opcode cost; minutes of CPU"]
    fn gil_overhead_bench() {
        const RUNS: usize = 4; // 1 warm-up (discarded) + 3 measured

        eprintln!();
        eprintln!("GIL tax — median of {} runs (1 warm-up discarded)", RUNS - 1);
        eprintln!(
            "{:<10} {:>12} {:>11} {:>12} {:>11} {:>10}",
            "workload", "green", "ns/opcode", "os-gil", "ns/opcode", "os-gil/green"
        );
        eprintln!("{}", "-".repeat(72));
        for (class_file, expected, _) in BENCH_WORKLOADS {
            let short = class_file.trim_start_matches("java/").trim_end_matches(".class");
            let (mut green_times, mut os_times) = (Vec::new(), Vec::new());
            let mut opcodes = 0;
            for run in 0..RUNS {
                let (v, steps, elapsed) = bench_green(class_file);
                assert_eq!(v, expected, "{short} (green): wrong result");
                opcodes = steps;
                let (v, os_elapsed) = bench_os_gil(class_file);
                assert_eq!(v, expected, "{short} (os-gil): wrong result");
                if run > 0 {
                    green_times.push(elapsed);
                    os_times.push(os_elapsed);
                }
            }
            green_times.sort();
            os_times.sort();
            let (green, os) = (median(&green_times), median(&os_times));
            eprintln!(
                "{:<10} {:>11.1?} {:>11.2} {:>12.1?} {:>11.2} {:>9.1}x",
                short,
                green,
                green.as_nanos() as f64 / opcodes as f64,
                os,
                os.as_nanos() as f64 / opcodes as f64,
                os.as_nanos() as f64 / green.as_nanos() as f64,
            );
        }
        eprintln!();
    }
}
