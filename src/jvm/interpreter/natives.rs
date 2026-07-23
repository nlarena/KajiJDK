//! The **native bridge** — the escape hatch from bytecode to real native code.
//!
//! A `native` method has no `Code`; the interpreter, instead of pushing a frame,
//! calls in here. This is where the JVM reaches the outside world (I/O, the OS) —
//! the things Java can't do itself. In a real JVM these are C/C++ via JNI; ours are
//! Rust functions matched by `(class, name, descriptor)`.
//!
//! Right now there's one: `PrintStream.println(int)`, so `System.out.println(n)`
//! prints for real — the wall the whole interpreter has been building toward.

use std::fmt::Write;

use super::bytecode_interpreter::class_operations;
use super::frame::Value;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;
use super::strings;

/// Runs the native method `class.name descriptor` with `args` (slot 0 is the
/// receiver for an instance method), returning its result (`None` for `void`).
/// `heap` lets a native read object memory (e.g. an object's header); anything the
/// method "prints" is appended to `out` — the program's stdout, which the caller
/// surfaces (the visualizer shows it; a headless run would flush it).
pub fn dispatch(
    class: &str,
    name: &str,
    descriptor: &str,
    args: &[Value],
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    out: &mut String,
) -> Option<Value> {
    match (class, name, descriptor) {
        // --- I/O: PrintStream.println --------------------------------------------
        // The receiver is args[0]; the value follows. The native `write` the real
        // java.io chain bottoms out at.
        ("java/io/PrintStream", "println", "(I)V") => {
            if let Value::Int(n) = args[1] {
                let _ = writeln!(out, "{n}");
            }
            None
        }
        // println(String): the arg is a heap String reference; read its bytes back.
        ("java/io/PrintStream", "println", "(Ljava/lang/String;)V") => {
            let _ = writeln!(out, "{}", strings::read(heap, reference(&args[1])));
            None
        }

        // --- Introspection / identity (things Java can't read of itself) ---------
        // getClass(): the receiver's header `class_id` *is* its Class<…> mirror.
        ("java/lang/Object", "getClass", "()Ljava/lang/Class;") => {
            Some(Value::Reference(heap.read_u32(reference(&args[0])) as usize))
        }
        // hashCode() (identity): the object's heap offset is its identity.
        ("java/lang/Object", "hashCode", "()I") => Some(Value::Int(reference(&args[0]) as i32)),
        // System.identityHashCode(Object): the same, as a static.
        ("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I") => {
            Some(Value::Int(reference(&args[0]) as i32))
        }

        // --- Math (would map to CPU instructions under a JIT) --------------------
        ("java/lang/Math", "abs", "(I)I") => Some(Value::Int(int(&args[0]).abs())),
        ("java/lang/Math", "max", "(II)I") => Some(Value::Int(int(&args[0]).max(int(&args[1])))),
        ("java/lang/Math", "min", "(II)I") => Some(Value::Int(int(&args[0]).min(int(&args[1])))),

        // --- Integer bit ops (popcnt / lzcnt) -----------------------------------
        ("java/lang/Integer", "bitCount", "(I)I") => {
            Some(Value::Int(int(&args[0]).count_ones() as i32))
        }
        ("java/lang/Integer", "numberOfLeadingZeros", "(I)I") => {
            Some(Value::Int(int(&args[0]).leading_zeros() as i32))
        }

        // --- Arrays: System.arraycopy -------------------------------------------
        // Bulk copy between arrays — the memcpy the VM does for you. Assumes 4-byte
        // elements (int/reference arrays); byte/char arrays would need their width.
        ("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V") => {
            const ARRAY_HEADER: usize = 12; // object header (8) + length word (4)
            const ELEM: usize = 4;
            let (src, src_pos) = (reference(&args[0]), int(&args[1]) as usize);
            let (dst, dst_pos) = (reference(&args[2]), int(&args[3]) as usize);
            let length = int(&args[4]) as usize;
            for i in 0..length {
                let value = heap.read_u32(src + ARRAY_HEADER + (src_pos + i) * ELEM);
                heap.write_u32(dst + ARRAY_HEADER + (dst_pos + i) * ELEM, value);
            }
            None
        }

        // --- Class.isInstance: the subtype check, reusing is_subtype -------------
        // The receiver is a Class mirror; args[1] is the object to test. `null` is
        // never an instance.
        ("java/lang/Class", "isInstance", "(Ljava/lang/Object;)Z") => {
            let object = reference(&args[1]);
            if object == 0 {
                return Some(Value::Int(0));
            }
            let target = metaspace.class_name_at_mirror(reference(&args[0])).map(str::to_string);
            let runtime =
                metaspace.class_name_at_mirror(heap.read_u32(object) as usize).map(str::to_string);
            let is = match (target, runtime) {
                (Some(t), Some(r)) => class_operations::is_subtype(metaspace, &r, &t),
                _ => false,
            };
            Some(Value::Int(is as i32))
        }

        // --- String -------------------------------------------------------------
        ("java/lang/String", "length", "()I") => {
            // The receiver is a heap String; its length word holds the UTF-8 byte count.
            Some(Value::Int(strings::length(heap, reference(&args[0])) as i32))
        }
        // charAt(int): the i-th byte (ASCII; our String is UTF-8, fine for ASCII).
        ("java/lang/String", "charAt", "(I)C") => {
            Some(Value::Int(strings::char_at(heap, reference(&args[0]), int(&args[1]) as usize) as i32))
        }
        // equals(Object): true if the other is a String with the same text.
        // (Simplified: assumes the argument is a String reference.)
        ("java/lang/String", "equals", "(Ljava/lang/Object;)Z") => {
            let other = reference(&args[1]);
            let equal = other != 0 && strings::read(heap, reference(&args[0])) == strings::read(heap, other);
            Some(Value::Int(equal as i32))
        }
        // hashCode(): Java's `s[0]*31^(n-1) + … + s[n-1]` over the bytes (ASCII).
        ("java/lang/String", "hashCode", "()I") => {
            let text = strings::read(heap, reference(&args[0]));
            let hash = text.bytes().fold(0i32, |h, b| h.wrapping_mul(31).wrapping_add(b as i32));
            Some(Value::Int(hash))
        }
        // startsWith(prefix): whether the receiver begins with the argument String.
        ("java/lang/String", "startsWith", "(Ljava/lang/String;)Z") => {
            let text = strings::read(heap, reference(&args[0]));
            let prefix = strings::read(heap, reference(&args[1]));
            Some(Value::Int(text.starts_with(&prefix) as i32))
        }

        _ => panic!("no native implementation for {class}.{name}{descriptor}"),
    }
}

/// The `int` payload of an argument (a verifier-guaranteed `Int`).
fn int(value: &Value) -> i32 {
    match value {
        Value::Int(n) => *n,
        other => panic!("native: expected an int argument, found {other:?}"),
    }
}

/// The heap offset of a reference argument (a verifier-guaranteed `Reference`).
fn reference(value: &Value) -> usize {
    match value {
        Value::Reference(offset) => *offset,
        other => panic!("native: expected a reference argument, found {other:?}"),
    }
}
