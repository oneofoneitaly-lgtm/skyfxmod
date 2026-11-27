package main

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

// OptiFine cannot be redistributed (its licence only allows the official download page), so the launcher does not
// ship it. Instead it opens the official page, then finds the file you downloaded and puts it in the mods folder.
// The official page: https://optifine.net/downloads  (Minecraft 1.8.9, "HD U M5", the Forge-compatible build).

var optiName = regexp.MustCompile(`(?i)^optifine.*1[._]8[._]9.*\.jar$`)

func modsPath() string {
	g, _, _, _ := gameDirs()
	return filepath.Join(g, "mods")
}

// installedOptifine returns the file name of OptiFine in the mods folder ("" if missing).
func installedOptifine() string {
	es, err := os.ReadDir(modsPath())
	if err != nil {
		return ""
	}
	for _, e := range es {
		if strings.HasSuffix(strings.ToLower(e.Name()), ".jar") && strings.Contains(strings.ToLower(e.Name()), "optifine") {
			return e.Name()
		}
	}
	return ""
}

// downloadedOptifine looks for a fresh OptiFine 1.8.9 jar in the usual download places.
func downloadedOptifine() string {
	home, _ := os.UserHomeDir()
	if home == "" {
		return ""
	}
	best, bestT := "", time.Time{}
	for _, d := range []string{"Downloads", "Desktop", "Documents"} {
		dir := filepath.Join(home, d)
		es, err := os.ReadDir(dir)
		if err != nil {
			continue
		}
		for _, e := range es {
			if e.IsDir() || !optiName.MatchString(e.Name()) {
				continue
			}
			info, err := e.Info()
			if err != nil || info.Size() < 1_000_000 { // a partial or fake download is smaller than this
				continue
			}
			if info.ModTime().After(bestT) {
				best, bestT = filepath.Join(dir, e.Name()), info.ModTime()
			}
		}
	}
	return best
}

func optifineState() string {
	b, _ := json.Marshal(map[string]interface{}{
		"installed": installedOptifine(),
		"found":     downloadedOptifine(),
	})
	return string(b)
}

// installOptifine copies the downloaded jar into the mods folder. Returns "" on success or an error text.
func installOptifine() string {
	src := downloadedOptifine()
	if src == "" {
		return "I could not find the OptiFine file in Downloads. Download OptiFine 1.8.9 (HD U M5) from the official page first."
	}
	if err := copyFile(src, filepath.Join(modsPath(), filepath.Base(src))); err != nil {
		return err.Error()
	}
	return ""
}

func copyFile(src, dst string) error {
	b, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	if len(b) < 1_000_000 {
		return errors.New("that file looks incomplete")
	}
	os.MkdirAll(filepath.Dir(dst), 0o755)
	return os.WriteFile(dst, b, 0o644)
}
