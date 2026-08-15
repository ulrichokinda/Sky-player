<?php
/**
 * check.php — SkyPlayer Pro Unified Device Check
 * Endpoint: /api/devices/check
 *
 * Ce script vérifie à la fois le statut de la licence (trial/premium)
 * et s'il y a une playlist associée à l'adresse MAC.
 */

require_once '../../config.php';

$requestId = uniqid('device_check_', true);
skyLog("{$requestId} - Début de la requête api/devices/check.php", 'info');

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-App-Key');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

requireAppKey();

// ── Récupération des données ────────────────────────────────────────────────
$input = json_decode(file_get_contents('php://input'), true);
$mac = strtoupper(trim($input['mac_address'] ?? ''));
$android_id = trim($input['android_id'] ?? '');
$brand = trim($input['brand'] ?? '');
$model = trim($input['model'] ?? '');
$version = trim($input['android_version'] ?? '');

skyLog("{$requestId} - MAC: {$mac}, Android ID: {$android_id}, Device: {$brand} {$model}", 'info');

// ── Validation des entrées ──────────────────────────────────────────────────
// Format identique à check_mac.php : 6 à 8 groupes hexadécimaux séparés par ':'
// (le device_id stocké est "MAC|ANDROID_ID" : valider la MAC empêche d'usurper
//  le device_id complet d'un autre appareil via la branche device_id = :mac)
if (!preg_match('/^([0-9A-F]{2}:){5,7}[0-9A-F]{2}$/', $mac)) {
    skyLog("{$requestId} - Erreur: Format mac_address invalide: {$mac}", 'error');
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Format mac_address invalide']);
    exit;
}

// Android ID : hexadécimal classique, on borne la longueur (colonne VARCHAR(100))
if ($android_id !== '' && !preg_match('/^[A-Za-z0-9_-]{1,64}$/', $android_id)) {
    skyLog("{$requestId} - Erreur: android_id invalide", 'error');
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'android_id invalide']);
    exit;
}

// Bornes alignées sur le schéma SQL (brand/model VARCHAR(100), android_version VARCHAR(20))
$brand   = substr($brand, 0, 100);
$model   = substr($model, 0, 100);
$version = substr($version, 0, 20);

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME, DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    skyLog("{$requestId} - Erreur DB: " . $e->getMessage(), 'error');
    // L'app traite le statut 'offline' comme un fallback local (comportement inchangé)
    echo json_encode(['status' => 'offline', 'message' => 'Database error']);
    exit;
}

try {
    // ── 1. Gérer l'appareil (Enregistrement / Mise à jour) ─────────────────
    $deviceId = "$mac|$android_id";
    $stmt = $pdo->prepare("SELECT * FROM devices WHERE device_id = :did OR device_id = :mac LIMIT 1");
    $stmt->execute([':did' => $deviceId, ':mac' => $mac]);
    $device = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$device) {
        // Durée d'essai unique : alignée sur LicenseManager.TRIAL_DAYS (app Android = 14)
        // et server.js (activation-service). Surchargeable via TRIAL_DAYS (config.php) ou env.
        $trial_days = defined('TRIAL_DAYS') ? (int)TRIAL_DAYS : 14;
        $now = date('Y-m-d H:i:s');
        $expire = date('Y-m-d H:i:s', strtotime("+$trial_days days"));

        skyLog("{$requestId} - Nouvel appareil enregistré: {$deviceId}", 'info');

        try {
            $stmt = $pdo->prepare("INSERT INTO devices (device_id, brand, model, android_version, trial_start, trial_expire, is_activated)
                                   VALUES (:did, :brand, :model, :ver, :start, :expire, 0)");
            $stmt->execute([
                ':did' => $deviceId,
                ':brand' => $brand,
                ':model' => $model,
                ':ver' => $version,
                ':start' => $now,
                ':expire' => $expire
            ]);

            $device = [
                'is_activated' => 0,
                'trial_expire' => $expire
            ];
        } catch (PDOException $e) {
            // Course : un autre appel a inséré cet appareil entre le SELECT et l'INSERT
            if ((int)$e->errorInfo[1] === 1062) { // Duplicate entry (SQLSTATE 23000)
                skyLog("{$requestId} - Appareil déjà enregistré (course), relecture", 'warn');
                $stmt = $pdo->prepare("SELECT * FROM devices WHERE device_id = :did LIMIT 1");
                $stmt->execute([':did' => $deviceId]);
                $device = $stmt->fetch(PDO::FETCH_ASSOC);
                if (!$device) {
                    throw $e;
                }
            } else {
                throw $e;
            }
        }
    } else {
        // Mise à jour de la dernière vue
        skyLog("{$requestId} - Mise à jour de l'appareil existant: {$device['id']}", 'info');
        $stmt = $pdo->prepare("UPDATE devices SET last_seen = CURRENT_TIMESTAMP, brand = :b, model = :m, android_version = :v WHERE id = :id");
        $stmt->execute([':b' => $brand, ':m' => $model, ':v' => $version, ':id' => $device['id']]);
    }

    // ── 2. Déterminer le statut de la licence ──────────────────────────────
    $status = 'expired';
    $days_remaining = 0;

    if ($device['is_activated']) {
        $status = 'premium_active';
    } else {
        $expire_ts = strtotime($device['trial_expire']);
        $now_ts = time();

        if ($expire_ts !== false && $expire_ts > $now_ts) {
            $status = 'trial_active';
            $days_remaining = ceil(($expire_ts - $now_ts) / 86400);
        } else {
            $status = 'expired';
        }
    }

    skyLog("{$requestId} - Statut licence: {$status}, jours restants: {$days_remaining}", 'info');

    // ── 3. Chercher une playlist associée ──────────────────────────────────
    $stmt = $pdo->prepare("SELECT * FROM playlists WHERE mac_address = :mac AND is_active = 1 AND (expire_date IS NULL OR expire_date >= CURDATE()) LIMIT 1");
    $stmt->execute([':mac' => $mac]);
    $playlist = $stmt->fetch(PDO::FETCH_ASSOC);

    $response = [
        'status' => $status,
        'days_remaining' => (int)$days_remaining
    ];

    if ($playlist) {
        $response['playlist_name'] = $playlist['playlist_name'];
        $response['type'] = $playlist['playlist_type'];
        skyLog("{$requestId} - Playlist trouvée: {$playlist['playlist_name']} ({$playlist['playlist_type']})", 'info');

        if ($playlist['playlist_type'] === 'xtream') {
            $response['playlist_url'] = rtrim($playlist['xtream_server_url'], '/')
                . '/get.php?username=' . urlencode($playlist['xtream_username'])
                . '&password='         . urlencode($playlist['xtream_password'])
                . '&type=m3u_plus&output=ts';
            $response['xtream_username'] = $playlist['xtream_username'];
            $response['xtream_password'] = $playlist['xtream_password'];
            $response['xtream_server_url'] = $playlist['xtream_server_url'];
        } else {
            $response['playlist_url'] = $playlist['playlist_url'];
        }
    }

    skyLog("{$requestId} - Réponse envoyée avec succès", 'info');

    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
} catch (PDOException $e) {
    skyLog("{$requestId} - Erreur traitement: " . $e->getMessage(), 'error');
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'Internal error']);
}
