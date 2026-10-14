# 2026-10-14 — Codec fallback + bitrate verification (AV1/HEVC @ 600) and instrument notes

## Why
Verify that (a) the AV1 rounds were really AV1 end-to-end and the HEVC round really HEVC
(no silent fallback anywhere in any direction), (b) no bitrate clamp (a 450 Mbps
SteamVR-side cap was reported), and how both codecs handle elevated Mbps.

## Instruments (what to grep, what each proves)
- Host, per 10 s (tap heartbeat): `NvencTap: heartbeat ... avg=X max=Y | init ...
  | reconf N (ovr O, ovrFail->ok F, fail G, stockFail S)` = applied RC + override state
  counters; `NvencTap: last 10s: vrlink asked avg/min/max (n=)` = vrlink's per-frame
  request; `NvencTap: frames N avg=KB` = ACTUAL encoded bytes (KB x fps x 8 = Mbit/s).
- Host, session start: `nvEncInitializeEncoder override: ... codec HEVC->AV1; avg 0M->NM`
  (swap applied) vs the same line WITHOUT the codec fragment (HEVC round);
  `stream quality ... N Mbit/s (Advanced bandwidth override)`.
- Client: `GXRCodecsSupport` (build string, `first AU reads as X; stock HEVC path kept`,
  `switched decoder to video/av01`, `geo:` lines); `QC2Comp [av1D_n]/[hevcD_n] Output
  Format at frame 0: ...` = decoder geometry; `Stats` ~90 fps.
- Byte level: tap AU dump `%TEMP%\gxr_au_dump.bin` (written only for non-HEVC selections)
  decodes with libdav1d (`-f obu`) = proof of the transmitted bitstream.

## Round results (2026-10-14)
| session | selected | host proof | client proof | AU dump (byte-level) |
|---|---|---|---|---|
| 16:53 (v5) | AV1 | override `codec HEVC->AV1`, SPLIT accepted | swap + geo BAIL (v5 TD bug); av1D_65; Output 3200x8192 (bug) | AV1 (libdav1d) |
| 17:22 (v6) | AV1 | override `codec HEVC->AV1`, SPLIT accepted | swap + geo OK; av1D_71; Output 1536x6144 | AV1 (libdav1d) |
| 17:24 (v6) | HEVC | init codec=HEVC, NO swap fragment | `reads as HEVC; stock kept`; hevcD_72 | none (gate: non-HEVC only) |
| 18:34 (v6) | AV1 @600 + overlay | init `avg 0M->600M`; heartbeats `avg=600M`; targetBandwidth 600 | swap + geo; av1D_74 | AV1 (libdav1d), sizes to ~363 KB |
| 18:41 (v6) | HEVC @600 | init no swap, `avg 0M->600M`; SPLIT accepted | `reads as HEVC`; hevcD_75 | none (correct) |

Fallback counters: `ovrFail->ok 0` and `fail 0` in every clean session. The pristine-
retry path (which would run vrlink's HEVC params on a swapped session) never succeeded
anywhere — no mid-session codec flip ever occurred.

## Bitrate findings
- NO 450 Mbps clamp exists anywhere measured: no log/GUI/code path rewrites to 450;
  the encoder target reached 600M on BOTH codecs (init `avg 0M->600M`; AV1 heartbeats
  sustained `avg=600M`). GUI: Bitrate (Debug) 0-600 step 25; Bandwidth Override 0-600
  step 25; code clamp [10,2000].
- vrlink's OWN per-frame request ceilings at ~350 Mbps regardless of `targetBandwidth`
  (measured `asked max=350M` with both 350 and 600 targets) — its known internal cap
  (cf. NvencTap.h "run 3: 600 asked, 350 sent"); the tap's scaling
  (`request x bitrateMbit / vrlinkClampMbit(350)`, capped at bitrateMbit) lifts the
  encoder target to 600.
- Actual wire rate is content-driven: ~230-270 Mbit/s on the static SteamVR lobby even
  at a 600 ceiling (CBR undershoots static content). Near-600 actual needs a moving
  game scene.

## New finding: hot-editing settings on a LIVE session wedges the reconfigure path
Twice (17:23:26, 18:38:58) a settings.json edit made while the stream was live (both
included a codec flip 2->0) put the tap into a reconfigure-failure loop:
`SPLIT-FRAME: nvEncReconfigureEncoder REJECTED the 12.1-versioned structs (status 8) —
latched off`, then repeating `WITH override failed (8)` + `pristine retry ALSO failed
(8)` + `detail: invalid color configuration` at ~90/s for the rest of the session;
vrlink logs `Error: Failed during nvEncReconfigureEncoder. Errorcode: 8 // Would be
fatal: 1`; the video keeps flowing on the last good config. Fresh restarts are clean
(fail 0). The client's `Buffer item consumer returned null buffer` warnings are baseline
face-bridge noise — present at similar rates in every captured session, clean ones
included (2k-24k per capture) — not a cascade symptom.
Rule: change driver settings only with SteamVR fully stopped.

## The "lighter square" (user report during the 18:34 round)
= the vrlink debug overlay's region tint (`debugRegionColoring`): it vanished the moment
the key was switched off (live hot-reload) and is absent from all overlay-off captures.
Test-only, switched off.

## Artifacts
../builds/steamlink-patches/build/codec-verify-20261014/
(round-baseline, round-fixed, round-hevc, round-600av1, round-600hevc, audump,
verify-wave with FINDINGS-A/B + review findings; host log snapshots incl. cascade
evidence in round-600hevc/)
