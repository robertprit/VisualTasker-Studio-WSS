# VisualTasker Studio WSS

Android-App (Jetpack Compose), die als **Workspace-Shell** fuer ein visuelles Task-/Workflow-System dient.  
Der Fokus liegt auf frei positionierbaren Panels, Panel-Lifecycle und UI-Projektionen - nicht auf Runtime- oder Workflow-Truth im Workspace selbst.

## Projektstatus

Dieses Repository enthaelt einen aktiven Prototyp mit:
- Multi-Panel-Workspace (verschieben, groesse aendern, minimieren, schliessen)
- Docking/Minimized Dock und Raster-Unterstuetzung (Snap/Grid)
- Tool-/Funktions-Panels (u. a. Recorder, Blockeditor, Flowchart, Runtime-Log, Browser)
- Theme-Umschaltung (System/Hell/Dunkel)
- Session-Persistenz fuer Workspace-/Panel-Zustand

## Architekturprinzip

Die Architektur folgt dem Vertrag in `VISUALTASKER_ARCHITECTURE_CONTRACT.md`:

`Workspace UI -> PanelAction -> Adapter -> WorkflowDocumentAction -> WorkflowDocument -> Projektionen -> Panels`

Kurz gesagt:
- Workspace verwaltet Panel-UI und Interaktion
- Fachliche Workflow-Wahrheit bleibt ausserhalb der Workspace-Shell
- Keine Runtime-/EMScript-Logik direkt in der Workspace-Shell

## Tech-Stack

- Kotlin + Jetpack Compose
- Android Gradle Plugin (KTS)
- minSdk 29, targetSdk 35, compileSdk 35
- Java/Kotlin Ziel: 17

## Voraussetzungen

- Android Studio (aktuelle Version empfohlen)
- Android SDK 35
- JDK 17

## Build und Start

### Mit Android Studio

1. Projekt importieren/oeffnen
2. Gradle-Sync abwarten
3. App auf Emulator oder Geraet starten

### Mit Gradle CLI

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Optional Tests:

```bash
./gradlew test
```

## Wichtige Pfade

- App-Einstieg: `app/src/main/java/com/visualtasker/wss/MainActivity.kt`
- Workspace UI: `app/src/main/java/com/visualtasker/wss/workspace/ui/WorkspaceScreen.kt`
- Architekturvertrag: `VISUALTASKER_ARCHITECTURE_CONTRACT.md`

## Hinweis

Der lokale Projektname ist `VisualTaskerStudio-WSS`.
