# Codex Masterprompt — AGGR ChartGraph Compose Plugin
## Native Chart-, Telemetrie- und Market-Time-Series-Visualisierung für VisionTasker Studio WSS

## 0. Auftrag

Entwickle **AGGR ChartGraph** als eigenständiges, natives Jetpack-Compose-Plugin für VisionTasker Studio WSS.

AGGR ChartGraph soll eine wiederverwendbare Visualisierungsengine für quantitative, kategorische und zeitbezogene Daten werden. Es ist ausdrücklich **keine fachliche Datenquelle** und kein neuer Domain-Owner.

Phase 1 soll mindestens enthalten:

- Line Chart
- Bar Chart
- Pie Chart
- Donut Chart
- gemeinsame Scale-/Geometry-/Interaction-Infrastruktur
- Zoom/Pan/Selection
- vorbereitete externe TimeViewport-Integration für Railchart
- Live-/Streaming-Datenfähigkeit
- eine Demo mit synthetischen VT-Telemetriedaten
- eine **Bitcoin-Market-Demo mit auswählbaren Intervallen von 5 Sekunden bis 1 Monat**
- frei einstellbarem Zeitraum
- Candlestick als vorbereitete/optionale Market-Erweiterung, falls dies ohne Architekturbruch sinnvoll in Phase 1 passt
- klare Vorbereitung auf ein späteres **AGGR-eigenes Scripting**

Wichtig:

> ChartGraph rendert Daten. ChartGraph besitzt nicht deren fachliche Wahrheit.

Datenquellen wie Worldview, Dataset, Runtime, Recorder, Watchdog oder ein MarketDataProvider bleiben außerhalb des Chart-Kerns.

---

# 1. Architekturziel

```text
Domain / External Data
        ↓
Projection / Data Adapter
        ↓
      ChartSpec
        ↓
  AGGR ChartGraph
        ↓
Scale / Layout / Geometry
        ↓
Interaction / Viewport
        ↓
Native Compose Rendering
```

Mögliche Consumer:

```text
Inspector
Dataset Manager
Railchart / Timeline
Runtime Trace
Recorder
Watchdog
Worldview projections
Perception diagnostics
EMScript
AGGR Script
Plugin APIs
Demo / Market Data
```

ChartGraph darf keine dieser Domains in seinen Kern importieren.

---

# 2. Technische Grundregeln

Verwende:

- Kotlin
- Jetpack Compose
- Material 3
- native Compose Canvas / DrawScope
- Coroutines / Flow für Live-Daten, wo sinnvoll
- immutable UI-/Spec-Modelle soweit praktikabel

Nicht verwenden:

- WebView
- HTML
- JavaScript-Chartbibliotheken
- DOM-basierte Charts
- unnötige externe Chart-Libraries

Große Datenserien dürfen nicht einen Composable pro Datenpunkt erzeugen.

---

# 3. Vor jeder Änderung: Bestandsanalyse

1. Repository, Branch und Dirty Working Tree prüfen.
2. Bestehende Änderungen dokumentieren und nicht zurücksetzen.
3. Kein reset/clean/rebase/history rewrite.
4. Kein Commit/Push ohne ausdrücklichen Auftrag.
5. Bestehende Plugin-Contracts suchen.
6. WSS-/Visual-Abstraction-Contracts suchen.
7. Railchart-/Timeline-TimeViewport suchen.
8. bestehende Canvas-, Gesture-, Zoom-, Pan- und Selection-Komponenten suchen.
9. bestehende Theme-/Material-/VisualSemantic-Tokens suchen.
10. bestehende Script-/EMScript-/Plugin-Runtime-Abstraktionen prüfen.
11. vorhandene Netzwerk-/HTTP-/Provider-Abstraktionen prüfen, bevor für die Bitcoin-Demo etwas Neues gebaut wird.

Vorhandene Contracts haben Vorrang vor neuen Paralleltypen.

---

# 4. Architektur-Invarianten

```text
Chart ≠ Data Source
ChartGraph ≠ Telemetry Collector
ChartGraph ≠ Market Data Provider
ChartGraph ≠ Dataset
ChartGraph ≠ Worldview
ChartGraph ≠ Railchart
ChartGraph ≠ Script Runtime

Domain Data
    ↓
Adapter
    ↓
ChartSpec
    ↓
ChartGraph
```

