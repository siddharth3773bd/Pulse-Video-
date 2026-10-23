# Echo Music–Style Wavy SeekBar & Audio Mini-Player Upgrade

Replace the unnatural `LinearWavyProgressIndicator` implementation with a custom Echo Music / Android 16 style continuous sine-wave `WavySeekBar` that smoothly flattens to a straight line when paused, features a prominent vertical stick handle with clean symmetric gaps, and powers both the fullscreen video player and the audio mini-player.

## User Review & Critical Decisions

> [!IMPORTANT]
> Both design and scope choices have been confirmed during clarification and govern this implementation:

- **Confirmed Decision 1 (Echo Music Sine-Wave Canvas + Vertical Stick)**: Build `WavySeekBar` using a continuous sine-wave Compose `Canvas` (`wavelength = 22.dp`, `waveHeight = 3.5.dp`, `strokeWidth = 4.dp`, rounded caps) paired with an animated vertical pill stick handle (`4.dp × 28.dp`, narrowing to `2.5.dp` while dragging) and a symmetric gap separating the played wave, handle, and unplayed flat track.
- **Confirmed Decision 2 (Audio Mini-Player Layout)**: Remove both the old `LinearWavyProgressIndicator` and the `Slider` stacked beneath it in the Audio mini-player, replacing them with a single `WavySeekBar` that matches the current compact mini-player card layout.

## 1. Overview & Core Concept

- **What It Does**: Provides a unified, responsive `WavySeekBar(progress, onSeekPreview, onSeekFinished, isPlaying, modifier)` used across all media playback surfaces (Video Player and Audio Mini-Player) while leaving preference sliders (Skip Duration and Custom Theme Color HSV sliders) untouched.
- **Target Audience / Persona**: Users who want authentic Android 16 / Echo Music media playback controls with a natural, gentle wave that animates smoothly during active playback and flattens completely into a straight line the moment playback pauses or ends.
- **Key Value**: Eliminates the cached-draw bug and overly steep curves of `LinearWavyProgressIndicator`, removes the duplicate stacked seek bars in the Audio tab, and delivers a tactile vertical stick handle with zero-spam seek-on-release scrubbing.

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Active Playback (`isPlaying = true`)**: The played portion Left of the handle renders a soft, continuous sine wave (`22.dp` wavelength, `3.5.dp` peak amplitude, `4.dp` stroke thickness with `StrokeCap.Round`) that glides smoothly forward at a steady phase velocity.
  2. **Paused or Ended (`isPlaying = false`)**: The wave amplitude animates smoothly to `0.dp` via `MaterialTheme.motionScheme.defaultEffectsSpec()`, turning the played portion into a perfectly flat horizontal line while freezing the phase in place so the wave never jumps or keeps wiggling when stopped.
  3. **Scrubbing & Seeking (Tap or Drag)**:
     - The full seek bar has a minimum `48.dp` vertical touch target.
     - Touching or dragging anywhere along the bar immediately updates the visual preview position and narrows the vertical stick handle from `4.dp` to `2.5.dp` (`MaterialTheme.motionScheme.fastSpatialSpec()`).
     - `seekTo` is invoked only when the user releases their finger (`onSeekFinished`), preventing player seek spam during scrubbing.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Clean Android 16 / Echo Music Material 3 Expressive media controls.
  - *Color Palette & Mood*:
    - Played wave & vertical stick handle: `MaterialTheme.colorScheme.primary`.
    - Unplayed flat rounded track: `MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)`.
  - *Component Styling & Layout*:
    - **Played Wave**: Drawn from the left cap up to `handleCenterX - halfHandleWidth - gap`, with a gentle amplitude taper at the very ends so the wave never clips abruptly against the caps.
    - **Vertical Stick Handle**: `4.dp` wide (`2.5.dp` when pressed/dragged) by `28.dp` tall with `CircleShape` (fully rounded pill), centered vertically and positioned at the current playback fraction.
    - **Symmetric Gap**: A clean `5.dp` visual gap on both sides of the vertical stick handle separating the wave on the left and the flat unplayed track on the right.

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Continuous Sine-Wave `Canvas` Over `LinearWavyProgressIndicator`**
  - *Chosen Approach*: Render the wave path directly in a Compose `Canvas` using sampled `sin(x * 2π / wavelength - phase)` points with `amplitudePx = maxAmplitudePx * animatedAmplitudeFraction`.
  - *Why*: `LinearWavyProgressIndicator` caches its path inside an internal modifier node that fails to invalidate when state captured inside the `amplitude` lambda changes, causing the wave to stay frozen in a tall wavy state when paused. Direct sine-wave rendering guarantees `y = centerY` when `animatedAmplitudeFraction == 0f` (100% flat) and gives exact pixel control over wavelength (`22.dp`), height (`3.5.dp`), and the gap before the vertical stick handle.
  - *Alternatives Considered*: `LinearWavyProgressIndicator` (rejected due to draw-cache bug and unnatural curve geometry) and third-party slider libraries (passed over per user preference for the self-contained Echo Music sine-wave + vertical stick approach).
