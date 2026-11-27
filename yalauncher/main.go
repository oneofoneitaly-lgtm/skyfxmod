package main

import (
	_ "embed"
	"encoding/base64"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"sync"
	"time"

	webview2 "github.com/jchv/go-webview2"
)

//go:embed ui/index.html
var indexHTML string

//go:embed ui/logo.png
var logoPNG []byte

// set at build time: -X main.repo=owner/name (the GitHub repository that publishes the mod jars)
var repo = ""

var dataDir string

type modInfo struct{ ID, Name, Desc, File, Loader string }

// forLoader: Forge mods only load with Forge, Fabric mods only with Fabric ("" = Forge, the default).
func (m modInfo) forLoader(l string) bool {
	if l == "fabric" {
		return m.Loader == "fabric"
	}
	return m.Loader != "fabric"
}

var modCatalog = []modInfo{
	{"yamods", "YA MODS", "112 PvP, HUD and utility modules. Right Shift opens the menu.", "YAMods.jar", ""},
	{"yamenu", "YA MENU", "YA CLIENT style for the title screen, singleplayer, multiplayer and pause menu.", "YAMenu.jar", ""},
	{"skyfx", "SkyFX", "Custom animated skies.", "SkyFX.jar", ""},
	{"yafabric", "YA CLIENT (Fabric)", "The YA CLIENT mods for the Fabric loader (test build: YA home screen so far).", "YAFabric.jar", "fabric"},
}

type server struct {
	Name string `json:"name"`
	Host string `json:"host"`
}

type state struct {
	Account     *account        `json:"account"`
	RAM         int             `json:"ram"`
	Mods        map[string]bool `json:"mods"`
	Servers     []server        `json:"servers"`
	Join        string          `json:"join"`
	JavaPath    string          `json:"javaPath"`
	JVM         string          `json:"jvm"`
	CloseOnPlay bool            `json:"closeOnPlay"`
	Fullscreen  bool            `json:"fullscreen"`
	Loader      string          `json:"loader"`
	Configured  bool            `json:"configured"`
	Width       int             `json:"width"`
	Height      int             `json:"height"`
	mu          sync.Mutex
}

func (s *state) save() {
	s.mu.Lock()
	defer s.mu.Unlock()
	b, _ := json.MarshalIndent(s, "", "  ")
	os.WriteFile(filepath.Join(dataDir, "state.json"), b, 0o644)
}

func loadState() *state {
	s := &state{RAM: 2048, Mods: map[string]bool{"yamods": true, "yamenu": true}}
	if b, err := os.ReadFile(filepath.Join(dataDir, "state.json")); err == nil {
		json.Unmarshal(b, s)
	}
	if s.Mods == nil {
		s.Mods = map[string]bool{}
	}
	return s
}

