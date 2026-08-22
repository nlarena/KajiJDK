//! **`jimage`** — the container the JDK keeps its modules in (`lib/modules`) — Fase J, hito J3.
//!
//! An image is one file holding every resource of every linked module, plus an index that
//! finds a resource **by name in O(1)**, without scanning. The layout:
//!
//! ```text
//! [ header 28 B ][ redirects ][ offsets ][ locations ][ strings ][ resource data ]
//! ```
//!
//! Two things are worth knowing up front, because both bite:
//!
//! - It is **little-endian**, unlike the class file format (big-endian, JVMS §4.1). The
//!   same project reads both, so the cursor here is deliberately its own thing.
//! - `redirects` and `offsets` are two parallel tables of `table_length` 4-byte entries.
//!   Together they are a **perfect hash**: the name hashes into a slot, the redirect there
//!   says either "the answer is at index N" or "re-hash with this seed". That is what J3
//!   builds up to; this file starts at the header, which is what `jimage info` prints.

/// The image header: the first 28 bytes, seven little-endian `u32`s.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Header {
    pub magic: u32,
    pub major: u16,
    pub minor: u16,
    pub flags: u32,
    /// How many resources the image holds.
    pub resource_count: u32,
    /// How many slots the hash tables have. Equal to `resource_count` in the JDK's own
    /// images, but they are separate numbers: the table may be sized independently.
    pub table_length: u32,
    pub locations_size: u32,
    pub strings_size: u32,
}

/// `0xCAFEDADA` — the image's magic, cousin of the class file's `0xCAFEBABE`.
pub const MAGIC: u32 = 0xCAFE_DADA;

/// The header is seven `u32`s.
pub const HEADER_SIZE: usize = 28;

impl Header {
    /// Reads a header from the start of `bytes`. `None` if it is too short or the magic
    /// doesn't match — a wrong file should be rejected, not misread.
    pub fn parse(bytes: &[u8]) -> Option<Header> {
        if bytes.len() < HEADER_SIZE {
            return None;
        }
        let word = |i: usize| {
            u32::from_le_bytes([bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]])
        };
        let magic = word(0);
        if magic != MAGIC {
            return None;
        }
        // The version is one word: major in the high half, minor in the low one.
        let version = word(4);
        Some(Header {
            magic,
            major: (version >> 16) as u16,
            minor: (version & 0xFFFF) as u16,
            flags: word(8),
            resource_count: word(12),
            table_length: word(16),
            locations_size: word(20),
            strings_size: word(24),
        })
    }

    /// Bytes of the offsets table — one `u32` per slot. Derived, not stored.
    pub fn offsets_size(&self) -> u32 {
        self.table_length * 4
    }

    /// Bytes of the redirects table — same shape as the offsets one.
    pub fn redirects_size(&self) -> u32 {
        self.table_length * 4
    }

    /// The whole index: header plus the four sections before the resource data. This is
    /// also where the resource bytes start.
    pub fn index_size(&self) -> u32 {
        HEADER_SIZE as u32
            + self.redirects_size()
            + self.offsets_size()
            + self.locations_size
            + self.strings_size
    }

    /// Renders what `jimage info` prints: a leading space, the label padded to 16, the
    /// value. Kept identical to the reference tool so the two can be diffed directly.
    pub fn info(&self) -> String {
        let rows: [(&str, u64); 10] = [
            ("Major Version:", self.major as u64),
            ("Minor Version:", self.minor as u64),
            ("Flags:", self.flags as u64),
            ("Resource Count:", self.resource_count as u64),
            ("Table Length:", self.table_length as u64),
            ("Offsets Size:", self.offsets_size() as u64),
            ("Redirects Size:", self.redirects_size() as u64),
            ("Locations Size:", self.locations_size as u64),
            ("Strings Size:", self.strings_size as u64),
            ("Index Size:", self.index_size() as u64),
        ];
        rows.iter().map(|(label, value)| format!(" {label:<16}{value}\n")).collect()
    }
}


// -- the index: locations, strings, and the entries they describe ----------

/// One entry's *location*: where its bytes are and what it is called.
///
/// Names are stored split, not as one path: the module, the parent (directory), the base
/// name and the extension are four separate offsets into the strings blob. Sharing those
/// pieces is why 30473 entries need only ~680 KB of names.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Location {
    pub module: String,
    pub parent: String,
    pub base: String,
    pub extension: String,
    /// Where the bytes start, relative to the end of the index.
    pub offset: u64,
    /// Stored size; `0` means the entry is not compressed.
    pub compressed: u64,
    /// Size once uncompressed — the real length of the resource.
    pub uncompressed: u64,
}

impl Location {
    /// The path within its module, as `jimage list` prints it: `parent/base.extension`.
    pub fn path(&self) -> String {
        let mut path = String::new();
        if !self.parent.is_empty() {
            path.push_str(&self.parent);
            path.push('/');
        }
        path.push_str(&self.base);
        if !self.extension.is_empty() {
            path.push('.');
            path.push_str(&self.extension);
        }
        path
    }

    /// The full name, module included — the key the perfect hash is built over.
    ///
    /// The image has two **root** entries, `/modules` and `/packages`, that head the meta
    /// namespaces. They carry no module and keep their whole name in `base` (leading slash
    /// included), so composing a name for them would double the separators.
    pub fn full_name(&self) -> String {
        if self.module.is_empty() {
            self.path()
        } else {
            format!("/{}/{}", self.module, self.path())
        }
    }
}

