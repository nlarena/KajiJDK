//! The member-visibility filter behind javap's `-public` / `-protected` /
//! `-package` / `-private` (`-p`) flags. Each level defines a *minimum*
//! visibility; members below it are hidden. The default is `Package`, which
//! matches plain `javap` (it hides only `private` members).

/// JVM access-flag bits relevant to visibility (JVM spec §4.5/§4.6).
const ACC_PUBLIC: u16 = 0x0001;
const ACC_PRIVATE: u16 = 0x0002;
const ACC_PROTECTED: u16 = 0x0004;

#[derive(Clone, Copy)]
pub enum Visibility {
    Public,
    Protected,
    Package,
    Private,
}

impl Visibility {
    /// Reads the visibility flag from the CLI args. The last matching flag wins;
    /// with none, javap defaults to `Package`.
    pub fn from_args(args: &[String]) -> Visibility {
        let mut level = Visibility::Package;
        for a in args {
            level = match a.as_str() {
                "-public" => Visibility::Public,
                "-protected" => Visibility::Protected,
                "-package" => Visibility::Package,
                "-p" | "-private" => Visibility::Private,
                _ => continue,
            };
        }
        level
    }

    /// Whether a member with these `access_flags` is shown at this level.
    pub fn is_visible(self, access_flags: u16) -> bool {
        match self {
            Visibility::Public => access_flags & ACC_PUBLIC != 0,
            Visibility::Protected => access_flags & (ACC_PUBLIC | ACC_PROTECTED) != 0,
            // package-private has none of the three bits set; only `private` is hidden.
            Visibility::Package => access_flags & ACC_PRIVATE == 0,
            Visibility::Private => true,
        }
    }
}
