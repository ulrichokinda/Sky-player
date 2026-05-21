$ErrorActionPreference = "Stop"

Write-Host "== SkyPlayer Pro: deploy Firebase rules =="

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    Write-Error "Firebase CLI non installee. Installe d'abord: npm install -g firebase-tools"
}

if (-not (Test-Path "firebase.json")) {
    Write-Error "firebase.json introuvable a la racine du projet."
}

Write-Host "1) Verification login Firebase..."
firebase login:list | Out-Null

Write-Host "2) Projet courant Firebase..."
firebase use

Write-Host "3) Deploiement des regles Realtime Database..."
firebase deploy --only database

Write-Host "Deploiement termine."