/// Attribute kinds inside a location (JDK's `ImageLocation`). Each attribute is one tag
/// byte — `(kind << 3) | (length - 1)` — followed by `length` **big-endian** bytes. The
/// header is little-endian and these are not: the two halves of the format disagree, and
/// reading either one the other way yields plausible-looking garbage.
const ATTRIBUTE_END: u8 = 0;
const ATTRIBUTE_MODULE: u8 = 1;
const ATTRIBUTE_PARENT: u8 = 2;
const ATTRIBUTE_BASE: u8 = 3;
const ATTRIBUTE_EXTENSION: u8 = 4;
const ATTRIBUTE_OFFSET: u8 = 5;
const ATTRIBUTE_COMPRESSED: u8 = 6;
const ATTRIBUTE_UNCOMPRESSED: u8 = 7;

/// A parsed image index: enough to enumerate and find entries, without the resource bytes.
pub struct Index {
    pub header: Header,
    /// Perfect-hash redirect table; the offsets table it selects into.
    pub redirects: Vec<i32>,
    pub offsets: Vec<u32>,
    locations: Vec<u8>,
    strings: Vec<u8>,
}

impl Index {
    /// Parses the index out of the first `index_size` bytes of an image.
    pub fn parse(bytes: &[u8]) -> Option<Index> {
        let header = Header::parse(bytes)?;
        if bytes.len() < header.index_size() as usize {
            return None;
        }
        let slots = header.table_length as usize;
        let word = |i: usize| u32::from_le_bytes([bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]]);

        let redirects_at = HEADER_SIZE;
        let offsets_at = redirects_at + slots * 4;
        let locations_at = offsets_at + slots * 4;
        let strings_at = locations_at + header.locations_size as usize;

        let redirects = (0..slots).map(|i| word(redirects_at + i * 4) as i32).collect();
        let offsets = (0..slots).map(|i| word(offsets_at + i * 4)).collect();
        Some(Index {
            redirects,
            offsets,
            locations: bytes[locations_at..strings_at].to_vec(),
            strings: bytes[strings_at..strings_at + header.strings_size as usize].to_vec(),
            header,
        })
    }

    /// The NUL-terminated string at `offset` in the strings blob.
    fn string(&self, offset: usize) -> String {
        let rest = &self.strings[offset.min(self.strings.len())..];
        let end = rest.iter().position(|&b| b == 0).unwrap_or(rest.len());
        String::from_utf8_lossy(&rest[..end]).into_owned()
    }

    /// Decodes the location stored at `offset` in the locations blob. Offset `0` is the
    /// reserved empty location, which is how an unused hash slot is spelled.
    pub fn location(&self, offset: usize) -> Option<Location> {
        if offset == 0 || offset >= self.locations.len() {
            return None;
        }
        let mut location = Location::default();
        let mut i = offset;
        loop {
            let tag = *self.locations.get(i)?;
            if tag == ATTRIBUTE_END {
                break;
            }
            let (kind, length) = (tag >> 3, (tag & 7) as usize + 1);
            i += 1;
            let mut value: u64 = 0;
            for k in 0..length {
                value = (value << 8) | *self.locations.get(i + k)? as u64; // big-endian
            }
            i += length;
            match kind {
                ATTRIBUTE_MODULE => location.module = self.string(value as usize),
                ATTRIBUTE_PARENT => location.parent = self.string(value as usize),
                ATTRIBUTE_BASE => location.base = self.string(value as usize),
                ATTRIBUTE_EXTENSION => location.extension = self.string(value as usize),
                ATTRIBUTE_OFFSET => location.offset = value,
                ATTRIBUTE_COMPRESSED => location.compressed = value,
                ATTRIBUTE_UNCOMPRESSED => location.uncompressed = value,
                _ => {}
            }
        }
        Some(location)
    }

    /// Every entry in the image, in hash-table order.
    pub fn entries(&self) -> Vec<Location> {
        self.offsets.iter().filter_map(|&o| self.location(o as usize)).collect()
    }
}

// -- the perfect hash: finding an entry by name in O(1) -------------------

/// The multiplier of the image's string hash — the FNV-1a prime, and also the seed the
/// first round uses.
pub const HASH_MULTIPLIER: u32 = 0x0100_0193;

/// The image's string hash: FNV-1a over the bytes, with the sign bit cleared.
///
/// The `seed` is what makes the table *perfect*. A first pass hashes every name with
/// `HASH_MULTIPLIER`; names that collide in a bucket are re-hashed with a seed chosen so
/// that they land on distinct free slots. Which round applies is recorded per bucket in
/// the redirects table.
pub fn hash_name(name: &str, seed: u32) -> u32 {
    let mut hash = seed;
    for byte in name.as_bytes() {
        hash = hash.wrapping_mul(HASH_MULTIPLIER) ^ (*byte as u32);
    }
    hash & 0x7FFF_FFFF
}

