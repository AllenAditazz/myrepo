#!/bin/zsh
cd /Users/atg/git2025/repository/FullPlayer
java  -Xms2g  --module-path /Users/atg/javafx-sdk-26/lib --add-modules javafx.controls,javafx.fxml,javafx.media  -jar Player.jar config.txt
 