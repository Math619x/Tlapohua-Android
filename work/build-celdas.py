from pathlib import Path
import csv
import os
import shutil
from math import prod
import sqlite3
import subprocess
import zipfile

base = Path(__file__).resolve().parent
project = base / "celdas"
toolchain = Path(os.environ.get("TLAPOHUA_TOOLCHAIN", base / "toolchain"))
sdk = Path(os.environ.get("ANDROID_SDK_ROOT", os.environ.get("ANDROID_HOME", toolchain)))
android = Path(os.environ.get("ANDROID_JAR", sdk / "platforms/android-36/android.jar"))
native = Path(os.environ.get("ANDROID_BUILD_TOOLS", sdk / "build-tools/36.0.0"))
java_home = os.environ.get("JAVA_HOME")
java = Path(java_home) / "bin/java.exe" if java_home else Path(shutil.which("java") or "java.exe")
java17 = java
ecj = Path(os.environ.get("ECJ_JAR", toolchain / "ecj.jar"))
bundletool = Path(os.environ.get("BUNDLETOOL_JAR", toolchain / "bundletool-1.18.3.jar"))
keytool = java.with_name("keytool.exe")
jarsigner = java.with_name("jarsigner.exe")
for dependency in (android, native / "aapt2.exe", native / "lib/d8.jar", ecj, bundletool):
    if not dependency.is_file():
        raise SystemExit(f"Falta una herramienta: {dependency}. Consulta README.md.")
for executable in (java, keytool, jarsigner):
    if not executable.is_file():
        raise SystemExit("Configura JAVA_HOME con un JDK 17 completo. Consulta README.md.")
build = project / "build"
classes = build / "classes"
tests = build / "tests"
for directory in (classes, tests):
    directory.mkdir(parents=True, exist_ok=True)


def run(*args):
    result = subprocess.run([str(arg) for arg in args], check=False)
    if result.returncode:
        raise SystemExit(f"Falló la herramienta {Path(args[0]).name}. Revisa el mensaje anterior.")


def compile_java(destination, *sources, classpath=None):
    options = ("-classpath", classpath) if classpath else ()
    run(java, "-jar", ecj, "-source", "1.8", "-target", "1.8",
        "-encoding", "UTF-8", "-d", destination, *options, *sources)


sources = project / "src/com/rey/celdas"
catalog = project / "catalogo.tsv"
database_path = build / "tableros.sqlite"
compile_java(tests, sources / "Puzzle.java", sources / "Journey.java", sources / "ForestBoards.java",
             sources / "Lesson.java", sources / "BoardAttempt.java", sources / "DailyDates.java", sources / "ContestRun.java",
             sources / "ContestCooldown.java", sources / "PracticeInsights.java", sources / "TimedPractice.java",
             sources / "FriendCard.java", sources / "FriendLink.java", sources / "ProgressBackup.java", sources / "ProgressRestore.java",
             project / "tests/PuzzleCheck.java", project / "tests/ContestCheck.java", project / "tests/FriendCheck.java",
             project / "tests/ProgressCheck.java")
run(java, "-cp", tests, "com.rey.celdas.PuzzleCheck", catalog)
run(java, "-cp", tests, "com.rey.celdas.FriendCheck")
run(java, "-cp", tests, "com.rey.celdas.ProgressCheck")
contest_asset = build / "concurso-auditado.tsv"
finalists_asset = project / "finalistas.tsv"
run(java, "-cp", tests, "com.rey.celdas.ContestCheck", catalog, project / "concurso.tsv",
    project / "concurso-clasicos.txt", contest_asset, finalists_asset)
with catalog.open(encoding="utf-8", newline="") as source:
    rows = list(csv.DictReader(source, delimiter="\t"))
with (project / "concurso.tsv").open(encoding="utf-8", newline="") as source:
    contest_rows = list(csv.DictReader(source, delimiter="\t"))
with finalists_asset.open(encoding="utf-8", newline="") as source:
    finalist_rows = list(csv.DictReader(source, delimiter="\t"))

