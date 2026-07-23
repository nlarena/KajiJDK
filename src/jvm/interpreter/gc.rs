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

use super::bytecode_interpreter::objects_operations::{field_slots, HEADER_SIZE, SLOT_SIZE};
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
pub fn mark(metaspace: &MetaspaceService, heap: &mut HeapService, threads: &[GreenThread]) -> MarkReport {
    // 1. Start from a clean slate — last pass's marks are stale.
    heap.clear_all_marks();

    // 2. Seed the worklist with the roots, and trace transitively. `seen` guards
    //    against cycles (object A → B → A) and re-visiting shared objects.
    let mut worklist = roots(metaspace, heap, threads);
    let mut seen: HashSet<usize> = HashSet::new();
    while let Some(offset) = worklist.pop() {
        if offset == 0 || !seen.insert(offset) {
            continue; // `null`, or an object we've already marked
        }
        heap.set_mark(offset);
        // Follow each outgoing *strong* reference: `strong_reference_slots` gives the
        // heap addresses of this object's reference words, **minus** the weak `referent`
        // of a `java.lang.ref.Reference` — so a weakly-referenced object isn't kept alive.
        for slot in strong_reference_slots(metaspace, heap, offset) {
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
pub fn sweep(metaspace: &MetaspaceService, heap: &mut HeapService, threads: &[GreenThread]) -> MarkReport {
    let report = mark(metaspace, heap, threads);
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
pub fn minor(metaspace: &MetaspaceService, heap: &mut HeapService, threads: &mut [GreenThread], tenure: u8) -> MinorReport {
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
/// ⚠️ Like the rest of the GC, this is only fully correct once [`reference_slots`]
/// is implemented: today it rewrites the precise **frame roots** (so locals/operands
/// follow moved objects), but *inter-object* references (a field pointing at a moved
/// object) are left until the slot walk exists. Safe for object graphs without such
/// references (e.g. the demos).
pub fn compact(metaspace: &MetaspaceService, heap: &mut HeapService, threads: &mut [GreenThread]) -> CompactReport {
    // 1. Mark — only the live get relocated — then process weak references (clear dead
    //    referents + enqueue) before anything moves.
    mark(metaspace, heap, &*threads);
    process_weak_references(metaspace, heap);

    // Pinned set: the mirror offsets (they stay put).
    let pinned: HashSet<usize> =
        metaspace.class_object_offsets().iter().map(|&(_, _, off)| off).collect();
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
fn roots(metaspace: &MetaspaceService, _heap: &HeapService, threads: &[GreenThread]) -> Vec<usize> {
    let mut roots = Vec::new();

    // Stacks + locals of every frame on every thread's call stack. `Value` is tagged,
    // so this is *precise*: we add exactly the references, never an int that looks like one.
    for frame in threads.iter().flat_map(|t| t.frames.iter()) {
        for value in frame.stack().iter().chain(frame.locals()) {
            if let Value::Reference(offset) = value {
                roots.push(*offset);
            }
        }
    }

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

/// The strong outgoing references of an object — [`reference_slots`] minus the **weak**
/// `referent` of a `java.lang.ref.Reference`. The major **mark** uses this so an object
/// isn't kept alive merely because a weak reference points at it. (Compaction and the
/// minor still use the full slot set: compaction must relocate a *surviving* referent,
/// and the minor deliberately keeps young referents alive — see the module note.)
fn strong_reference_slots(metaspace: &MetaspaceService, heap: &HeapService, offset: usize) -> Vec<usize> {
    let mut slots = reference_slots(metaspace, heap, offset);
    if let Some(referent) = referent_slot(metaspace, heap, offset) {
        slots.retain(|&s| s != referent);
    }
    slots
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

/// Whether `class` is `java.lang.ref.Reference` or a subclass — walking the superclass
/// chain (immutably).
fn is_reference_subclass(metaspace: &MetaspaceService, class: &str) -> bool {
    let mut current = Some(class.to_string());
    while let Some(name) = current {
        if name == "java/lang/ref/Reference" {
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
            if cf.utf8(f.name_index) == Some(field) {
                return Some(HEADER_SIZE + index * SLOT_SIZE);
            }
            index += field_slots(cf.utf8(f.descriptor_index).unwrap_or(""));
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
    // A **synthetic** class (one the VM minted, like a lambda's) has no class file to
    // read a layout from, so it declares one. Without this its captured references would
    // be invisible to the collector: never marked, never rewritten when the object moves.
    if let Some(slots) = metaspace.synthetic_reference_slots(&class) {
        return slots.iter().map(|&within| offset + within).collect();
    }

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
            if is_reference_descriptor(cf, field.descriptor_index) {
                slots.push(offset + HEADER_SIZE + index * SLOT_SIZE);
            }
            // Width-aware: a `long`/`double` field consumes two slots, so a reference
            // declared after it lands two slots further along.
            index += field_slots(cf.utf8(field.descriptor_index).unwrap_or(""));
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
        if is_reference_descriptor(cf, f.descriptor_index) {
            slots.push(mirror + HEADER_SIZE + index * SLOT_SIZE);
        }
        // Width-aware: a `long`/`double` static consumes two slots.
        index += field_slots(cf.utf8(f.descriptor_index).unwrap_or(""));
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
        sweep(&metaspace, &mut heap, &[]);
        assert!(heap.allocations().iter().any(|a| a.offset == young), "young kept");
        assert!(!heap.allocations().iter().any(|a| a.offset == old), "old reclaimed");
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
        assert_eq!(jvm.call_java(twice, vec![Value::Int(21)], &[1]), Some(Value::Int(42)));

        // And a second call is independent — the nested loop leaves no residue.
        assert_eq!(jvm.call_java(twice, vec![Value::Int(-3)], &[1]), Some(Value::Int(-6)));
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

    /// Like `run_int` but forces the **OS-thread + GIL** substrate (real `std::thread`s),
    /// bypassing the `JVM_THREADS` env so parallel tests don't race on a global.
    fn run_int_os(class_file: &str) -> i32 {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute_os;
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
        match execute_os(metaspace, frame) {
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
}
