# Custom-icon-mc-mod — Fabric mod for Minecraft 1.21.1

[![GitHub release](https://img.shields.io/github/v/release/IamSboby/Custom-icon-mcmod?style=flat&color=181717)](https://github.com/IamSboby/custom-icon-mcmod/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric_Loader-0.16.9+-DBB589?style=flat)](https://fabricmc.net)
[![License](https://img.shields.io/github/license/IamSboby/Custom-icon-mcmod?style=flat&label=License)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Reversal_-5865F2?style=flat&logo=discord&logoColor=white)](https://discord.gg/jxQtq3XBKv)

Replaces the Minecraft OS window icon (taskbar / dock / title bar) with a user-supplied `logo.png`, with zero configuration and no extra dependencies.

---

## Table of contents 

- [How it works](#how-it-works)
- [Project structure](#project-structure)
- [Building](#building)
- [Installation](#installation)
- [Technical details](#technical-details)
- [Requirements](#requirements)

---

## How it works

Minecraft sets its window icon during startup by calling `Window.setIcon()`, which passes a set of resolution variants to GLFW. This mod injects into that method via a **SpongePowered Mixin**, cancels the vanilla call, and substitutes it with the user's own image.

The injection pipeline:

```
game startup
    └─> Window.setIcon()              <- vanilla call
            └─> WindowMixin (HEAD)    <- mixin intercepts here
                    ├─ resolve logo.png from .minecraft/ or mods/
                    ├─ decode with STBImage  (LWJGL, already bundled)
                    ├─ pass pixel buffer to glfwSetWindowIcon()
                    └─ ci.cancel()    <- vanilla icon is never applied
```

The mod targets the `client` source set exclusively (`"environment": "client"` in `fabric.mod.json`) and will never be loaded on a dedicated server.

---

## Project structure

```
src/
├── main/
│   └── resources/
│       └── fabric.mod.json              # mod metadata & mixin registration
└── client/
    ├── java/com/customlogo/
    │   ├── CustomLogoClient.java        # ClientModInitializer entrypoint
    │   └── mixin/
    │       └── WindowMixin.java         # core Mixin — intercepts Window.setIcon()
    └── resources/
        └── customlogo.mixins.json       # Mixin config (client-only)
```

Fabric Loom's `splitEnvironmentSourceSets()` is enabled, which separates the `main` and `client` compile classpaths. All client-only classes — including the Mixin — must live under `src/client/` to resolve Minecraft's client-side symbols (e.g. `net.minecraft.client.util.Window`).

---

## Building

The project ships with a **Gradle Wrapper** pinned to Gradle 8.8, which is required by Fabric Loom 1.7.4.

```bash
# Unix / macOS
./gradlew build

# Windows
gradlew.bat build
```

> Do **not** invoke `gradle` directly — a system-wide Gradle installation may differ from the required version and cause `LoomGradleExtensionImpl` instantiation errors.

Output: `build/libs/Custom-icon-mc-mod-1.0.0.jar`

### Key build configuration

| Property | Value |
|---|---|
| Fabric Loom | 1.7.4 |
| Gradle | 8.8 |
| Yarn mappings | `1.21.1+build.3` |
| Java target | 21 |
| Mixin compatibility | `JAVA_21` |

---

## Installation

1. Place `Custom-icon-mc-mod-x.x.x.jar` in `.minecraft/mods/`
2. Place `logo.png` in `.minecraft/` (root) or `.minecraft/mods/` as a fallback

```
.minecraft/
├── mods/
│   └── Custom-icon-mc-mod-1.0.0.jar
├── logo.png        <- primary lookup path
└── ...
```

The mod logs its actions to the game output:

```
[CustomLogo] Loading custom icon from: C:\Users\..\.minecraft\logo.png
[CustomLogo] Custom icon applied successfully (64x64).
```

If `logo.png` is not found, the mod exits the injection silently and vanilla behaviour is preserved.

### Image format

| Property | Requirement |
|---|---|
| Format | PNG |
| Color space | RGBA (4 channels) — STBImage forces `comp=4` on decode |
| Recommended size | 32×32 or 64×64 px |
| Filename | Must be exactly `logo.png` (case-sensitive on Unix) |

GLFW handles icon scaling internally; the OS picks the most appropriate resolution from the buffer.

---

## Technical details

### Mixin target

```java
@Mixin(Window.class)
public class WindowMixin {

    @Shadow private long handle;   // native GLFW window handle

    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
    private void onSetIcon(CallbackInfo ci) { ... }
}
```

`setIcon` is targeted at `HEAD` with `cancellable = true`. The mixin reads `handle` (the underlying `long` GLFW pointer, shadowed from `Window`) and passes it directly to `glfwSetWindowIcon()`.

### Image decoding

```java
ByteBuffer pixels = STBImage.stbi_load_from_memory(rawBuffer, w, h, comp, 4);

GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);
icons.position(0).width(w.get(0)).height(h.get(0)).pixels(pixels);

GLFW.glfwSetWindowIcon(handle, icons);
STBImage.stbi_image_free(pixels);
```

All native buffers are allocated off-heap via `MemoryUtil.memAlloc` and freed explicitly after the call — no GC pressure, no memory leaks.

---

## Requirements

- Java **21**
- Minecraft **1.21.1**
- Fabric Loader **≥ 0.16.9**
- Fabric API

---

*© 2025 [IamSboby](https://github.com/IamSboby) — MIT License*