def audit_answer(row, contest):
    size = int(row["size"])
    layout = list(map(int, row["regions"].split(",")))
    solution = list(map(int, row["solution"]))
    targets = list(map(int, row["targets"].split(",")))
    operations = row["operations"]
    digits = set(range(1, size + 1))
    for i in range(size):
        if set(solution[i * size:(i + 1) * size]) != digits or set(solution[i::size]) != digits:
            raise ValueError(f"Fila o columna inválida en {row['id']}")
    for group in set(layout):
        cells = [i for i, value in enumerate(layout) if value == group]
        values = [solution[i] for i in cells]
        operation = operations[group]
        target = targets[group]
        if not contest and len(values) != len(set(values)):
            raise ValueError(f"Dígito repetido en {row['id']}: región {group}")
        if contest and operation in ("-", "/"):
            values.sort(reverse=True)
        valid = (
            (operation == "=" and values == [target])
            or (operation == "+" and sum(values) == target)
            or (operation == "*" and prod(values) == target)
            or (operation == "-" and len(values) == 2 and values[0] - values[1] == target)
            or (operation == "/" and len(values) == 2 and values[0] == values[1] * target)
        )
        if not valid:
            raise ValueError(f"Respuesta inválida en {row['id']}: región {group} ({target}{operation})")
        remaining = set(cells)
        frontier = [remaining.pop()]
        while frontier:
            cell = frontier.pop()
            for neighbor in (cell - size, cell + size,
                             cell - 1 if cell % size else -1,
                             cell + 1 if cell % size != size - 1 else -1):
                if neighbor in remaining:
                    remaining.remove(neighbor)
                    frontier.append(neighbor)
        if remaining:
            raise ValueError(f"Región desconectada en {row['id']}: {group}")

for row in rows:
    audit_answer(row, contest=False)
with contest_asset.open(encoding="utf-8", newline="") as source:
    audited_contest_rows = list(csv.DictReader(source, delimiter="\t"))
for row in audited_contest_rows + finalist_rows:
    audit_answer(row, contest=True)
if len({row["id"] for row in rows + contest_rows + finalist_rows}) != len(rows) + len(contest_rows) + len(finalist_rows):
    raise ValueError("IDs duplicados entre catálogos de origen")
print(f"Respuestas y regiones auditadas con un verificador independiente: {len(rows)} clásicas y "
      f"{len(audited_contest_rows) + len(finalist_rows)} de concurso.")

with sqlite3.connect(":memory:") as catalog_db, sqlite3.connect(database_path) as disk:
    catalog_db.executescript("""
        CREATE TABLE puzzles (
            id TEXT PRIMARY KEY NOT NULL,
            family TEXT NOT NULL CHECK (family IN ('patio', 'plaza', 'senderos', 'archivo')),
            title TEXT NOT NULL,
            size INTEGER NOT NULL CHECK (size BETWEEN 2 AND 9),
            regions TEXT NOT NULL,
            targets TEXT NOT NULL,
            operations TEXT NOT NULL,
            solution TEXT NOT NULL
        );
        CREATE INDEX puzzles_family_size ON puzzles(family, size, id);
        PRAGMA user_version=1;
    """)
    catalog_db.executemany("""
        INSERT INTO puzzles (id, family, title, size, regions, targets, operations, solution)
        VALUES (:id, :family, :title, :size, :regions, :targets, :operations, :solution)
    """, rows)
    catalog_db.commit()
    assert catalog_db.execute("PRAGMA integrity_check").fetchone() == ("ok",)
    catalog_db.backup(disk)
compile_java(classes, *sources.glob("*.java"), classpath=android)
run(native / "aapt2.exe", "compile", "--dir", project / "res", "-o", build / "compiled.zip")
run(native / "aapt2.exe", "link", "--manifest", project / "AndroidManifest.xml", "-I", android,
    "-o", build / "resources.apk", build / "compiled.zip")
app_jar = build / "app.jar"
with zipfile.ZipFile(app_jar, "w") as archive:
    for compiled in classes.rglob("*.class"):
        archive.write(compiled, compiled.relative_to(classes).as_posix())
run(java17, "-cp", native / "lib/d8.jar", "com.android.tools.r8.D8", "--release", "--min-api", "23",
    "--lib", android, "--output", build, app_jar)