impl Index {
    /// Looks a full name (`/module/path`) up through the perfect hash — one or two hashes
    /// and one array read, no scanning.
    ///
    /// The redirect at the name's bucket says which: `0` is an empty bucket (not found),
    /// a **negative** value is the answer outright (`-value - 1` indexes the offsets), and
    /// a **positive** value is the seed to hash with again.
    pub fn find(&self, name: &str) -> Option<Location> {
        let slots = self.header.table_length;
        if slots == 0 {
            return None;
        }
        let bucket = (hash_name(name, HASH_MULTIPLIER) % slots) as usize;
        let redirect = *self.redirects.get(bucket)?;
        let slot = match redirect {
            0 => return None,
            r if r < 0 => (-r - 1) as usize,
            seed => (hash_name(name, seed as u32) % slots) as usize,
        };
        let location = self.location(*self.offsets.get(slot)? as usize)?;
        // The table is perfect for the names it was built from, not for any name: a name
        // that was never in it still lands somewhere. Confirming the hit is what makes a
        // lookup of an absent name return `None` instead of somebody else's resource.
        (location.full_name() == name).then_some(location)
    }
}

// -- the writer: building the perfect hash (J4) ---------------------------

/// One resource on its way into an image.
pub struct Resource {
    /// Full name, `/module/path`.
    pub name: String,
    pub bytes: Vec<u8>,
}

/// How many seeds to try for one bucket before declaring the table too tight. A bucket
/// that cannot be placed is not a dead end — the table simply needs to be bigger.
const SEED_ATTEMPTS: u32 = 20_000;

/// Builds the two tables of the perfect hash for `names`, or `None` if this table size is
/// too tight. Use [`perfect_hash`] to have the size chosen for you.
///
/// This is the inverse of [`Index::find`], and the only genuinely hard part of writing an
/// image. Every name hashes into a bucket with the fixed seed; buckets with one name are
/// answered outright, and buckets with several need a **seed that scatters them onto
/// distinct free slots**. Because a seed that works for a big bucket is rarer, the buckets
/// are solved **largest first** — while there is still plenty of free space.
///
/// Failure is real and has to be handled: at a load factor of 1.0 the last buckets have
/// almost no free slots left, and for some name sets *no* seed places them. Searching
/// forever is the bug that hides here.
pub fn build_perfect_hash(
    names: &[String],
    table_length: usize,
) -> Option<(Vec<i32>, Vec<Option<usize>>)> {
    let mut buckets: Vec<Vec<usize>> = vec![Vec::new(); table_length];
    for (i, name) in names.iter().enumerate() {
        buckets[(hash_name(name, HASH_MULTIPLIER) % table_length as u32) as usize].push(i);
    }

    let mut order: Vec<Option<usize>> = vec![None; table_length];
    let mut redirects: Vec<i32> = vec![0; table_length];

    let mut indices: Vec<usize> = (0..table_length).filter(|&b| !buckets[b].is_empty()).collect();
    indices.sort_by_key(|&b| std::cmp::Reverse(buckets[b].len()));

    let mut singles = Vec::new();
    for bucket in indices {
        if buckets[bucket].len() == 1 {
            singles.push(bucket); // se resuelven al final, sobre lo que quede libre
            continue;
        }
        // Buscar una semilla que mande a todo el bucket a slots libres y distintos.
        let mut seed: u32 = 1;
        let mut placed = false;
        while seed <= SEED_ATTEMPTS {
            let mut slots = Vec::with_capacity(buckets[bucket].len());
            let fits = buckets[bucket].iter().all(|&name| {
                let slot = (hash_name(&names[name], seed) % table_length as u32) as usize;
                if order[slot].is_some() || slots.contains(&slot) {
                    return false;
                }
                slots.push(slot);
                true
            });
            if fits {
                for (&name, &slot) in buckets[bucket].iter().zip(slots.iter()) {
                    order[slot] = Some(name);
                }
                redirects[bucket] = seed as i32; // positivo: volver a hashear con esta semilla
                placed = true;
                break;
            }
            seed += 1;
        }
        if !placed {
            return None; // tabla demasiado ajustada para este conjunto
        }
    }

    // Los buckets de un solo nombre apuntan directo a cualquier slot libre: sin segundo
    // hash. Se codifica negado (`-slot - 1`) para distinguirlo de una semilla.
    let mut free = 0usize;
    for bucket in singles {
        while order[free].is_some() {
            free += 1;
        }
        order[free] = Some(buckets[bucket][0]);
        redirects[bucket] = -(free as i32) - 1;
    }
    Some((redirects, order))
}

/// Builds the perfect hash, growing the table until it fits.
///
/// It starts at one slot per name — the load factor the JDK's own images use — and grows by
/// a quarter on failure. This is why `table_length` is a **separate field** from
/// `resource_count` in the header: they are equal in a stock JDK image, but they need not be.
pub fn perfect_hash(names: &[String]) -> (usize, Vec<i32>, Vec<Option<usize>>) {
    let mut table_length = names.len().max(1);
    loop {
        if let Some((redirects, order)) = build_perfect_hash(names, table_length) {
            return (table_length, redirects, order);
        }
        table_length += table_length / 4 + 1;
    }
}

/// Serialises `resources` into a complete image.
///
/// The five sections are laid out in order, and the awkward part is that `locations` needs
/// string offsets while `strings` needs to already contain them: the blob is therefore
/// built first, interning each distinct piece, and the locations refer into it.
///
/// Names are split the way the format wants them — module, parent directory, base name,
/// extension — which is also what makes the strings blob small: the pieces are shared.
pub fn write_image(resources: &[Resource]) -> Vec<u8> {
    write_image_with(resources, false)
}

