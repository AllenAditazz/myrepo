#!/bin/sh 
cd /Volumes/WD14/a/convert
for fn in *; do
    /Users/atg/ffmpeg -i "$fn" -c:v h264  -c:a aac  "$fn.mp4" &
done
