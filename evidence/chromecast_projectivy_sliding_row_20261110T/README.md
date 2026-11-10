# Projectivy Sliding row A/B

- Device: Chromecast `sabrina`, Android 14/API 34; Projectivy 4.73/code 98; AuraMusic 3.2.0/code 29.
- Row: AuraMusic. Initial per-channel `Sliding row` setting was on.
- Ran identical no-click Angie → Africa → Angie DPAD navigation with the setting on, temporarily off, and restored it to on.
- Projectivy stayed foreground. Focus hierarchies: with Sliding row on, both focused cards occupied `[0,535][486,775]`; with it off, Africa occupied `[326,535][882,775]` while Angie occupied `[0,535][486,775]`.
- This confirms that the Projectivy setting controls whether cards slide under a fixed focus slot. It does not, by itself, prove exact video Surface pixels stay within card boundaries.
- Current device setting restored to the original on state. User can turn it off just for AuraMusic: Projectivy Settings → Edit channels → AuraMusic → Sliding row.
- Machine result: [`result.json`](result.json). Videos: [`sliding-on.mp4`](sliding-on.mp4), [`sliding-off.mp4`](sliding-off.mp4).
