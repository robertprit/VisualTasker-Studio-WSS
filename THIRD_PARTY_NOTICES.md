# Third-Party Notices

VisualTasker Studio WSS is open-source software. The following components are
used under their respective licenses. This file complements, but does not
replace, the license texts distributed with those components.

## Compose DND

- Project: Compose DND
- Version: 0.5.0
- Copyright: Copyright 2023 Mohamed Rejeb
- License: Apache License 2.0
- Source: https://github.com/MohamedRejeb/compose-dnd

Compose DND provides Compose gesture, reorder, auto-scroll and drop-animation
primitives. VisualTasker's payloads, capability checks and drop semantics remain
owned by the VisualTasker plugin contracts.

## AndroidX and Jetpack Compose

- Project: AndroidX / Jetpack Compose
- License: Apache License 2.0
- Source: https://android.googlesource.com/platform/frameworks/support/

## LiteRT

- Project: Google AI Edge LiteRT
- License: Apache License 2.0
- Source: https://github.com/google-ai-edge/LiteRT

Model files can have separate licenses. Every model bundled or downloaded by a
VisualTasker provider must therefore carry an explicit model card and license
record in the provider manifest.

## Vico

- Project: Vico
- Version: 1.15.0 (legacy ChartGraph demo only)
- License: Apache License 2.0
- Source: https://github.com/patrykandpatrick/vico

## Guava

- Project: Guava
- Version: 33.4.8-android (Vision demo only)
- License: Apache License 2.0
- Source: https://github.com/google/guava

## Ultralytics YOLO26 models

The model files in `VisualTasker Vision AI Plugin` originate from the
Ultralytics toolchain. Ultralytics publishes its software and models under
AGPL-3.0 or an Enterprise license. They are deliberately not embedded in the
Apache-2.0 WSS host APK. The separately distributed Vision provider must carry
its applicable model license and source obligations before a public release.
