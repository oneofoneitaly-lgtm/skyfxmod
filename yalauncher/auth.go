package main

import (
	"bytes"
	"crypto/md5"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

// Microsoft login by device code (no browser window inside the launcher, nothing to embed).
// Client id of the public Minecraft launcher application.
const msClient = "00000000402b5328"
const msScope = "service::user.auth.xboxlive.com::MBI_SSL"

type account struct {
	Type    string `json:"type"` // offline | microsoft
	Name    string `json:"name"`
	UUID    string `json:"uuid"`
	Token   string `json:"token,omitempty"`   // minecraft access token (short lived)
	Expires int64  `json:"expires,omitempty"` // unix seconds
	Refresh string `json:"refresh,omitempty"`
}

func offlineUUID(name string) string {
	h := md5.Sum([]byte("OfflinePlayer:" + name))
	h[6] = (h[6] & 0x0f) | 0x30
	h[8] = (h[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x", h[:])
}

func postForm(u string, form url.Values) (map[string]interface{}, error) {
	res, err := client.PostForm(u, form)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	b, _ := io.ReadAll(res.Body)
	m := map[string]interface{}{}
	json.Unmarshal(b, &m)
	return m, nil
}

func postJSON(u string, body interface{}, bearer string) (map[string]interface{}, int, error) {
	b, _ := json.Marshal(body)
	req, _ := http.NewRequest("POST", u, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	if bearer != "" {
		req.Header.Set("Authorization", "Bearer "+bearer)
	}
	res, err := client.Do(req)
	if err != nil {
		return nil, 0, err
	}
	defer res.Body.Close()
	rb, _ := io.ReadAll(res.Body)
	m := map[string]interface{}{}
	json.Unmarshal(rb, &m)
	return m, res.StatusCode, nil
}

func str(m map[string]interface{}, k string) string {
	s, _ := m[k].(string)
	return s
}

// msLogin runs the device code flow; show() is called with the code and the page to open.
func msLogin(show func(code, link string)) (*account, error) {
	d, err := postForm("https://login.live.com/oauth20_connect.srf", url.Values{
		"client_id": {msClient}, "scope": {msScope}, "response_type": {"device_code"}})
	if err != nil || str(d, "device_code") == "" {
		return nil, errors.New("could not start the Microsoft login")
	}
	show(str(d, "user_code"), str(d, "verification_uri"))
	interval := 5
	if v, ok := d["interval"].(float64); ok {
		interval = int(v)
	}
	deadline := time.Now().Add(14 * time.Minute)
	for time.Now().Before(deadline) {
		time.Sleep(time.Duration(interval) * time.Second)
		t, err := postForm("https://login.live.com/oauth20_token.srf", url.Values{
			"client_id": {msClient}, "device_code": {str(d, "device_code")},
			"grant_type": {"urn:ietf:params:oauth:grant-type:device_code"}})
		if err != nil {
			continue
		}
		if str(t, "access_token") != "" {
			return finishMS(str(t, "access_token"), str(t, "refresh_token"))
		}
		if e := str(t, "error"); e != "authorization_pending" && e != "" {
			return nil, errors.New("Microsoft login: " + e)
		}
	}
	return nil, errors.New("login timed out")
}

func msRefresh(refresh string) (*account, error) {
	t, err := postForm("https://login.live.com/oauth20_token.srf", url.Values{
		"client_id": {msClient}, "refresh_token": {refresh}, "grant_type": {"refresh_token"}, "scope": {msScope}})
	if err != nil || str(t, "access_token") == "" {
		return nil, errors.New("session expired, log in again")
	}
	return finishMS(str(t, "access_token"), str(t, "refresh_token"))
}

func finishMS(access, refresh string) (*account, error) {
	var xbl map[string]interface{}
	var err error
	for _, ticket := range []string{access, "t=" + access, "d=" + access} {
		var code int
		xbl, code, err = postJSON("https://user.auth.xboxlive.com/user/authenticate", map[string]interface{}{
			"Properties":   map[string]interface{}{"AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com", "RpsTicket": ticket},
			"RelyingParty": "http://auth.xboxlive.com", "TokenType": "JWT"}, "")
		if err == nil && code == 200 && str(xbl, "Token") != "" {
			break
		}
		xbl = nil
	}
	if xbl == nil {
		return nil, errors.New("Xbox login failed")
	}
	xsts, code, err := postJSON("https://xsts.auth.xboxlive.com/xsts/authorize", map[string]interface{}{
		"Properties":   map[string]interface{}{"SandboxId": "RETAIL", "UserTokens": []string{str(xbl, "Token")}},
		"RelyingParty": "rp://api.minecraftservices.com/", "TokenType": "JWT"}, "")
	if err != nil || code != 200 {
		return nil, errors.New("this Microsoft account has no Xbox profile or is blocked (XSTS)")
	}
	uhs := ""
	if dc, ok := xsts["DisplayClaims"].(map[string]interface{}); ok {
		if xs, ok := dc["xui"].([]interface{}); ok && len(xs) > 0 {
			if x0, ok := xs[0].(map[string]interface{}); ok {
				uhs = str(x0, "uhs")
			}
		}
	}
	mc, code, err := postJSON("https://api.minecraftservices.com/authentication/login_with_xbox",
		map[string]interface{}{"identityToken": "XBL3.0 x=" + uhs + ";" + str(xsts, "Token")}, "")
	if err != nil || code != 200 || str(mc, "access_token") == "" {
		return nil, errors.New("Minecraft login failed")
	}
	tok := str(mc, "access_token")
	req, _ := http.NewRequest("GET", "https://api.minecraftservices.com/minecraft/profile", nil)
	req.Header.Set("Authorization", "Bearer "+tok)
	res, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		return nil, errors.New("this Microsoft account does not own Minecraft Java")
	}
	var p struct{ ID, Name string }
	b, _ := io.ReadAll(res.Body)
	json.Unmarshal(b, &p)
	if p.ID == "" {
		return nil, errors.New("no Minecraft profile")
	}
	exp := time.Now().Add(20 * time.Hour).Unix()
	return &account{Type: "microsoft", Name: p.Name, UUID: strings.ToLower(strings.ReplaceAll(p.ID, "-", "")), Token: tok, Expires: exp, Refresh: refresh}, nil
}
