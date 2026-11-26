# YA CLIENT launcher (no Electron)

One small Windows exe written in Go. The window is a native WebView2 page (Windows 10/11 already has it),
styled like the YA menus. It does everything itself:

* logs in offline (nickname) or with Microsoft (device code: open the page, type the code),
* downloads Java 8 (only if missing), Minecraft 1.8.9, Forge 1.8.9 and the assets, once,
* downloads the mods you switch on (YA MODS, YA MENU, SkyFX) from this repository's release "latest",
* starts the game. The game files live in `%APPDATA%\.yaclient\game`.

Build: GitHub Actions job `launcher` (artifact `YA-CLIENT-Launcher`).
The mod jars are published by the job `release` as the release `latest` (the repository must be public).

## v2 sections
Play (account, join-server-on-start), Mods (YA mods on/off + your own jars), Servers (add/remove, live ping with icon, players, ms), Settings (RAM, fullscreen, window size, Java path, extra JVM args, close on play), Logs (last start).

## v3
Console page (live game output, Clear/Copy/Stop game) and a first-run setup wizard (account, mods, memory/fullscreen). Cleaner layout.

## Auto-update
Every push builds YA-CLIENT.exe and publishes it with version.txt (commit id) in the release "latest". On start the launcher compares its own commit with version.txt; if different it downloads the new exe, swaps itself and restarts. The very first exe must be downloaded by hand from Actions → Artifacts; after that it updates itself.

## News and partner servers
Edit `data/news.json` and `data/servers.json` in the GitHub repo (repo root). The launcher reads them at start. Players cannot add servers.
