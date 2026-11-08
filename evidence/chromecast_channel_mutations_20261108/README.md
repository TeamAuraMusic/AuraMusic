# Chromecast PreviewProbe mutation convergence

Device: Chromecast `sabrina`, Android 14/API 34; Projectivy 4.73. PreviewProbe debug APK SHA-256 `d68bf235d11d7af99ad4f15df8aca7c399231271ef2f6fbe46359887ec1b06ac` installed in place with `adb install -r`; AuraMusic package and data were not modified.

- Add: published A,B; TvProvider logged count=2; Projectivy showed exactly A,B.
- Remove: published B only; provider logged count=1; Projectivy showed B.
- Reorder + add: published B,A,C; after Projectivy force-stop/relaunch, focused card was B and visible row order was B,A,C. Provider count was 3.
- Restore: published the prior A-E row, relaunched Projectivy, and restored AuraMusic `Ancestry` focus. Final screenshot shows original art and animated waveform.

No Select pressed. Projectivy process restart was required to observe the reordered row; the first 12-second live refresh sample remained stale. This validates Android TvProvider and Projectivy row behavior with PreviewProbe, not AuraMusic-specific replacement of real-song metadata.

Machine results: [`mutation sequence`](installed_probe_candidate/result.json), [`reorder after launcher restart`](reorder_refresh/result.json).
