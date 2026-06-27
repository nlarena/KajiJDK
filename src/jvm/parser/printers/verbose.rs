//! `javap -v` verbose dump. This module is just the orchestration: it wires the
//! file header, class header, constant pool and per-member dumps together in
//! javap's order. The heavy lifting lives in sibling modules — `file_header`,
//! `pool_comments` and `member_dump`.

use super::{dump_common, file_header, member_dump, pool_comments, Visibility};
use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::attributes::{
    annotations, bootstrap_methods, inner_classes, module, nest, record, signature,
};

pub struct Verbose<'a> {
    cf: &'a ClassFile,
    /// Path of the `.class` on disk, for the file-metadata header.
    path: &'a str,
    visibility: Visibility,
}

impl<'a> Verbose<'a> {
    pub fn new(cf: &'a ClassFile, path: &'a str, visibility: Visibility) -> Self {
        Self { cf, path, visibility }
    }

    pub fn print(&self) {
        file_header::print(self.cf, self.path);
        self.print_header();
        pool_comments::print_listing(self.cf);
        crate::pln!("{{");
        // Fields then methods. javap emits a blank line *after* every field, and
        // after every method except the last member. So when a class ends in a
        // field (i.e. has no methods at all) there is a trailing blank before the
        // closing brace; a method-terminated class has none.
        let members = dump_common::visible_members(self.cf, self.visibility);
        let count = members.len();
        for (i, (m, is_method)) in members.iter().enumerate() {
            member_dump::print(self.cf, m, *is_method);
            let is_last = i + 1 == count;
            if !*is_method || !is_last {
                crate::pln!();
            }
        }
        crate::pln!("}}");
        // Class-level attributes, in their file order (e.g. Signature, SourceFile).
        for attr in &self.cf.attributes {
            match self.cf.utf8(attr.name_index) {
                Some("Signature") => {
                    if let Some(idx) = signature::index(&attr.info) {
                        let left = format!("Signature: #{idx}");
                        crate::pln!("{left:<40}// {}", self.cf.utf8(idx).unwrap_or(""));
                    }
                }
                Some("SourceFile") => {
                    if let Some(src) = dump_common::source_file(self.cf) {
                        crate::pln!("SourceFile: \"{src}\"");
                    }
                }
                Some("Deprecated") => crate::pln!("Deprecated: true"),
                Some(label @ ("RuntimeVisibleAnnotations" | "RuntimeInvisibleAnnotations")) => {
                    annotations::print_block(self.cf, label, &attr.info, 0);
                }
                Some(
                    label @ ("RuntimeVisibleTypeAnnotations" | "RuntimeInvisibleTypeAnnotations"),
                ) => annotations::print_type_block(self.cf, label, &attr.info, 0),
                Some("NestHost") => {
                    if let Some(idx) = nest::host(&attr.info) {
                        crate::pln!("NestHost: class {}", self.cf.class_name(idx).unwrap_or("?"));
                    }
                }
                Some("NestMembers") => {
                    crate::pln!("NestMembers:");
                    for idx in nest::members(&attr.info) {
                        crate::pln!("  {}", self.cf.class_name(idx).unwrap_or("?"));
                    }
                }
                // PermittedSubclasses (sealed) has the same layout as NestMembers.
                Some("PermittedSubclasses") => {
                    crate::pln!("PermittedSubclasses:");
                    for idx in nest::members(&attr.info) {
                        crate::pln!("  {}", self.cf.class_name(idx).unwrap_or("?"));
                    }
                }
                Some("EnclosingMethod") if attr.info.len() >= 4 => {
                    // The `#class.#method` pair is always shown (method index 0
                    // when the enclosing context is a class, not a method); only
                    // the comment drops the method name in that case.
                    let ci = u16::from_be_bytes([attr.info[0], attr.info[1]]);
                    let mi = u16::from_be_bytes([attr.info[2], attr.info[3]]);
                    let class = self.cf.class_name(ci).unwrap_or("?").replace('/', ".");
                    let comment = if mi == 0 {
                        class
                    } else {
                        format!("{class}.{}", self.cf.name_and_type_name(mi).unwrap_or("?"))
                    };
                    crate::pln!("{:<40}// {comment}", format!("EnclosingMethod: #{ci}.#{mi}"));
                }
                Some("Record") => {
                    crate::pln!("Record:");
                    for component in record::parse(&attr.info).iter() {
                        member_dump::print_record_component(self.cf, component);
                        crate::pln!();
                    }
                }
                Some("InnerClasses") => {
                    // javap filters entries by visibility and omits the whole block
                    // (header included) when nothing remains visible.
                    let visible: Vec<_> = inner_classes::parse(&attr.info)
                        .into_iter()
                        .filter(|ic| self.visibility.is_visible(ic.access_flags))
                        .collect();
                    if visible.is_empty() {
                        continue;
                    }
                    crate::pln!("InnerClasses:");
                    for ic in visible {
                        let mut left = format!("  {}", dump_common::inner_class_modifiers(ic.access_flags));
                        let mut comment = String::new();
                        if ic.inner_name != 0 {
                            left.push_str(&format!("#{}= ", ic.inner_name));
                            comment.push_str(&format!("{}=", self.cf.utf8(ic.inner_name).unwrap_or("")));
                        }
                        left.push_str(&format!("#{}", ic.inner_class_info));
                        comment.push_str(&format!(
                            "class {}",
                            self.cf.class_name(ic.inner_class_info).unwrap_or("")
                        ));
                        if ic.outer_class_info != 0 {
                            left.push_str(&format!(" of #{}", ic.outer_class_info));
                            comment.push_str(&format!(
                                " of class {}",
                                self.cf.class_name(ic.outer_class_info).unwrap_or("")
                            ));
                        }
                        left.push(';');
                        // javap pads to column 42, or leaves one space when the
                        // line already overflows it.
                        let pad = if left.len() < 42 { 42 } else { left.len() + 1 };
                        crate::pln!("{left:<pad$}// {comment}");
                    }
                }
                Some("Module") => module::print(self.cf, &attr.info),
                Some("ModulePackages") => module::print_packages(self.cf, &attr.info),
                Some("ModuleMainClass") => module::print_main_class(self.cf, &attr.info),
                Some("ModuleTarget") => module::print_target(self.cf, &attr.info),
                Some("ModuleHashes") => module::print_hashes(self.cf, &attr.info),
                Some("BootstrapMethods") => {
                    crate::pln!("BootstrapMethods:");
                    for (i, bm) in bootstrap_methods::parse(&attr.info).iter().enumerate() {
                        crate::pln!(
                            "  {i}: #{} {}",
                            bm.method_ref,
                            pool_comments::method_handle_text(self.cf, bm.method_ref)
                        );
                        crate::pln!("    Method arguments:");
                        for &arg in &bm.arguments {
                            crate::pln!("      #{arg} {}", pool_comments::bootstrap_arg_text(self.cf, arg));
                        }
                    }
                }
                _ => {}
            }
        }
    }

    /// The class header: declaration, version, flags, this/super and counts.
    fn print_header(&self) {
        let cf = self.cf;
        crate::pln!("{}", dump_common::class_declaration(cf, true));
        crate::pln!("  minor version: {}", cf.minor_version);
        crate::pln!("  major version: {}", cf.major_version);
        crate::pln!(
            "  flags: ({:#06x}) {}",
            cf.access_flags,
            dump_common::class_flag_names(cf.access_flags).join(", ")
        );
        let tc = format!("  this_class: #{}", cf.this_class);
        crate::pln!("{tc:<42}// {}", cf.class_name(cf.this_class).unwrap_or("?"));
        let sc = format!("  super_class: #{}", cf.super_class);
        match cf.class_name(cf.super_class) {
            Some(s) => crate::pln!("{sc:<42}// {s}"),
            None => crate::pln!("{sc}"),
        }
        crate::pln!(
            "  interfaces: {}, fields: {}, methods: {}, attributes: {}",
            cf.interfaces.len(),
            cf.fields.len(),
            cf.methods.len(),
            cf.attributes.len()
        );
    }
}
