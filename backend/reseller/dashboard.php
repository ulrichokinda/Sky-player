<?php
require_once '../config.php';

header('X-Frame-Options: DENY');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: same-origin');

// Session durcie : cookie HttpOnly + SameSite (Secure si HTTPS)
if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'httponly' => true,
        'samesite' => 'Lax',
        'secure'   => !empty($_SERVER['HTTPS']),
    ]);
    session_start();
}

if (!isset($_SESSION['reseller_id'])) {
    header('Location: index.php');
    exit;
}

// Jeton CSRF (créé à la connexion, recréé si absent)
if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}
$csrfToken = $_SESSION['csrf_token'];

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME, DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    // Ne jamais exposer les détails de connexion DB au navigateur
    skyLog("Dashboard: Erreur de connexion DB: " . $e->getMessage(), 'error');
    die("Erreur de connexion à la base de données.");
}

$message = '';

// Traitement de l'ajout
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add') {
    // ── Protection CSRF ──
    $sentToken = $_POST['csrf_token'] ?? '';
    if (!hash_equals($csrfToken, $sentToken)) {
        skyLog("Dashboard: tentative CSRF bloquée (IP " . ($_SERVER['REMOTE_ADDR'] ?? '?') . ")", 'warn');
        $message = "❌ Session expirée, veuillez réessayer.";
    } else {
        $mac = strtoupper(trim($_POST['mac_address'] ?? ''));
        $name = trim($_POST['playlist_name'] ?? '');
        $type = (string)($_POST['playlist_type'] ?? '');
        $url = trim($_POST['playlist_url'] ?? '');
        $user = trim($_POST['xtream_username'] ?? '');
        $pass = trim($_POST['xtream_password'] ?? '');
        $server = trim($_POST['xtream_server_url'] ?? '');
        $expire = !empty($_POST['expire_date']) ? $_POST['expire_date'] : null;

        // ── Validation des entrées ──
        // Whitelist du type : élimine le vecteur de XSS stockée (valeur réinjectée dans l'attribut class)
        $type = in_array($type, ['m3u', 'xtream'], true) ? $type : 'm3u';
        $macOk = preg_match('/^([0-9A-F]{2}:){5,7}[0-9A-F]{2}$/', $mac);
        $expireOk = $expire === null || preg_match('/^\d{4}-\d{2}-\d{2}$/', $expire);

        if (!$macOk) {
            $message = "❌ Format d'adresse MAC invalide (ex: AA:BB:CC:DD:EE:FF).";
        } elseif (!$expireOk) {
            $message = "❌ Date d'expiration invalide.";
        } elseif (strlen($name) > 255) {
            $message = "❌ Nom de playlist trop long (max 255 caractères).";
        } elseif ($type === 'm3u' && $url === '') {
            $message = "❌ L'URL M3U est requise pour ce type.";
        } elseif ($type === 'xtream' && ($user === '' || $server === '')) {
            $message = "❌ Le serveur et l'utilisateur sont requis pour Xtream.";
        } else {
            try {
                $sql = "INSERT INTO playlists (mac_address, playlist_name, playlist_type, playlist_url, xtream_username, xtream_password, xtream_server_url, expire_date)
                        VALUES (:mac, :name, :type, :url, :user, :pass, :server, :expire)";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([
                    ':mac' => $mac,
                    ':name' => substr($name, 0, 255),
                    ':type' => $type,
                    ':url' => $type === 'm3u' ? $url : null,
                    ':user' => $type === 'xtream' ? $user : null,
                    ':pass' => $type === 'xtream' ? $pass : null,
                    ':server' => $type === 'xtream' ? $server : null,
                    ':expire' => $expire
                ]);
                $message = "✅ Playlist ajoutée avec succès pour $mac";
                skyLog("Dashboard: playlist ajoutée pour {$mac} par revendeur", 'info');
            } catch (PDOException $e) {
                skyLog("Dashboard: erreur INSERT playlist: " . $e->getMessage(), 'error');
                $message = "❌ Erreur lors de l'enregistrement.";
            }
        }
    }
}