with zipfile.ZipFile(build / "resources.apk") as src, zipfile.ZipFile(build / "unsigned.apk", "w") as dst:
    for info in src.infolist():
        dst.writestr(info, src.read(info.filename))
    dst.write(build / "classes.dex", "classes.dex", compress_type=zipfile.ZIP_DEFLATED)
    dst.write(database_path, "assets/tableros.sqlite", compress_type=zipfile.ZIP_DEFLATED)
    dst.write(contest_asset, "assets/concurso.tsv", compress_type=zipfile.ZIP_DEFLATED)
    dst.write(finalists_asset, "assets/finalistas.tsv", compress_type=zipfile.ZIP_DEFLATED)
run(native / "zipalign.exe", "-f", "4", build / "unsigned.apk", build / "aligned.apk")
key = Path(os.environ.get("TLAPOHUA_KEYSTORE", base / "local-keys/tlapohua-local.jks"))
alias = os.environ.get("TLAPOHUA_KEY_ALIAS", "tlapohua")
custom_key = bool(os.environ.get("TLAPOHUA_KEYSTORE"))
if custom_key and not os.environ.get("TLAPOHUA_STOREPASS"):
    raise SystemExit("Define TLAPOHUA_STOREPASS para tu clave privada.")
storepass = os.environ.get("TLAPOHUA_STOREPASS", "android")
keypass = os.environ.get("TLAPOHUA_KEYPASS", storepass)
os.environ["TLAPOHUA_STOREPASS"] = storepass
os.environ["TLAPOHUA_KEYPASS"] = keypass
if custom_key and not key.is_file():
    raise SystemExit("No existe el archivo indicado en TLAPOHUA_KEYSTORE.")
if not key.exists():
    key.parent.mkdir(parents=True, exist_ok=True)
    run(keytool, "-genkeypair", "-keystore", key, "-storepass:env", "TLAPOHUA_STOREPASS",
        "-keypass:env", "TLAPOHUA_KEYPASS", "-alias", alias, "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "10000", "-dname", "CN=Tlapohua Local Testing")
(base.parent / "outputs").mkdir(parents=True, exist_ok=True)
output = base.parent / "outputs/Tlapohua-0.31.apk"
signer = native / "lib/apksigner.jar"
run(java17, "-jar", signer, "sign", "--ks", key, "--ks-key-alias", alias, "--ks-pass", "env:TLAPOHUA_STOREPASS",
    "--key-pass", "env:TLAPOHUA_KEYPASS", "--out", output, build / "aligned.apk")
run(java17, "-jar", signer, "verify", "--verbose", output)
run(native / "zipalign.exe", "-c", "4", output)
run(native / "aapt2.exe", "dump", "badging", output)
with zipfile.ZipFile(output) as apk:
    assert apk.testzip() is None
    assert apk.read("assets/tableros.sqlite") == database_path.read_bytes()
(output.parent / "Tlapohua-tableros.sqlite").write_bytes(database_path.read_bytes())

proto = build / "resources-proto.apk"
run(native / "aapt2.exe", "convert", "--output-format", "proto", "-o", proto, build / "resources.apk")
module = build / "base-module.zip"
with zipfile.ZipFile(proto) as src, zipfile.ZipFile(module, "w", compression=zipfile.ZIP_DEFLATED) as dst:
    for info in src.infolist():
        if info.filename.startswith("META-INF/"):
            continue
        target = "manifest/AndroidManifest.xml" if info.filename == "AndroidManifest.xml" else info.filename
        dst.writestr(target, src.read(info.filename))
    dst.write(build / "classes.dex", "dex/classes.dex")
    for source, filename in ((database_path, "tableros.sqlite"), (contest_asset, "concurso.tsv"), (finalists_asset, "finalistas.tsv")):
        dst.write(source, "assets/" + filename)
bundle = output.with_suffix(".aab")
run(java17, "-jar", bundletool, "build-bundle", f"--modules={module}", f"--output={bundle}", "--overwrite")
run(jarsigner, "-keystore", key, "-storepass:env", "TLAPOHUA_STOREPASS", "-keypass:env", "TLAPOHUA_KEYPASS",
    "-sigalg", "SHA256withRSA", "-digestalg", "SHA-256", bundle, alias)
run(java17.with_name("jarsigner.exe"), "-verify", bundle)
run(java17, "-jar", bundletool, "validate", f"--bundle={bundle}")
