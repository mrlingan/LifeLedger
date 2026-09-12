<#
    把播好的演示数据推回模拟器。

        pwsh -File scripts/screenshots/push_demo.ps1 -Db <db> -Prefs <xml> -WithMedia

    - 只会对着 -Serial 指定的设备操作（默认 emulator-5554），不会碰真机
    - 先 force-stop 应用，再写文件，最后删掉旧的 WAL/SHM：
      不删的话 Room 可能读到"半新半旧"的库
    - 写完把数据库读回来，用 inspect_db.py 打印行数，确认真的写进去了
#>
param(
    [string]$Serial = 'emulator-5554',
    [string]$Package = 'com.Anchored.mylife',
    [string]$Db = '',
    [string]$Prefs = '',
    [switch]$WithMedia,
    [string]$Adb = '',
    [string]$Python = 'py',
    [string]$WorkDir = ''
)

$ErrorActionPreference = 'Stop'
if (-not $WorkDir) { $WorkDir = Join-Path $PSScriptRoot 'work' }
if (-not $Db) { $Db = Join-Path $WorkDir 'db\zh.db' }
if (-not $Prefs) { $Prefs = Join-Path $WorkDir 'prefs.xml' }

function Resolve-Adb {
    param([string]$Explicit)
    if ($Explicit) { return $Explicit }
    foreach ($root in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if ($root) {
            $candidate = Join-Path $root 'platform-tools\adb.exe'
            if (Test-Path $candidate) { return $candidate }
        }
    }
    if ($env:LOCALAPPDATA) {
        # Android Studio 默认把 SDK 装在这里
        $candidate = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
        if (Test-Path $candidate) { return $candidate }
    }
    $onPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    throw '找不到 adb：把它加进 PATH，或者设置 ANDROID_HOME，或者用 -Adb 指定'
}

$adb = Resolve-Adb $Adb

function Invoke-RunAs {
    param([string]$Command)
    & $adb -s $Serial shell "run-as $Package sh -c '$Command'"
}

function Push-AppFile {
    param([string]$Local, [string]$DeviceRelative)
    $output = cmd /c "`"$adb`" -s $Serial exec-in run-as $Package sh -c `"cat > $DeviceRelative`" < `"$Local`" 2>&1"
    if ($LASTEXITCODE -ne 0) { throw "push 失败：$Local -> $DeviceRelative`n$output" }
}

& $adb -s $Serial shell am force-stop $Package | Out-Null
Start-Sleep -Milliseconds 800

Invoke-RunAs 'mkdir -p files/media files/profile' | Out-Null

Push-AppFile $Db 'databases/achievements.db'
Push-AppFile $Prefs 'shared_prefs/lifeledger_settings.xml'
if ($WithMedia) {
    foreach ($name in @('photo_1.jpg', 'photo_2.jpg', 'photo_3.jpg', 'photo_4.jpg', 'photo_5.jpg')) {
        Push-AppFile (Join-Path $WorkDir "media\$name") "files/media/$name"
    }
    Push-AppFile (Join-Path $WorkDir 'media\avatar.jpg') 'files/profile/avatar_random.jpg'
}

Invoke-RunAs 'rm -f databases/achievements.db-wal databases/achievements.db-shm' | Out-Null

Write-Host '--- 设备上的文件 ---'
Invoke-RunAs 'ls -l databases files/media files/profile'

Write-Host '--- 回读校验 ---'
$tmp = Join-Path $WorkDir 'verify.db'
cmd /c "`"$adb`" -s $Serial exec-out run-as $Package cat databases/achievements.db > `"$tmp`"" | Out-Null
& $Python (Join-Path $PSScriptRoot 'inspect_db.py') $tmp
Remove-Item -LiteralPath $tmp -Force
