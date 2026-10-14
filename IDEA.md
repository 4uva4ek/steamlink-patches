# XR Porting Hub ideas

Roadmap ideas, not committed work. Each item still needs an exact base, decoded-APK evidence and headset validation.

- Port more Meta Quest / Pico apps to Android XR: a new app is a new package with its own sources, payloads, extension task and compatibility list - never a widening of an existing app.
- Reuse the 4XVR launch pattern (launch compatibility plus Meta platform decoupling) as the template for other Quest-only apps.
- Steam Link hitch regression: continue only with new mechanisms; read diagnostics/steamlink-hitches/TRIED-EXPERIMENTS.md first - retired experiments must not be re-derived.
- Record headset validation per app the same way every time (logcat tags, capture steps, executed/skipped counts).
- Keep the README support table and each app's exact bases current before any release claim.
