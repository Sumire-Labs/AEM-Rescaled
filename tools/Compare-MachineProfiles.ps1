param(
    [Parameter(Mandatory = $true)][string]$Before,
    [Parameter(Mandatory = $true)][string]$After
)

$ErrorActionPreference = 'Stop'
$beforeLines = Get-Content -LiteralPath (Join-Path $Before 'results.txt')
$afterLines = Get-Content -LiteralPath (Join-Path $After 'results.txt')
$metricPattern = '^(?<name>.+)\.ns=(?<ns>\d+),bytes=(?<bytes>\d+),operations=(?<operations>\d+)$'
$beforeBehavior = ($beforeLines | Where-Object { $_ -notmatch $metricPattern }) -join "`n"
$afterBehavior = ($afterLines | Where-Object { $_ -notmatch $metricPattern }) -join "`n"
if ($beforeBehavior -cne $afterBehavior) {
    throw 'Behavior snapshots differ. Compare the result files before accepting the optimization.'
}
Write-Output 'Behavior snapshots match.'

function Read-Metrics($lines) {
    foreach ($line in $lines) {
        if ($line -match $metricPattern) {
            [pscustomobject]@{
                Workload = $Matches.name
                MillisecondsPerOperation = [double]$Matches.ns / 1000000 / [double]$Matches.operations
                BytesPerOperation = [double]$Matches.bytes / [double]$Matches.operations
            }
        }
    }
}

function Median($values) {
    $sorted = @($values | Sort-Object)
    $middle = [int][Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}

$beforeMetrics = @(Read-Metrics $beforeLines)
$afterMetrics = @(Read-Metrics $afterLines)
foreach ($workload in ($beforeMetrics.Workload | Sort-Object -Unique)) {
    $old = @($beforeMetrics | Where-Object Workload -eq $workload)
    $new = @($afterMetrics | Where-Object Workload -eq $workload)
    if ($new.Count -eq 0) { throw "Missing measurements for $workload" }
    [pscustomobject]@{
        Workload = $workload
        BeforeMsPerOperation = Median $old.MillisecondsPerOperation
        AfterMsPerOperation = Median $new.MillisecondsPerOperation
        BeforeBytesPerOperation = Median $old.BytesPerOperation
        AfterBytesPerOperation = Median $new.BytesPerOperation
    }
}
