<#
    图鉴文案本地化生成器

    用法（在项目根目录执行）：
        pwsh -File scripts/preset_i18n/generate_preset_i18n.ps1

    输入：
        source.tsv  从 assets/preset_achievements.json 导出的中文原文（id / title / desc / category / rarity / rate / story）
        en.json     英文翻译

    输出：
        out/res/values/preset_strings.xml      英文（默认语言）
        out/res/values-zh/preset_strings.xml   中文
        out/kotlin/PresetStringRes.kt          id -> R.string 的索引表

    改完图鉴内容之后：改 source.tsv（或重新从 JSON 导出）、en.json，再跑一次脚本，
    然后把 out/ 里的三个文件覆盖到：
        app/src/main/res/values/preset_strings.xml
        app/src/main/res/values-zh/preset_strings.xml
        app/src/main/java/com/Anchored/mylife/ui/PresetStringRes.kt

    为什么不用 getIdentifier()：写成 R.string.xxx 的话，XML 和 Kotlin 对不上会直接编译失败，
    getIdentifier 只会安静地返回 0。
#>

param(
    [string]$Source = (Join-Path $PSScriptRoot 'source.tsv'),
    [string]$English = (Join-Path $PSScriptRoot 'en.json'),
    [string]$OutDir = (Join-Path $PSScriptRoot 'out')
)

$ErrorActionPreference = 'Stop'

function Esc([string]$value) {
    if ($null -eq $value) { return '' }
    $r = $value -replace '&', '&amp;'
    $r = $r -replace '<', '&lt;'
    $r = $r -replace '>', '&gt;'
    $r = $r -replace "'", "\'"
    $r = $r -replace '"', '\"'
    return $r
}

function Write-Xml([string]$path, $lines) {
    $text = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`r`n<resources>`r`n" +
            (($lines | ForEach-Object { '    ' + $_ }) -join "`r`n") +
            "`r`n</resources>`r`n"
    $dir = Split-Path -Parent $path
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    [System.IO.File]::WriteAllText($path, $text, [System.Text.UTF8Encoding]::new($false))
}

$categories = [ordered]@{
    '成长'   = @{ key = 'growth';         en = 'Growth' }
    '生活'   = @{ key = 'life';           en = 'Life' }
    '旅行'   = @{ key = 'travel';         en = 'Travel' }
    '学业'   = @{ key = 'education';      en = 'Education' }
    '情感'   = @{ key = 'relationships';  en = 'Relationships' }
    '娱乐'   = @{ key = 'entertainment';  en = 'Entertainment' }
    '社交'   = @{ key = 'social';         en = 'Social' }
    '兴趣'   = @{ key = 'hobbies';        en = 'Hobbies' }
    '技能'   = @{ key = 'skills';         en = 'Skills' }
    '职业'   = @{ key = 'career';         en = 'Career' }
    '家庭'   = @{ key = 'family';         en = 'Family' }
    '新手村' = @{ key = 'starter_village'; en = 'Starter Village' }
    '健康'   = @{ key = 'health';         en = 'Health' }
    '财务'   = @{ key = 'finance';        en = 'Finance' }
}

$rows = Get-Content -LiteralPath $Source -Encoding UTF8 |
    Where-Object { $_.Trim().Length -gt 0 } |
    ForEach-Object {
        $f = $_ -split "`t"
        if ($f.Count -ne 7) { throw "source.tsv 这一行列数不对：$_" }
        [pscustomobject]@{
            id       = [int]$f[0]
            title    = $f[1]
            desc     = $f[2]
            category = $f[3]
            rarity   = $f[4]
            rate     = $f[5]
            story    = $f[6]
        }
    }

if ($rows.Count -eq 0) { throw 'source.tsv 是空的' }

$en = Get-Content -LiteralPath $English -Raw -Encoding UTF8 | ConvertFrom-Json

for ($i = 0; $i -lt $rows.Count; $i++) {
    if ($rows[$i].id -ne ($i + 1)) {
        throw "id 必须是 1..N 连续递增，第 $($i + 1) 行是 $($rows[$i].id)"
    }
}

