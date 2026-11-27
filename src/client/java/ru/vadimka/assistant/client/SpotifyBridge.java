package ru.vadimka.assistant.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Нулевые лаги Windows 10 Spotify Connect:
 * Вместо перезапуска powershell.exe каждые 2 секунды (что грузило CPU!),
 * держит ОДИН тихий фоновый процесс WinRT GSMTC, который:
 * - принимает мгновенные команды (TOGGLE / NEXT / PREV / VOL_UP / VOL_DOWN / POLL) через stdin
 * - отдаёт статус трека в реальном времени через stdout!
 */
public final class SpotifyBridge {
    private SpotifyBridge() {}

    private static volatile boolean connected = false;
    private static volatile boolean playing = false;
    private static volatile String title = "Ожидание Spotify...";
    private static volatile String artist = "Включи песню на ПК";
    private static volatile String source = "Windows Media";
    private static volatile int posSec = 0;
    private static volatile int durSec = 0;

    private static volatile boolean started = false;
    private static volatile Process daemonProc = null;
    private static volatile BufferedWriter daemonStdin = null;
    private static final Object WRITE_LOCK = new Object();

    public static boolean isConnected() { return connected; }
    public static boolean isPlaying() { return playing; }
    public static String getTitle() { return title; }
    public static String getArtist() { return artist; }
    public static String getSource() { return source; }
    public static int getPosSec() { return posSec; }
    public static int getDurSec() { return durSec; }

    private static final String DAEMON_PS =
        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n"
        + "[Console]::InputEncoding = [System.Text.Encoding]::UTF8\n"
        + "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n"
        + "Add-Type -TypeDefinition 'using System.Runtime.InteropServices; public class VK { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo); }'\n"
        + "$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n"
        + "function Await($op, $type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(1200) | Out-Null; return $t.Result }\n"
        + "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null\n"
        + "$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n"
        + "function GetTarget() {\n"
        + "  if (-not $mgr) { return $null }\n"
        + "  foreach ($s in $mgr.GetSessions()) { if ($s.SourceAppUserModelId -match 'Spotify') { return $s } }\n"
        + "  return $mgr.GetCurrentSession()\n"
        + "}\n"
        + "function EmitState() {\n"
        + "  try {\n"
        + "    $target = GetTarget\n"
        + "    if ($target) {\n"
        + "      $props = Await ($target.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n"
        + "      $pb = $target.GetPlaybackInfo()\n"
        + "      $tl = $target.GetTimelineProperties()\n"
        + "      $t = if ($props -and $props.Title) { $props.Title.Replace('|',' ') } else { '' }\n"
        + "      $a = if ($props -and $props.Artist) { $props.Artist.Replace('|',' ') } else { 'Spotify' }\n"
        + "      $st = if ($pb -and $pb.PlaybackStatus -eq 'Playing') { 'PLAYING' } else { 'PAUSED' }\n"
        + "      $p = if ($tl) { [int]$tl.Position.TotalSeconds } else { 0 }\n"
        + "      $d = if ($tl) { [int]$tl.EndTime.TotalSeconds } else { 0 }\n"
        + "      $src = if ($target.SourceAppUserModelId) { $target.SourceAppUserModelId.Replace('|',' ') } else { 'Spotify' }\n"
        + "      if ($t.Length -gt 0) { Write-Output \"STATE|$st|$a|$t|$p|$d|$src\"; return }\n"
        + "    }\n"
        + "  } catch {}\n"
        + "  Write-Output 'NONE||||0|0|'\n"
        + "}\n"
        + "EmitState\n"
        + "while ($true) {\n"
        + "  $cmd = [Console]::In.ReadLine()\n"
        + "  if ($null -eq $cmd) { break }\n"
        + "  $c = $cmd.Trim().ToUpper()\n"
        + "  try {\n"
        + "    $target = GetTarget\n"
        + "    if ($c -eq 'TOGGLE') { if ($target) { $target.TryTogglePlayPauseAsync() | Out-Null } else { [VK]::keybd_event(0xB3,0,0,0); [VK]::keybd_event(0xB3,0,2,0) }; Start-Sleep -Milliseconds 180 }\n"
        + "    elseif ($c -eq 'NEXT') { if ($target) { $target.TrySkipNextAsync() | Out-Null } else { [VK]::keybd_event(0xB0,0,0,0); [VK]::keybd_event(0xB0,0,2,0) }; Start-Sleep -Milliseconds 220 }\n"
        + "    elseif ($c -eq 'PREV') { if ($target) { $target.TrySkipPreviousAsync() | Out-Null } else { [VK]::keybd_event(0xB1,0,0,0); [VK]::keybd_event(0xB1,0,2,0) }; Start-Sleep -Milliseconds 220 }\n"
        + "    elseif ($c -eq 'VOL_UP') { [VK]::keybd_event(0xAF,0,0,0); [VK]::keybd_event(0xAF,0,2,0) }\n"
        + "    elseif ($c -eq 'VOL_DOWN') { [VK]::keybd_event(0xAE,0,0,0); [VK]::keybd_event(0xAE,0,2,0) }\n"
        + "  } catch {}\n"
        + "  EmitState\n"
        + "}\n";

    public static void ensureStarted() {
        if (started) return;
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return;
        started = true;

        Thread reader = new Thread(() -> {
            try {
                String b64 = Base64.getEncoder().encodeToString(DAEMON_PS.getBytes(StandardCharsets.UTF_16LE));
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-EncodedCommand", b64
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                daemonProc = proc;
                daemonStdin = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));

                // Авто-тикер: раз в 1.5с отправляет легчайшую строку "POLL" в уже открытый процесс (0 спавна!)
                Thread poller = new Thread(() -> {
                    while (proc.isAlive()) {
                        try {
                            Thread.sleep(1500);
                            sendCommand("POLL");
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }, "Vadimka-Spotify-Ticker");
                poller.setDaemon(true);
                poller.start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        parseStateLine(line.trim());
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                connected = false;
                playing = false;
                started = false;
            }
        }, "Vadimka-Spotify-Daemon");
        reader.setDaemon(true);
        reader.start();
    }

    public static void sendCommand(String cmd) {
        ensureStarted();
        synchronized (WRITE_LOCK) {
            try {
                if (daemonStdin != null && daemonProc != null && daemonProc.isAlive()) {
                    daemonStdin.write(cmd);
                    daemonStdin.newLine();
                    daemonStdin.flush();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void parseStateLine(String line) {
        if (line.startsWith("STATE|")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 7) {
                connected = true;
                playing = "PLAYING".equals(p[1]);
                artist = p[2].isEmpty() ? "Spotify" : p[2];
                title = p[3].isEmpty() ? "Без названия" : p[3];
                posSec = parseInt(p[4]);
                durSec = parseInt(p[5]);
                source = p[6].toLowerCase(Locale.ROOT).contains("spotify") ? "Spotify (ПК)" : "Медиа (ПК)";
            }
        } else if (line.startsWith("NONE|")) {
            connected = false;
            playing = false;
            title = "Spotify на паузе / не играет";
            artist = "Включи песню на ПК";
            posSec = 0;
            durSec = 0;
        }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Throwable ignored) { return 0; }
    }
}