- **Decision 2: Unified Playback SeekBar Across Video and Audio**
  - *Chosen Approach*: Use the same `WavySeekBar` composable in both the fullscreen video player (between the elapsed and total/remaining time labels, preserving the `"player_seek_slider"` test tag) and the Audio tab's now-playing mini-player card (replacing the old stacked `LinearWavyProgressIndicator` + `Slider`).
  - *Why*: Ensures every media playback seek bar in the app shares the exact same wave physics, vertical stick thumb, and touch behavior, while keeping non-playback settings sliders (`MeTabScreen`) as standard Material 3 `Slider`s.

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌───────────────────────────────────────────────────────────────────────────┐
│                         WavySeekBar Composable                            │
│  (progress, onSeekPreview, onSeekFinished, isPlaying, modifier)           │
├───────────────────────────────────────────────────────────────────────────┤
│  • Amplitude Animation: animateFloatAsState(if (isPlaying) 1f else 0f)    │
│  • Phase Animation: withInfiniteAnimationFrameMillis (runs only when      │
│    animatedAmplitude > 0f; freezes phase in place when paused/flat)       │
│  • Touch Gesture Layer: 48.dp min height, awaitEachGesture for tap+drag   │
│    previewing dragProgress locally & firing onSeekFinished on release     │
│  • Canvas Layer:                                                          │
│    [Played Sine Wave (primary)] ── (gap) ── [Vertical Stick 4x28.dp]      │
│                                              ── (gap) ── [Flat Track]     │
└───────────────────▲───────────────────────────────────▲───────────────────┘
                    │                                   │
    ┌───────────────┴───────────────┐   ┌───────────────┴───────────────┐
    │       VideoPlayerModal        │   │       AudioLibraryView        │
    │ • Placed between time labels  │   │ • Replaces stacked Wavy +     │
    │ • Updates preview time text   │   │   Slider in Now Playing card  │
    │ • Commits seekTo on release   │   │ • Commits onSeekAudio on      │
    │ • Keeps "player_seek_slider"  │   │   release with live preview   │
    └───────────────────────────────┘   └───────────────────────────────┘
```
- **Data Model & State**:
  - `WavySeekBar` manages internal `isDragging: Boolean`, `dragProgress: Float`, `phaseShift: Float`, and animated `amplitudeFraction: Float` (`0f..1f`).
  - `AudioLibraryView` and `VideoPlayerModal` pass playback progress (`0f..1f`) and `isPlaying` (`Boolean`) into `WavySeekBar`, updating preview state while dragging and committing the final seek position on release.
- **Interactive Component & State Mapping**:
  - When `isPlaying` transitions from `true` to `false` (or playback ends), `amplitudeFraction` smoothly animates to `0f` and the frame loop halts once flat, leaving a straight `4.dp` line.
  - When the user presses or drags `WavySeekBar`, `isDragging` becomes `true`, the vertical stick handle narrows from `4.dp` to `2.5.dp`, `onSeekPreview` updates any bound UI preview, and releasing the pointer resets `isDragging = false` and invokes `onSeekFinished`.
