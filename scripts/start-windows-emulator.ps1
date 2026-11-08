param(
  [Parameter(Mandatory = $true)][string]$Avd,
  [Parameter(Mandatory = $true)][string]$SdkRoot,
  [Parameter(Mandatory = $true)][string]$AvdHome,
  [Parameter(Mandatory = $true)][string]$AdbPort,
  [switch]$NoSnapshot
)

$ErrorActionPreference = 'Stop'
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
$env:ANDROID_AVD_HOME = $AvdHome
$env:ANDROID_ADB_SERVER_PORT = $AdbPort
$env:ADB_SERVER_SOCKET = "tcp:127.0.0.1:$AdbPort"
$emulator = Join-Path $SdkRoot 'emulator\emulator.exe'
$arguments = @('-avd', $Avd, '-gpu', 'host')
if ($NoSnapshot) { $arguments += '-no-snapshot' }
Start-Process -FilePath $emulator -ArgumentList $arguments -WorkingDirectory (Split-Path $emulator) | Out-Null
exit 0
