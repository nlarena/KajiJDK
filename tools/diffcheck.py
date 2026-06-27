import subprocess, sys, os, difflib, threading
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

JP = sys.argv[1]                       # JDK 25 javap.exe
LIST = sys.argv[2]                     # file with class paths
OUR = os.path.abspath("./target/debug/jvm")
paths = [l.strip() for l in open(LIST) if l.strip()]
B = 100                                # classes per javap batch
WORKERS = 6

KEYWORDS = [
    "RuntimeVisibleParameterAnnotations", "RuntimeInvisibleParameterAnnotations",
    "RuntimeVisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations",
    "AnnotationDefault", "PermittedSubclasses", "EnclosingMethod",
    "ModulePackages", "ModuleMainClass", "Module:", "SourceDebugExtension",
]

def split_chunks(out):
    chunks, cur, key = {}, [], None
    for line in out.split("\n"):
        if line.startswith("Classfile "):
            if key is not None:
                chunks[key] = "\n".join(cur)
            key, cur = line, [line]
        else:
            cur.append(line)
    if key is not None:
        chunks[key] = "\n".join(cur)
    return chunks

samples, slock = [], threading.Lock()

def run(cmd):
    r = subprocess.run(cmd, capture_output=True, encoding="utf-8", errors="replace")
    return (r.stdout or "").replace("\r", "")

def process_batch(batch):
    res = Counter()
    refs = split_chunks(run([JP, "-v"] + batch))
    for p in batch:
        ours = run([OUR, "-v", p])
        if not ours.strip():
            res["ERR"] += 1; continue
        ref = refs.get(ours.split("\n", 1)[0])
        if ref is None:
            res["REFERR"] += 1; continue
        if ours.rstrip("\n") == ref.rstrip("\n"):
            res["OK"] += 1; continue
        diff = [l for l in difflib.unified_diff(ref.split("\n"), ours.split("\n"), lineterm="")
                if l[:1] in "+-" and not l.startswith(("+++", "---"))]
        dt = "\n".join(diff)
        cat = next((k for k in KEYWORDS if k in dt), "OTHER")
        res["DIFF:" + cat] += 1
        if cat == "OTHER":
            with slock:
                if len(samples) < 12:
                    samples.append(os.path.basename(p) + "\n" + dt[:500])
    return res

batches = [paths[i:i + B] for i in range(0, len(paths), B)]
total = Counter()
done = 0
with ThreadPoolExecutor(max_workers=WORKERS) as ex:
    for fut in as_completed([ex.submit(process_batch, b) for b in batches]):
        total += fut.result()
        done += 1
        n = sum(total.values())
        print(f"[{done}/{len(batches)} batches] {n} classes  OK={total['OK']} "
              f"DIFF={sum(v for k,v in total.items() if k.startswith('DIFF'))} "
              f"ERR={total['ERR']+total['REFERR']}", flush=True)

print("\n===== RESULT =====")
n = sum(total.values())
print(f"total={n}  OK={total['OK']} ({100*total['OK']//max(n,1)}%)")
for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
    if k != "OK":
        print(f"  {k}: {v}")
print("\n----- OTHER samples -----")
for s in samples:
    print("--", s, sep="\n")
