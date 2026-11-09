# AI TV — Metal UI design system

## Identity
Metal UI is a native Android TV design language: opaque graphite surfaces, vivid spectral highlights, directional metallic sheen, and crisp typography. Avoid glass blur, translucent cards, and washed-out pastels.

## Palette (sRGB)
- Graphite #080B15
- Titanium #242C3B
- Electric cobalt #005CFF
- Cyan #00E5FF
- Magenta #FF2B7A
- Teal #00EAD3
- Warm gold #FFC857
- Primary text #FFFFFF; secondary text #D4DEEF

## Motion
- Ambient light drift: 18 seconds, slow continuous cycle
- Focus enter/exit target: 140–220 ms, with spring-like ease-out
- Card elevation and halo only on focused element
- Reduced-motion and Lite modes disable nonessential animation
- Avoid large blur passes and continuous layout invalidation

## Mini-LED considerations
Mini-LED is a backlight technology, not an Android graphics API. Maximize perceived contrast with dark opaque backgrounds, restrained small bright accents, legible white text, and user-selectable intensity. Avoid large bright static surfaces and aggressive strobing.
The current implementation renders in standard Android SDR. Actual HDR/wide-color output requires device-specific display capability detection, color-management tests and a supported HDR rendering pipeline; do not label this implementation HDR10/Dolby Vision.

## Distinctive roadmap
1. Native metallic focus cards and physically plausible directional highlights
2. Adaptive brightness intensity: Cinema, Vivid, Comfortable
3. Smart performance governor based on power-save mode and device capability
4. Contextual long-press command palette
5. Offline-first search of installed apps, settings and HDMI inputs
6. Intelligent quick-resume row (local-only activity history)
7. Mini-LED contrast calibration screen with black clipping and highlight steps

## QA gates
Build APK, test remote DPAD, confirm layering and accessibility, verify reduced motion, measure GPU frame times on low-end Android TV hardware, then tune palette on a real Mini-LED screen.
