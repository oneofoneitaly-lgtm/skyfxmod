package main

import (
	"archive/zip"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

var client = &http.Client{Timeout: 5 * time.Minute}

func httpGet(url string) ([]byte, error) {
	req, _ := http.NewRequest("GET", url, nil)
	req.Header.Set("User-Agent", "YA-CLIENT-Launcher")
	res, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		return nil, fmt.Errorf("HTTP %d for %s", res.StatusCode, url)
	}
	return io.ReadAll(res.Body)
}

func getJSON(url string, v interface{}) error {
	b, err := httpGet(url)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, v)
}

func fileSha1(path string) string {
	f, err := os.Open(path)
	if err != nil {
		return ""
	}
	defer f.Close()
	h := sha1.New()
	io.Copy(h, f)
	return hex.EncodeToString(h.Sum(nil))
}

// download tries every url in turn, writes to a temp file and renames it when complete.
func download(dest string, sha string, urls ...string) error {
	if st, err := os.Stat(dest); err == nil && st.Size() > 0 && (sha == "" || fileSha1(dest) == sha) {
		return nil
	}
	os.MkdirAll(filepath.Dir(dest), 0o755)
	var last error
	for _, u := range urls {
		if u == "" {
			continue
		}
		for try := 0; try < 2; try++ {
			b, err := httpGet(u)
			if err != nil {
				last = err
				continue
			}
			tmp := dest + ".part"
			if err := os.WriteFile(tmp, b, 0o644); err != nil {
				return err
			}
			if sha != "" && fileSha1(tmp) != sha {
				last = errors.New("checksum mismatch for " + u)
				os.Remove(tmp)
				continue
			}
			return os.Rename(tmp, dest)
		}
	}
	if last == nil {
		last = errors.New("no download url")
	}
	return last
}

type job func() error

// runParallel runs the jobs with a few workers and calls tick after each one.
func runParallel(jobs []job, workers int, tick func(done, total int)) error {
	var mu sync.Mutex
	var first error
	done := 0
	ch := make(chan job)
	var wg sync.WaitGroup
	for i := 0; i < workers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := range ch {
				err := j()
				mu.Lock()
				if err != nil && first == nil {
					first = err
				}
				done++
				d := done
				mu.Unlock()
				if tick != nil {
					tick(d, len(jobs))
				}
			}
		}()
	}
	for _, j := range jobs {
		ch <- j
	}
	close(ch)
	wg.Wait()
	return first
}

func unzipTo(zipPath, dest string, filter func(name string) bool) error {
	r, err := zip.OpenReader(zipPath)
	if err != nil {
		return err
	}
	defer r.Close()
	for _, f := range r.File {
		if f.FileInfo().IsDir() || (filter != nil && !filter(f.Name)) {
			continue
		}
		target := filepath.Join(dest, filepath.FromSlash(f.Name))
		if !strings.HasPrefix(target, filepath.Clean(dest)+string(os.PathSeparator)) {
			continue // zip slip
		}
		os.MkdirAll(filepath.Dir(target), 0o755)
		rc, err := f.Open()
		if err != nil {
			return err
		}
		out, err := os.Create(target)
		if err != nil {
			rc.Close()
			return err
		}
		io.Copy(out, rc)
		out.Close()
		rc.Close()
	}
	return nil
}

func mavenPath(name string) string {
	p := strings.Split(name, ":")
	if len(p) < 3 {
		return ""
	}
	file := p[1] + "-" + p[2]
	if len(p) > 3 {
		file += "-" + p[3]
	}
	return strings.ReplaceAll(p[0], ".", "/") + "/" + p[1] + "/" + p[2] + "/" + file + ".jar"
}