/// Serialises an image, optionally storing every resource as a **compressed resource**
/// (`--compress`). See [`zlib_stored`] for what "compressed" means here.
pub fn write_image_with(resources: &[Resource], compress: bool) -> Vec<u8> {
    let names: Vec<String> = resources.iter().map(|r| r.name.clone()).collect();
    let (table_length, redirects, order) = perfect_hash(&names);

    // -- strings: un blob de cadenas NUL-terminadas, con el offset 0 reservado a "" --
    let mut strings: Vec<u8> = vec![0];
    let mut interned: std::collections::HashMap<String, u32> = std::collections::HashMap::new();
    let mut intern = |text: &str, strings: &mut Vec<u8>| -> u32 {
        if text.is_empty() {
            return 0;
        }
        if let Some(&at) = interned.get(text) {
            return at;
        }
        let at = strings.len() as u32;
        strings.extend_from_slice(text.as_bytes());
        strings.push(0);
        interned.insert(text.to_string(), at);
        at
    };

    // -- locations: el offset 0 es la location vacía reservada --
    let mut locations: Vec<u8> = vec![0];
    let mut location_offsets = vec![0u32; resources.len()];
    let mut data_offset: u64 = 0;
    // El nombre del descompresor vive en el mismo blob de strings que los nombres.
    let zip_offset = if compress { intern("zip", &mut strings) } else { 0 };
    let mut payloads: Vec<Vec<u8>> = Vec::with_capacity(resources.len());
    for (i, resource) in resources.iter().enumerate() {
        let (module, parent, base, extension) = split_name(&resource.name);
        let payload = if compress {
            compressed_resource(&zlib_stored(&resource.bytes), resource.bytes.len(), zip_offset)
        } else {
            resource.bytes.clone()
        };
        let attributes = [
            (ATTRIBUTE_MODULE, intern(&module, &mut strings) as u64),
            (ATTRIBUTE_PARENT, intern(&parent, &mut strings) as u64),
            (ATTRIBUTE_BASE, intern(&base, &mut strings) as u64),
            (ATTRIBUTE_EXTENSION, intern(&extension, &mut strings) as u64),
            (ATTRIBUTE_OFFSET, data_offset),
            (ATTRIBUTE_COMPRESSED, if compress { payload.len() as u64 } else { 0 }),
            (ATTRIBUTE_UNCOMPRESSED, resource.bytes.len() as u64),
        ];
        location_offsets[i] = locations.len() as u32;
        for (kind, value) in attributes {
            if value == 0 && kind != ATTRIBUTE_OFFSET {
                continue; // un atributo ausente vale cero; no hace falta escribirlo
            }
            let width = byte_width(value);
            locations.push((kind << 3) | (width as u8 - 1));
            for shift in (0..width).rev() {
                locations.push((value >> (shift * 8)) as u8); // big-endian
            }
        }
        locations.push(ATTRIBUTE_END);
        data_offset += payload.len() as u64;
        payloads.push(payload);
    }

    // -- ensamblado --
    let mut image = Vec::new();
    for word in [
        MAGIC,
        0x0001_0000,
        0,
        resources.len() as u32,
        table_length as u32,
        locations.len() as u32,
        strings.len() as u32,
    ] {
        image.extend_from_slice(&word.to_le_bytes());
    }
    for redirect in &redirects {
        image.extend_from_slice(&redirect.to_le_bytes());
    }
    for slot in &order {
        let offset = slot.map(|i| location_offsets[i]).unwrap_or(0);
        image.extend_from_slice(&offset.to_le_bytes());
    }
    image.extend_from_slice(&locations);
    image.extend_from_slice(&strings);
    for payload in &payloads {
        image.extend_from_slice(payload);
    }
    image
}

/// Splits `/module/parent/base.extension` into its four stored pieces.
fn split_name(full: &str) -> (String, String, String, String) {
    let trimmed = full.trim_start_matches('/');
    let (module, rest) = match trimmed.split_once('/') {
        Some((m, r)) => (m.to_string(), r),
        None => (String::new(), trimmed),
    };
    let (parent, file) = match rest.rsplit_once('/') {
        Some((p, f)) => (p.to_string(), f),
        None => (String::new(), rest),
    };
    match file.rsplit_once('.') {
        Some((base, ext)) => (module, parent, base.to_string(), ext.to_string()),
        None => (module, parent, file.to_string(), String::new()),
    }
}

/// How many bytes an attribute value needs (they are stored minimally).
fn byte_width(value: u64) -> usize {
    (1..=8).find(|&w| value < (1u64 << (w * 8))).unwrap_or(8)
}

/// Unwraps a compressed resource back to its original bytes.
///
/// Handles any zlib stream: the *stored* blocks our own `--compress zip-0` writes, and the
/// Huffman-coded ones a real `jlink --compress zip-6` produces — see [`crate::jvm::inflate`].
/// The declared uncompressed size is checked against what came out, so a stream that
/// inflates to the wrong length is a failure rather than a silently short resource.
pub fn decompress_resource(bytes: &[u8]) -> Option<Vec<u8>> {
    if bytes.len() < COMPRESSED_HEADER {
        return None;
    }
    let word = |i: usize| u32::from_le_bytes([bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]]);
    if word(0) != COMPRESSED_MAGIC {
        return None;
    }
    let uncompressed = u64::from_le_bytes(bytes[12..20].try_into().ok()?) as usize;
    let out = crate::jvm::inflate::zlib_inflate(&bytes[COMPRESSED_HEADER..])?;
    (out.len() == uncompressed).then_some(out)
}

