//! Bytecode disassembler: turns a method's raw `code[]` bytes into a list of
//! decoded [`Instruction`]s (JVM spec §6, "The JVM Instruction Set").
//!
//! Each instruction is a 1-byte opcode optionally followed by operand bytes.
//! This same opcode table is the backbone of the interpreter's dispatch loop:
//! decoding `0x60 -> iadd` here and *executing* `iadd` later share the structure.

/// A single decoded instruction.
#[derive(Debug, Clone)]
pub struct Instruction {
    /// Byte offset of this instruction within the method's `code[]` (its "pc").
    pub pc: usize,
    /// Mnemonic, e.g. `iload_0`, `invokestatic`.
    pub mnemonic: &'static str,
    /// Rendered operands, e.g. `#7`, `100`, a branch target; empty if none.
    pub operands: String,
    /// Total length of the instruction in bytes (opcode + operands).
    pub length: usize,
}

/// Disassembles a whole `code[]` array into instructions, in order.
pub fn disassemble(code: &[u8]) -> Vec<Instruction> {
    let mut out = Vec::new();
    let mut pc = 0;
    while pc < code.len() {
        let ins = decode(code, pc);
        pc += ins.length.max(1); // .max(1) guards against any 0-length surprise
        out.push(ins);
    }
    out
}

/// Decodes the single instruction starting at `pc`.
pub fn decode(code: &[u8], pc: usize) -> Instruction {
    match code[pc] {
        0xaa => decode_tableswitch(code, pc),
        0xab => decode_lookupswitch(code, pc),
        0xc4 => decode_wide(code, pc),
        op => {
            let (mnemonic, operand) = fixed(op);
            let (operands, length) = render(operand, code, pc);
            Instruction { pc, mnemonic, operands, length }
        }
    }
}

/// Operand shape of a fixed-length instruction (decides length + rendering).
enum Operand {
    None,            // no operands
    LocalIndex,      // u1 local-variable index
    ByteValue,       // i1 immediate (bipush)
    ShortValue,      // i2 immediate (sipush)
    CpIndex1,        // u1 constant-pool index (ldc)
    CpIndex2,        // u2 constant-pool index
    Branch2,         // i2 branch offset (relative to this pc)
    Branch4,         // i4 branch offset
    Iinc,            // u1 index + i1 const
    InvokeInterface, // u2 cp + u1 count + u1 zero
    InvokeDynamic,   // u2 cp + u1 zero + u1 zero
    NewArray,        // u1 atype
    MultiANewArray,  // u2 cp + u1 dimensions
}

fn render(operand: Operand, code: &[u8], pc: usize) -> (String, usize) {
    use Operand::*;
    match operand {
        None => (String::new(), 1),
        LocalIndex => (format!("{}", u1(code, pc + 1)), 2),
        ByteValue => (format!("{}", u1(code, pc + 1) as i8), 2),
        ShortValue => (format!("{}", i2(code, pc + 1)), 3),
        CpIndex1 => (format!("#{}", u1(code, pc + 1)), 2),
        CpIndex2 => (format!("#{}", u2(code, pc + 1)), 3),
        Branch2 => (format!("{}", pc as i64 + i2(code, pc + 1) as i64), 3),
        Branch4 => (format!("{}", pc as i64 + i4(code, pc + 1) as i64), 5),
        Iinc => (format!("{}, {}", u1(code, pc + 1), u1(code, pc + 2) as i8), 3),
        // invokeinterface is `B9 idx idx count 0`; javap shows the arg count.
        InvokeInterface => (format!("#{},  {}", u2(code, pc + 1), u1(code, pc + 3)), 5),
        // invokedynamic is `BA idx idx 0 0`; javap renders the trailing zero byte.
        InvokeDynamic => (format!("#{},  0", u2(code, pc + 1)), 5),
        // javap aligns the atype one column further right than other operands.
        NewArray => (format!(" {}", atype_name(u1(code, pc + 1))), 2),
        // javap renders the dimension count after `#index,` with two spaces,
        // like the count on invokeinterface/invokedynamic.
        MultiANewArray => (format!("#{},  {}", u2(code, pc + 1), u1(code, pc + 3)), 4),
    }
}

// --- variable-length instructions ---------------------------------------

