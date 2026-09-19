#!/bin/sh
######################## Transcode the files using ... ########################
vcodec="h264"
acodec="mp4a"
vb="1024"
ab="128"
mux="mp4"
###############################################################################
#   vlc --sout "#transcode{acodec=mp3,ab=128,channels=2,samplerate=44100}:std{access=file,mux=raw,dst=OUTPUT}" INPUT
#/Applications/VLC.app/Contents/MacOS/VLC 02.wmv  I dummy --sout '#transcode{vcodec="$vcodec",vb="$vb",acodec="$acodec",ab="$ab"}:standard{mux="$mux",dst="02.transcoded",access=file}'
#> "%PROGRAMFILES%\VideoLAN\VLC\vlc.exe" --no-repeat --no-loop -vv "D:\688497.flv" --sout='#transcode{vcodec=mp4v,acodec=mpga,vb=800,ab=128,deinterlace}:standard{access=file,mux=ts,dst="D:\asd.mpg"}'



#/Applications/VLC.app/Contents/MacOS/VLC --sout "#transcode{acodec=mp3,ab=128,channels=2,samplerate=44100}:std{access=file,mux=raw,dst=022.mp4}" 02.wmv  vlc://quit

/Applications/VLC.app/Contents/MacOS/VLC  -I dummy  --sout "#transcode{vcodec=h264,vb=1024,acodec=mp4a,ab=128,channels=2,samplerate=44100}:std{access=file,mux=mp4,dst=03.mp4}" 03.mkv  vlc://quit 