// -- the image as a class source (J5) -------------------------------------

/// A runtime image opened for **class loading**: the bootstrap loader's `lib/modules`.
///
/// The image is keyed by *full* name (`/module/path`), but a loader asks for a binary name
/// (`java/lang/Object`) and does not know which module holds it — that is what the image's
/// `/packages/…` meta entries are for. Rather than walk that indirection on every lookup,
/// the map is built once when the image is opened: a few tens of thousands of entries, and
/// class loading then costs one hash lookup.
pub struct BootImage {
    bytes: Vec<u8>,
    data_start: usize,
    /// Binary name (`java/lang/Object`) → `(offset, stored length, compressed length)`.
    /// A non-zero compressed length means the stored bytes are a compressed resource.
    classes: std::collections::HashMap<String, (usize, usize, usize)>,
    /// Binary name → the module that defines it. This is what makes the JPMS access rule
    /// checkable at run time: without it a class is just a name with no owner.
    modules: std::collections::HashMap<String, String>,
    /// Module name → its `module-info.class`. It needs its own map: every module's
    /// descriptor has the *same* binary name (`module-info`), so they would collide.
    module_infos: std::collections::HashMap<String, (usize, usize, usize)>,
}

impl BootImage {
    /// Opens an image file and indexes its classes by binary name.
    pub fn open(path: &str) -> Option<BootImage> {
        Self::from_bytes(std::fs::read(path).ok()?)
    }

    /// Indexes an image already in memory.
    pub fn from_bytes(bytes: Vec<u8>) -> Option<BootImage> {
        let index = Index::parse(&bytes)?;
        let data_start = index.header.index_size() as usize;
        let mut classes = std::collections::HashMap::new();
        let mut modules = std::collections::HashMap::new();
        let mut module_infos = std::collections::HashMap::new();
        for location in index.entries() {
            // Only real resources: the meta namespaces describe the image, not code.
            if location.module.is_empty() || location.module == "modules" || location.module == "packages" {
                continue;
            }
            if location.extension != "class" {
                continue;
            }
            let path = location.path();
            let name = path.strip_suffix(".class").unwrap_or(&path).to_string();
            let stored = if location.compressed != 0 { location.compressed } else { location.uncompressed };
            if name == "module-info" {
                module_infos.insert(
                    location.module.clone(),
                    (location.offset as usize, stored as usize, location.compressed as usize),
                );
                continue;
            }
            modules.insert(name.clone(), location.module.clone());
            classes.insert(
                name,
                (location.offset as usize, stored as usize, location.compressed as usize),
            );
        }
        Some(BootImage { bytes, data_start, classes, modules, module_infos })
    }

    /// The bytes of a class by binary name, decompressing the resource when the image
    /// stores it compressed. `None` if the image doesn't hold it — or if it is compressed
    /// in a way we cannot undo (see [`decompress_resource`]), which is a refusal, not a
    /// silent wrong answer.
    pub fn class_bytes(&self, binary_name: &str) -> Option<Vec<u8>> {
        let &(offset, length, compressed) = self.classes.get(binary_name)?;
        let at = self.data_start + offset;
        let stored = self.bytes.get(at..at + length)?;
        if compressed == 0 {
            Some(stored.to_vec())
        } else {
            decompress_resource(stored)
        }
    }

    /// The module that defines a class, by binary name.
    pub fn module_of(&self, binary_name: &str) -> Option<&str> {
        self.modules.get(binary_name).map(String::as_str)
    }

    /// Every module in the image with its descriptor, read from its `module-info.class`.
    /// This is what a booted VM resolves the readability relation over.
    pub fn module_descriptors(
        &self,
    ) -> std::collections::BTreeMap<String, crate::jvm::parser::attributes::module::ModuleDescriptor>
    {
        let mut out = std::collections::BTreeMap::new();
        for (module, &(offset, length, compressed)) in &self.module_infos {
            let at = self.data_start + offset;
            let Some(stored) = self.bytes.get(at..at + length) else { continue };
            let bytes = if compressed == 0 {
                stored.to_vec()
            } else {
                match decompress_resource(stored) {
                    Some(b) => b,
                    None => continue,
                }
            };
            if let Ok(cf) = crate::jvm::class_file::ClassFile::from_bytes(&bytes) {
                if let Some(d) = crate::jvm::parser::attributes::module::descriptor(&cf) {
                    out.insert(module.clone(), d);
                }
            }
        }
        out
    }

    /// How many classes the image offers.
    pub fn len(&self) -> usize {
        self.classes.len()
    }

    pub fn is_empty(&self) -> bool {
        self.classes.is_empty()
    }
}

// -- compressed resources (J6 `--compress`) -------------------------------

/// Magic of a compressed resource — sibling of the image's own `0xCAFEDADA`.
pub const COMPRESSED_MAGIC: u32 = 0xCAFE_FAFA;

/// The header a compressed resource carries before its payload: 29 bytes, little-endian
/// except that the payload itself is a **zlib** stream. `content_size` counts only the
/// payload, so the `compressed` attribute in the location is `29 + content_size` — which is
/// why a tiny resource can come out *larger* compressed than raw.
const COMPRESSED_HEADER: usize = 29;