Für Zeitdaten:

```text
TimeSeries Source
      ↓
TimeSeries Adapter
      ↓
ChartSpec
      ↓
ChartGraph
```

Für Railchart:

```text
Railchart
    ↓
Shared TimeViewport
    ↓
ChartGraph Renderer
```

---

# 5. Chart-Familien

Phase 1 verpflichtend:

```text
LINE
BAR
PIE
DONUT
```

Architektur vorbereiten für:

```text
AREA
STACKED_BAR
SCATTER
HISTOGRAM
CANDLESTICK
OHLC
HEATMAP
GAUGE
RADAR
RANGE
EVENT
```

Nicht alle Erweiterungen sofort implementieren.

Für die Bitcoin-Demo ist eine Candlestick-/OHLC-Darstellung sehr erwünscht. Wenn sie sauber auf der gemeinsamen Scale-/Geometry-Architektur aufsetzt und den Slice nicht unverhältnismäßig vergrößert, implementiere sie als zusätzliche Phase-1-Demo. Andernfalls implementiere zunächst eine hochwertige Line-/Area-Time-Series-Darstellung und dokumentiere Candlestick als exakt nächsten Chart-Slice.

---

# 6. ChartSpec

Entwirf einen neutralen, typsicheren Contract, beispielsweise:

```kotlin
sealed interface ChartSpec {
    val id: ChartId
    val title: String?
}

data class LineChartSpec(...) : ChartSpec
data class BarChartSpec(...) : ChartSpec
data class PieChartSpec(...) : ChartSpec
```

Für Market-Daten bei implementiertem Candlestick:

```kotlin
data class CandlestickChartSpec(...) : ChartSpec
```

Keine Domain-Typen wie `WorldviewEntity`, `RecordingEvidence`, `BitcoinQuote` oder `WorkflowNode` im Chart-Kern.

---

# 7. Basis-Datenmodelle

Zeitserie konzeptionell:

```kotlin
data class ChartPoint(
    val x: Double,
    val y: Double
)
```

Line:

```kotlin
data class LineSeries(
    val id: String,
    val label: String,
    val points: List<ChartPoint>
)
```

Bar:

```kotlin
data class BarEntry(
    val id: String,
    val label: String,
    val value: Double
)
```

Pie:

```kotlin
data class PieSlice(
    val id: String,
    val label: String,
    val value: Double
)
```

Market/OHLC außerhalb des generischen Kernmodells oder als neutraler Chart-Datentyp:

```kotlin
data class OhlcPoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double?
)
```

Keine `Any`-Universalstruktur bauen.

---

# 8. Scale-System

Mindestens:

```text
LinearScale
CategoryScale
TimeScale
```

Konzeptionell:

```kotlin
interface ChartScale<D> {
    fun toPixel(value: D): Float
}
```

Berücksichtigen:

- domain
- range
- min/max
- padding
- zero baseline
- clamping
- reversed domains falls später erforderlich
- TimeViewport

Tests für Min, Max und Midpoint.

---

# 9. Rendering Pipeline

Bevorzugte Trennung:

```text
ChartSpec
    ↓
ChartLayoutEngine
    ↓
ChartGeometry
    ↓
ChartRenderer
    ↓
Canvas
```

Interaction/Hit-Test muss dieselbe Geometry verwenden wie Rendering.

Keine getrennten mathematischen Welten für Renderer und Hit-Test.

---

# 10. Line Chart

Implementiere:

- X/Y Scale
- Axis
- Grid optional
- Path
- Points optional
- Labels
- Selection
- Crosshair/Playhead
- Viewport
- Zoom/Pan
- Value tooltip/inspection

Geeignet für:

- Scanrate
- Confidence über Zeit
- Latency
- Entity count
- Bitcoin price
- Runtime metrics

---

# 11. Bar Chart

Implementiere:

- vertikale Bars
- Selection
- Labels
- Value Display
- grouped architecture vorbereiten
- stacked architecture vorbereiten

VT-Demo:

```text
Accessibility  0.96
OCR            0.84
OpenCV         0.78
YOLO           0.91
Fusion         0.97
```

Diese Werte sind ausdrücklich synthetische Demo-Daten.

---

# 12. Pie / Donut

