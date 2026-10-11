# M5-H Headless Runtime Host

Stand: 2026-10-11

## Status

`DONE` fuer den Headless-Host-Vertrag. Die rein visuelle RailTrace-Abnahme einer laufenden
Headless-Session bleibt ein nachgelagertes UI-Gate und aendert den Runtime-Vertrag nicht.

## Ziel und Ownership

M5-H verschiebt die Lebensdauer einer Ausfuehrung aus Compose in einen prozessweiten Android-Host:

`WorkspaceShell -> RuntimeClient -> ExecutionSessionManager -> AndroidRuntimeHost -> ExecutionRunner`

- `RuntimeClient` ist ein duennes UI-Handle und besitzt weder Job noch Runner.
- `ExecutionSessionManager` besitzt Sessionzustand, Cancellation, Plan, Trace und Ergebnis.
- `AndroidRuntimeHost` stellt genau diesen Manager pro App-Prozess bereit.
- `HeadlessRuntimeService` haelt eine gestartete Session als Android-36-konformen
  Foreground-Service am Leben.
- `DeterministicExecutionRunner` und `ExecutionProviderRegistry` bleiben die einzige Runtime- und
  Provider-Ausfuehrung. M5-H fuehrt keine zweite Engine ein.

## ExecutionSession-Vertrag

Eine Session besitzt eine stabile `ExecutionSessionId`, Workflow-ID, Workspace-Revision, den beim
Start eingefrorenen `WorkspaceDocument`-Stand, den daraus erzeugten `ExecutionPlan`, die
Provider-Policy, Trace, Zeitpunkte, typisiertes Ergebnis und optional einen Abbruchgrund.

Zulaessige Zustandsfolge:

`CREATED -> STARTING -> RUNNING -> STOPPING -> CANCELLED`

Erfolgs- und Fehlerpfade enden alternativ in `COMPLETED` oder `FAILED`. Nach Prozessverlust wird ein
persistierter aktiver Zustand einmalig als `INTERRUPTED` wiederhergestellt. Terminale Zustaende
werden nie erneut gestartet.

Invarianten:

1. Pro Prozess existiert hoechstens eine aktive oder reservierte Session.
2. Attach/Detach eines Clients startet keinen Runner neu.
3. Der laufende Plan wird nicht durch spaetere Editor-Aenderungen neu kompiliert.
4. Session-, Workflow- und Entity-IDs bleiben im Trace stabil.
5. Runtime-Ausfuehrung mutiert weder WorkspaceDocument noch Projektionen.
6. Stop-Grund und Provider-Provenienz bleiben diagnostisch sichtbar.

## Android-Lifecycle

Der Service verwendet `foregroundServiceType="specialUse"`, eine dauerhaft sichtbare
Low-Importance-Notification und `START_NOT_STICKY`. Der Start ist ausschliesslich explizit und muss
aus einem von Android erlaubten Kontext erfolgen. Ein verbotener Background-Start wird als
`START_FAILURE` beendet; es gibt keinen versteckten Fallback.

Die Notification bietet eine Stop-Aktion. UI-Stop, Notification-Stop, Serviceverlust,
Provider-Cancellation und Prozessverlust besitzen getrennte `ExecutionCancellationReason`-Werte.
Ein Service- oder Prozessneustart spielt einen Workflow niemals automatisch erneut ab.

`POST_NOTIFICATIONS` wird deklariert, aber nicht automatisch erteilt. Auf Android-Versionen mit
Runtime-Permission bleibt die Systemdarstellung deshalb von der Nutzerentscheidung abhaengig; der
Foreground-Service-Vertrag selbst bleibt davon unberuehrt.

## Recovery und Persistenz

`AndroidExecutionSessionStore` speichert nur einen begrenzten Checkpoint: Session-/Workflow-ID,
Revision, Zustand, Zeitpunkte, Trace-Anzahl, letzte Entity/Provider-ID und Abbruchgrund. Argumente,
Variablen, Screenshots und sonstige sensitive Nutzdaten werden nicht persistiert.

Ein beim Prozessstart gefundener aktiver Checkpoint wird in `INTERRUPTED` ueberfuehrt und mit der
typisierten Diagnose `RUNTIME_PROCESS_LOSS` abgeschlossen. Eine Fortsetzung erfordert spaeter eine
explizite neue Session; M5-H rekonstruiert keinen Runner aus einem Checkpoint.

## Trace und RailTrace

Trace-Ereignisse entstehen weiterhin im bestehenden Runner. Der Session-Manager sammelt sie nach
Index dedupliziert und die vorhandene `ExecutionTrace`-/RailTrace-Projektion verwendet die
Session-ID als Run-ID. Start-/Endzeit, Entity-ID, Capability, Provider-ID, Selection Reason,
Diagnose und Abbruchgrund bleiben erhalten. Compose darf sich jederzeit an denselben StateFlow
anhaengen.

## Verifikation

Die M5-H-Tests decken ab:

1. stabile Session-ID, Plan und Revision,
2. erfolgreicher Headless-`wait`,
3. UI-Detach/Reattach ohne Neustart,
4. keine doppelten Trace-Ereignisse,
5. Doppelstart-Abweisung,
6. Neustart nach terminalem Zustand,
7. UI-Stop waehrend `wait`,
8. Notification-Stop,
9. Stop einer reservierten Session,
10. Abweisung veralteter Session-IDs,
11. Provider-Policy und Provenienz,
12. eingefrorener Plan trotz Editor-Aenderung,
13. Prozessverlust als `INTERRUPTED`,
14. kein Auto-Replay nach Prozessverlust,
15. terminaler Checkpoint wird nicht als aktiv geladen,
16. ungueltiger Workflow endet typisiert,
17. terminale Zustandsmaschine ist irreversibel,
18. privater Service und `specialUse`-Deklaration,
19. Prozesshost ist ueber Clients identisch,
20. echter Foreground-Service-Start und -Stop auf SM-S918B.

Nachweis am 2026-10-11: vier `HeadlessRuntimeHostInstrumentedTest`-Tests auf SM-S918B/API 36
bestanden. Die JVM-Session- und bestehenden Bridge-Tests sind gruen. Die visuelle Darstellung der
laufenden Headless-Session in RailTrace wird separat per freigegebenem Screenshot-/Klicktest
abgenommen.

## Grenzen

- Kein automatischer Workflow-Neustart nach Prozessverlust.
- Kein Hintergrundstart ausserhalb der von Android erlaubten Startbedingungen.
- Kein neuer externer Provider und keine neue EMScript-Semantik.
- Kein Display-, Recorder-, Vision-, Tasker-, Shizuku- oder VT2VT-Ausbau.
- Langlauf-, Retention- und komplette Release-Matrix bleiben M5-10.
