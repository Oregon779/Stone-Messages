# Stone Message – Wiki

Stone Message ersetzt die Vanilla-Join- und Leave-Nachrichten auf Paper-Servern durch frei konfigurierbare Nachrichten – wahlweise im Chat, in der Actionbar oder als Title – mit Platzhaltern, Farben, eigener Erstbeitritt-Begrüßung und optionaler LuckPerms-/PlaceholderAPI-Anbindung.

| | |
|---|---|
| **Version** | 1.0.0 |
| **Server** | Paper 26.2 (`api-version: '26.2'`) |
| **Java** | 25 |
| **Folia** | nicht unterstützt |
| **Optionale Plugins** | LuckPerms, PlaceholderAPI |

## Inhalt

1. [Installation](#installation)
2. [Dateien & Ordner](#dateien--ordner)
3. [Features im Überblick](#features-im-überblick)
4. [Befehle](#befehle)
5. [Permissions](#permissions)
6. [Konfiguration (config.yml)](#konfiguration-configyml)
7. [Platzhalter](#platzhalter)
8. [Farben & Formatierung](#farben--formatierung)
9. [Sprachen (messages.yml)](#sprachen-messagesyml)
10. [Update-Checker](#update-checker)
11. [Spielerdaten (playerdata.yml)](#spielerdaten-playerdatayml)
12. [Integrationen](#integrationen)
13. [Performance](#performance)
14. [FAQ & Fehlerbehebung](#faq--fehlerbehebung)
15. [Aus dem Quellcode bauen](#aus-dem-quellcode-bauen)
16. [Changelog](#changelog)

---

## Installation

1. Paper-Server **26.2** mit **Java 25** bereitstellen.
2. `StoneMessage-1.0.0.jar` in den Ordner `plugins/` legen.
3. *(Optional)* **LuckPerms** und/oder **PlaceholderAPI** installieren – beide werden automatisch erkannt und vor Stone Message geladen.
4. Server starten. Stone Message legt beim ersten Start alle Dateien an (siehe unten).
5. `config.yml` anpassen und mit `/stonemessage reload` übernehmen – kein Neustart nötig.

> **Hinweis bei bestehenden Servern:** Stone Message führt eine eigene Spielerliste. Direkt nach der Installation ist sie leer – deshalb sieht **jeder** Spieler bei seinem ersten Login nach der Installation einmalig die Erstbeitritt-Nachricht. Wer das vermeiden will, setzt vorher `join.first-join.enabled: false` und schaltet es später wieder ein.

## Dateien & Ordner

```
plugins/StoneMessage/
├── config.yml              Einstellungen + alle Join-/Leave-Texte
├── playerdata.yml          Erstbeitritt & "zuletzt gesehen" pro Spieler
└── languages/
    ├── en/messages.yml     Befehls- und Systemtexte (Englisch)
    └── de/messages.yml     Befehls- und Systemtexte (Deutsch)
```

Kurzzeitig kann beim Speichern eine `playerdata.yml.tmp` auftauchen – das ist normal (atomares Schreiben, siehe [Spielerdaten](#spielerdaten-playerdatayml)).

## Features im Überblick

| Feature | Kurz erklärt |
|---|---|
| **Join-Nachrichten** | Ersetzen „*Spieler ist dem Spiel beigetreten*". Die Vanilla-Meldung wird **immer** ausgeblendet – auch wenn die eigene Nachricht deaktiviert ist. |
| **Leave-Nachrichten** | Dasselbe für „*Spieler hat das Spiel verlassen*". |
| **Erstbeitritt-Nachricht** | Eigener Begrüßungstext, der genau **einmal** beim allerersten Beitritt eines Spielers erscheint. |
| **3 Anzeige-Kanäle** | Pro Join/Leave wählbar: `CHAT`, `ACTIONBAR` oder `TITLE` (Title + Subtitle, Timing einstellbar). |
| **Platzhalter** | 20 eigene `{platzhalter}` (Name, Welt, Koordinaten, Ping, TPS, Offline-Dauer, Rang …) plus alle `%platzhalter%` von PlaceholderAPI. |
| **Farben** | `&`-Codes, Hex-Farben (`&#FF00AA`) und MiniMessage (`<gradient:…>`) – beliebig kombinierbar. |
| **LuckPerms** | `{prefix}`, `{suffix}` und `{rank}` direkt aus LuckPerms, inkl. Farben. |
| **Mehrsprachig** | Befehls-/Systemtexte in Englisch und Deutsch, eigene Sprachen möglich. |
| **Auto-Config-Update** | Neue Optionen werden bei Plugin-Updates automatisch ergänzt, eigene Werte bleiben unangetastet. |
| **Update-Checker** | Prüft Modrinth auf neue Versionen, meldet sie in der Konsole und Admins beim Login. |
| **Admin-Befehl** | `/stonemessage` zum Neuladen, Update-Prüfen und für die Hilfe. |

## Befehle

Hauptbefehl: `/stonemessage` – Aliase: `/sm`, `/stonemsg`

| Befehl | Wirkung |
|---|---|
| `/stonemessage` oder `/stonemessage help` | Zeigt die Hilfe. |
| `/stonemessage reload` | Lädt `config.yml` und alle `messages.yml` neu, erkennt LuckPerms/PlaceholderAPI neu und startet den Update-Checker mit dem aktuellen Intervall neu. `playerdata.yml` bleibt davon unberührt. |
| `/stonemessage checkupdate` | Prüft sofort auf Modrinth nach einer neuen Version. Das Ergebnis erscheint in der Konsole (und bei neuer Version zusätzlich bei allen berechtigten Spielern). |

- **Alle** Unterbefehle – auch die Hilfe – benötigen `stonemessage.admin`. Ohne Permission kommt nur „Keine Berechtigung".
- Unbekannte Unterbefehle zeigen die Hilfe.
- Tab-Completion schlägt die Unterbefehle nur Spielern mit Permission vor.
- Alles funktioniert auch aus der **Konsole**.

## Permissions

| Permission | Standard | Erlaubt |
|---|---|---|
| `stonemessage.admin` | `op` | Den kompletten Befehl `/stonemessage` (reload, checkupdate, help) **und** Update-Hinweise im Chat beim Login. |

Update-Hinweise bekommt jeder, der **OP ist oder** `stonemessage.admin` hat. Über ein Permission-Plugin (z. B. LuckPerms) lässt sich die Permission auch an Nicht-OPs vergeben:

```
/lp group moderator permission set stonemessage.admin true
```

## Konfiguration (config.yml)

### Referenz

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `language` | `en` | Sprache der Befehls-/Systemtexte. Muss einem Ordner unter `languages/` entsprechen (`en`, `de` oder eigene). |
| `date-format` | `dd.MM.yyyy HH:mm` | Format für `{last_seen_date}` und `{first_join_date}` ([SimpleDateFormat](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/text/SimpleDateFormat.html)). Ungültig → Warnung in der Konsole und Standardformat. |
| `join.enabled` | `true` | `false` = gar keine Join-Nachricht (auch keine Erstbeitritt-Nachricht). |
| `join.notification` | `CHAT` | `CHAT`, `ACTIONBAR` oder `TITLE`. Ungültiger Wert → `CHAT`. |
| `join.messages.chat` | *(Text)* | Text für den Chat-Kanal. |
| `join.messages.actionbar` | *(Text)* | Text für die Actionbar. |
| `join.messages.title` / `.subtitle` | *(Text)* | Title und Subtitle für den Title-Kanal. |
| `join.first-join.enabled` | `true` | Eigene Nachricht beim allerersten Beitritt. `false` = auch beim ersten Mal die normale Join-Nachricht. |
| `join.first-join.messages.*` | *(Text)* | Wie `join.messages.*`, nur für den Erstbeitritt. Nutzt denselben Kanal wie `join.notification`. |
| `leave.enabled` | `true` | `false` = gar keine Leave-Nachricht. |
| `leave.notification` | `CHAT` | `CHAT`, `ACTIONBAR` oder `TITLE`. |
| `leave.messages.*` | *(Text)* | `chat`, `actionbar`, `title`, `subtitle` – wie bei Join. |
| `title-timing.fade-in-ticks` | `5` | Einblenden (20 Ticks = 1 Sekunde). Gilt für Join und Leave. |
| `title-timing.stay-ticks` | `40` | Anzeigedauer. |
| `title-timing.fade-out-ticks` | `10` | Ausblenden. |
| `update-checker.enabled` | `true` | Update-Prüfung ein/aus. |
| `update-checker.check-interval-minutes` | `60` | Prüfintervall in Minuten, **mindestens 5**. |

### Wichtig zu wissen

- Pro Nachricht bleiben **alle** Kanal-Texte gespeichert (`chat`, `actionbar`, `title`, `subtitle`). `notification` wählt nur aus, welcher angezeigt wird – so kann man jederzeit umschalten, ohne Texte neu zu schreiben.
- Nachrichten gehen an **alle** Online-Spieler, inklusive des Spielers, der gerade joint.
- Fehlende Schlüssel werden beim Start automatisch ergänzt (auch in verschachtelten Abschnitten). Eigene Werte werden **nie** überschrieben. Kommentare in einer bestehenden Datei werden dabei nicht aktualisiert.

### Beispiel: Join als Title, Leave in der Actionbar

```yaml
join:
  enabled: true
  notification: TITLE
  messages:
    chat: "&8[&a+&8] &a{player} &7joined the game."
    actionbar: "&a➜ {player} joined the game"
    title: "&a&l{player}"
    subtitle: "&7ist dem Server beigetreten"
  first-join:
    enabled: true
    messages:
      chat: "&8[&e★&8] &eWillkommen, &f{player}&e!"
      actionbar: "&e★ Willkommen, {player}!"
      title: "<gradient:#FFC24A:#FF6B6B><bold>Willkommen!</bold></gradient>"
      subtitle: "&7{player}, schön dass du da bist"

leave:
  enabled: true
  notification: ACTIONBAR
  messages:
    chat: "&8[&c-&8] &c{player} &7left the game."
    actionbar: "&c➜ {prefix}{player} &7hat den Server verlassen"
    title: "&c&l{player}"
    subtitle: "&7left the game"

title-timing:
  fade-in-ticks: 10    # 0,5 s
  stay-ticks: 60       # 3 s
  fade-out-ticks: 20   # 1 s
```

## Platzhalter

### Eigene Platzhalter

| Platzhalter | Wert | Join | Leave |
|---|---|:---:|:---:|
| `{player}` | Spielername | ✅ | ✅ |
| `{displayname}` | Anzeigename (berücksichtigt Nicknames/Prefixe anderer Plugins) | ✅ | ✅ |
| `{uuid}` | UUID des Spielers | ✅ | ✅ |
| `{world}` | Name der Welt | ✅ | ✅ |
| `{x}` `{y}` `{z}` | Block-Koordinaten | ✅ | ✅ |
| `{online}` | Spieler online | ✅ | ✅ |
| `{max}` | Maximale Spielerzahl | ✅ | ✅ |
| `{ping}` | Ping in ms | ✅ | ✅ |
| `{gamemode}` | `SURVIVAL`, `CREATIVE`, `ADVENTURE`, `SPECTATOR` | ✅ | ✅ |
| `{tps}` | Server-TPS (1-Minuten-Schnitt, 2 Nachkommastellen) | ✅ | ✅ |
| `{server_name}` | MOTD des Servers | ✅ | ✅ |
| `{prefix}` | LuckPerms-Prefix (leer ohne LuckPerms) | ✅ | ✅ |
| `{suffix}` | LuckPerms-Suffix (leer ohne LuckPerms) | ✅ | ✅ |
| `{rank}` | Primäre LuckPerms-Gruppe, z. B. `admin` (leer ohne LuckPerms) | ✅ | ✅ |
| `{first_join_date}` | Datum des allerersten Beitritts | ✅ | ❌ |
| `{last_seen_date}` | Datum, wann der Spieler zuletzt online war | ✅ | ❌ |
| `{days_offline}` | Volle Tage seit dem letzten Mal online | ✅ | ❌ |
| `{hours_offline}` | Stunden seit dem letzten Mal online | ✅ | ❌ |

- Platzhalter mit ❌ werden in Leave-Nachrichten **nicht** ersetzt und erscheinen dort wörtlich.
- Beim allerersten Join ist `{last_seen_date}` leer und `{days_offline}` / `{hours_offline}` sind `0`.
- In **Leave**-Nachrichten zählt `{online}` den gehenden Spieler noch mit, weil Paper das Quit-Event auslöst, bevor der Spieler aus der Online-Liste entfernt wird.

### PlaceholderAPI

Ist PlaceholderAPI installiert, funktioniert jeder `%platzhalter%` jeder installierten Expansion in **allen Join-/Leave-Texten der `config.yml`** – aufgelöst für den Spieler, der joint bzw. geht. Keine Einrichtung nötig.

```yaml
chat: "&8[&a+&8] %luckperms_prefix%&f{player} &7– Kontostand: &6%vault_eco_balance_formatted%"
```

In den Systemtexten der `messages.yml` werden `%platzhalter%` **nicht** aufgelöst.

### Sicherheit

Werte eigener Platzhalter (z. B. Nicknames über `{displayname}`, Prefixe über `{prefix}`) dürfen **Farben und Formatierung** enthalten – farbige Ränge und Nicknames werden also korrekt angezeigt. **Interaktive** Tags in diesen Werten (`<click>`, `<hover>`, `<insert>`, `<newline>` usw.) werden dagegen als normaler Text dargestellt. So kann niemand über einen manipulierten Nickname eine klickbare Nachricht einschleusen, die beim Anklicken einen Befehl im Namen eines anderen Spielers ausführt.

In den **eigenen Texten** der `config.yml` / `messages.yml` sind weiterhin alle MiniMessage-Tags erlaubt – inklusive Click und Hover.

## Farben & Formatierung

Alle drei Formate sind in jedem Text nutzbar und dürfen gemischt werden.

**`&`-Codes** (auch mit `§`):

| Farben | | Formatierung | |
|---|---|---|---|
| `&0` Schwarz | `&8` Dunkelgrau | `&l` **Fett** | |
| `&1` Dunkelblau | `&9` Blau | `&o` *Kursiv* | |
| `&2` Dunkelgrün | `&a` Grün | `&n` Unterstrichen | |
| `&3` Dunkeltürkis | `&b` Türkis | `&m` Durchgestrichen | |
| `&4` Dunkelrot | `&c` Rot | `&k` Verschleiert | |
| `&5` Lila | `&d` Pink | `&r` Zurücksetzen | |
| `&6` Gold | `&e` Gelb | | |
| `&7` Grau | `&f` Weiß | | |

**Hex-Farben:** `&#RRGGBB`, z. B. `&#FF00AA` (genau 6 Hex-Zeichen).

**MiniMessage:** z. B. `<red>`, `<bold>`, `<gradient:#FF0000:#0000FF>Text</gradient>`, `<rainbow>Text</rainbow>`, `<hover:show_text:'Hallo'>Text</hover>` – siehe [MiniMessage-Dokumentation](https://docs.advntr.dev/minimessage/format.html).

## Sprachen (messages.yml)

Die `messages.yml` enthält nur **Befehls- und Systemtexte**. Die Join-/Leave-Texte stehen bewusst in der `config.yml`, weil dort auch der Anzeige-Kanal festgelegt wird.

| Schlüssel | Verwendung | Platzhalter |
|---|---|---|
| `prefix` | Wird vor `general.*` und `update.check-triggered` gesetzt | – |
| `general.no-permission` | Keine Berechtigung | – |
| `general.reload-success` | Nach `/stonemessage reload` | – |
| `help.header`, `help.reload`, `help.checkupdate`, `help.help` | Hilfe-Ausgabe | – |
| `update.available` | Update-Hinweis für Admins | `{version}`, `{current}`, `{behind}` |
| `update.versions-behind` | Wird in `{behind}` eingesetzt | `{count}` |
| `update.versions-behind-unknown` | `{behind}`, wenn die Anzahl unbekannt ist | – |
| `update.check-triggered` | Nach `/stonemessage checkupdate` | – |

- Mitgeliefert: `en` (Standard bei Neuinstallation) und `de`.
- Fehlt ein Schlüssel in der aktiven Sprache, wird der englische Text verwendet.
- Neue Schlüssel aus Plugin-Updates werden in `en` und `de` automatisch ergänzt.

### Eigene Sprache hinzufügen

1. `plugins/StoneMessage/languages/en/messages.yml` nach `plugins/StoneMessage/languages/<code>/messages.yml` kopieren (z. B. `fr`).
2. Texte übersetzen.
3. In der `config.yml` `language: fr` setzen.
4. `/stonemessage reload`.

Die Datei muss vorher existieren – für nicht mitgelieferte Sprachen legt das Plugin keine Vorlage an.

## Update-Checker

- Fragt die Modrinth-API für das Projekt `stone-message` ab – rein **asynchron**, der Server wird nie blockiert.
- Erste Prüfung ca. 5 Sekunden nach dem Start, danach alle `check-interval-minutes` (mindestens 5).
- Das Ergebnis steht **immer** in der Konsole. Bei einer neuen Version bekommen zusätzlich alle online berechtigten Spieler (OP oder `stonemessage.admin`) einen Hinweis – und danach bei jedem Login, bis das Plugin aktualisiert ist. Der Hinweis nennt auch, wie viele Versionen man zurückliegt.
- `/stonemessage checkupdate` löst eine Prüfung sofort aus.
- Abschalten mit `update-checker.enabled: false`.

## Spielerdaten (playerdata.yml)

Pro Spieler (UUID) werden zwei Zeitstempel in Millisekunden gespeichert:

```yaml
players:
  069a79f4-44e9-4726-a5be-fca90e38aaf5:
    first-join: 1790000000000   # erster Beitritt
    last-seen: 1790003600000    # letzter Logout
```

- `first-join` wird beim ersten Beitritt geschrieben und entscheidet, ob die Erstbeitritt-Nachricht kommt.
- `last-seen` wird beim Verlassen geschrieben und speist `{last_seen_date}`, `{days_offline}` und `{hours_offline}`.
- Gespeichert wird **gebündelt**: alle 30 Sekunden asynchron (nur wenn sich etwas geändert hat) und einmal beim Herunterfahren.
- Geschrieben wird erst in `playerdata.yml.tmp` und dann atomar ersetzt – ein Absturz mitten im Speichern kann die bestehende Datei nicht beschädigen.
- **Einzelnen Spieler zurücksetzen** (bekommt wieder die Erstbeitritt-Nachricht): Server stoppen, den Eintrag mit seiner UUID löschen, Server starten.
- **Alles zurücksetzen:** Server stoppen, `playerdata.yml` löschen.

## Integrationen

### LuckPerms

- Liefert `{prefix}`, `{suffix}` und `{rank}` (primäre Gruppe) inkl. Farben.
- Wird beim Start und bei `/stonemessage reload` erkannt. Die Konsole zeigt, ob LuckPerms gefunden wurde.
- Ohne LuckPerms bleiben die drei Platzhalter leer – alles andere funktioniert normal.

### PlaceholderAPI

- Löst `%platzhalter%` in allen Join-/Leave-Texten auf (siehe [PlaceholderAPI](#placeholderapi)).
- Benötigte Expansions müssen in PlaceholderAPI installiert sein, z. B. `/papi ecloud download Vault`.

## Performance

Stone Message ist für große Server mit vielen gleichzeitigen Spielern ausgelegt:

- Keine Listener auf häufige Events (Bewegung, Schaden usw.) – nur Join und Quit.
- Jede Nachricht wird pro Ereignis **einmal** aufgebaut und dann an alle verteilt, nicht pro Empfänger neu.
- LuckPerms-Daten werden pro Ereignis mit **einer** Cache-Abfrage gelesen.
- Festplattenzugriffe (Spielerdaten) und Netzwerkzugriffe (Update-Check) laufen **asynchron**, nicht im Main-Thread.

## FAQ & Fehlerbehebung

**Es erscheint trotzdem noch eine andere Join-/Leave-Nachricht.**
Ein anderes Plugin (z. B. EssentialsX) setzt seine eigene Nachricht. Deaktiviere die Join-/Leave-Nachrichten in diesem Plugin.

**`{prefix}`, `{suffix}` oder `{rank}` sind leer.**
LuckPerms ist nicht installiert oder war beim Start noch nicht bereit – die Konsole zeigt eine entsprechende Meldung. Nach dem vollständigen Start hilft `/stonemessage reload`.

**Ein `%platzhalter%` wird nicht ersetzt.**
PlaceholderAPI oder die passende Expansion fehlt – oder der Platzhalter steht in der `messages.yml`, wo PlaceholderAPI nicht unterstützt wird.

**In der Leave-Nachricht steht wörtlich `{first_join_date}`.**
Die Datums-/Offline-Platzhalter gibt es nur in Join-Nachrichten, siehe [Platzhalter](#eigene-platzhalter).

**`{online}` ist in der Leave-Nachricht um 1 zu hoch.**
Der gehende Spieler wird noch mitgezählt (siehe oben). Am besten `{online}` in Leave-Nachrichten weglassen.

**Die Erstbeitritt-Nachricht erscheint nicht.**
Prüfe, ob `join.enabled` **und** `join.first-join.enabled` auf `true` stehen. Steht der Spieler schon in der `playerdata.yml`, gilt er nicht mehr als neu.

**Nach der Installation sahen alle Spieler die Erstbeitritt-Nachricht.**
Erwartetes Verhalten – siehe Hinweis unter [Installation](#installation).

**Der Title ist zu kurz / nicht sichtbar.**
`title-timing` erhöhen (20 Ticks = 1 Sekunde) und prüfen, dass `title` bzw. `subtitle` nicht leer sind.

**Konsole: „Update checker: check failed" oder „Modrinth responded with status …".**
Der Server erreicht Modrinth nicht (Firewall/Netzwerk) oder das Projekt ist dort nicht verfügbar. Das beeinträchtigt nichts anderes; bei Bedarf `update-checker.enabled: false`.

**Meine Änderungen an der config.yml wirken nicht.**
`/stonemessage reload` ausführen. Tippfehler bei `notification` fallen still auf `CHAT` zurück, ein ungültiges `date-format` erzeugt eine Warnung in der Konsole.

## Aus dem Quellcode bauen

**Voraussetzungen:** JDK 25, Maven 3.9+

```bash
git clone https://github.com/Oregon779/Stone-Messages.git
cd Stone-Messages
mvn clean package
```

Die fertige JAR liegt danach unter `target/StoneMessage-1.0.0.jar`. Die Tests laufen dabei automatisch mit; überspringen mit `-DskipTests`.

**Abhängigkeiten** (alle `provided` – nichts davon wird in die JAR gepackt):

| Artefakt | Version | Quelle |
|---|---|---|
| `io.papermc.paper:paper-api` | `26.2.build.124-stable` | repo.papermc.io |
| `net.luckperms:api` | `5.4` | Maven Central / repo.luckperms.net |
| `me.clip:placeholderapi` | `2.11.6` | repo.extendedclip.com |
| `org.junit.jupiter:junit-jupiter` *(nur Tests)* | `6.0.3` | Maven Central |

Sind die Repositories nicht erreichbar, lassen sich vorhandene JARs manuell in den lokalen Maven-Cache legen, z. B.:

```bash
mvn install:install-file -Dfile=paper-api-26.2.build.124-stable.jar \
    -DpomFile=paper-api-26.2.build.124-stable.pom
```

### Projektstruktur

```
src/main/java/dev/stonemessage/plugin/
├── StoneMessage.java                 Hauptklasse: Start, Stopp, Reload
├── command/StoneMessageCommand.java  /stonemessage + Tab-Completion
├── config/ConfigUpdater.java         Ergänzt fehlende Config-Schlüssel
├── listener/JoinListener.java        Join-Nachricht
├── listener/QuitListener.java        Leave-Nachricht + "zuletzt gesehen"
├── manager/ConfigManager.java        Liest die config.yml
├── manager/MessageManager.java       messages.yml, Farben, Platzhalter-Ersetzung
├── manager/NotificationManager.java  Versand über Chat / Actionbar / Title
├── manager/PlaceholderManager.java   Berechnet die {platzhalter}-Werte
├── manager/PlayerDataManager.java    playerdata.yml
├── manager/IntegrationManager.java   LuckPerms & PlaceholderAPI
├── manager/UpdateChecker.java        Modrinth-Update-Prüfung
└── model/MessageDisplayType.java     CHAT / ACTIONBAR / TITLE

src/test/java/dev/stonemessage/plugin/
├── config/ConfigUpdaterTest.java
├── manager/MessageManagerTest.java
└── manager/PlayerDataManagerTest.java
```

Die Tests laufen ohne Minecraft-Server. MockBukkit wird derzeit nicht verwendet, weil es noch keine Registry-Daten für Paper 26.2 mitbringt.

## Changelog

### Unveröffentlicht

- **Portierung** auf Paper API 26.2 und Java 25 (`api-version: '26.2'`). Keine Code-Änderungen dafür nötig.
- **Sicherheit:** Platzhalter-Werte (z. B. Nicknames) konnten klickbare Befehle, Hover-Texte o. Ä. in Join-/Leave-Nachrichten einschleusen. Jetzt sind in Werten nur noch Farben und Formatierung erlaubt.
- **Fix:** Nach einem Serverabsturz bekamen Spieler, die seit ihrem ersten Join nicht regulär ausgeloggt waren, die Erstbeitritt-Nachricht erneut.
- **Fix:** Ein Absturz während des Speicherns konnte die komplette `playerdata.yml` leeren. Gespeichert wird jetzt atomar.
- Update-Checker nutzt eine wiederverwendete HTTP-Verbindung.
- Korrigierte Kommentare in der mitgelieferten `config.yml` (Ordnerpfad, `{first_join_date}` nur bei Join).
- Automatisierte Tests (JUnit) für Config-Migration, Platzhalter-Ersetzung und Spielerdaten.

### 1.0.0

- Erstveröffentlichung.
