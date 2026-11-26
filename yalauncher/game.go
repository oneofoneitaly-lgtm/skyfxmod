package main

import (
	"time"
	"archive/zip"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"syscall"
)

const mcVersion = "1.8.9"
const forgeVersion = "1.8.9-11.15.1.2318-1.8.9"
const forgeInstaller = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + forgeVersion + "/forge-" + forgeVersion + "-installer.jar"

type progressFn func(text string, frac float64)

type rule struct {
	Action string `json:"action"`
	OS     *struct {
		Name string `json:"name"`
	} `json:"os"`
}

type artifact struct {
	Path string `json:"path"`
	URL  string `json:"url"`
	Sha1 string `json:"sha1"`
}

type library struct {
	Name      string `json:"name"`
	URL       string `json:"url"`
	ClientReq *bool  `json:"clientreq"`
	Rules     []rule `json:"rules"`
	Natives   map[string]string `json:"natives"`
	Downloads *struct {
		Artifact    *artifact           `json:"artifact"`
		Classifiers map[string]artifact `json:"classifiers"`
	} `json:"downloads"`
}

type versionJSON struct {
	AssetIndex struct {
		ID, URL, Sha1 string
	} `json:"assetIndex"`
	Assets    string `json:"assets"`
	Downloads struct {
		Client struct{ URL, Sha1 string } `json:"client"`
	} `json:"downloads"`
	Libraries []library `json:"libraries"`
	MainClass string    `json:"mainClass"`
	Args      string    `json:"minecraftArguments"`
}

func allowed(rules []rule) bool {
	if len(rules) == 0 {
		return true
	}
	ok := false
	for _, r := range rules {
		match := r.OS == nil || r.OS.Name == "windows"
		if match {
			ok = r.Action == "allow"
		}
	}
	return ok
}

func gameDirs() (game, libs, assets, natives string) {
	game = filepath.Join(dataDir, "game")
	return game, filepath.Join(game, "libraries"), filepath.Join(game, "assets"), filepath.Join(game, "natives")
}

// ensureJava returns the path of a Java 8 javaw.exe (downloads a small Temurin JRE when none is installed).
func ensureJava(p progressFn) (string, error) {
	own := findJavaw(filepath.Join(dataDir, "java8"))
	if own != "" {
		return own, nil
	}
	var cands []string
	for _, root := range []string{os.Getenv("ProgramFiles"), os.Getenv("ProgramFiles(x86)")} {
		for _, sub := range []string{"Java", "Eclipse Adoptium", "Amazon Corretto", "Zulu", "Microsoft"} {
			m, _ := filepath.Glob(filepath.Join(root, sub, "*1.8*"))
			cands = append(cands, m...)
			m, _ = filepath.Glob(filepath.Join(root, sub, "*jdk-8*"))
			cands = append(cands, m...)
			m, _ = filepath.Glob(filepath.Join(root, sub, "*jre-8*"))
			cands = append(cands, m...)
			m, _ = filepath.Glob(filepath.Join(root, sub, "*8.0*"))
			cands = append(cands, m...)
		}
	}
	if jh := os.Getenv("JAVA_HOME"); jh != "" && strings.Contains(jh, "8") {
		cands = append(cands, jh)
	}
	for _, c := range cands {
		if w := findJavaw(c); w != "" {
			return w, nil
		}
	}
	p("Downloading Java 8 (one time, about 40 MB)...", 0.02)
	zipPath := filepath.Join(dataDir, "cache", "java8.zip")
	if err := download(zipPath, "", "https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"); err != nil {
		return "", errors.New("could not download Java 8: " + err.Error())
	}
	if err := unzipTo(zipPath, filepath.Join(dataDir, "java8"), nil); err != nil {
		return "", err
	}
	if w := findJavaw(filepath.Join(dataDir, "java8")); w != "" {
		return w, nil
	}
	return "", errors.New("Java 8 not found after download")
}

func findJavaw(root string) string {
	var found string
	filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil || found != "" {
			return nil
		}
		if !info.IsDir() && strings.EqualFold(info.Name(), "javaw.exe") {
			found = path
		}
		return nil
	})
	return found
}

type installed struct {
	classpath []string
	mainClass string
	args      string
	assets    string
	natives   string
}

