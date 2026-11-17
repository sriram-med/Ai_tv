# AI Studio and upstream updates

AI TV 2.2.0 pins techgenius1/tvOS-Launcher v2.2.0, preserving native Java UI and Apache attribution. Our updater follows only sriram-med/Ai_tv and exact ai-tv.apk assets, retaining our signing certificate and additions. It downloads newer versions when automatic updates are enabled. Android permission and installation confirmation still apply unless the TV already permits the existing ADB update mode.

A default-branch scheduled workflow checks stable upstream releases every six hours. It pins the source, increments Android versionCode, applies our overlay, verifies catalog/model/navigation behavior, builds, signs and runs Android TV emulator tests before publishing. Source drift or failing tests prevents release publication. No upstream APK is installed directly.

The AI design adapts the cloned MIT shadcn/ui dark surfaces, borders and input patterns to native TV controls. See vendor/shadcn-ui for original CSS, commit and license. React is not loaded on TV. Two toolbar rows leave space for search and filters. An always-visible mood selector combines with original-language filters and content type; recommendation rails initially build twelve cards per type and reveal more on demand. Native bounded perspective focus animations respect Android animator settings. Cinemeta metadata is cached; bundled regional catalogues work offline. Exact searches use SEARCH, while recommendations avoid extra literal network title lookups.

Nuvio discovery checks both TV and regular launchers for the official namespace or Nuvio label, retaining upstream linked-fork preferences and per-package title links. Movie booking providers remain excluded. Discovery does not configure Nuvio add-ons or subscriptions.