Implementiere:

- Pie
- Donut
- Slice Selection
- Labels
- Legend
- optionales Center Label

VT-Demo:

```text
Known       24
Unknown      5
Ambiguous    2
Ignored      3
```

Pie/Donut ist Snapshot-/Composition-Darstellung, keine Zeitreihe.

---

# 13. Candlestick / OHLC — Market-Erweiterung

Wenn im Scope sauber möglich, implementiere:

```text
CANDLESTICK
```

mit:

- open
- high
- low
- close
- optional volume
- TimeScale
- Selection
- Crosshair
- Zoom/Pan
- sichtbare Zeitspanne
- Value Inspection

Farben dürfen nicht die einzige Information über Up/Down sein. Körper-/Stroke-/Pattern-/Semantik berücksichtigen.

Wenn Candlestick nicht in Phase 1 implementiert wird, muss dieselbe Bitcoin-Demo vollständig mit Line Chart funktionieren.

---

# 14. Einheitlicher TimeViewport

Zeitbezogene Charts benötigen einen expliziten Viewport.

Konzeptionell:

```kotlin
data class TimeViewport(
    val visibleStart: Long,
    val visibleEnd: Long,
    val playhead: Long?
)
```

Wenn Railchart bereits einen entsprechenden Contract besitzt, diesen wiederverwenden.

Nicht zwei inkompatible Zeitachsen bauen.

ChartGraph muss sowohl einen eigenen lokalen Viewport in der Demo als auch später einen extern kontrollierten Railchart-Viewport unterstützen.

---

# 15. Interaktion

First-class:

- tap
- selection
- pointer hover, falls verfügbar
- drag
- scrub
- pinch zoom
- horizontal pan
- long press optional
- crosshair

Ein Chart-Element besitzt stabile IDs.

Konzeptionell:

```kotlin
sealed interface ChartSelection {
    data class Point(...) : ChartSelection
    data class Bar(...) : ChartSelection
    data class Slice(...) : ChartSelection
    data class Candle(...) : ChartSelection
}
```

Der Consumer entscheidet über fachliche Bedeutung.

---

# 16. Live-Daten

ChartGraph muss Live-/Streaming-Daten effizient darstellen können.

Berücksichtige:

- bounded history
- ring buffer oder vergleichbare Struktur
- immutable snapshots
- viewport filtering
- kein ungebremstes Wachstum
- kein kompletter UI-Neuaufbau pro Sample
- späteres Downsampling vorbereiten

ChartGraph selbst ruft keine Bitcoin-API auf.

Ein externer Demo-Provider liefert Daten.

---

# 17. Bitcoin Market Demo

Erstelle im Demo-Bereich einen vollständigen **Bitcoin Time-Series Demo Screen**.

Ziel ist nicht Trading-Funktionalität, sondern ein realistischer Belastungs- und Interaktionstest für ChartGraph.

Der Screen soll ungefähr besitzen:

```text
┌─────────────────────────────────────────────┐
│ BTC Market Demo                             │
│ BTC / Quote Currency                 LIVE   │
├─────────────────────────────────────────────┤
│ [5s] [15s] [30s] [1m] [5m] [15m] [1h]     │
│ [4h] [1d] [1w] [1M]                        │
├─────────────────────────────────────────────┤
│ Zeitraum                                    │
│ [Von: ............] [Bis: ............]     │
│ [1H] [1D] [1W] [1M] [3M] [1Y] [MAX]       │
├─────────────────────────────────────────────┤
│                                             │
│                 BTC PRICE                   │
│                                             │
│       ╭──────╮       ╭────────              │
│ ──────╯      ╰───────╯                      │
│                                             │
│                     │ Crosshair             │
│                     ●  Price                │
│                        Timestamp            │
│                                             │
├─────────────────────────────────────────────┤
│ Range: ...    Interval: ...    Points: ...  │
└─────────────────────────────────────────────┘
```

Bei Candlestick:

```text
┌─────────────────────────────────────────────┐
│ BTC / Quote                                 │
│                                             │
│     │        │                              │
│    ███       █                              │
│     │       ███      │                      │
│              │      ███                     │
│                      │                      │
└─────────────────────────────────────────────┘
```

---

# 18. Intervall-Auswahl Bitcoin Demo

