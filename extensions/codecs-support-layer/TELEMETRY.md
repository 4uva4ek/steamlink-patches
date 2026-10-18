# Opt-in Colour Lab decoder probe

Codecs Support v7 adds independent `GXRDecoderProbe` observation to the existing
exact-base helper. It does not reinstate retired decoder staging/buffering or
change the codec-swap/geometry rules. Supported pairs remain
`com.valvesoftware.steamlinkvr` **2.0.20/5001712** and **2.0.23/5002363**.
Pinned anchors, GOT slots and installer scene preconditions are unchanged.

SteamVRColourLab writes a random session token into
`/sdcard/Android/data/com.valvesoftware.steamlinkvr/files/colourlab-probe.request`
when the user connects ADB. A worker reads this bounded regular file at most
once a second. Its 30-second mtime lease expires if the PC stops refreshing it.
Absent a valid request, no decoder counters/captures are collected. Only
explicitly admitted video decoder handles and their replacements are observed;
unrelated audio handles and lifecycle calls are excluded.

`event=stats` logs token, stream generation and device `monotonic_ns`, plus
cumulative `accepted_input_bytes`, `accepted_input_frames`, `output_frames`,
`queue_to_output_count`, `queue_to_output_sum_ns`, `queue_to_output_max_ns`,
queue successes/failures, ambiguous PTS, unmatched outputs and pending evictions.
Config/EOS/empty input is excluded from video payload; partial input contributes
bytes but not complete-frame counts. PTS correlates a bounded codec-specific
pending map, never a clock. Duplicate/ambiguous PTS does not yield timing.
Queue-to-output residence includes buffering and dequeue scheduling. Decoder
outputs are not physical display frames; accepted bytes are not network receive
bytes. These counters deliberately do not implement the old `GXRStreamProbe`
transport-ID/receive protocol that Colour Lab's earlier analyzer expected.

Verify writes `colourlab-verify.request` containing the session token and a new
nonce. The helper waits up to 15 seconds for sequence headers, then permits a
headerless sample which the PC conservatively reports inconclusive. It copies
at most one complete AU up to 16 MiB from a range-checked input buffer before
the helper's optional codec rewrite. Only successful original queue calls can
produce evidence. The internally requeued swap AU is omitted. A worker hashes
the owned bytes and atomically replaces `colourlab-capture.bin`, then emits
`event=capture token=... nonce=... stream=... monotonic_ns=... size=... sha256=...`
with `path`, `accepted=1`, `scope=decoder_input`, `stage=before_codec_rewrite`.
The PC rejects wrong tokens/nonces, length/hash mismatches and malformed codec
syntax. One accepted AU identifies its codec; it does not establish sustained
delivery, decoder conformance or panel output. Request/capture files contain
only diagnostic data. No APK installation or app/SteamVR restart is automated.

Local validation on 2026-10-18:

- Native probe: 63 checks, 0 failures. Existing codec native suite: 54 checks,
  0 failures. `tests/Run-Tests.ps1` now runs both suites and accepts an external
  `-OutputDirectory`.
- Android ARM64 native build passed with `-Wall -Wextra -Werror`.
- Bundled helper SHA-256 and Kotlin pin:
  `723ff06e4375563d192444acd7a0c6da367a1bcd58d8d5a93ace0e38eb94340c`.
- Focused `CodecsSupportPatchTest` passed, including actual decoded renderer
  guards, installation, reapplication and payload tamper checks. See the local
  XML report for executed/skipped counts.

Build logs and tests for this Colour Lab integration are external under
`../builds/SteamVRColourLab/build/telemetry-native/`. The payload is staged as
a tracked patch input with its hash pin. This is source/native/package
validation only: no APK was installed, no device commands were executed, and
new telemetry/verification has not been tested on a headset. Android storage
permissions and actual live callback behavior remain runtime validation points.
