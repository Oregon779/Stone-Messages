# REGELN FÜR DIE VERSIONIERUNG & BUILD-ERSTELLUNG

Für dieses Minecraft-Plugin trennen wir strikt zwischen Entwicklungs-Builds (Dev-Builds) und Release-Builds für Modrinth. Halte dich bei jeder Änderung an der Codebase zwingend an folgenden Workflow:

1. **Stabile Hauptversion:** Die aktuelle Hauptversionsnummer (z. B. `1.1`) darf von dir NIEMALS selbstständig geändert oder erhöht werden.
2. **Das Dev-Suffix:** Für alle laufenden Änderungen, Fehlerbehebungen und Code-Tests nutzt du ein Entwickler-Suffix. Das Format ist `[Hauptversion]-dev[Nummer]` (Beispiel: `1.1-dev1`, `1.1-dev2`).
3. **Automatisches Hochzählen:** Bei jedem neuen Build, den du generierst, zählst du *ausschließlich* die Nummer des Dev-Suffixes um +1 hoch. Aktualisiere diese Version in allen relevanten Dateien (z.B. `plugin.yml` und `build.gradle` bzw. `pom.xml`).
4. **Der Release-Befehl:** Erst wenn ich ausdrücklich den Befehl **"Baue das Release"** gebe, entfernst du das `-dev`-Suffix vollständig. Du trägst dann die reine Hauptversion (z.B. `1.1`) in die Dateien ein und erstellst die finale, saubere `.jar`-Datei.
5. **Nächster Zyklus:** Nachdem das Release gebaut wurde und neue Änderungen anstehen, fragst du mich, wie die nächste Hauptversion lauten soll, und wir starten wieder bei `-dev1`.

## Wo die Version steht

Die Version muss in beiden Dateien immer identisch sein:

- `pom.xml` → `<version>` des Projekts (nicht die Versionen der Dependencies). Daraus entsteht auch der JAR-Name `target/StoneMessage-<version>.jar`.
- `src/main/resources/plugin.yml` → `version:`

Die aktuelle Version liest du immer aus diesen Dateien, nicht aus dem Gedächtnis.

## Build

- JDK 25 und Maven: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn clean package`
- Der Build führt alle Tests aus. Ein Build mit fehlschlagenden Tests zählt nicht und erhöht keine Dev-Nummer.