func main() {
	cfg, _ := os.UserConfigDir()
	dataDir = filepath.Join(cfg, ".yaclient")
	os.MkdirAll(filepath.Join(dataDir, "cache"), 0o755)
	cleanOldExe()
	if checkUpdate(func(string) {}) {
		return
	}
	st := loadState()

	w := webview2.NewWithOptions(webview2.WebViewOptions{
		Debug:     false,
		AutoFocus: true,
		WindowOptions: webview2.WindowOptions{
			Title: "YA Client", Width: 940, Height: 640, Center: true,
		},
	})
	if w == nil {
		os.Exit(1)
	}
	defer w.Destroy()
	w.SetSize(940, 640, webview2.HintFixed)

	emit := func(event string, v interface{}) {
		b, _ := json.Marshal(v)
		w.Dispatch(func() { w.Eval("window.onEvent && window.onEvent(" + jsString(event) + "," + string(b) + ")") })
	}
	conSink = func(line string) { emit("console", line) }
	w.Bind("getConsole", func() string { return conAll() })
	w.Bind("clearConsole", func() { conClear() })
	w.Bind("killGame", func() { killGame() })
	w.Bind("finishSetup", func() { st.Configured = true; st.save() })
	busy := false
	var busyMu sync.Mutex

	w.Bind("getState", func() string {
		mods := []map[string]interface{}{}
		for _, m := range modCatalog {
			if !m.forLoader(st.Loader) {
				continue
			}
			mods = append(mods, map[string]interface{}{"id": m.ID, "name": m.Name, "desc": m.Desc, "on": st.Mods[m.ID]})
		}
		g, _, _, _ := gameDirs()
		ext := []string{}
		if es, err := os.ReadDir(filepath.Join(g, "mods")); err == nil {
			for _, e := range es {
				n := e.Name()
				if strings.HasSuffix(strings.ToLower(n), ".jar") && !isOurJar(n) {
					ext = append(ext, n)
				}
			}
		}
		out := map[string]interface{}{
			"account": st.Account, "ram": st.RAM, "mods": mods, "join": st.Join,
			"javaPath": st.JavaPath, "jvm": st.JVM, "closeOnPlay": st.CloseOnPlay, "fullscreen": st.Fullscreen,
			"width": st.Width, "height": st.Height, "configured": st.Configured, "loader": st.Loader, "version": version, "running": gameRunning(), "external": ext, "dataDir": dataDir,
			"logo": "data:image/png;base64," + base64.StdEncoding.EncodeToString(logoPNG), "repo": repo,
		}
		b, _ := json.Marshal(out)
		return string(b)
	})
	w.Bind("loginOffline", func(name string) string {
		name = regexp.MustCompile(`[^A-Za-z0-9_]`).ReplaceAllString(name, "")
		if len(name) > 16 {
			name = name[:16]
		}
		if len(name) < 3 {
			return "Name must have 3 to 16 letters, numbers or _"
		}
		st.Account = &account{Type: "offline", Name: name, UUID: offlineUUID(name)}
		st.save()
		return ""
	})
	w.Bind("loginMicrosoft", func() {
		go func() {
			a, err := msLogin(func(code, link string) { emit("code", map[string]string{"code": code, "link": link}) })
			if err != nil {
				emit("loginError", err.Error())
				return
			}
			st.Account = a
			st.save()
			emit("loggedIn", a.Name)
		}()
	})
	w.Bind("logout", func() { st.Account = nil; st.save() })
	w.Bind("setMod", func(id string, on bool) { st.Mods[id] = on; st.save() })
	w.Bind("setLoader", func(l string) {
		if l != "fabric" {
			l = "forge"
		}
		st.Loader = l
		if l == "fabric" {
			st.Mods["yafabric"] = true
		}
		st.save()
	})
	w.Bind("setRam", func(mb int) { st.RAM = mb; st.save() })
	w.Bind("saveSettings", func(js string) {
		var v struct {
			JavaPath    string `json:"javaPath"`
			JVM         string `json:"jvm"`
			CloseOnPlay bool   `json:"closeOnPlay"`
			Fullscreen  bool   `json:"fullscreen"`
			Width       int    `json:"width"`
			Height      int    `json:"height"`
		}
		if json.Unmarshal([]byte(js), &v) == nil {
			st.JavaPath, st.JVM, st.CloseOnPlay, st.Fullscreen, st.Width, st.Height = strings.TrimSpace(v.JavaPath), strings.TrimSpace(v.JVM), v.CloseOnPlay, v.Fullscreen, v.Width, v.Height
			st.save()
		}
	})
	// news and partner servers are files in the GitHub repo: only the owner can change them
	w.Bind("getRemote", func(name string) string {
		if name != "news" && name != "servers" {
			return "[]"
		}
		if repo == "" {
			return "[]"
		}
		bts, err := httpGet("https://raw.githubusercontent.com/" + repo + "/HEAD/data/" + name + ".json?t=" + time.Now().Format("20060102150405"))
		if err != nil || !json.Valid(bts) {
			return "[]"
		}
		return string(bts)
	})
	w.Bind("setJoin", func(host string) { st.Join = host; st.save() })
	w.Bind("pingServer", func(host string) {
		go func() { emit("ping", pingServer(host)) }()
	})
	w.Bind("getLog", func() string {
		b, err := os.ReadFile(filepath.Join(dataDir, "last-launch.log"))
		if err != nil {
			return "No log yet. Press Play once."
		}
		lines := strings.Split(string(b), "\n")
		if len(lines) > 250 {
			lines = lines[len(lines)-250:]
		}
		return strings.Join(lines, "\n")
	})
	w.Bind("optifineState", func() string { return optifineState() })
	w.Bind("installOptifine", func() string { return installOptifine() })
	w.Bind("openMods", func() {
		g, _, _, _ := gameDirs()
		os.MkdirAll(filepath.Join(g, "mods"), 0o755)
		exec.Command("explorer", filepath.Join(g, "mods")).Start()
	})
	w.Bind("openLink", func(u string) {
		if strings.HasPrefix(u, "https://") {
			exec.Command("rundll32", "url.dll,FileProtocolHandler", u).Start()
		}
	})
	w.Bind("openFolder", func() {
		g, _, _, _ := gameDirs()
		exec.Command("explorer", g).Start()
	})
	w.Bind("play", func() {
		busyMu.Lock()
		if busy {
			busyMu.Unlock()
			return
		}
		busy = true
		busyMu.Unlock()
		go func() {
			err := launch(func(text string, frac float64) { emit("progress", map[string]interface{}{"text": text, "frac": frac}) }, st)
			busyMu.Lock()
			busy = false
			busyMu.Unlock()
			if err != nil {
				emit("playError", err.Error())
			} else {
				if gameRunning() {
					emit("started", "")
				}
				if st.CloseOnPlay && gameRunning() {
					time.Sleep(2 * time.Second)
					w.Dispatch(func() { w.Terminate() })
				}
			}
		}()
	})

	w.SetHtml(indexHTML)
	w.Run()
}

func jsString(s string) string {
	b, _ := json.Marshal(s)
	return string(b)
}

func isOurJar(name string) bool {
	for _, m := range modCatalog {
		if strings.EqualFold(m.File, name) {
			return true
		}
	}
	return false
}
