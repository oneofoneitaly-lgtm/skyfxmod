package main

import (
	"io"
	"os"
	"os/exec"
	"strings"
	"sync"
)

// Live game output for the Console page. Keeps the last lines so the page can be opened at any time.
var (
	conMu    sync.Mutex
	conLines []string
	conSink  func(line string)
	gameProc *exec.Cmd
)

func conAdd(line string) {
	conMu.Lock()
	conLines = append(conLines, line)
	if len(conLines) > 2000 {
		conLines = conLines[len(conLines)-1500:]
	}
	sink := conSink
	conMu.Unlock()
	if sink != nil {
		sink(line)
	}
}

func conAll() string {
	conMu.Lock()
	defer conMu.Unlock()
	return strings.Join(conLines, "\n")
}

func conClear() {
	conMu.Lock()
	conLines = nil
	conMu.Unlock()
}

// lineWriter splits the game output into lines, writes them to the log file and to the console.
type lineWriter struct {
	mu   sync.Mutex
	buf  string
	file io.Writer
}

func (l *lineWriter) Write(p []byte) (int, error) {
	if l.file != nil {
		l.file.Write(p)
	}
	l.mu.Lock()
	l.buf += string(p)
	var out []string
	for {
		i := strings.IndexByte(l.buf, '\n')
		if i < 0 {
			break
		}
		out = append(out, strings.TrimRight(l.buf[:i], "\r"))
		l.buf = l.buf[i+1:]
	}
	l.mu.Unlock()
	for _, s := range out {
		conAdd(s)
	}
	return len(p), nil
}

func gameRunning() bool {
	return gameProc != nil && gameProc.ProcessState == nil && gameProc.Process != nil
}

func killGame() {
	if gameRunning() {
		gameProc.Process.Kill()
	}
}

var _ = os.Stdout
