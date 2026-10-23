#!/usr/bin/env bash
# Prints the manual comparison checklist.
set -euo pipefail
cat <<'TXT'
QuillCore bench compare checklist (run on a real gaming PC)

1. Minecraft 1.21.8 + Fabric Loader + Fabric API
2. Fixed: world seed, render distance, simulation distance, graphics preset, VSync/FPS cap
3. Scenarios (one at a time):
   - vanilla (QuillCore unloaded or masterEnabled=false)
   - quillcore alone
   - sodium + lithium alone
   - sodium + lithium + quillcore
4. For each: /quillcore bench start → 60s same camera path → /quillcore bench report
5. Record avg FPS, 1% low and tick ms

For the automated bench see docs/TESTING.md
TXT
