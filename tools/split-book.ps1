# This file is part of Gottsch Books.
# Copyright (c) 2026 Mark Gottschling (gottsch)
#
# Gottsch Books is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Gottsch Books is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Gottsch Books.  If not, see <http://www.gnu.org/licenses/lgpl>.

<#
.SYNOPSIS
Writes the files of a Gottsch Books book from an AI's answers. The same as split-book.py, for Windows without Python.

.DESCRIPTION
An AI following AI-INSTRUCTIONS.md gives each file as its path on a line of its own, then the file in a fenced block.
Save the AI's answers as text files and run, from a PowerShell window:

    powershell -ExecutionPolicy Bypass -File split-book.ps1 answer1.txt answer2.txt -Out path\to\src\main\resources

Each file is written under -Out (the current folder if not given). A path must be relative and sit under an assets\
or data\ folder (or be a pack.mcmeta); anything else is skipped, so an answer can never write elsewhere on your
computer. -DryRun lists what would be written.
#>
param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]] $Answers,
    [string] $Out = ".",
    [switch] $DryRun
)

$pathLine = '^\s*(?:#+\s*)?(?:\*\*)?(?:file:\s*)?`?(?<path>[A-Za-z0-9_./-]+\.(?:md|json|mcmeta))`?(?:\*\*)?:?\s*$'
$fenceOpen = '^\s*(?<fence>`{3,}|~{3,})\s*[A-Za-z0-9_-]*\s*$'

function Test-Allowed([string] $path) {
    # relative, no '..', and under an assets/ or data/ folder, or a pack.mcmeta
    if ($path.StartsWith('/') -or $path.Contains(':')) { return $false }
    $parts = $path.Split('/')
    if ($parts -contains '..') { return $false }
    if ($parts[-1] -eq 'pack.mcmeta') { return $true }
    $folders = if ($parts.Count -gt 1) { $parts[0..($parts.Count - 2)] } else { @() }
    return ($folders -contains 'assets') -or ($folders -contains 'data')
}

function Get-BookFiles([string] $text) {
    $lines = $text.Replace("`r`n", "`n").Replace("`r", "`n").Split("`n")
    $i = 0
    while ($i -lt $lines.Count) {
        $m = [regex]::Match($lines[$i], $pathLine, 'IgnoreCase')
        if (-not $m.Success) { $i++; continue }
        $path = $m.Groups['path'].Value
        $j = $i + 1
        while ($j -lt $lines.Count -and -not $lines[$j].Trim()) { $j++ }
        $fence = if ($j -lt $lines.Count) { [regex]::Match($lines[$j], $fenceOpen) } else { $null }
        if (-not $fence -or -not $fence.Success) { $i++; continue }
        $mark = $fence.Groups['fence'].Value
        # it closes on a line of the same fence character, at least as long, and nothing else
        $close = '^\s*' + [regex]::Escape($mark.Substring(0, 1)) + '{' + $mark.Length + ',}\s*$'
        $k = $j + 1
        $body = New-Object System.Collections.Generic.List[string]
        while ($k -lt $lines.Count -and -not [regex]::IsMatch($lines[$k], $close)) {
            $body.Add($lines[$k])
            $k++
        }
        $warning = $null
        if ($k -ge $lines.Count) {
            $warning = 'its block never closes; the answer may have been cut off'
        } elseif ($mark.Length -eq 3 -and $path.EndsWith('.md') -and ($body | Where-Object { $_.Trim().StartsWith('```') })) {
            $warning = 'it was fenced with three backticks, so a block inside it may have ended it early'
        }
        [pscustomobject]@{ Path = $path; Content = (($body -join "`n").TrimEnd("`n") + "`n"); Warning = $warning }
        $i = $k + 1
    }
}

$texts = if ($Answers) {
    $Answers | ForEach-Object { [IO.File]::ReadAllText((Resolve-Path $_).Path, [Text.Encoding]::UTF8) }
} else {
    @([Console]::In.ReadToEnd())
}

$utf8 = New-Object System.Text.UTF8Encoding $false
$outDir = [IO.Path]::GetFullPath((Join-Path (Get-Location).Path $Out))
$written = 0
$skipped = 0
$seen = @{}
foreach ($text in $texts) {
    foreach ($file in Get-BookFiles $text) {
        if (-not (Test-Allowed $file.Path)) {
            Write-Output "skipped  $($file.Path)  (not under assets/ or data/, or not a safe relative path)"
            $skipped++
            continue
        }
        $target = Join-Path $outDir ($file.Path.Replace('/', '\'))
        $note = if ($seen.ContainsKey($file.Path)) { 'again (the later version wins)' } elseif (Test-Path $target) { 'replaced' } else { 'new' }
        $seen[$file.Path] = $true
        if ($file.Warning) { $note += "; check it: $($file.Warning)" }
        if (-not $DryRun) {
            [void](New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent))
            [IO.File]::WriteAllText($target, $file.Content, $utf8)
        }
        $verb = if ($DryRun) { 'would write' } else { 'wrote' }
        Write-Output "$verb  $($file.Path)  ($note)"
        $written++
    }
}

if ($written -eq 0 -and $skipped -eq 0) {
    Write-Output 'No files found. Each file should be its path on a line of its own, then a fenced block.'
    exit 1
}
$summary = "$written file(s) " + $(if ($DryRun) { 'to write' } else { 'written' }) + " under $outDir"
if ($skipped) { $summary += ", $skipped skipped" }
Write-Output $summary
if (-not $DryRun -and $written) { Write-Output 'Next: start the game (or press F3+T in it) and run /gottschbooks check' }
