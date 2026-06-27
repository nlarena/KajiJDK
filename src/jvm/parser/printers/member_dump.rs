//! Verbose dump of one field or method: its signature, descriptor and flags,
//! plus (for methods) the `Code` attribute — bytecode disassembly with resolved
//! `// …` comments, the `LineNumberTable` and the `StackMapTable`.

use super::dump_common;
use super::pool_comments;
use crate::jvm::class_file::ClassFile;
use crate::jvm::opcode::{self, Instruction};
use crate::jvm::parser::attributes::{
    annotations, constant_value, exceptions, local_variables, method_parameters, signature,
};
use crate::jvm::parser::{stack_map_table, MemberInfo};

pub fn print(cf: &ClassFile, m: &MemberInfo, is_method: bool) {
    let name = cf.utf8(m.name_index).unwrap_or("?");
    let desc = cf.utf8(m.descriptor_index).unwrap_or("?");

    crate::pln!("  {};", dump_common::signature(cf, m, name, desc, is_method, true));
    crate::pln!("    descriptor: {desc}");
    let flag_names = dump_common::member_flag_names(m.access_flags, is_method).join(", ");
    let flag_suffix = if flag_names.is_empty() {
        String::new()
    } else {
        format!(" {flag_names}")
    };
    crate::pln!("    flags: ({:#06x}){}", m.access_flags, flag_suffix);

    // Member attributes in file order: Code, Exceptions, Signature.
    for attr in &m.attributes {
        match cf.utf8(attr.name_index) {
            Some("Code") => print_code(cf, m, desc),
            Some("ConstantValue") => {
                if let Some(idx) = constant_value::index(&attr.info) {
                    crate::pln!("    ConstantValue: {}", pool_comments::constant_value_text(cf, idx));
                }
            }
            Some("Deprecated") => crate::pln!("    Deprecated: true"),
            Some(label @ ("RuntimeVisibleAnnotations" | "RuntimeInvisibleAnnotations")) => {
                annotations::print_block(cf, label, &attr.info, 4);
            }
            Some(
                label @ ("RuntimeVisibleParameterAnnotations"
                | "RuntimeInvisibleParameterAnnotations"),
            ) => annotations::print_parameter_block(cf, label, &attr.info, 4),
            Some(
                label @ ("RuntimeVisibleTypeAnnotations" | "RuntimeInvisibleTypeAnnotations"),
            ) => annotations::print_type_block(cf, label, &attr.info, 4),
            Some("AnnotationDefault") => annotations::print_default(cf, &attr.info, 4),
            Some("Exceptions") => print_exceptions(cf, &attr.info),
            Some("MethodParameters") => print_method_parameters(cf, &attr.info),
            Some("Signature") => print_signature_line(cf, &attr.info),
            _ => {}
        }
    }
}

fn print_method_parameters(cf: &ClassFile, info: &[u8]) {
    crate::pln!("    MethodParameters:");
    crate::pln!("      {:<31}{}", "Name", "Flags");
    for p in method_parameters::parse(info) {
        let name = if p.name_index == 0 {
            "<no name>".to_string()
        } else {
            cf.utf8(p.name_index).unwrap_or("?").to_string()
        };
        let flags = method_parameters::flag_names(p.access_flags);
        if flags.is_empty() {
            crate::pln!("      {name}");
        } else {
            crate::pln!("      {name:<31}{flags}");
        }
    }
}

/// One record component, rendered like a field but without the `flags:` line.
pub fn print_record_component(cf: &ClassFile, m: &MemberInfo) {
    let name = cf.utf8(m.name_index).unwrap_or("?");
    let desc = cf.utf8(m.descriptor_index).unwrap_or("?");
    crate::pln!("  {};", dump_common::signature(cf, m, name, desc, false, true));
    crate::pln!("    descriptor: {desc}");
    for attr in &m.attributes {
        match cf.utf8(attr.name_index) {
            Some("Signature") => print_signature_line(cf, &attr.info),
            Some(label @ ("RuntimeVisibleAnnotations" | "RuntimeInvisibleAnnotations")) => {
                annotations::print_block(cf, label, &attr.info, 4);
            }
            _ => {}
        }
    }
}

/// The `    Signature: #N    // …` line for a member or record component.
fn print_signature_line(cf: &ClassFile, info: &[u8]) {
    if let Some(idx) = signature::index(info) {
        let left = format!("    Signature: #{idx}");
        crate::pln!("{left:<44}// {}", cf.utf8(idx).unwrap_or(""));
    }
}

