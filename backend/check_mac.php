<?php
/**
 * check_mac.php — SkyPlayer Pro Backend
 * Déployer sur : https://skyplayerapp.xyz/api/playlist/check_mac.php
 *
 * Méthode : GET ou POST
 *   Paramètre : mac_address  (ex: "AA:BB:CC:DD:EE:FF" ou "AA-BB-CC-DD-EE-FF")
 *
 * Réponse JSON :
 *   { "status": "no_playlist" }                           — MAC inconnue ou expirée
 *   { "status": "active", "name": "...", "url": "...",    — Playlist active
 *     "expire": "2026-12-31", "type": "m3u|xtream" }
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-App-Key');

// Gestion preflight CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ── Configuration base de données ────────────────────────────────────────────
define('DB_HOST', 'localhost');
define('DB_NAME', 'skyplayer_db');
define('DB_USER', 'skyplayer_user');
define('DB_PASS', 'VOTRE_MOT_DE_PASSE_ICI');
define('DB_CHARSET', 'utf8mb4');

// ── Clé d'authentification applicative optionnelle ───────────────────────────
define('APP_KEY', 'skyplayer_pro');

// ── Validation clé d'app (optionnel) ─────────────────────────────────────────
$appKey = $_SERVER['HTTP_X_APP_KEY'] ?? '';
// Décommenter pour activer la validation stricte :
// if ($appKey !== APP_KEY) { jsonError(401, 'Unauthorized'); }

// ── Récupération de l'adresse MAC ────────────────────────────────────────────
$rawMac = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $contentType = $_SERVER['CONTENT_TYPE'] ?? '';
    if (strpos($contentType, 'application/json') !== false) {
        $body    = json_decode(file_get_contents('php://input'), true);
        $rawMac  = $body['mac_address'] ?? $body['device_id'] ?? '';
    } else {
        $rawMac = $_POST['mac_address'] ?? $_POST['device_id'] ?? '';
    }
} else {
    $rawMac = $_GET['mac_address'] ?? $_GET['device_id'] ?? '';
}

if (empty($rawMac)) {
    jsonError(400, 'Paramètre mac_address manquant');
}

// ── Normalisation : accepter XX:XX:XX ou XX-XX-XX, tout en majuscules ────────
$cleanMac = strtoupper(trim($rawMac));
$cleanMac = preg_replace('/[-\s]/', ':', $cleanMac);  // tirets → deux-points

// Validation format (accepte 6 ou 8 groupes pour les IDs virtuels de l'app)
if (!preg_match('/^([0-9A-F]{2}:){5,7}[0-9A-F]{2}$/', $cleanMac)) {
    jsonError(400, 'Format mac_address invalide : ' . $cleanMac);
}

// ── Connexion PDO ─────────────────────────────────────────────────────────────
try {
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);
} catch (PDOException $e) {
    error_log('[SkyPlayer] DB connection error: ' . $e->getMessage());
    jsonError(503, 'Service temporairement indisponible');
}

// ── Requête : chercher une playlist active pour cette MAC ─────────────────────
$sql = "
    SELECT
        playlist_name,
        playlist_url,
        playlist_type,
        expire_date,
        xtream_username,
        xtream_password,
        xtream_server_url
    FROM playlists
    WHERE mac_address   = :mac
      AND is_active     = 1
      AND (expire_date IS NULL OR expire_date >= CURDATE())
    ORDER BY creation_date DESC
    LIMIT 1
";

$stmt = $pdo->prepare($sql);
$stmt->execute([':mac' => $cleanMac]);
$row = $stmt->fetch();

if (!$row) {
    echo json_encode(['status' => 'no_playlist']);
    exit;
}

// ── Construction de la réponse selon le type de playlist ─────────────────────
$response = [
    'status'  => 'active',
    'name'    => $row['playlist_name'],
    'expire'  => $row['expire_date'] ?? null,
    'type'    => $row['playlist_type'] ?? 'm3u',  // 'm3u' ou 'xtream'
];

if ($row['playlist_type'] === 'xtream') {
    // Pour Xtream Codes : renvoie les credentials (jamais l'URL raw)
    $response['xtream_username']   = $row['xtream_username'];
    $response['xtream_password']   = $row['xtream_password'];
    $response['xtream_server_url'] = $row['xtream_server_url'];
    // URL M3U générée depuis les credentials Xtream
    $response['url'] = rtrim($row['xtream_server_url'], '/')
        . '/get.php?username=' . urlencode($row['xtream_username'])
        . '&password='         . urlencode($row['xtream_password'])
        . '&type=m3u_plus&output=ts';
} else {
    // Pour M3U classique : URL directe
    $response['url'] = $row['playlist_url'];
}

echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
exit;

// ── Helpers ───────────────────────────────────────────────────────────────────
function jsonError(int $code, string $message): void {
    http_response_code($code);
    echo json_encode(['status' => 'error', 'message' => $message]);
    exit;
}
