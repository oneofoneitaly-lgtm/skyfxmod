package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// version is set at build time (-X main.version=<commit>). The release "latest" carries a
// version.txt with the commit of the newest launcher and YA-CLIENT.exe itself.
var version = "dev"

func releaseURL(file string) string {
	return "https://github.com/" + repo + "/releases/download/latest/" + file
}

// cleanOldExe removes the previous launcher left behind by an update.
func cleanOldExe() {
	if self, err := os.Executable(); err == nil {
		os.Remove(self + ".old")
	}
}

// checkUpdate downloads a newer launcher, swaps it in and restarts. Returns true when it restarted.
// Renaming a running exe is allowed on Windows; the old file is deleted on the next start.
func checkUpdate(status func(string)) bool {
	if repo == "" || version == "dev" {
		return false
	}
	b, err := httpGet(releaseURL("version.txt") + "?t=" + time.Now().Format("20060102150405"))
	if err != nil {
		return false
	}
	latest := strings.TrimSpace(string(b))
	if len(latest) < 7 || len(latest) > 64 || latest == version {
		return false
	}
	self, err := os.Executable()
	if err != nil {
		return false
	}
	status("Updating the launcher...")
	tmp := filepath.Join(filepath.Dir(self), "YA-CLIENT.new.exe")
	os.Remove(tmp)
	if err := download(tmp, "", releaseURL("YA-CLIENT.exe")); err != nil {
		os.Remove(tmp)
		return false
	}
	if fi, err := os.Stat(tmp); err != nil || fi.Size() < 1<<20 {
		os.Remove(tmp)
		return false
	}
	os.Remove(self + ".old")
	if err := os.Rename(self, self+".old"); err != nil {
		os.Remove(tmp)
		return false
	}
	if err := os.Rename(tmp, self); err != nil {
		os.Rename(self+".old", self) // put the old one back
		return false
	}
	exec.Command(self).Start()
	return true
}
