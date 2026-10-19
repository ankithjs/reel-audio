# Reel Audio

A small Android app that saves the audio from Instagram reels so you can listen later,
like a podcast or music app, including with the screen locked.

## Install

On your phone, open the [latest release](https://github.com/ankithjs/reel-audio/releases/latest),
download **ReelAudio.apk** and open it. Android will ask you to allow installing apps from your
browser the first time. New versions install over the old one and keep your library.

## Use it

- **Save a reel:** in Instagram tap **Share** on a reel, then pick **Reel Audio** (you may need
  to tap *More* the first time). Choose a group and tap Save. You can also copy a link, tap **+**
  in the app and paste. Paste several links (one per line) to save many at once.
- **Groups:** tap *New group* to make your own (Philosophy, Poetry, Motivation…). Use the ⋮ menu
  on a clip to put it in one or more groups. Hold a group to rename or delete it.
- **Listening:** tap a clip to play the current list. Playback continues with the screen off and
  shows controls on the lock screen. Shuffle, repeat, ±10 s and playback speed are in the player.
- **Captions:** the reel's caption and author are saved with each clip. Timed subtitles are saved
  too whenever the source provides them.
- **Shared files:** you can also share a video or audio file (for example a screen recording) to
  Reel Audio to add it.

### If a reel won't save

Instagram sometimes refuses requests that aren't signed in. Open **Settings → Sign in to
Instagram**, log in once, then tap a failed clip to retry. The login stays on your phone and is
only used to fetch reels you share. **Settings → Update extractor** pulls the latest fix when
Instagram changes something.

## Storage

Audio is stored on the phone so it plays offline. A typical reel is about 0.5–1 MB, so 1,000
saved reels take roughly 1 GB.

## How it works

Kotlin + Jetpack Compose, Room for clips and groups, Media3/ExoPlayer with a media session for
background and lock-screen playback, and [youtubedl-android](https://github.com/JunkFood02/youtubedl-android)
(yt-dlp) to fetch audio from a shared link. GitHub Actions builds the APK on every push to `main`
and publishes it to the `latest` release.

Builds are signed with `app/shared-debug.keystore` so updates install cleanly. To sign with your
own private key instead, add the secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` (switching keys means uninstalling once).

For personal use only. Saving content from Instagram may go against Instagram's terms of use.