Unterstütze mindestens folgende gewünschte Ansichten:

```text
5 seconds
15 seconds
30 seconds
1 minute
5 minutes
15 minutes
1 hour
4 hours
1 day
1 week
1 month
```

Die UI kann kompakte Labels verwenden:

```text
5s
15s
30s
1m
5m
15m
1h
4h
1d
1w
1M
```

WICHTIG:

Nicht voraussetzen, dass jede externe Datenquelle historische 5-Sekunden-Candles oder alle Intervalle nativ bereitstellt.

Trenne daher:

```text
RequestedInterval
        ↓
MarketDataProvider capability
        ↓
native interval available?
   ├── yes → use native data
   └── no  → supported aggregation/resampling OR explicit unavailable state
```

Keine erfundenen Marktdaten erzeugen.

---

# 19. Zeitraum-Einstellung

Die Demo benötigt zusätzlich zur Intervallgröße einen frei wählbaren Zeitraum.

Mindestens:

```text
From timestamp/date
To timestamp/date
```

plus Presets:

```text
1H
1D
1W
1M
3M
1Y
MAX
```

Intervall und Zeitraum sind unterschiedliche Konzepte:

```text
INTERVAL
= Granularität eines Samples/Candles

RANGE
= betrachteter Gesamtzeitraum
```

Beispiel:

```text
Interval = 5m
Range = last 7 days
```

Diese Trennung muss im Datenmodell sichtbar sein.

---

# 20. MarketQuery

Definiere außerhalb des Chart-Kerns einen neutralen Demo-/Provider-Request, beispielsweise:

```kotlin
data class MarketQuery(
    val instrument: String,
    val quoteCurrency: String,
    val interval: MarketInterval,
    val rangeStart: Instant,
    val rangeEnd: Instant
)
```

oder passend zu bestehenden Projekt-Contracts.

Der Chart-Kern kennt `MarketQuery` nicht.

---

# 21. MarketDataProvider

Bitcoin-Daten werden über eine austauschbare Provider-Grenze geliefert.

Konzeptionell:

```kotlin
interface MarketDataProvider {
    suspend fun load(query: MarketQuery): MarketSeries
    fun live(query: MarketQuery): Flow<MarketUpdate>
}
```

Nur anlegen, wenn keine passende bestehende Provider-/Capability-Abstraktion existiert.

Bevorzugt vorhandene Netzwerk-/Capability-/Plugin-Contracts.

Der Provider muss seine Fähigkeiten deklarieren können:

```text
supported intervals
historical range limits
live streaming support
rate limits
OHLC support
volume support
```

---

# 22. Demo Data Modes

Die Market-Demo soll zwei klar getrennte Modi unterstützen:

## SYNTHETIC

Deterministische lokale Demo-Daten.

Zweck:

- UI Preview
- Unit Tests
- Offline Demo
- Performance Tests
- reproduzierbare Screenshots

Diese Daten müssen sichtbar als `DEMO` / `SYNTHETIC` gekennzeichnet sein.

## LIVE

Reale Bitcoin-Daten über einen austauschbaren MarketDataProvider.

Die Datenquelle muss sichtbar identifizierbar sein.

Keine synthetischen Werte als reale Marktpreise darstellen.

Wenn keine Live-Quelle konfiguriert/verfügbar ist:

```text
Live data unavailable
```

und nicht still auf Fake-Daten umschalten.

---

# 23. Aggregation / Resampling

AGGR darf für Market-Time-Series eine generische Aggregationsschicht besitzen, sofern diese sauber außerhalb des Renderers liegt.

Beispiel:

```text
5-second observations
        ↓
TimeBucketAggregator
        ↓
1m OHLC
5m OHLC
15m OHLC
...
```

Für OHLC:

```text
open  = first value
high  = maximum
low   = minimum
close = last value
volume = sum, falls vorhanden
```

Aggregation darf nur erfolgen, wenn ausreichend granulare Quelldaten vorliegen.

Nie feinere historische Daten aus gröberen Candles erfinden.

Beispiel:

```text
1m → 5m
YES

5m → 1m
NO
```

---

# 24. Große Zeiträume

Die Kombination:

```text
5-second interval
+
1-year range
```

kann Millionen Punkte erzeugen.

ChartGraph darf das nicht naiv vollständig rendern.

