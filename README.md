# Life 3D

Conway's Game of Life in three dimensions for Android, built with
[RedByteFX](https://github.com/i-redbyte/redbytefx) 1.2.0: the generations run in an OpenGL ES 3.1
compute shader written in the RedByteFX Kotlin DSL, and the cells are drawn as instanced spheres in
a viscous liquid with OpenGL ES 3.0. The camera sits inside the medium, so the colony fills the
screen instead of sitting in a box.

[Русская версия](README.ru.md)

| Looped torus · Emerald · Lava | Infinite field · Aurora · Lava |
| :---: | :---: |
| <img src="media/torus.gif" width="300" alt="Lava rule on a looped torus in emerald"> | <img src="media/infinite.gif" width="300" alt="Lava rule in an infinite field with the Aurora gradient"> |

## Features

- Two worlds: a looped torus, and an infinite field whose simulated window follows the colony while
  you travel through it with one finger.
- Seven rules in Generations notation (B/S/C) over the 26 neighbours of a cell: Lava, Reef, Amoeba,
  Waves, Builder, Moss, and Carter Bays' classic 4555. With more than two states a dying cell fades
  through refractory states, which is what makes waves, shells, and colonies that die out.
- Cell colours: seven solid colours, the Aurora gradient, and your own gradient picked in a dialog
  by hue, saturation, and brightness.
- Settings for field size (24³ to 64³), starting cells (a number or random), and speed.
- Gestures: turn, tilt, zoom, and tap to spark a cloud of new life.
- English and Russian.

## How it works

| Piece | Where |
| --- | --- |
| One generation as a compute shader, two grids in one storage buffer, rules in a uniform block | [`gl/LifeCompute.kt`](app/src/main/java/ru/redbyte/life3d/gl/LifeCompute.kt) |
| Cells and liquid in one GLES 3.0 program | [`gl/LifeScene.kt`](app/src/main/java/ru/redbyte/life3d/gl/LifeScene.kt) |
| CPU reference of the same rule | [`domain/CpuLife.kt`](app/src/main/java/ru/redbyte/life3d/domain/CpuLife.kt) |
| Generation loop, read back, window following | [`engine/LifeEngine.kt`](app/src/main/java/ru/redbyte/life3d/engine/LifeEngine.kt) |
| Cells around the camera, with every torus copy in view | [`domain/CellView.kt`](app/src/main/java/ru/redbyte/life3d/domain/CellView.kt) |
| Frame drawing | [`engine/LifeRenderer.kt`](app/src/main/java/ru/redbyte/life3d/engine/LifeRenderer.kt) |

`GpuMatchesCpuTest` runs the compute shader on a device and compares every cell with the CPU
reference, for every rule and both worlds.

## Requirements

Android 7.0 (API 24) or newer with OpenGL ES 3.1. The Android Emulator on Apple silicon exposes only
OpenGL ES 3.0, so run the game on a device or on an emulator with SwiftShader.

## Download

[Download the latest APK](https://github.com/i-redbyte/life3d/releases/download/apk/life3d.apk). CI builds it from every push to
`main` once the unit tests and lint pass. The APK is debug-signed, so installing a newer build over an older one may require
removing the old one first.

## Build

```bash
./gradlew :app:installDebug
```

Unit tests: `./gradlew :app:testDebugUnitTest`. GPU test on a connected device:
`./gradlew :app:connectedDebugAndroidTest`.
