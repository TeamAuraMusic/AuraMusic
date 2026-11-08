param(
    [Parameter(Mandatory = $true)][string]$RepoRoot,
    [string]$Root = 'I:\temp\AuraMusicPreviewHttps',
    [string]$Python = 'C:\Users\nos\AppData\Local\Programs\Python\Python312\python.exe'
)

$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $Root | Out-Null
Copy-Item -Force (Join-Path $RepoRoot 'scripts\preview_https_server.py') (Join-Path $Root 'preview_https_server.py')
Copy-Item -Force (Join-Path $RepoRoot 'previewprobe\src\main\res\raw\fixture_a.mp4') (Join-Path $Root 'fixture_a.mp4')
Copy-Item -Force (Join-Path $RepoRoot 'previewprobe\src\main\res\raw\fixture_b.mp4') (Join-Path $Root 'fixture_b.mp4')

$stdout = Join-Path $Root 'server.stdout.log'
$stderr = Join-Path $Root 'server.stderr.log'
$requestLog = Join-Path $Root 'requests.jsonl'
$args = @(
    (Join-Path $Root 'preview_https_server.py'),
    '--root', $Root,
    '--cert', (Join-Path $Root 'preview-server.crt'),
    '--key', (Join-Path $Root 'preview-server.key'),
    '--log', $requestLog,
    '--host', '0.0.0.0',
    '--port', '8443'
)
$process = Start-Process -FilePath $Python -ArgumentList $args -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $stdout -RedirectStandardError $stderr
Set-Content -NoNewline -Path (Join-Path $Root 'server.pid') -Value $process.Id
Write-Output "Started local diagnostic HTTPS server pid=$($process.Id) root=$Root"