// install makes sure Minecraft 1.8.9 and Forge are on disk and returns what is needed to start them.
func install(p progressFn) (*installed, error) {
	game, libsDir, assetsDir, nativesDir := gameDirs()
	os.MkdirAll(game, 0o755)

	p("Reading Minecraft versions...", 0.05)
	var manifest struct {
		Versions []struct{ ID, URL string } `json:"versions"`
	}
	if err := getJSON("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", &manifest); err != nil {
		// offline: use the copy from the last run
		if b, e := os.ReadFile(filepath.Join(game, "versions", mcVersion, mcVersion+".json")); e == nil {
			manifest.Versions = nil
			_ = b
		} else {
			return nil, errors.New("no internet connection")
		}
	}
	vjPath := filepath.Join(game, "versions", mcVersion, mcVersion+".json")
	for _, v := range manifest.Versions {
		if v.ID == mcVersion {
			if err := download(vjPath, "", v.URL); err != nil {
				return nil, err
			}
		}
	}
	var vj versionJSON
	b, err := os.ReadFile(vjPath)
	if err != nil {
		return nil, errors.New("Minecraft 1.8.9 data missing")
	}
	json.Unmarshal(b, &vj)

	p("Minecraft 1.8.9 game file...", 0.08)
	clientJar := filepath.Join(game, "versions", mcVersion, mcVersion+".jar")
	if err := download(clientJar, vj.Downloads.Client.Sha1, vj.Downloads.Client.URL); err != nil {
		return nil, err
	}

	// ---- libraries (vanilla)
	classpath := map[string]string{} // key group:artifact -> path
	var order []string
	var jobs []job
	var nativeJars []string
	add := func(name, path string) {
		parts := strings.Split(name, ":")
		key := name
		if len(parts) >= 2 {
			key = parts[0] + ":" + parts[1]
		}
		if _, ok := classpath[key]; !ok {
			order = append(order, key)
		}
		classpath[key] = path
	}
	for _, l := range vj.Libraries {
		l := l
		if !allowed(l.Rules) {
			continue
		}
		if l.Natives != nil {
			cls := strings.ReplaceAll(l.Natives["windows"], "${arch}", "64")
			if cls != "" && l.Downloads != nil {
				if a, ok := l.Downloads.Classifiers[cls]; ok {
					dest := filepath.Join(libsDir, filepath.FromSlash(a.Path))
					nativeJars = append(nativeJars, dest)
					jobs = append(jobs, func() error { return download(dest, a.Sha1, a.URL) })
				}
			}
			continue
		}
		if l.Downloads != nil && l.Downloads.Artifact != nil {
			a := l.Downloads.Artifact
			dest := filepath.Join(libsDir, filepath.FromSlash(a.Path))
			add(l.Name, dest)
			jobs = append(jobs, func() error { return download(dest, a.Sha1, a.URL) })
		}
	}

	// ---- Forge
	p("Forge...", 0.1)
	installer := filepath.Join(dataDir, "cache", "forge-"+forgeVersion+"-installer.jar")
	if err := download(installer, "", forgeInstaller); err != nil {
		return nil, errors.New("could not download Forge: " + err.Error())
	}
	zr, err := zip.OpenReader(installer)
	if err != nil {
		os.Remove(installer)
		return nil, errors.New("Forge installer is damaged, try again")
	}
	defer zr.Close()
	var profile struct {
		Install struct {
			Path     string `json:"path"`
			FilePath string `json:"filePath"`
		} `json:"install"`
		VersionInfo struct {
			MainClass string    `json:"mainClass"`
			Args      string    `json:"minecraftArguments"`
			Libraries []library `json:"libraries"`
		} `json:"versionInfo"`
	}
	for _, f := range zr.File {
		if f.Name == "install_profile.json" {
			rc, _ := f.Open()
			pb, _ := io.ReadAll(rc)
			rc.Close()
			json.Unmarshal(pb, &profile)
		}
	}
	if profile.Install.Path == "" {
		return nil, errors.New("Forge profile not understood")
	}
	forgeJar := filepath.Join(libsDir, filepath.FromSlash(mavenPath(profile.Install.Path)))
	if _, err := os.Stat(forgeJar); err != nil {
		for _, f := range zr.File {
			if f.Name == profile.Install.FilePath {
				os.MkdirAll(filepath.Dir(forgeJar), 0o755)
				rc, _ := f.Open()
				out, e := os.Create(forgeJar)
				if e != nil {
					return nil, e
				}
				io.Copy(out, rc)
				out.Close()
				rc.Close()
			}
		}
	}
	for _, l := range profile.VersionInfo.Libraries {
		l := l
		if strings.HasPrefix(l.Name, "net.minecraftforge:forge:") || (l.ClientReq != nil && !*l.ClientReq) {
			continue
		}
		rel := mavenPath(l.Name)
		dest := filepath.Join(libsDir, filepath.FromSlash(rel))
		var urls []string
		if l.URL != "" {
			base := strings.Replace(l.URL, "http://", "https://", 1)
			urls = append(urls, strings.TrimRight(base, "/")+"/"+rel)
		}
		urls = append(urls, "https://libraries.minecraft.net/"+rel, "https://maven.minecraftforge.net/"+rel, "https://repo1.maven.org/maven2/"+rel)
		add(l.Name, dest)
		jobs = append(jobs, func() error { return download(dest, "", urls...) })
	}

	p("Downloading libraries...", 0.15)
	if err := runParallel(jobs, 8, func(d, t int) { p(fmt.Sprintf("Libraries %d/%d", d, t), 0.15+0.25*float64(d)/float64(t)) }); err != nil {
		return nil, err
	}

	// ---- natives
	os.RemoveAll(nativesDir)
	os.MkdirAll(nativesDir, 0o755)
	for _, nj := range nativeJars {
		unzipTo(nj, nativesDir, func(n string) bool { return !strings.HasPrefix(n, "META-INF") && strings.HasSuffix(n, ".dll") })
	}

	// ---- assets
	p("Sounds and textures...", 0.4)
	idx := filepath.Join(assetsDir, "indexes", vj.AssetIndex.ID+".json")
	if err := download(idx, vj.AssetIndex.Sha1, vj.AssetIndex.URL); err != nil {
		return nil, err
	}
	var ai struct {
		Objects map[string]struct {
			Hash string `json:"hash"`
		} `json:"objects"`
	}
	ib, _ := os.ReadFile(idx)
	json.Unmarshal(ib, &ai)
	var ajobs []job
	seen := map[string]bool{}
	for _, o := range ai.Objects {
		h := o.Hash
		if seen[h] {
			continue
		}
		seen[h] = true
		dest := filepath.Join(assetsDir, "objects", h[:2], h)
		if st, err := os.Stat(dest); err == nil && st.Size() > 0 {
			continue
		}
		ajobs = append(ajobs, func() error {
			return download(dest, h, "https://resources.download.minecraft.net/"+h[:2]+"/"+h)
		})
	}
	if len(ajobs) > 0 {
		if err := runParallel(ajobs, 16, func(d, t int) { p(fmt.Sprintf("Assets %d/%d", d, t), 0.4+0.5*float64(d)/float64(t)) }); err != nil {
			return nil, err
		}
	}

	var cp []string
	for _, k := range order {
		cp = append(cp, classpath[k])
	}
	cp = append(cp, forgeJar, clientJar)
	args := profile.VersionInfo.Args
	if args == "" {
		args = vj.Args
	}
	main := profile.VersionInfo.MainClass
	if main == "" {
		main = "net.minecraft.launchwrapper.Launch"
	}
	return &installed{classpath: cp, mainClass: main, args: args, assets: assetsDir, natives: nativesDir}, nil
}