// javap renders switches as a multi-line block: `{ // <header>`, one
// `<right-justified key>: <target>` line per case (cases, then `default`), and a
// closing `}`. We build the whole block into `operands`; the per-instruction
// printer emits it verbatim. Case keys are right-justified to column 24.
fn decode_tableswitch(code: &[u8], pc: usize) -> Instruction {
    let mut i = pc + 1 + padding(pc); // skip 0-3 bytes of alignment padding
    let default = i4(code, i);
    i += 4;
    let low = i4(code, i);
    i += 4;
    let high = i4(code, i);
    i += 4;
    let count = (high - low + 1).max(0) as usize;
    let mut s = format!("{{ // {low} to {high}");
    for k in 0..count {
        let off = i4(code, i);
        i += 4;
        // javap prints a blank line before each numbered case (but not before
        // `default` or the closing brace).
        s.push_str(&format!("\n\n{:>24}: {}", low as i64 + k as i64, pc as i64 + off as i64));
    }
    s.push_str(&format!("\n{:>24}: {}\n            }}", "default", pc as i64 + default as i64));
    Instruction { pc, mnemonic: "tableswitch", operands: s, length: i - pc }
}

fn decode_lookupswitch(code: &[u8], pc: usize) -> Instruction {
    let mut i = pc + 1 + padding(pc);
    let default = i4(code, i);
    i += 4;
    let npairs = i4(code, i).max(0) as usize;
    i += 4;
    let mut s = format!("{{ // {npairs}");
    for _ in 0..npairs {
        let m = i4(code, i);
        i += 4;
        let off = i4(code, i);
        i += 4;
        s.push_str(&format!("\n\n{:>24}: {}", m, pc as i64 + off as i64));
    }
    s.push_str(&format!("\n{:>24}: {}\n            }}", "default", pc as i64 + default as i64));
    Instruction { pc, mnemonic: "lookupswitch", operands: s, length: i - pc }
}

fn decode_wide(code: &[u8], pc: usize) -> Instruction {
    // javap renders a wide instruction as the wrapped mnemonic with a `_w`
    // suffix (e.g. `iinc_w`, `iload_w`), not as a `wide <op>` prefix.
    let inner = u1(code, pc + 1);
    if inner == 0x84 {
        // wide iinc: u2 index, i2 const  -> 6 bytes total
        Instruction {
            pc,
            mnemonic: "iinc_w",
            operands: format!("{}, {}", u2(code, pc + 2), i2(code, pc + 4)),
            length: 6,
        }
    } else {
        // wide <load/store/ret>: u2 index  -> 4 bytes total
        Instruction {
            pc,
            mnemonic: wide_mnemonic(inner),
            operands: u2(code, pc + 2).to_string(),
            length: 4,
        }
    }
}

/// The `_w` mnemonic javap shows for a `wide`-prefixed load/store/ret.
fn wide_mnemonic(inner: u8) -> &'static str {
    match inner {
        0x15 => "iload_w",
        0x16 => "lload_w",
        0x17 => "fload_w",
        0x18 => "dload_w",
        0x19 => "aload_w",
        0x36 => "istore_w",
        0x37 => "lstore_w",
        0x38 => "fstore_w",
        0x39 => "dstore_w",
        0x3a => "astore_w",
        0xa9 => "ret_w",
        _ => "wide",
    }
}

/// Bytes of padding after a switch opcode so the jump table is 4-byte aligned
/// (measured from the start of `code[]`).
fn padding(pc: usize) -> usize {
    (4 - ((pc + 1) % 4)) % 4
}

fn atype_name(t: u8) -> &'static str {
    match t {
        4 => "boolean",
        5 => "char",
        6 => "float",
        7 => "double",
        8 => "byte",
        9 => "short",
        10 => "int",
        11 => "long",
        _ => "?",
    }
}

// --- raw little readers (saturating: out-of-range -> 0) ------------------

fn u1(code: &[u8], i: usize) -> u8 {
    code.get(i).copied().unwrap_or(0)
}
fn u2(code: &[u8], i: usize) -> u16 {
    ((u1(code, i) as u16) << 8) | u1(code, i + 1) as u16
}
fn i2(code: &[u8], i: usize) -> i16 {
    u2(code, i) as i16
}
fn i4(code: &[u8], i: usize) -> i32 {
    ((u1(code, i) as i32) << 24)
        | ((u1(code, i + 1) as i32) << 16)
        | ((u1(code, i + 2) as i32) << 8)
        | (u1(code, i + 3) as i32)
}