Implementiere oder bereite eine klare Data Density Policy vor:

```text
requested range
      +
viewport pixel width
      ↓
maximum useful visible samples
      ↓
downsample / aggregate / request coarser data
```

Phase 1 darf hierfür eine einfache Max-Points-Policy verwenden.

Keine Daten still verfälschen.

UI soll anzeigen können:

```text
Requested interval: 5s
Rendered resolution: 1m
```

falls Visualisierung aggregiert wurde.

---

# 25. Bitcoin-Demo als Belastungstest

Nutze die Demo gezielt, um zu testen:

- 5s Live-Updates
- lange historische Serien
- Range-Wechsel
- Interval-Wechsel
- Zoom
- Pan
- Crosshair
- Selection
- Daten-Nachladen
- leere Daten
- Provider-Fehler
- Rate Limit
- Offline
- große Punktmengen

Die Demo darf den Chart-Kern nicht mit Market-Semantik kontaminieren.

---

# 26. AGGR-eigenes Scripting — Architektur vorbereiten

AGGR soll später ein eigenes kleines Scripting/API-Modell verwenden können.

WICHTIG:

Das AGGR-Scripting ist **nicht** die kanonische Workflow-Sprache von VisionTasker.

Es dient zur Beschreibung und Steuerung von Charts/Visualisierungen.

Es darf später beispielsweise ausdrücken:

```text
chart("btc")
    .type(CANDLE)
    .source("market.btc")
    .interval(5m)
    .range(7d)
    .showVolume(true)
```

oder:

```text
line("scanRate")
    .source("runtime.scanRate")
    .window(60s)
```

Die endgültige Syntax wird in diesem Slice NICHT festgelegt.

---

# 27. AGGR Script Architektur

Später gewünschter Pfad:

```text
AGGR Script
     ↓
Parser
     ↓
AGGR Script IR / Commands
     ↓
Validator
     ↓
ChartSpec / DataBindingSpec
     ↓
ChartGraph
```

Nicht:

```text
Script
  ↓
direkte Canvas-Zeichenbefehle überall
```

Das Scripting soll möglichst deklarativ sein.

ChartGraph bleibt Renderer.

---

# 28. AGGR Script und EMScript

AGGR-Scripting und EMScript dürfen später kooperieren, aber ihre Verantwortungen bleiben getrennt.

Beispiel:

```text
EMScript
   ↓
Automation / Runtime

AGGR Script
   ↓
Visualization Definition
```

EMScript könnte später AGGR aufrufen:

```text
chart.show("runtimeTelemetry")
```

AGGR Script könnte definieren, wie dieser Chart aufgebaut ist.

Keine vierte kanonische Workflow-Wahrheit erzeugen.

Canonical automation truth bleibt WorkflowGraph/IR.

---

# 29. Script-fähige ChartSpec-Grenze

Damit später AGGR Script möglich ist, sollen ChartSpecs:

- serialisierbar
- validierbar
- möglichst deterministisch
- ohne Compose-Typen im Domain Contract
- versionierbar

sein.

Beispiel:

```text
AGGR Script
      ↓
ChartSpec
      ↓
Compose Renderer
```

Dadurch können später dieselben Specs entstehen aus:

```text
Kotlin API
AGGR Script
EMScript adapter
Dataset Manager
Inspector
Railchart
Plugin
```

---

# 30. Keine Script Engine in Phase 1

In diesem Slice NICHT implementieren:

- vollständigen Lexer
- Parser
- VM
- Interpreter
- REPL
- Script Editor
- Script Debugger

Aber:

Die ChartSpec-/DataBinding-Architektur darf späteres Scripting nicht blockieren.

Dokumentiere am Ende kurz:

```text
How AGGR scripting can be added without changing ChartGraph core
```

---

# 31. Performance

Für große Serien:

- Canvas/Path statt Composable pro Punkt
- Geometry caching, wo sinnvoll
- viewport clipping
- sichtbare Daten filtern
- bounded live history
- keine O(N)-Vollarbeit bei jeder Pointerbewegung, wenn vermeidbar
- effizientes Hit Testing
- späteres Downsampling ermöglichen

Teste mindestens:

```text
1,000 points
10,000 points
100,000 points
```

soweit auf dem Entwicklungsgerät/Testumfeld sinnvoll.

