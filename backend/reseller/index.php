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

// ── Déconnexion explicite (le lien "Déconnexion" du dashboard pointe ici) ──
if (isset($_GET['logout'])) {
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $p = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $p['path'], $p['domain'], $p['secure'], $p['httponly']);
    }
    session_destroy();
    header('Location: index.php');
    exit;
}

// Déjà connecté → redirection directe
if (!empty($_SESSION['reseller_id'])) {
    header('Location: dashboard.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = (string)($_POST['password'] ?? '');

    // ── Anti brute-force : 5 échecs → verrouillage 15 minutes ──
    $now = time();
    $failures = (int)($_SESSION['login_failures'] ?? 0);
    $lastFail = (int)($_SESSION['login_last_fail'] ?? 0);

    if ($failures >= 5 && ($now - $lastFail) < 900) {
        $error = "Trop de tentatives. Réessayez dans quelques minutes.";
        skyLog("Blocage brute-force revendeur (IP " . ($_SERVER['REMOTE_ADDR'] ?? '?') . ")", 'warn');
    } else {
        // ── Refus des mots de passe par défaut / faibles ──
        $weakPasswords = ['change-me-in-production', 'admin123', 'changeme', 'password', '123456'];
        if (in_array(RESELLER_PASS, $weakPasswords, true)) {
            $error = "Le mot de passe par défaut est interdit. Changez RESELLER_PASS dans backend/config.php.";
            skyLog("Connexion refusée : mot de passe par défaut (RESELLER_PASS)", 'warn');
        } elseif (hash_equals(RESELLER_USER, $username) && hash_equals(RESELLER_PASS, $password)) {
            session_regenerate_id(true); // Anti fixation de session
            $_SESSION['reseller_id'] = 1;
            $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
            unset($_SESSION['login_failures'], $_SESSION['login_last_fail']);
            header('Location: dashboard.php');
            exit;
        } else {
            $error = "Identifiants invalides";
            $_SESSION['login_failures'] = $failures + 1;
            $_SESSION['login_last_fail'] = $now;
            usleep(500000); // Ralentit le brute-force
        }
    }
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>SkyPlayer Pro - Espace Revendeur</title>
    <style>
        body { font-family: sans-serif; background: #0F0F0F; color: white; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
        .login-box { background: #1A1A1A; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.5); width: 300px; }
        input { width: 100%; padding: 0.5rem; margin: 0.5rem 0; background: #333; border: none; color: white; border-radius: 4px; box-sizing: border-box; }
        button { width: 100%; padding: 0.7rem; background: #00A3FF; border: none; color: white; border-radius: 4px; cursor: pointer; font-weight: bold; }
        h2 { text-align: center; color: #00A3FF; }
        .error { color: #FF4D4D; font-size: 0.8rem; margin-bottom: 1rem; }
    </style>
</head>
<body>
    <div class="login-box">
        <h2>REVENDEUR</h2>
        <?php if ($error !== ''): ?>
            <p class="error"><?php echo htmlspecialchars($error); ?></p>
        <?php endif; ?>
        <form method="POST">
            <input type="text" name="username" placeholder="Nom d'utilisateur" required autocomplete="username">
            <input type="password" name="password" placeholder="Mot de passe" required autocomplete="current-password">
            <button type="submit">SE CONNECTER</button>
        </form>
    </div>
</body>
</html>