// Récupération des playlists
try {
    $playlists = $pdo->query("SELECT * FROM playlists ORDER BY id DESC")->fetchAll(PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    skyLog("Dashboard: erreur SELECT playlists: " . $e->getMessage(), 'error');
    $playlists = [];
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Dashboard Revendeur - SkyPlayer Pro</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0F0F0F; color: #E0E0E0; margin: 0; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; }
        h1, h2 { color: #00A3FF; }
        .card { background: #1A1A1A; border-radius: 12px; padding: 25px; margin-bottom: 30px; box-shadow: 0 8px 32px rgba(0,0,0,0.5); border: 1px solid #333; }
        .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 15px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; font-size: 0.9rem; color: #AAA; }
        input, select, textarea { width: 100%; padding: 10px; background: #2A2A2A; border: 1px solid #444; color: white; border-radius: 6px; box-sizing: border-box; }
        button { background: #00A3FF; color: white; border: none; padding: 12px 25px; border-radius: 6px; cursor: pointer; font-weight: bold; transition: background 0.3s; }
        button:hover { background: #0082CC; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; background: #1A1A1A; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #333; }
        th { background: #252525; color: #00A3FF; }
        tr:hover { background: #222; }
        .badge { padding: 4px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: bold; }
        .badge-m3u { background: #E91E63; }
        .badge-xtream { background: #9C27B0; }
        .alert { padding: 15px; border-radius: 6px; margin-bottom: 20px; font-weight: bold; }
        .alert-info { background: rgba(0, 163, 255, 0.1); color: #00A3FF; border: 1px solid #00A3FF; }
    </style>
    <script>
        function toggleFields() {
            const type = document.getElementById('playlist_type').value;
            document.getElementById('m3u_fields').style.display = type === 'm3u' ? 'block' : 'none';
            document.getElementById('xtream_fields').style.display = type === 'xtream' ? 'block' : 'none';
        }
    </script>
</head>
<body>
    <div class="container">
        <header style="display: flex; justify-content: space-between; align-items: center;">
            <h1>SkyPlayer Pro <span style="color: #FFD700; font-size: 0.8rem; vertical-align: middle;">DASHBOARD</span></h1>
            <a href="index.php?logout=1" style="color: #AAA; text-decoration: none;">Déconnexion</a>
        </header>

        <?php if ($message !== ''): ?>
            <div class="alert alert-info"><?php echo htmlspecialchars($message); ?></div>
        <?php endif; ?>

        <div class="card">
            <h2>Ajouter une Playlist (Liaison MAC)</h2>
            <form method="POST">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars($csrfToken); ?>">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Adresse MAC (ou Device ID)</label>
                        <input type="text" name="mac_address" placeholder="Ex: AA:BB:CC:DD:EE:FF" required>
                    </div>
                    <div class="form-group">
                        <label>Nom de la Playlist</label>
                        <input type="text" name="playlist_name" placeholder="Ex: Abonnement Gold" required>
                    </div>
                    <div class="form-group">
                        <label>Type</label>
                        <select name="playlist_type" id="playlist_type" onchange="toggleFields()">
                            <option value="m3u">Fichier M3U (URL)</option>
                            <option value="xtream">Xtream Codes (User/Pass)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Date d'expiration</label>
                        <input type="date" name="expire_date">
                    </div>
                </div>

                <div id="m3u_fields">
                    <div class="form-group">
                        <label>URL de la Playlist (.m3u / .m3u8)</label>
                        <input type="url" name="playlist_url" placeholder="https://...">
                    </div>
                </div>

                <div id="xtream_fields" style="display:none;">
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Serveur URL</label>
                            <input type="url" name="xtream_server_url" placeholder="http://serveur.com:8080">
                        </div>
                        <div class="form-group">
                            <label>Utilisateur</label>
                            <input type="text" name="xtream_username">
                        </div>
                        <div class="form-group">
                            <label>Mot de passe</label>
                            <input type="text" name="xtream_password">
                        </div>
                    </div>
                </div>

                <button type="submit">ACTIVER LA PLAYLIST</button>
            </form>
        </div>

        <div class="card">
            <h2>Playlists Actives</h2>
            <table>
                <thead>
                    <tr>
                        <th>MAC / ID</th>
                        <th>Nom</th>
                        <th>Type</th>
                        <th>Expiration</th>
                        <th>Statut</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($playlists as $p): ?>
                    <tr>
                        <td><code><?php echo htmlspecialchars($p['mac_address']); ?></code></td>
                        <td><?php echo htmlspecialchars($p['playlist_name']); ?></td>
                        <td>
                            <span class="badge badge-<?php echo htmlspecialchars($p['playlist_type']); ?>">
                                <?php echo htmlspecialchars(strtoupper($p['playlist_type'])); ?>
                            </span>
                        </td>
                        <td><?php echo htmlspecialchars($p['expire_date'] ?: 'Illimité'); ?></td>
                        <td>
                            <span style="color: <?php echo $p['is_active'] ? '#4CAF50' : '#F44336'; ?>">
                                ● <?php echo $p['is_active'] ? 'Active' : 'Inactive'; ?>
                            </span>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
