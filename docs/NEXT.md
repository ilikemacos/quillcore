# Next (post-0.4.0)

Shipped: **FSR1 EASU+RCAS** on the vanilla main-RT path.

## Reasonable follow-ups
1. Validate FSR1 with Sodium mc1.21.8 in a real GPU client; tighten the warning if Sodium bypasses resize.
2. FSR2 only if a motion-vector + jitter path appears (Iris/Sodium API or custom velocity pass) — do not stub-fake.
3. Multi-version Loom targets (1.21.9–1.21.11) per `PORTING.md`.
4. Honest `BenchHarness` sessions on a GPU box (never fabricate).

## Done for now unless new evidence
- FSR3 frame generation
- XeSS / in-process FrameFX
- Chunk rebuild coalescing
- CSR (permanently excluded)
