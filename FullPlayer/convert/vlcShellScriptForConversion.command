#!/bin/sh                                                                                                                                                     
######################## Transcode the files using ... ########################
vcodec="mp4v"
acodec="mp4a"
vb="1024"
ab="128"
mux="mp4"
###############################################################################

vlc="/Applications/Utilities/VLC.app/Contents/MacOS/VLC"

# Sanity check
if ! command -pv "$vlc" >/dev/null 2>&1; then
    printf '%s\n' "Cannot find path to VLC. Abort." >&2
    exit 1
fi

for filename in *; do
    printf '%s\n' "=> Transcoding '$filename'... "
    /Applications/Utilities/VLC.app/Contents/MacOS/VLC -I dummy -q "$filename" \
       --sout '#transcode{vcodec="$vcodec",vb="$vb",acodec="$acodec",ab="$ab"}:standard{mux="$mux",dst="$filename.transcoded",access=file}' \
       vlc://quit
    ls -lh "$filename" "$filename.transcoded"
    printf '\n'
done
The wildcard *.transcoded will select all of the transcoded files for group operations.

To move files:

mv *.transcoded <directory>
To remove all filename extensions (including .transcoded):

for filename in *.transcoded; do mv "$filename" "${filename%%.*}"; done
To remove all filename extensions and replace with another (e.g. .mp3):

for filename in *.transcoded; do mv "$filename" "${filename%%.*}.mp3"; done