Messwerte dokumentieren, nicht erfinden.

---

# 32. Accessibility / Farbwahrnehmung

Information nie ausschließlich über Farbe codieren.

Zusätzlich verwenden:

- Label
- Shape
- Stroke
- Pattern
- Legend
- Value
- Selection outline

Charts benötigen sinnvolle Accessibility-Semantics:

- Chart description
- Series
- selected value
- timestamp
- value
- labels

---

# 33. Material 3 / WSS

Optik:

- Material 3
- WSS-kompatibel
- Dark/Light
- responsive
- Smartphone-first
- große Touch Targets
- kompakte Controls
- expressive Styling nur dekorativ

Grundsatz:

> Semantics must be explicit. Expressiveness may be decorative.

WSS-/VisualPolicy-Contracts wiederverwenden, falls vorhanden.

---

# 34. Demo Screen Gesamtaufbau

Erstelle einen isolierten AGGR Demo Screen mit mindestens vier Bereichen/Tabs:

```text
AGGR DEMO
│
├── Telemetry Line
├── Confidence Bars
├── Entity Donut
└── BTC Market
```

BTC Market enthält:

```text
instrument
current mode: SYNTHETIC / LIVE
provider status
interval selector
range selector
custom from/to
chart
crosshair
selected timestamp/value
point count
rendered resolution
loading/error state
```

---

# 35. Tests

Mindestens:

## Scale

- min → min
- max → max
- midpoint → midpoint

## Line Geometry

- bekannte Punkte → erwartete Canvas-Koordinaten

## Bar Geometry

- positive/zero/negative values

## Pie Geometry

- korrekte Winkelanteile
- zero/empty handling

## Hit Testing

- Point
- Bar
- Slice
- Candle, falls implementiert

## TimeViewport

- Range mapping
- zoom
- pan bounds

## Market Interval

- 5s
- 15s
- 30s
- 1m
- 5m
- 15m
- 1h
- 4h
- 1d
- 1w
- 1M

## Market Range

- preset ranges
- custom from/to
- invalid from > to
- empty range

## Aggregation

Prüfe beispielsweise:

```text
12 x 5s → 1 x 1m OHLC
```

mit deterministischen Testwerten.

## No fake upsampling

Prüfe:

```text
1m source → requested 5s
```

muss als unsupported/insufficient granularity behandelt werden.

## Live Buffer

- bounded growth
- ordered timestamps
- duplicate timestamp policy

## Empty / Invalid

- empty
- NaN
- Infinity
- provider error
- offline

---

# 36. Acceptance Criteria

Phase 1 ist abgeschlossen, wenn:

- [ ] vollständig native Compose-Lösung
- [ ] kein WebView
- [ ] Line funktioniert
- [ ] Bar funktioniert
- [ ] Pie funktioniert
- [ ] Donut funktioniert
- [ ] neutrale ChartSpecs
- [ ] Scale/Layout/Geometry/Renderer sinnvoll getrennt
- [ ] Rendering und Hit-Test verwenden dieselbe Geometry
- [ ] Selection funktioniert
- [ ] Zoom/Pan für Time-Series funktioniert
- [ ] externer TimeViewport ist möglich
- [ ] Live-/Streaming-Daten sind möglich
- [ ] große Datenserien erzeugen nicht einen Composable pro Punkt
- [ ] BTC Demo vorhanden
- [ ] BTC Intervalle 5s bis 1M im UI modelliert
- [ ] Zeitraum-Presets vorhanden
- [ ] Custom From/To vorhanden
- [ ] SYNTHETIC und LIVE klar getrennt
- [ ] keine erfundenen Live-Marktdaten
- [ ] Provider Capability für nicht unterstützte Intervalle berücksichtigt
- [ ] Aggregation nur von fein → grob
- [ ] ChartSpec ist für späteres AGGR-Scripting geeignet
- [ ] Dark/Light funktioniert
- [ ] Accessibility-Grundlage vorhanden
- [ ] Unit Tests grün
- [ ] betroffenes Modul baut

---

# 37. Nicht tun

Nicht:

- WorkflowGraph ändern
- Worldview ändern
- Recorder umbauen
- Watchdog umbauen
- Inspector umbauen
- Dataset Manager umbauen
- Railchart komplett umbauen
- EMScript jetzt erweitern
- vollständige AGGR Script Engine implementieren
- Trading-/Order-Funktionen implementieren
- Wallet integrieren
- API Keys hardcoden
- Market Provider in ChartGraph-Core einbauen
- Fake-Kurse als live darstellen
- historische 5s-Daten aus 1m-Daten erfinden
- externe WebView-Chartbibliothek einbauen
- unnötigen God-Manager bauen

---

# 38. Empfohlene Paketgrenzen

Nur verwenden, wenn sie zum bestehenden Projekt passen:

```text
aggr-chart-core
    ChartSpec
    Scale
    Geometry
    Viewport
    Selection

aggr-chart-compose
    Compose Renderer
    Interaction
    M3/WSS Adapter

aggr-chart-market-demo
    MarketQuery
    MarketDataProvider adapter
    Synthetic provider
    Live provider
    aggregation/resampling
    demo UI
```

Nicht künstlich neue Gradle-Module erzeugen, wenn bestehende Plugin-Struktur eine bessere Grenze bietet.

---

# 39. Abschlussbericht

## Existing Architecture
Welche vorhandenen Contracts wurden wiederverwendet?

## Added Architecture
Welche Typen wurden neu benötigt?

## Files Changed
Datei → Verantwortung → Änderung.

## Supported Charts

```text
Line         YES/NO
Bar          YES/NO
Pie          YES/NO
Donut        YES/NO
Candlestick  YES/NO
```

## Market Demo

Berichte:

```text
Synthetic data: YES/NO
Live provider: YES/NO
Intervals:
5s ...
1M ...
Custom range: YES/NO
Zoom/Pan: YES/NO
Crosshair: YES/NO
Aggregation: YES/NO
```

## Data Provider
Welche Quelle/Provider-Abstraktion wurde verwendet und welche Intervalle unterstützt sie tatsächlich?

## AGGR Scripting Readiness
Erkläre konkret, wie später:

```text
AGGR Script → ChartSpec → ChartGraph
```

ergänzt werden kann, ohne den Renderer umzubauen.

## Performance
Gemessene Ergebnisse, falls ausgeführt.

## Tests
Commands + Ergebnisse.

## Build
Command + Result.

## Scope Audit

Explizit:

```text
WorkflowGraph changed: NO
Worldview changed: NO
Recorder changed: NO
Watchdog changed: NO
EMScript changed: NO
Trading functionality added: NO
WebView added: NO
Fake live market data added: NO
```

## Next Slice
Genau einen nächsten sinnvollen Slice nennen und NICHT implementieren.

---

# 40. Definition of Done

Der Demo-Bereich muss mindestens zeigen:

```text
AGGR ChartGraph

[Telemetry] [Confidence] [Entities] [BTC Market]

BTC Market
BTC / Quote                  [SYNTHETIC | LIVE]

Interval:
[5s][15s][30s][1m][5m][15m][1h][4h][1d][1w][1M]

Range:
[1H][1D][1W][1M][3M][1Y][MAX]
From [........]  To [........]

┌─────────────────────────────────────────────┐
│                                             │
│      BTC Time Series / Candlestick          │
│                                             │
│          ╭──╮       ╭────────               │
│ ─────────╯  ╰───────╯                       │
│                    │                        │
│                    ● selected value         │
│                                             │
└─────────────────────────────────────────────┘

Selected:
Timestamp: ...
Value/OHLC: ...

Requested interval: 5s
Rendered resolution: 5s
Visible range: ...
Points: ...
Provider: ...
```

Der Benutzer muss:

1. Intervall ändern können.
2. Zeitraum-Preset ändern können.
3. From/To manuell setzen können.
4. zoomen und horizontal verschieben können.
5. einen Zeitpunkt/Datenpunkt selektieren können.
6. zwischen reproduzierbaren Demo-Daten und einer verfügbaren Live-Quelle unterscheiden können.
7. sehen können, wenn ein gewünschtes Intervall vom Provider nicht unterstützt wird.

Und die Architektur muss ermöglichen, später:

```text
AGGR Script
     ↓
ChartSpec
     ↓
ChartGraph
```

einzuführen, ohne ChartGraph neu zu entwerfen.

Das ist der Scope dieses Auftrags.
