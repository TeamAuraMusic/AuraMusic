#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/src/main/res/raw"
FONT="${PREVIEWPROBE_FONT:-/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf}"
DURATION="${PREVIEWPROBE_DURATION_SECONDS:-120}"

command -v ffmpeg >/dev/null || { echo 'ffmpeg is required' >&2; exit 2; }
[[ -f "$FONT" ]] || { echo "font not found: $FONT" >&2; exit 2; }
mkdir -p "$OUT"

for spec in 'a|440|red|A 440 Hz' 'b|880|blue|B 880 Hz'; do
  IFS='|' read -r id hz color title <<<"$spec"
  output="$OUT/fixture_${id}.mp4"
  video_filter="drawtext=fontfile=${FONT}:text='${title}':fontcolor=white:fontsize=44:x=24:y=22,drawtext=fontfile=${FONT}:text='████':fontcolor=yellow:fontsize=30:x='mod(t*60,W-text_w)':y='260+18*sin(t*2)'"
  ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "color=c=${color}:s=640x360:r=24:d=${DURATION}" \
    -f lavfi -i "sine=frequency=${hz}:sample_rate=44100:duration=${DURATION}" \
    -vf "$video_filter" -t "$DURATION" \
    -c:v libx264 -preset veryfast -crf 25 -profile:v baseline -pix_fmt yuv420p \
    -c:a aac -b:a 96k -movflags +faststart "$output"
  echo "$output"
done
