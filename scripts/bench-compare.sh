#!/usr/bin/env bash
# Documents how to compare QuillCore vs vanilla / Sodium+Lithium on a real PC.
# Does not invent numbers — print this checklist and exit.
set -euo pipefail
cat <<'TXT'
QuillCore bench compare checklist (run on a real gaming PC)

1. Minecraft 1.21.8 + Fabric Loader + Fabric API
2. Fixed: world seed, render distance, simulation distance, graphics preset, VSync/FPS cap
3. Scenarios (one at a time):
   - vanilla (QuillCore unloaded or masterEnabled=false)
   - quillcore alone
   - sodium + lithium alone
   - (retired: sodium + lithium + quillcore — cannot load together, ADR-001)
4. For each: /quillcore bench start → 60s same camera path → /quillcore bench report
5. Record avg FPS, 1% low, tick ms — leave blank if unmeasured
6. Never paste fabricated comparisons into README or releases

See docs/BENCH.md
TXT
