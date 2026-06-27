//! `javap` brief listing (the default mode): the source file, the class
//! declaration, and the non-private member signatures.

use super::dump_common;
use super::Visibility;
use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::MemberInfo;

pub struct Brief<'a> {
    cf: &'a ClassFile,
    visibility: Visibility,
}

impl<'a> Brief<'a> {
    pub fn new(cf: &'a ClassFile, visibility: Visibility) -> Self {
        Self { cf, visibility }
    }

    pub fn print(&self) {
        if let Some(src) = dump_common::source_file(self.cf) {
            crate::pln!("Compiled from \"{src}\"");
        }
        crate::pln!("{} {{", dump_common::class_declaration(self.cf, false));
        for (m, is_method) in dump_common::visible_members(self.cf, self.visibility) {
            self.print_member(m, is_method);
        }
        crate::pln!("}}");
    }

    fn print_member(&self, m: &MemberInfo, is_method: bool) {
        let name = self.cf.utf8(m.name_index).unwrap_or("?");
        let desc = self.cf.utf8(m.descriptor_index).unwrap_or("?");
        crate::pln!("  {};", dump_common::signature(self.cf, m, name, desc, is_method, false));
    }
}
