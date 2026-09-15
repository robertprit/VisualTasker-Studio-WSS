# EMScript Browser Contract — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_BROWSER_CONTRACT.md`

## Ziel

Diese Referenz trennt externes Browsing von korrelierten Custom Tabs und bewahrt die alte Session-/Event-Semantik als Ausgangspunkt für die heutige Browser-Capability.

## Kanonische Operationen der alten v0.1-Spezifikation

```emscript
Browser.open(url=Str, target=BrowserTarget.SYSTEM, packageName=Str?)
Browser.openCustomTab(url=Str, ...)
Browser.waitForNavigation(session=BrowserSession, event=BrowserNavigationEventType, timeout=Num)
```

Wichtig ist nicht die exakte alte Signatur, sondern die Trennung:

- `Browser.open` = externer/default Handler;
- `Browser.openCustomTab` = korrelierte Session;
- `Browser.waitForNavigation` = Warten auf ein Event derselben Session.

## Session-Korrelation

Ein Browser-Event darf nur eine Wait-Bedingung erfüllen, wenn Session, Run und Source eindeutig korrelieren. Globale oder veraltete Navigation Events dürfen nicht still als Treffer gelten.

Der alte Vertrag verwendete u. a.:

- `browserSessionId`
- `runId`
- `sourceSessionId`
- URL / previous URL
- Timestamp
- Fehlercode / Fehlermeldung

Diese Trennung bleibt für WSS besonders relevant, sobald mehrere Workflows, Tabs, Geräte oder Remote Provider parallel existieren.

## Browser Targets

Der alte Vertrag kannte u. a. `SYSTEM`, `CHROME`, `FIREFOX`, `LEMUR`, `BRAVE`, `EDGE`, `CUSTOM_TABS`, `CUSTOM_PACKAGE`.

Die Liste ist historische Referenz. Ein explizites Ziel darf nicht still auf ein anderes Ziel zurückfallen. Provider-Verfügbarkeit entscheidet zur Laufzeit, ob ein Target unterstützt wird.

## Legacy-Normalisierung

Historisch wurden unter anderem normalisiert:

- `OPEN` -> `Browser.open`
- `OPENCT` -> `Browser.openCustomTab`
- `WAIT LOAD` -> korrelierter FINISHED-Wait
- `BPARAM` -> Legacy-Konfiguration für den nächsten Custom-Tab-Start

Neue Generatoren sollten diese Formen nicht neu erzeugen; ein Compatibility Parser darf sie weiter erkennen, falls WSS diese Kompatibilität beibehalten will.

## WSS-Anpassung

Browser-Funktionen gehören in das allgemeine Capability-/Provider-Modell. BrowserSession und Navigation Events sind Runtime-Daten, nicht Workflow-Authority und nicht Panel-State.

DOM-/Browser-Observations können später in Worldview einfließen, müssen dabei aber dieselben Observation/Evidence-Regeln einhalten wie A11y, OCR oder Vision.
