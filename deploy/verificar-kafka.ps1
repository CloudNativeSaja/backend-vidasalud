# Verificacion rapida de Kafka en VidaSalud (para correr antes de una demostracion).
#
# Consulta directamente a los microservicios por 127.0.0.1 (sin pasar por el BFF ni el JWT):
#   kafka-admin :8088  -> cluster, topicos y consumer groups
#   report      :8086  -> KPIs
#   audit       :8087  -> linea de tiempo
#
# Uso:  .\deploy\verificar-kafka.ps1

$ErrorActionPreference = 'Stop'
$fallos = 0

function Get-Json($url) {
    try {
        return Invoke-RestMethod -Uri $url -TimeoutSec 10
    } catch {
        return $null
    }
}

function Titulo($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }
function Ok($texto)     { Write-Host "  [OK]    $texto" -ForegroundColor Green }
function Falla($texto)  { Write-Host "  [FALLA] $texto" -ForegroundColor Red; $script:fallos++ }

Titulo 'Brokers'
$cluster = Get-Json 'http://127.0.0.1:8088/api/kafka/cluster'
if ($null -eq $cluster) {
    Falla 'kafka-admin no responde o Kafka esta caido (docker compose ps en infra/kafka y en el backend)'
} else {
    if ($cluster.brokers.Count -ge 3) { Ok "$($cluster.brokers.Count) brokers (controlador: $($cluster.controllerId))" }
    else { Falla "solo $($cluster.brokers.Count) broker(s); se esperaban 3" }
}

Titulo 'Topicos'
$topicos = Get-Json 'http://127.0.0.1:8088/api/kafka/topics'
foreach ($nombre in 'vidasalud.appointments.events', 'vidasalud.audit.events') {
    $t = $topicos | Where-Object { $_.name -eq $nombre }
    if ($null -eq $t) { Falla "falta el topico $nombre" }
    elseif ($t.partitions -eq 3 -and $t.replicationFactor -eq 3) { Ok "$nombre (3 particiones x 3 replicas)" }
    else { Falla "$nombre tiene $($t.partitions) particiones x $($t.replicationFactor) replicas" }
}

Titulo 'Consumer groups'
$grupos = Get-Json 'http://127.0.0.1:8088/api/kafka/consumer-groups'
foreach ($grupo in 'reportes', 'auditoria', 'auditoria-timeline') {
    $g = $grupos | Where-Object { $_.groupId -eq $grupo }
    if ($null -eq $g) { Falla "no existe el grupo $grupo (esta STREAMING_ENABLED=true y los servicios arriba?)"; continue }
    $detalle = Get-Json "http://127.0.0.1:8088/api/kafka/consumer-groups/$grupo"
    Ok "$grupo : $($g.state), $($detalle.members.Count) consumidor(es), lag total $($detalle.totalLag)"
}

Titulo 'Reportes (KPIs desde Kafka)'
$kpis = Get-Json 'http://127.0.0.1:8086/api/report/kpis?range=last24h'
if ($null -eq $kpis) { Falla 'report no responde' }
else { Ok "citas en 24 h: $($kpis.totalAppointments), cancelacion: $($kpis.cancellationRate), ultimo evento: $($kpis.lastEventAt)" }

Titulo 'Auditoria (linea de tiempo desde Kafka)'
$eventos = Get-Json 'http://127.0.0.1:8087/api/audit/events?limit=5'
if ($null -eq $eventos) { Falla 'audit no responde' }
else {
    Ok "$($eventos.Count) evento(s) recientes"
    $eventos | ForEach-Object { Write-Host "          $($_.ocurridoEn)  $($_.accion)  $($_.correlationId)" }
}

Write-Host ''
if ($fallos -eq 0) { Write-Host 'Todo en orden.' -ForegroundColor Green }
else { Write-Host "$fallos comprobacion(es) fallaron." -ForegroundColor Red; exit 1 }