fn print_code(cf: &ClassFile, m: &MemberInfo, desc: &str) {
    let Some(code) = cf.member_code(m) else { return };
    let static_method = m.access_flags & 0x0008 != 0;
    let args_size = dump_common::arg_count(desc) + usize::from(!static_method);
    crate::pln!("    Code:");
    crate::pln!(
        "      stack={}, locals={}, args_size={}",
        code.max_stack, code.max_locals, args_size
    );
    for ins in opcode::disassemble(&code.code) {
        print_instruction(cf, &ins);
    }
    // The exception table (try/catch handlers), after the bytecode.
    if !code.exception_table.is_empty() {
        crate::pln!("      Exception table:");
        crate::pln!("         from    to  target type");
        for e in &code.exception_table {
            let ty = if e.catch_type == 0 {
                "any".to_string()
            } else {
                format!("Class {}", cf.class_name(e.catch_type).unwrap_or("?"))
            };
            crate::pln!("{:>14}{:>6}{:>6}   {}", e.start_pc, e.end_pc, e.handler_pc, ty);
        }
    }
    // Decode the nested Code attributes we understand, in file order.
    for attr in &code.attributes {
        match cf.utf8(attr.name_index) {
            Some("LineNumberTable") => {
                crate::pln!("      LineNumberTable:");
                for (start_pc, line) in line_number_table(&attr.info) {
                    crate::pln!("        line {line}: {start_pc}");
                }
            }
            Some("StackMapTable") => {
                if let Ok(table) = stack_map_table::parse(&attr.info) {
                    crate::pln!("      StackMapTable: number_of_entries = {}", table.frames.len());
                    for frame in &table.frames {
                        frame.print(cf);
                    }
                }
            }
            Some(label @ ("LocalVariableTable" | "LocalVariableTypeTable")) => {
                crate::pln!("      {label}:");
                crate::pln!(
                    "        {:>5}{:>8}{:>6}{:>6}   {}",
                    "Start", "Length", "Slot", "Name", "Signature"
                );
                for v in local_variables::parse(&attr.info) {
                    // Name is right-justified in width 6, but always keeps at least
                    // one leading space (so longer names widen the column).
                    let name = cf.utf8(v.name_index).unwrap_or("?");
                    let nw = (name.len() + 1).max(6);
                    crate::pln!(
                        "        {:>5}{:>8}{:>6}{:>nw$}   {}",
                        v.start_pc,
                        v.length,
                        v.slot,
                        name,
                        cf.utf8(v.type_index).unwrap_or("?")
                    );
                }
            }
            _ => {}
        }
    }
}

fn print_exceptions(cf: &ClassFile, info: &[u8]) {
    let names: Vec<String> = exceptions::parse(info)
        .iter()
        .map(|&i| cf.class_name(i).unwrap_or("?").replace('/', "."))
        .collect();
    crate::pln!("    Exceptions:");
    crate::pln!("      throws {}", names.join(", "));
}

fn print_instruction(cf: &ClassFile, ins: &Instruction) {
    let left = if ins.operands.is_empty() {
        format!("      {:>4}: {}", ins.pc, ins.mnemonic)
    } else {
        format!("      {:>4}: {:<13} {}", ins.pc, ins.mnemonic, ins.operands)
    };
    let comment = operand_comment(cf, ins);
    if comment.is_empty() {
        crate::pln!("{left}");
    } else {
        crate::pln!("{left:<46}// {comment}");
    }
}

/// Resolved comment text for an instruction whose operand is a `#index`.
fn operand_comment(cf: &ClassFile, ins: &Instruction) -> String {
    let Some(rest) = ins.operands.strip_prefix('#') else {
        return String::new();
    };
    // Take only the leading digits: operands like `#7,  0` (invokedynamic) carry
    // a trailing `,  0` that isn't part of the constant-pool index.
    let digits: String = rest.chars().take_while(|c| c.is_ascii_digit()).collect();
    let Ok(index) = digits.parse::<u16>() else {
        return String::new();
    };
    pool_comments::instruction_comment(cf, index)
}

/// Decodes a `LineNumberTable` attribute body into (start_pc, line) pairs.
fn line_number_table(info: &[u8]) -> Vec<(u16, u16)> {
    let u16 = |i: usize| {
        ((info.get(i).copied().unwrap_or(0) as u16) << 8) | info.get(i + 1).copied().unwrap_or(0) as u16
    };
    let mut out = Vec::new();
    if info.len() < 2 {
        return out;
    }
    let count = u16(0) as usize;
    let mut i = 2;
    for _ in 0..count {
        if i + 4 > info.len() {
            break;
        }
        out.push((u16(i), u16(i + 2)));
        i += 4;
    }
    out
}