/// Maps a fixed-length opcode to its mnemonic and operand shape (JVM spec §6).
fn fixed(op: u8) -> (&'static str, Operand) {
    use Operand::*;
    match op {
        0x00 => ("nop", None),
        0x01 => ("aconst_null", None),
        0x02 => ("iconst_m1", None),
        0x03 => ("iconst_0", None),
        0x04 => ("iconst_1", None),
        0x05 => ("iconst_2", None),
        0x06 => ("iconst_3", None),
        0x07 => ("iconst_4", None),
        0x08 => ("iconst_5", None),
        0x09 => ("lconst_0", None),
        0x0a => ("lconst_1", None),
        0x0b => ("fconst_0", None),
        0x0c => ("fconst_1", None),
        0x0d => ("fconst_2", None),
        0x0e => ("dconst_0", None),
        0x0f => ("dconst_1", None),
        0x10 => ("bipush", ByteValue),
        0x11 => ("sipush", ShortValue),
        0x12 => ("ldc", CpIndex1),
        0x13 => ("ldc_w", CpIndex2),
        0x14 => ("ldc2_w", CpIndex2),
        0x15 => ("iload", LocalIndex),
        0x16 => ("lload", LocalIndex),
        0x17 => ("fload", LocalIndex),
        0x18 => ("dload", LocalIndex),
        0x19 => ("aload", LocalIndex),
        0x1a => ("iload_0", None),
        0x1b => ("iload_1", None),
        0x1c => ("iload_2", None),
        0x1d => ("iload_3", None),
        0x1e => ("lload_0", None),
        0x1f => ("lload_1", None),
        0x20 => ("lload_2", None),
        0x21 => ("lload_3", None),
        0x22 => ("fload_0", None),
        0x23 => ("fload_1", None),
        0x24 => ("fload_2", None),
        0x25 => ("fload_3", None),
        0x26 => ("dload_0", None),
        0x27 => ("dload_1", None),
        0x28 => ("dload_2", None),
        0x29 => ("dload_3", None),
        0x2a => ("aload_0", None),
        0x2b => ("aload_1", None),
        0x2c => ("aload_2", None),
        0x2d => ("aload_3", None),
        0x2e => ("iaload", None),
        0x2f => ("laload", None),
        0x30 => ("faload", None),
        0x31 => ("daload", None),
        0x32 => ("aaload", None),
        0x33 => ("baload", None),
        0x34 => ("caload", None),
        0x35 => ("saload", None),
        0x36 => ("istore", LocalIndex),
        0x37 => ("lstore", LocalIndex),
        0x38 => ("fstore", LocalIndex),
        0x39 => ("dstore", LocalIndex),
        0x3a => ("astore", LocalIndex),
        0x3b => ("istore_0", None),
        0x3c => ("istore_1", None),
        0x3d => ("istore_2", None),
        0x3e => ("istore_3", None),
        0x3f => ("lstore_0", None),
        0x40 => ("lstore_1", None),
        0x41 => ("lstore_2", None),
        0x42 => ("lstore_3", None),
        0x43 => ("fstore_0", None),
        0x44 => ("fstore_1", None),
        0x45 => ("fstore_2", None),
        0x46 => ("fstore_3", None),
        0x47 => ("dstore_0", None),
        0x48 => ("dstore_1", None),
        0x49 => ("dstore_2", None),
        0x4a => ("dstore_3", None),
        0x4b => ("astore_0", None),
        0x4c => ("astore_1", None),
        0x4d => ("astore_2", None),
        0x4e => ("astore_3", None),
        0x4f => ("iastore", None),
        0x50 => ("lastore", None),
        0x51 => ("fastore", None),
        0x52 => ("dastore", None),
        0x53 => ("aastore", None),
        0x54 => ("bastore", None),
        0x55 => ("castore", None),
        0x56 => ("sastore", None),
        0x57 => ("pop", None),
        0x58 => ("pop2", None),
        0x59 => ("dup", None),
        0x5a => ("dup_x1", None),
        0x5b => ("dup_x2", None),
        0x5c => ("dup2", None),
        0x5d => ("dup2_x1", None),
        0x5e => ("dup2_x2", None),
        0x5f => ("swap", None),
        0x60 => ("iadd", None),
        0x61 => ("ladd", None),
        0x62 => ("fadd", None),
        0x63 => ("dadd", None),
        0x64 => ("isub", None),
        0x65 => ("lsub", None),
        0x66 => ("fsub", None),
        0x67 => ("dsub", None),
        0x68 => ("imul", None),
        0x69 => ("lmul", None),
        0x6a => ("fmul", None),
        0x6b => ("dmul", None),
        0x6c => ("idiv", None),
        0x6d => ("ldiv", None),
        0x6e => ("fdiv", None),
        0x6f => ("ddiv", None),
        0x70 => ("irem", None),
        0x71 => ("lrem", None),
        0x72 => ("frem", None),
        0x73 => ("drem", None),
        0x74 => ("ineg", None),
        0x75 => ("lneg", None),
        0x76 => ("fneg", None),
        0x77 => ("dneg", None),
        0x78 => ("ishl", None),
        0x79 => ("lshl", None),
        0x7a => ("ishr", None),
        0x7b => ("lshr", None),
        0x7c => ("iushr", None),
        0x7d => ("lushr", None),
        0x7e => ("iand", None),
        0x7f => ("land", None),
        0x80 => ("ior", None),
        0x81 => ("lor", None),
        0x82 => ("ixor", None),
        0x83 => ("lxor", None),
        0x84 => ("iinc", Iinc),
        0x85 => ("i2l", None),
        0x86 => ("i2f", None),
        0x87 => ("i2d", None),
        0x88 => ("l2i", None),
        0x89 => ("l2f", None),
        0x8a => ("l2d", None),
        0x8b => ("f2i", None),
        0x8c => ("f2l", None),
        0x8d => ("f2d", None),
        0x8e => ("d2i", None),
        0x8f => ("d2l", None),
        0x90 => ("d2f", None),
        0x91 => ("i2b", None),
        0x92 => ("i2c", None),
        0x93 => ("i2s", None),
        0x94 => ("lcmp", None),
        0x95 => ("fcmpl", None),
        0x96 => ("fcmpg", None),
        0x97 => ("dcmpl", None),
        0x98 => ("dcmpg", None),
        0x99 => ("ifeq", Branch2),
        0x9a => ("ifne", Branch2),
        0x9b => ("iflt", Branch2),
        0x9c => ("ifge", Branch2),
        0x9d => ("ifgt", Branch2),
        0x9e => ("ifle", Branch2),
        0x9f => ("if_icmpeq", Branch2),
        0xa0 => ("if_icmpne", Branch2),
        0xa1 => ("if_icmplt", Branch2),
        0xa2 => ("if_icmpge", Branch2),
        0xa3 => ("if_icmpgt", Branch2),
        0xa4 => ("if_icmple", Branch2),
        0xa5 => ("if_acmpeq", Branch2),
        0xa6 => ("if_acmpne", Branch2),
        0xa7 => ("goto", Branch2),
        0xa8 => ("jsr", Branch2),
        0xa9 => ("ret", LocalIndex),
        0xac => ("ireturn", None),
        0xad => ("lreturn", None),
        0xae => ("freturn", None),
        0xaf => ("dreturn", None),
        0xb0 => ("areturn", None),
        0xb1 => ("return", None),
        0xb2 => ("getstatic", CpIndex2),
        0xb3 => ("putstatic", CpIndex2),
        0xb4 => ("getfield", CpIndex2),
        0xb5 => ("putfield", CpIndex2),
        0xb6 => ("invokevirtual", CpIndex2),
        0xb7 => ("invokespecial", CpIndex2),
        0xb8 => ("invokestatic", CpIndex2),
        0xb9 => ("invokeinterface", InvokeInterface),
        0xba => ("invokedynamic", InvokeDynamic),
        0xbb => ("new", CpIndex2),
        0xbc => ("newarray", NewArray),
        0xbd => ("anewarray", CpIndex2),
        0xbe => ("arraylength", None),
        0xbf => ("athrow", None),
        0xc0 => ("checkcast", CpIndex2),
        0xc1 => ("instanceof", CpIndex2),
        0xc2 => ("monitorenter", None),
        0xc3 => ("monitorexit", None),
        0xc5 => ("multianewarray", MultiANewArray),
        0xc6 => ("ifnull", Branch2),
        0xc7 => ("ifnonnull", Branch2),
        0xc8 => ("goto_w", Branch4),
        0xc9 => ("jsr_w", Branch4),
        _ => ("???", None),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn disassembles_add_method() {
        // iload_0, iload_1, iadd, ireturn  (the body of `int add(int,int)`)
        let code = [0x1a, 0x1b, 0x60, 0xac];
        let ins = disassemble(&code);
        let names: Vec<&str> = ins.iter().map(|i| i.mnemonic).collect();
        assert_eq!(names, ["iload_0", "iload_1", "iadd", "ireturn"]);
        assert_eq!(ins[3].pc, 3);
    }

    #[test]
    fn renders_operands() {
        // invokestatic #7 (b8 00 07) ; bipush 100 (10 64)
        let invoke = disassemble(&[0xb8, 0x00, 0x07]);
        assert_eq!(invoke[0].mnemonic, "invokestatic");
        assert_eq!(invoke[0].operands, "#7");

        let bipush = disassemble(&[0x10, 0x64]);
        assert_eq!(bipush[0].mnemonic, "bipush");
        assert_eq!(bipush[0].operands, "100");
    }
}
