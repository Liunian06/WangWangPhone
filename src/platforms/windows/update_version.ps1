# PowerShell script to update Windows version info from global version.properties

$versionFile = Join-Path $PSScriptRoot "..\..\..\version.properties"
if (-not (Test-Path $versionFile)) {
    Write-Error "version.properties not found at $versionFile"
    exit 1
}

$content = Get-Content $versionFile -Raw
$versionCode = [regex]::Match($content, 'VERSION_CODE=(\d+)').Groups[1].Value
$versionName = [regex]::Match($content, 'VERSION_NAME=([^\r\n]+)').Groups[1].Value

Write-Host "Updating Windows version to Name: $versionName, Code: $versionCode"

# Note: In a real project, you would update AssemblyInfo.cs, .rc, or Package.appxmanifest
# For now, we print the values and set environment variables for CI/build integration
Write-Host "##vso[task.setvariable variable=VERSION_CODE]$versionCode"
Write-Host "##vso[task.setvariable variable=VERSION_NAME]$versionName"