/// Wraps `payload` (already a zlib stream) in the compressed-resource header.
fn compressed_resource(payload: &[u8], uncompressed: usize, name_offset: u32) -> Vec<u8> {
    let mut out = Vec::with_capacity(COMPRESSED_HEADER + payload.len());
    out.extend_from_slice(&COMPRESSED_MAGIC.to_le_bytes());
    out.extend_from_slice(&(payload.len() as u64).to_le_bytes());
    out.extend_from_slice(&(uncompressed as u64).to_le_bytes());
    out.extend_from_slice(&name_offset.to_le_bytes()); // → "zip"
    out.extend_from_slice(&u32::MAX.to_le_bytes()); // flags, como los escribe el tool real
    out.push(1); // is_terminal: no hay otro descompresor encadenado
    out.extend_from_slice(payload);
    out
}

/// Wraps `bytes` in a **zlib** stream using DEFLATE *stored* blocks.
///
/// This is `zip-0` — what the reference tool calls "No compression. Equivalent to zip-0".
/// A stored block is legal DEFLATE (`BTYPE=00`), so the result is a stream any inflater
/// accepts, including the JDK's; it simply doesn't shrink. Actual shrinking needs an LZ77 +
/// Huffman encoder, which is a project of its own rather than a plugin — and writing a
/// *wrong* stream would be far worse than writing an honest incompressible one.
pub fn zlib_stored(bytes: &[u8]) -> Vec<u8> {
    // CMF=0x78 (deflate, ventana de 32K) y FLG=0x01 elegido para que (CMF<<8|FLG) % 31 == 0,
    // que es el check que exige el formato.
    let mut out = vec![0x78, 0x01];
    if bytes.is_empty() {
        out.extend_from_slice(&[0x01, 0x00, 0x00, 0xFF, 0xFF]); // bloque final vacío
    }
    for (i, chunk) in bytes.chunks(0xFFFF).enumerate() {
        let last = (i + 1) * 0xFFFF >= bytes.len();
        out.push(if last { 1 } else { 0 }); // BFINAL en el bit 0, BTYPE=00 en los siguientes
        let len = chunk.len() as u16;
        out.extend_from_slice(&len.to_le_bytes());
        out.extend_from_slice(&(!len).to_le_bytes()); // NLEN es el complemento de LEN
        out.extend_from_slice(chunk);
    }
    out.extend_from_slice(&adler32(bytes).to_be_bytes()); // el checksum va big-endian
    out
}

