<#
.SYNOPSIS
    Clona o actualiza los microservicios en .\servicios (cada uno es su propio repositorio).

.DESCRIPTION
    El docker-compose.yml construye cada imagen desde .\servicios\<servicio>.
    Equivalente en PowerShell de deploy/servicios.sh.

.EXAMPLE
    .\deploy\servicios.ps1
    .\deploy\servicios.ps1 -Ref mi-rama
#>
[CmdletBinding()]
param(
    [string]$Org = 'https://github.com/CloudNativeSaja',
    [string]$Ref = 'main',
    [string[]]$Servicios = @(
        'ms-vidasalud-bff',
        'ms-vidasalud-appointments',
        'ms-vidasalud-catalog',
        'ms-vidasalud-notify',
        'ms-vidasalud-mq-admin',
        'ms-vidasalud-report',
        'ms-vidasalud-audit',
        'ms-vidasalud-kafka-admin'
    )
)

$ErrorActionPreference = 'Stop'

function Invoke-Git {
    param([string[]]$GitArgs)

    # git escribe su progreso por stderr: PowerShell 5.1 lo trata como error.
    # Por eso solo se falla segun el codigo de salida real de git.
    $previo = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $salida = & git @GitArgs 2>&1
        $codigo = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previo
    }

    if ($codigo -ne 0) {
        $detalle = ($salida | ForEach-Object { "$_" }) -join ' '
        throw "git $($GitArgs -join ' ') fallo (codigo $codigo): $detalle"
    }
}

$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path $raiz 'servicios'
New-Item -ItemType Directory -Force -Path $destino | Out-Null

foreach ($servicio in $Servicios) {
    $ruta = Join-Path $destino $servicio

    if (Test-Path (Join-Path $ruta '.git')) {
        Write-Host "== Actualizando $servicio ($Ref)"
        Invoke-Git @('-C', $ruta, 'fetch', '--quiet', 'origin')
        Invoke-Git @('-C', $ruta, 'checkout', '--quiet', $Ref)
        Invoke-Git @('-C', $ruta, 'pull', '--quiet', '--ff-only', 'origin', $Ref)
    } else {
        Write-Host "== Clonando $servicio ($Ref)"
        Invoke-Git @('clone', '--quiet', '--branch', $Ref, "$Org/$servicio.git", $ruta)
    }
}

Write-Host "Servicios listos en $destino (rama $Ref)" -ForegroundColor Green
