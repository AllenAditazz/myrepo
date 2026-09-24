# FullPlayer

JavaFX desktop app for browsing and playing a large personal video library.
Library metadata lives in one YAML file; playback is libvlc via vlcj.

## Layout

- This file sits in the project directory, `FullPlayer/`. The **git root is its
  parent** (`/Users/atg/git2025/repository`), so all git commands run one level
  up, and git reports paths as `FullPlayer/...`.
- Sources: `src/main/java/allen/` — standard Maven layout, moved there from
  `java/allen/` in Sept 2026. An empty `java/` may remain.
- Main class: `allen.App`. Program argument: the config filename (`config.txt`).

## Build and run

Eclipse is the primary build. Run configuration:

- Main class `allen.App`, program arguments `config.txt`,
  working directory `${workspace_loc:FullPlayer}`
- VM arguments:
  `-Xms2g --module-path /Users/atg/javafx-sdk-26/lib --add-modules javafx.controls,javafx.swing`
- `javafx.media` is deliberately absent — playback is vlcj, not
  `javafx.scene.media`. `javafx.fxml` is unused. `javafx.swing` IS required, for
  `SwingFXUtils` in the Snap button.

From Maven:

    mvn clean compile org.openjfx:javafx-maven-plugin:0.0.8:run

Two traps there. The short `javafx:run` prefix does not resolve, because
`org.openjfx` is not in Maven's default pluginGroups — add it to
`~/.m2/settings.xml` if you want the short form. And `javafx:run` does not
compile first, so `compile` has to be chained explicitly or you get
"Output directory is empty".

Versions: Java release 18, JavaFX 26, vlcj 4.12.1, vlcj-javafx 1.2.1,
snakeyaml 1.30. Requires VLC installed at `/Applications/VLC.app`; vlcj locates
libvlc by native discovery and no path is configured in code.

## Playback design

One `EmbeddedMediaPlayer` serves the whole session. `Player.playClip(index)`
loads a new MRL into it rather than creating a player per clip; earlier versions
pre-allocated a JavaFX `MediaPlayer` for every video.

Resume-where-you-left-off is a `resumePositions` map (Video to millis), replayed
by passing libvlc's `:start-time=` media option to `play()`. Do not "fix" this by
calling `setTime()` after `play()` — media loads asynchronously and the seek gets
dropped. The entry is cleared when a clip finishes naturally, so a full replay
starts from zero.

vlcj events arrive on a native callback thread; everything is marshalled onto the
FX thread with `Platform.runLater`.

## Deliberate choices that may look wrong

- **No format allowlist.** `Utility.MEDIA_EXTENSIONS` is a *scan* filter — which
  files a directory scan treats as video — not a playability gate. libvlc plays
  what it plays; failures surface in `PlayerEvents.error` and skip to the next
  clip. The old `FX_SUPPORTED_MEDIA_EXTENSIONS` (mp4/mp4v/flv/fxm) was a
  javafx.media limitation and is gone.
- **No ffmpeg, no MediaConverter, no `badCodec` or `converted` tags.** All removed
  Sept 2026 along with the Convert button; nothing needs transcoding now.
- **Buttons are `setFocusTraversable(false)`.** Space is the global play/pause
  shortcut, and a focused JavaFX Button treats space as its own activation key,
  so whichever button had focus would also fire.
- **The main window measures itself.** It sizes to the chooser's columns after an
  explicit `root.applyCss(); root.layout();` pass, because a control has no skin
  and reports `prefWidth` 0 until CSS is applied. A ScrollPane also reports a
  100px default preferred size, which is why its `prefViewportWidth/Height` are
  set explicitly before `sizeToScene()`.
- **Search uses `matcher(...).find()`**, i.e. substring. It used
  `String.matches()`, which requires the pattern to match the whole filename, so
  a bare word found nothing.

## config.txt  (`FullPlayer/config.txt`)

The database: directory list, tag vocabulary, and ~61.5k video entries with
ratings, tags and play counts. snakeyaml format with a `!!allen.Config` root tag.

**Not tracked by git** — it is ~14MB and the app rewrites it on nearly every run.
It is local state, and the repository is not a backup of it.

An empty tag list must be written `tags: []`. A bare `tags:` parses as null and
will NPE in `VideoPreference.matches()`. Any script that edits tags must handle
that, and must match list items by indentation — items sit at the same indent as
the `tags:` key, and the next entry's `- codecInfo:` line is at column 0.

## Git

The remote is named **`myrepo`**, not `origin`
(https://github.com/AllenAditazz/myrepo.git, public).

Branches: `master` is current; `vlcj-video-backend` is merged into it;
`remove-format-restrictions` holds the format/ffmpeg removal and the search fix;
`dev` and `copy` are stale.

History was rewritten in Sept 2026 to strip ~1.9GB of test video, jars and config
snapshots — three `convert/` files exceeded GitHub's hard 100MB per-file limit, so
the branch could not be pushed at all. The pre-rewrite state is preserved in tags
`pre-strip-backup` and `pre-config-strip-backup` plus `refs/original/`, which is
why the local clone is still ~1.9GB; dropping those and running
`git gc --prune=now` reclaims it. What GitHub serves is about 460KB.

`.gitignore` covers `convert/` media, the jars, config snapshots, `*.bak`,
`target/` and `base/`.

## Known rough edges

- Eclipse and Terminal on this machine cannot authenticate to GitHub; the
  keychain has no working PAT. Pushes have been done with a token supplied out of
  band. Worth fixing:
  `printf 'protocol=https\nhost=github.com\n\n' | git credential-osxkeychain erase`
  then push and paste a fresh token.
- Seven `.DS_Store` files are still tracked and keep appearing as modified.
  `git rm --cached` them.
- `Player.java` (~730 lines) mixes UI construction with playback control;
  `App.java` (~570) builds the entire chooser inline in `start()`.
- Sub-panes have no scroll bars; the window is sized to fit its content instead.
  Adding scroll bars to sub-panes is a known next step.