/// Adler-32 (RFC 1950): dos sumas módulo 65521, empacadas como `b << 16 | a`.
fn adler32(bytes: &[u8]) -> u32 {
    let (mut a, mut b) = (1u32, 0u32);
    for &byte in bytes {
        a = (a + byte as u32) % 65521;
        b = (b + a) % 65521;
    }
    (b << 16) | a
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A header built by hand, so the test needs no JDK on disk. The numbers are the ones
    /// the real `lib/modules` of JDK 25 carries, which is what makes the expected
    /// `index_size` below a real cross-check rather than a tautology.
    fn sample_bytes() -> Vec<u8> {
        let mut bytes = Vec::new();
        for word in [MAGIC, 0x0001_0000, 0, 30473, 30473, 629_215, 681_835] {
            bytes.extend_from_slice(&word.to_le_bytes());
        }
        bytes
    }

    #[test]
    fn the_header_is_seven_little_endian_words() {
        let h = Header::parse(&sample_bytes()).expect("cabecera válida");
        assert_eq!(h.magic, MAGIC);
        assert_eq!((h.major, h.minor), (1, 0));
        assert_eq!(h.flags, 0);
        assert_eq!(h.resource_count, 30473);
        assert_eq!(h.table_length, 30473);
        assert_eq!(h.locations_size, 629_215);
        assert_eq!(h.strings_size, 681_835);
    }

    #[test]
    fn the_derived_sizes_add_up_to_the_index() {
        let h = Header::parse(&sample_bytes()).unwrap();
        assert_eq!(h.offsets_size(), 121_892); // 30473 slots × 4 bytes
        assert_eq!(h.redirects_size(), 121_892);
        // The value the real `jimage info` reports for this image — the header plus its
        // four sections, and the offset the resource data starts at.
        assert_eq!(h.index_size(), 1_554_862);
    }


    /// Un índice completo armado a mano: una tabla de un solo slot, una location con los
    /// siete atributos y un blob de strings. Sin JDK en disco — el diferencial contra la
    /// imagen real de 145 MB se corre aparte con el binario.
    fn sample_index() -> Vec<u8> {
        // strings: offset 0 = "", 1 = "mod", 5 = "pkg", 9 = "Name", 14 = "class"
        let strings: &[u8] = b"\0mod\0pkg\0Name\0class\0";
        // locations: el offset 0 es la location vacía reservada; la real arranca en 1.
        // Cada atributo es (kind << 3) | (len - 1) y su valor va big-endian.
        let locations: &[u8] = &[
            0x00, // END de la location reservada
            0x08, 1, // MODULE    -> "mod"
            0x10, 5, // PARENT    -> "pkg"
            0x18, 9, // BASE      -> "Name"
            0x20, 14, // EXTENSION -> "class"
            0x29, 0x01, 0x02, // OFFSET (2 bytes) -> 0x0102 = 258
            0x38, 42, // UNCOMPRESSED -> 42
            0x00, // END
        ];
        let mut bytes = Vec::new();
        for word in [MAGIC, 0x0001_0000, 0, 1, 1, locations.len() as u32, strings.len() as u32] {
            bytes.extend_from_slice(&word.to_le_bytes());
        }
        bytes.extend_from_slice(&0i32.to_le_bytes()); // redirects[0]
        bytes.extend_from_slice(&1u32.to_le_bytes()); // offsets[0] -> location en 1
        bytes.extend_from_slice(locations);
        bytes.extend_from_slice(strings);
        bytes
    }

    #[test]
    fn a_location_decodes_its_attributes_and_names() {
        let index = Index::parse(&sample_index()).expect("índice válido");
        let entries = index.entries();
        assert_eq!(entries.len(), 1);
        let l = &entries[0];
        // Los nombres se guardan partidos en cuatro offsets al blob de strings, no como
        // una ruta: compartir esos pedazos es lo que abarata el índice.
        assert_eq!((l.module.as_str(), l.parent.as_str()), ("mod", "pkg"));
        assert_eq!((l.base.as_str(), l.extension.as_str()), ("Name", "class"));
        assert_eq!(l.path(), "pkg/Name.class");
        assert_eq!(l.full_name(), "/mod/pkg/Name.class");
        // Los valores de atributo son big-endian aunque la cabecera sea little-endian.
        assert_eq!(l.offset, 258);
        assert_eq!(l.uncompressed, 42);
        assert_eq!(l.compressed, 0, "sin atributo COMPRESSED significa sin comprimir");
    }

    #[test]
    fn the_reserved_location_zero_is_how_an_empty_slot_is_spelled() {
        let index = Index::parse(&sample_index()).unwrap();
        assert!(index.location(0).is_none(), "el offset 0 es la location vacía");
    }

    #[test]
    fn an_index_shorter_than_its_header_claims_is_rejected() {
        let bytes = sample_index();
        assert!(Index::parse(&bytes[..bytes.len() - 1]).is_none());
    }


    fn resource(name: &str, bytes: &[u8]) -> Resource {
        Resource { name: name.to_string(), bytes: bytes.to_vec() }
    }

    #[test]
    fn an_image_we_write_is_one_we_can_read_back() {
        let resources = vec![
            resource("/kaji.base/java/lang/Object.class", b"cafebabe-object"),
            resource("/kaji.base/java/lang/String.class", b"cafebabe-string"),
            resource("/kaji.base/java/util/List.class", b"cafebabe-list"),
            resource("/kaji.other/META-INF/services/Thing", b"a-service"),
        ];
        let bytes = write_image(&resources);
        let index = Index::parse(&bytes).expect("la imagen que escribimos se lee");
        assert_eq!(index.header.resource_count, 4);

        // Cada nombre se encuentra a sí mismo por el hash perfecto — que es lo que hace
        // falta construir bien, no sólo leer.
        for r in &resources {
            let found = index.find(&r.name).unwrap_or_else(|| panic!("no encontré {}", r.name));
            assert_eq!(found.full_name(), r.name);
            assert_eq!(found.uncompressed as usize, r.bytes.len());
        }
        assert!(index.find("/kaji.base/java/lang/Nope.class").is_none());
    }

    #[test]
    fn the_bytes_of_each_resource_survive_the_round_trip() {
        let resources = vec![
            resource("/m/a/B.class", b"primero"),
            resource("/m/a/C.class", b"segundo mas largo"),
        ];
        let bytes = write_image(&resources);
        let index = Index::parse(&bytes).unwrap();
        let data = index.header.index_size() as usize;
        for r in &resources {
            let l = index.find(&r.name).unwrap();
            let at = data + l.offset as usize;
            assert_eq!(&bytes[at..at + l.uncompressed as usize], &r.bytes[..]);
        }
    }

    #[test]
    fn a_bucket_with_several_names_gets_a_seed_and_the_lonely_ones_a_direct_index() {
        // Con una tabla chica y varios nombres, las colisiones son inevitables: el
        // constructor tiene que resolverlas con semillas.
        let names: Vec<String> = (0..40).map(|i| format!("/m/pkg/N{i}.class")).collect();
        let (slots, redirects, order) = perfect_hash(&names);
        assert!(slots >= 40, "la tabla arranca en un slot por nombre y crece si hace falta");
        assert_eq!(order.iter().flatten().count(), 40, "cada nombre ocupa un slot");
        assert_eq!(order.len(), slots);
        let mut slots: Vec<usize> = order.iter().flatten().copied().collect();
        slots.sort_unstable();
        slots.dedup();
        assert_eq!(slots.len(), 40, "sin dos nombres en el mismo slot");
        assert!(redirects.iter().any(|&r| r > 0), "algún bucket necesitó semilla");
        assert!(redirects.iter().any(|&r| r < 0), "algún bucket resolvió directo");
    }

    #[test]
    fn the_hash_ignores_the_sign_bit() {
        // El hash se enmascara a 31 bits: un valor negativo como índice de bucket sería
        // un pánico esperando a pasar.
        for name in ["/a/b/C.class", "/x", "", "/módulo/ñ.class"] {
            assert!(hash_name(name, HASH_MULTIPLIER) < 0x8000_0000);
        }
    }


    #[test]
    fn a_boot_image_serves_classes_by_binary_name() {
        let resources = vec![
            resource("/kaji.base/java/lang/Object.class", b"bytes-de-object"),
            resource("/kaji.base/java/util/ArrayList.class", b"bytes-de-arraylist"),
            // Un recurso que no es clase: el loader no tiene que ofrecerlo como tal.
            resource("/kaji.base/META-INF/services/Thing", b"un-servicio"),
        ];
        let image = BootImage::from_bytes(write_image(&resources)).expect("imagen válida");
        assert_eq!(image.len(), 2, "sólo las .class cuentan como clases");
        // El loader pide un nombre binario, sin módulo ni extensión.
        assert_eq!(image.class_bytes("java/lang/Object").as_deref(), Some(&b"bytes-de-object"[..]));
        assert_eq!(image.class_bytes("java/util/ArrayList").as_deref(), Some(&b"bytes-de-arraylist"[..]));
        assert!(image.class_bytes("java/lang/Nope").is_none());
        assert!(image.class_bytes("META-INF/services/Thing").is_none());
    }


    #[test]
    fn a_compressed_resource_round_trips() {
        let original = b"unas cuantas clases de bytes que van y vuelven".to_vec();
        let payload = zlib_stored(&original);
        // zlib: cabecera de 2 bytes, bloques stored, y adler32 al final.
        assert_eq!(&payload[..2], &[0x78, 0x01]);
        let wrapped = compressed_resource(&payload, original.len(), 7);
        assert_eq!(wrapped.len(), COMPRESSED_HEADER + payload.len());
        assert_eq!(decompress_resource(&wrapped).as_deref(), Some(&original[..]));
    }

    #[test]
    fn an_empty_resource_still_produces_a_valid_stream() {
        let payload = zlib_stored(&[]);
        let wrapped = compressed_resource(&payload, 0, 7);
        assert_eq!(decompress_resource(&wrapped), Some(Vec::new()));
    }

    #[test]
    fn a_resource_larger_than_one_stored_block_round_trips() {
        // Un bloque stored llega hasta 65535 bytes: con más, hay que encadenar bloques y
        // sólo el último lleva BFINAL.
        let original: Vec<u8> = (0..200_000u32).map(|i| (i % 251) as u8).collect();
        let wrapped = compressed_resource(&zlib_stored(&original), original.len(), 7);
        assert_eq!(decompress_resource(&wrapped), Some(original));
    }

    #[test]
    fn a_resource_whose_length_does_not_match_is_refused() {
        // La cabecera declara cuánto mide el original; si lo inflado no coincide, algo está
        // mal y devolver el recurso a medias sería peor que fallar.
        let original = b"contenido".to_vec();
        let mentiroso = compressed_resource(&zlib_stored(&original), original.len() + 1, 7);
        assert!(decompress_resource(&mentiroso).is_none());
    }

    #[test]
    fn a_compressed_image_is_readable_and_bootable() {
        let resources = vec![
            resource("/kaji.base/java/lang/Object.class", b"bytes-de-object"),
            resource("/kaji.base/java/util/ArrayList.class", b"bytes-de-arraylist"),
        ];
        let bytes = write_image_with(&resources, true);
        let index = Index::parse(&bytes).expect("la imagen comprimida se lee");
        for r in &resources {
            let l = index.find(&r.name).expect("se encuentra por el hash");
            assert!(l.compressed > 0, "la entrada quedó marcada como comprimida");
            assert_eq!(l.uncompressed as usize, r.bytes.len(), "el tamaño original se conserva");
        }
        // Y el loader la sirve descomprimida, que es lo que hace booteable la imagen.
        let image = BootImage::from_bytes(bytes).unwrap();
        assert_eq!(image.class_bytes("java/lang/Object").as_deref(), Some(&b"bytes-de-object"[..]));
    }


    #[test]
    fn an_image_remembers_which_module_defines_each_class() {
        let resources = vec![
            resource("/kaji.api/com/kaji/api/Service.class", b"api"),
            resource("/kaji.impl/com/kaji/impl/Worker.class", b"impl"),
        ];
        let image = BootImage::from_bytes(write_image(&resources)).unwrap();
        // Sin el dueño de cada clase, la regla de acceso de JPMS no es chequeable.
        assert_eq!(image.module_of("com/kaji/api/Service"), Some("kaji.api"));
        assert_eq!(image.module_of("com/kaji/impl/Worker"), Some("kaji.impl"));
        assert_eq!(image.module_of("no/such/Class"), None);
    }

    #[test]
    fn every_module_info_is_kept_apart_despite_sharing_a_name() {
        // Los `module-info` de todos los módulos tienen el MISMO nombre binario, así que
        // van en su propio mapa; si compartieran el de clases, se pisarían entre sí.
        let real = std::fs::read("java/kaji.sample/module-info.class").expect("fixture");
        let resources = vec![
            Resource { name: "/kaji.sample/module-info.class".to_string(), bytes: real.clone() },
            Resource { name: "/otro.modulo/module-info.class".to_string(), bytes: real },
        ];
        let image = BootImage::from_bytes(write_image(&resources)).unwrap();
        let descriptors = image.module_descriptors();
        assert_eq!(descriptors.len(), 2, "los dos descriptores sobreviven");
        assert!(descriptors.contains_key("kaji.sample") && descriptors.contains_key("otro.modulo"));
        // Y no se cuelan como clases normales.
        assert!(image.class_bytes("module-info").is_none());
    }

    #[test]
    fn a_file_that_is_not_an_image_is_rejected() {
        let mut bytes = sample_bytes();
        bytes[0] ^= 0xFF; // corromper el magic
        assert!(Header::parse(&bytes).is_none());
        assert!(Header::parse(&bytes[..20]).is_none(), "una cabecera corta no se lee a medias");
    }
}
