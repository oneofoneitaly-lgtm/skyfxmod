package main

import (
	"bytes"
	"encoding/binary"
	"encoding/json"
	"errors"
	"io"
	"net"
	"strconv"
	"strings"
	"time"
)

func writeVarInt(b *bytes.Buffer, v int) {
	u := uint32(v)
	for {
		if u&^0x7f == 0 {
			b.WriteByte(byte(u))
			return
		}
		b.WriteByte(byte(u&0x7f | 0x80))
		u >>= 7
	}
}

func readVarInt(r io.ByteReader) (int, error) {
	n, shift := 0, uint(0)
	for i := 0; i < 5; i++ {
		c, err := r.ReadByte()
		if err != nil {
			return 0, err
		}
		n |= int(c&0x7f) << shift
		if c&0x80 == 0 {
			return n, nil
		}
		shift += 7
	}
	return 0, errors.New("varint too long")
}

// pingServer asks a Java server for its status (the same request the server list sends).
func pingServer(addr string) map[string]interface{} {
	out := map[string]interface{}{"host": addr, "online": false}
	host, port := addr, 25565
	if i := strings.LastIndex(addr, ":"); i > 0 {
		if p, err := strconv.Atoi(addr[i+1:]); err == nil {
			host, port = addr[:i], p
		}
	}
	start := time.Now()
	conn, err := net.DialTimeout("tcp", net.JoinHostPort(host, strconv.Itoa(port)), 4*time.Second)
	if err != nil {
		return out
	}
	defer conn.Close()
	conn.SetDeadline(time.Now().Add(5 * time.Second))

	var hs bytes.Buffer
	writeVarInt(&hs, 0)
	writeVarInt(&hs, 47)
	writeVarInt(&hs, len(host))
	hs.WriteString(host)
	binary.Write(&hs, binary.BigEndian, uint16(port))
	writeVarInt(&hs, 1)
	var pkt bytes.Buffer
	writeVarInt(&pkt, hs.Len())
	pkt.Write(hs.Bytes())
	pkt.Write([]byte{1, 0}) // status request
	if _, err := conn.Write(pkt.Bytes()); err != nil {
		return out
	}
	br := &byteReader{c: conn}
	if _, err := readVarInt(br); err != nil {
		return out
	}
	if id, err := readVarInt(br); err != nil || id != 0 {
		return out
	}
	n, err := readVarInt(br)
	if err != nil || n <= 0 || n > 1<<20 {
		return out
	}
	buf := make([]byte, n)
	if _, err := io.ReadFull(br, buf); err != nil {
		return out
	}
	var st struct {
		Players struct{ Online, Max int } `json:"players"`
		Desc    json.RawMessage           `json:"description"`
		Favicon string                    `json:"favicon"`
		Version struct{ Name string }     `json:"version"`
	}
	if json.Unmarshal(buf, &st) != nil {
		return out
	}
	out["online"] = true
	out["players"] = st.Players.Online
	out["max"] = st.Players.Max
	out["ms"] = int(time.Since(start) / time.Millisecond)
	out["motd"] = motdText(st.Desc)
	out["icon"] = st.Favicon
	out["version"] = st.Version.Name
	return out
}

type byteReader struct{ c net.Conn }

func (b *byteReader) ReadByte() (byte, error) {
	var x [1]byte
	_, err := io.ReadFull(b.c, x[:])
	return x[0], err
}

func (b *byteReader) Read(p []byte) (int, error) { return b.c.Read(p) }

func motdText(raw json.RawMessage) string {
	var s string
	if json.Unmarshal(raw, &s) == nil {
		return stripCodes(s)
	}
	var o struct {
		Text  string            `json:"text"`
		Extra []json.RawMessage `json:"extra"`
	}
	if json.Unmarshal(raw, &o) == nil {
		t := o.Text
		for _, e := range o.Extra {
			t += motdText(e)
		}
		return stripCodes(t)
	}
	return ""
}

func stripCodes(s string) string {
	var b strings.Builder
	rs := []rune(s)
	for i := 0; i < len(rs); i++ {
		if rs[i] == '§' {
			i++
			continue
		}
		b.WriteRune(rs[i])
	}
	return strings.TrimSpace(strings.ReplaceAll(b.String(), "\n", " "))
}
