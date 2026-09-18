# MainScreen Retirement

Stand: 2026-09-18

## Entscheidung

Die Workspace Shell ist der einzige produktive App-Einstieg. Der alte
`screens/MainScreen.kt` bleibt vorerst als nicht erreichbare Referenz im Baum,
ist aber kein Runtime-, Navigations- oder gespeicherter Startmodus mehr.

## Begruendung

- Der MainScreen besitzt monolithische lokale Ableitungen und widerspricht dem
  gemeinsamen `WorkspaceDocument` mit getrennten Editor-Projektionen.
- Bereits migrierte Panels duerfen nicht ueber einen zweiten Host-Lifecycle
  oder parallele Save-/Dirty-/Runtime-Zustaende verfuegbar sein.
- Alte `startup_screen=MAIN` Preferences werden absichtlich ignoriert.

## Fixe Studio-Darstellung

Eine spaetere fixe Studio-Darstellung wird als gespeichertes Workspace-Layout
mit denselben Plugin-Instanzen, Contracts und dem gemeinsamen Workflow-State
umgesetzt. Sie ist kein Grund, den alten MainScreen wieder zu aktivieren.

Der Referenzcode darf erst geloescht werden, wenn ein abschliessender
Capability-Audit bestaetigt, dass keine noch zu migrierende Funktion exklusiv
darin vorhanden ist.
