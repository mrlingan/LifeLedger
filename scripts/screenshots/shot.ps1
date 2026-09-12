<#
    拍一张截图，同时把 UI 树 dump 下来。

        pwsh -File scripts/screenshots/shot.ps1 -Name zh_home -ScrollTop 3

    - 截图落在 <WorkDir>\shots\<name>.png（WorkDir 默认是 scripts/screenshots/work）
    - UI 树落在 <WorkDir>\ui_<name>.xml，可以用 dump_texts.py / find_node.py /
      check_layout.py 核对文案与坐标
    - -ScrollTop N：先向上滑 N 次回到顶部（首页要确保停在最上面）
#>
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [int]$ScrollTop = 0,
    [string]$Serial = 'emulator-5554',
    [string]$Adb = '',
    [string]$Python = 'py',
    [string]$WorkDir = ''
)

$ErrorActionPreference = 'Stop'
if (-not $WorkDir) { $WorkDir = Join-Path $PSScriptRoot 'work' }

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
New-Item -ItemType Directory -Force -Path (Join-Path $WorkDir 'shots') | Out-Null

for ($i = 0; $i -lt $ScrollTop; $i++) {
    & $adb -s $Serial shell input swipe 540 700 540 1800 250 | Out-Null
    Start-Sleep -Milliseconds 700
}

Start-Sleep -Milliseconds 900
$uiPath = Join-Path $WorkDir "ui_$Name.xml"
$shotPath = Join-Path $WorkDir "shots\$Name.png"

& $adb -s $Serial shell uiautomator dump /sdcard/shot.xml | Out-Null
cmd /c "`"$adb`" -s $Serial exec-out cat /sdcard/shot.xml > `"$uiPath`""
cmd /c "`"$adb`" -s $Serial exec-out screencap -p > `"$shotPath`""

Write-Host ("shot: {0}  {1} bytes" -f $shotPath, (Get-Item $shotPath).Length)
& $Python (Join-Path $PSScriptRoot 'dump_texts.py') $uiPath
