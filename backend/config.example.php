<?php
/**
 * Configuration exemple — copier vers config.php et adapter les valeurs.
 * ⚠️ NE PAS committer config.php : il contient vos secrets.
 *
 * Les secrets sont chargés UNIQUEMENT depuis les variables d'environnement.
 * Aucun secret ne doit apparaître en dur dans config.php.
 *
 * Variables REQUISES (l'application refuse de démarrer si absentes) :
 *   DB_USER         — Utilisateur MySQL
 *   DB_PASS         — Mot de passe MySQL
 *   APK_SECRET_KEY  — Clé secrète de validation des requêtes (webhooks, admin)
 *   RESELLER_USER   — Login de l'espace revendeur
 *   RESELLER_PASS   — Mot de passe de l'espace revendeur (fort, jamais admin123)
 *
 * Variables optionnelles :
 *   DEBUG_MODE      — true/false (défaut false)
 *   DB_HOST         — défaut localhost
 *   DB_NAME         — défaut skyplayer_db
 *   APP_KEY         — clé applicative supplémentaire
 *   TRIAL_DAYS      — durée d'essai en jours (défaut 14, aligné sur LicenseManager.TRIAL_DAYS)
 *
 * Exemples de définition :
 *   - cPanel : section "Variables d'environnement" de l'hébergeur
 *   - Apache : SetEnv DB_PASS "..." dans .htaccess (hors du webroot si possible)
 *   - nginx  : fastcgi_param DB_PASS "..." pour le bloc PHP
 */

define('DEBUG_MODE', filter_var(getenv('DEBUG_MODE') ?: 'false', FILTER_VALIDATE_BOOLEAN));

define('LOG_DIR', __DIR__ . '/logs');
if (!file_exists(LOG_DIR)) {
    mkdir(LOG_DIR, 0755, true);
}
ini_set('error_log', LOG_DIR . '/php_errors.log');

if (DEBUG_MODE) {
    error_reporting(E_ALL);
    ini_set('display_errors', 0);
    ini_set('log_errors', 1);
} else {
    error_reporting(0);
    ini_set('display_errors', 0);
    ini_set('log_errors', 1);
}

// ── Garde-fou : refuser de démarrer avec des secrets manquants (fail-closed) ─
$requiredEnv = ['DB_USER', 'DB_PASS', 'APK_SECRET_KEY', 'RESELLER_USER', 'RESELLER_PASS'];
$missing = array_values(array_filter(
    $requiredEnv,
    fn($var) => trim((string)getenv($var)) === ''
));
if ($missing) {
    $message = 'Configuration incomplète — variables d\'environnement manquantes : ' . implode(', ', $missing)
        . '. Définissez-les (hébergeur, .env, SetEnv...) — voir config.example.php.';
    error_log('[SkyPlayer] ' . $message);
    http_response_code(503);
    header('Content-Type: text/plain; charset=utf-8');
    echo $message;
    exit(1);
}

define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_NAME', getenv('DB_NAME') ?: 'skyplayer_db');
define('DB_USER', getenv('DB_USER'));
define('DB_PASS', getenv('DB_PASS'));

define('EXTERNAL_REQUEST_TIMEOUT', 30);
define('EXTERNAL_CONNECT_TIMEOUT', 10);
define('USER_AGENT', 'SkyPlayerPro/2.0 (Android; +https://skyplayerapp.xyz)');

/**
 * Durée d'essai en jours — DOIT rester alignée sur LicenseManager.TRIAL_DAYS (app Android = 14)
 * et sur server.js (activation-service). Surchargeable via l'environnement TRIAL_DAYS.
 */
define('TRIAL_DAYS', (int)(getenv('TRIAL_DAYS') ?: 14));

/**
 * Clés applicatives autorisées (header X-App-Key).
 * Les clés de l'app Android sont publiques (embarquées dans l'APK) : elles ne sont pas des secrets.
 * APP_KEY permet d'ajouter une clé serveur supplémentaire via l'environnement.
 */
define('APP_KEYS', array_values(array_unique(array_filter([
    getenv('APP_KEY') ?: '',
    'skyplayer_pro',
    'skyplayer_pro_v2',
]))));

define('APK_SECRET_KEY', getenv('APK_SECRET_KEY'));

define('RESELLER_USER', getenv('RESELLER_USER'));
define('RESELLER_PASS', getenv('RESELLER_PASS'));

function skyLog(string $message, string $level = 'info'): void {
    $logFile = LOG_DIR . '/skyplayer_' . date('Y-m-d') . '.log';
    $timestamp = date('Y-m-d H:i:s');
    $logEntry = "[{$timestamp}] [{$level}] {$message}" . PHP_EOL;
    file_put_contents($logFile, $logEntry, FILE_APPEND | LOCK_EX);
}

function validateAppKey(): bool {
    $appKey = $_SERVER['HTTP_X_APP_KEY'] ?? '';
    return in_array($appKey, APP_KEYS, true);
}

function requireAppKey(): void {
    if (!validateAppKey()) {
        http_response_code(401);
        echo json_encode(['status' => 'error', 'message' => 'Unauthorized']);
        exit;
    }
}

function fetchExternalUrl(string $url, array $options = []): array {
    $ch = curl_init();

    $defaultOptions = [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => EXTERNAL_REQUEST_TIMEOUT,
        CURLOPT_CONNECTTIMEOUT => EXTERNAL_CONNECT_TIMEOUT,
        CURLOPT_USERAGENT => USER_AGENT,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS => 5,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_HTTPHEADER => [
            'Accept: */*',
            'Accept-Language: fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7',
        ],
    ];

    foreach ($options as $key => $value) {
        $defaultOptions[$key] = $value;
    }

    curl_setopt_array($ch, $defaultOptions);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    $result = [
        'success' => false,
        'http_code' => $httpCode,
        'body' => $response,
        'error' => $error,
    ];

    if ($httpCode >= 200 && $httpCode < 300 && empty($error)) {
        $result['success'] = true;
    }

    return $result;
}