// syncMods puts the enabled mods into <game>/mods and removes the disabled ones.
func syncMods(p progressFn, st *state) error {
	game, _, _, _ := gameDirs()
	modsDir := filepath.Join(game, "mods")
	os.MkdirAll(modsDir, 0o755)
	for i, m := range modCatalog {
		on := st.Mods[m.ID]
		dest := filepath.Join(modsDir, m.File)
		if !on {
			os.Remove(dest)
			continue
		}
		if repo == "" {
			if _, err := os.Stat(dest); err != nil {
				return errors.New("mods repository not set in this launcher build")
			}
			continue
		}
		p("Updating "+m.Name+"...", 0.92+0.06*float64(i)/float64(len(modCatalog)))
		u := "https://github.com/" + repo + "/releases/download/latest/" + m.File
		b, err := httpGet(u)
		if err != nil {
			if _, e := os.Stat(dest); e == nil {
				continue // keep the old copy when offline
			}
			return errors.New("could not download " + m.Name)
		}
		if err := os.WriteFile(dest, b, 0o644); err != nil {
			return err
		}
	}
	return nil
}

func launch(p progressFn, st *state) error {
	acc := st.Account
	if acc == nil {
		return errors.New("log in first")
	}
	if acc.Type == "microsoft" && time.Now().Unix() > acc.Expires-300 {
		p("Refreshing Microsoft login...", 0.01)
		na, err := msRefresh(acc.Refresh)
		if err != nil {
			return err
		}
		st.Account = na
		acc = na
		st.save()
	}
	var java string
	var err error
	if st.JavaPath != "" {
		if _, e := os.Stat(st.JavaPath); e != nil {
			return errors.New("the Java path in Settings does not exist")
		}
		java = st.JavaPath
	} else if java, err = ensureJava(p); err != nil {
		return err
	}
	in, err := install(p)
	if err != nil {
		return err
	}
	if err := syncMods(p, st); err != nil {
		return err
	}
	game, _, _, _ := gameDirs()
	token, utype := "0", "legacy"
	if acc.Type == "microsoft" {
		token, utype = acc.Token, "msa"
	}
	rep := strings.NewReplacer(
		"${auth_player_name}", acc.Name, "${version_name}", "1.8.9-forge", "${game_directory}", game,
		"${assets_root}", in.assets, "${assets_index_name}", "1.8", "${auth_uuid}", acc.UUID,
		"${auth_access_token}", token, "${user_properties}", "{}", "${user_type}", utype, "${auth_session}", token)
	gameArgs := strings.Fields(rep.Replace(in.args))
	if !strings.Contains(in.args, "--assetIndex") {
		gameArgs = append(gameArgs, "--assetIndex", "1.8")
	}
	if !strings.Contains(in.args, "--userType") {
		gameArgs = append(gameArgs, "--userType", utype)
	}
	ram := st.RAM
	if ram < 1024 {
		ram = 2048
	}
	jvm := []string{
		fmt.Sprintf("-Xmx%dM", ram), "-Xms512M",
		"-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:G1NewSizePercent=20", "-XX:G1ReservePercent=20", "-XX:MaxGCPauseMillis=50",
		"-Djava.library.path=" + in.natives, "-Dminecraft.launcher.brand=YA-CLIENT",
		"-cp", strings.Join(in.classpath, ";"), in.mainClass,
	}
	if st.Fullscreen {
		gameArgs = append(gameArgs, "--fullscreen")
	}
	if st.Width >= 400 && st.Height >= 300 && !st.Fullscreen {
		gameArgs = append(gameArgs, "--width", fmt.Sprint(st.Width), "--height", fmt.Sprint(st.Height))
	}
	if st.Join != "" {
		host, port := st.Join, "25565"
		if i := strings.LastIndex(host, ":"); i > 0 {
			host, port = host[:i], host[i+1:]
		}
		gameArgs = append(gameArgs, "--server", host, "--port", port)
	}
	if st.JVM != "" {
		jvm = append(strings.Fields(st.JVM), jvm...)
	}
	cmd := exec.Command(java, append(jvm, gameArgs...)...)
	cmd.Dir = game
	if runtime.GOOS == "windows" {
		cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
	}
	logf, _ := os.Create(filepath.Join(dataDir, "last-launch.log"))
	conClear()
	conAdd("[launcher] Starting Minecraft 1.8.9 with " + java)
	lw := &lineWriter{}
	if logf != nil {
		lw.file = logf
	}
	cmd.Stdout = lw
	cmd.Stderr = lw
	p("Starting Minecraft...", 0.99)
	if err := cmd.Start(); err != nil {
		return err
	}
	gameProc = cmd
	go func() {
		err := cmd.Wait()
		if logf != nil {
			logf.Close()
		}
		if err != nil {
			conAdd("[launcher] Game closed: " + err.Error())
		} else {
			conAdd("[launcher] Game closed.")
		}
		if conSink != nil {
			conSink("\x00exit")
		}
	}()
	return nil
}