$firstId = $rows[0].id
$zhLines = New-Object System.Collections.ArrayList
$enLines = New-Object System.Collections.ArrayList
$titles = New-Object System.Collections.ArrayList
$descs = New-Object System.Collections.ArrayList
$stories = New-Object System.Collections.ArrayList

function Add-String($list, [string]$name, [string]$value) {
    $attr = ''
    if ($value -like '*%*') { $attr = ' formatted="false"' }
    [void]$list.Add("<string name=`"$name`"$attr>$(Esc $value)</string>")
}

foreach ($row in $rows) {
    $key = "preset_$($row.id)"
    $translated = $en."$($row.id)"
    if ($null -eq $translated) { throw "en.json 里缺少 id=$($row.id) 的翻译" }

    [void]$zhLines.Add("<!-- $($row.id) $(Esc $row.title) -->")
    Add-String $zhLines "$($key)_title" $row.title
    Add-String $zhLines "$($key)_desc" $row.desc
    Add-String $zhLines "$($key)_story" $row.story

    [void]$enLines.Add("<!-- $($row.id) -->")
    Add-String $enLines "$($key)_title" $translated.t
    Add-String $enLines "$($key)_desc" $translated.d
    Add-String $enLines "$($key)_story" $translated.s

    [void]$titles.Add("        R.string.$($key)_title,")
    [void]$descs.Add("        R.string.$($key)_desc,")
    [void]$stories.Add("        R.string.$($key)_story,")
}

[void]$zhLines.Add('')
[void]$zhLines.Add('<!-- 分类 -->')
[void]$enLines.Add('')
[void]$enLines.Add('<!-- Categories -->')
$categoryMap = New-Object System.Collections.ArrayList
foreach ($name in $categories.Keys) {
    $info = $categories[$name]
    Add-String $zhLines "preset_category_$($info.key)" $name
    Add-String $enLines "preset_category_$($info.key)" $info.en
    [void]$categoryMap.Add("        `"$name`" to R.string.preset_category_$($info.key),")
}

Write-Xml (Join-Path $OutDir 'res/values/preset_strings.xml') $enLines
Write-Xml (Join-Path $OutDir 'res/values-zh/preset_strings.xml') $zhLines

$kotlin = @"
package com.Anchored.mylife.ui

import com.Anchored.mylife.R

/**
 * 图鉴文案的资源索引。
 *
 * 由 scripts/preset_i18n/generate_preset_i18n.ps1 生成，不要手改。
 * 数组下标 = 图鉴条目 id - [FIRST_ID]，和 assets/preset_achievements.json 里的 id 一一对应。
 *
 * 写成 R.string.xxx 而不是 getIdentifier()：如果 XML 和这里对不上，编译会直接报错。
 */
internal object PresetStringRes {

    private const val FIRST_ID = ${firstId}L

    private val TITLE = intArrayOf(
$($titles -join "`r`n")
    )

    private val DESCRIPTION = intArrayOf(
$($descs -join "`r`n")
    )

    private val STORY = intArrayOf(
$($stories -join "`r`n")
    )

    /** 分类名（数据库里存的中文）-> 资源 id */
    private val CATEGORY = mapOf(
$($categoryMap -join "`r`n")
    )

    fun titleOf(id: Long): Int? = TITLE.getOrNull(indexOf(id))

    fun descriptionOf(id: Long): Int? = DESCRIPTION.getOrNull(indexOf(id))

    fun storyOf(id: Long): Int? = STORY.getOrNull(indexOf(id))

    fun categoryOf(name: String): Int? = CATEGORY[name]

    private fun indexOf(id: Long): Int = (id - FIRST_ID).toInt()
}
"@

$kotlinPath = Join-Path $OutDir 'kotlin/PresetStringRes.kt'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $kotlinPath) | Out-Null
[System.IO.File]::WriteAllText($kotlinPath, $kotlin, [System.Text.UTF8Encoding]::new($false))

Write-Host "OK  entries=$($rows.Count)  categories=$($categories.Count)"
Write-Host "    $(Join-Path $OutDir 'res/values/preset_strings.xml')"
Write-Host "    $(Join-Path $OutDir 'res/values-zh/preset_strings.xml')"
Write-Host "    $kotlinPath"
