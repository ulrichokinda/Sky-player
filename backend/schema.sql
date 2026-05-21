-- ============================================================
-- SkyPlayer Pro — Structure base de données (MySQL 5.7+)
-- Déployer sur : skyplayerapp.xyz
-- Base : skyplayer_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS skyplayer_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE skyplayer_db;

-- ── Table principale des playlists par MAC ──────────────────
CREATE TABLE IF NOT EXISTS playlists (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- Identifiant appareil (MAC Android virtuelle 8 segments)
    mac_address         VARCHAR(50) NOT NULL,

    -- Informations playlist
    playlist_name       VARCHAR(255) NOT NULL DEFAULT 'Ma Playlist',
    playlist_type       ENUM('m3u', 'xtream') NOT NULL DEFAULT 'm3u',

    -- Pour type M3U : URL directe du fichier .m3u / .m3u8
    playlist_url        TEXT NULL,

    -- Pour type Xtream Codes : credentials séparés
    xtream_username     VARCHAR(100) NULL,
    xtream_password     VARCHAR(100) NULL,
    xtream_server_url   VARCHAR(500) NULL,

    -- Gestion des accès
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    creation_date       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_date         DATE NULL COMMENT 'NULL = illimité',

    -- Métadonnées optionnelles
    reseller_id         INT UNSIGNED NULL COMMENT 'ID revendeur',
    notes               TEXT NULL,
    last_checked        DATETIME NULL,

    -- Index pour performance
    INDEX idx_mac        (mac_address),
    INDEX idx_active     (is_active, expire_date),
    INDEX idx_mac_active (mac_address, is_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── Table des appareils enregistrés (audit) ─────────────────
CREATE TABLE IF NOT EXISTS devices (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(100) NOT NULL UNIQUE,
    brand           VARCHAR(100) NULL,
    model           VARCHAR(100) NULL,
    android_version VARCHAR(20)  NULL,
    first_seen      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    trial_start     DATETIME NULL,
    trial_expire    DATETIME NULL,
    is_activated    TINYINT(1) NOT NULL DEFAULT 0,

    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── Données de test ──────────────────────────────────────────
-- (Remplacer par les vraies MACs de vos clients)
INSERT INTO playlists (mac_address, playlist_name, playlist_type, playlist_url, expire_date) VALUES
    ('AA:BB:CC:DD:EE:FF', 'Playlist Test M3U', 'm3u',
     'https://example.com/playlist.m3u', '2027-12-31'),
    ('11:22:33:44:55:66', 'Playlist Xtream Test', 'xtream',
     NULL, '2026-12-31');

-- Exemple Xtream (mettre les vrais credentials)
UPDATE playlists
SET xtream_username = 'client001',
    xtream_password = 'pass123',
    xtream_server_url = 'http://votre-serveur-xtream.com:8080'
WHERE mac_address = '11:22:33:44:55:66';
