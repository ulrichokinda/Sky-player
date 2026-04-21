# Firestore Security Specification - IPTV Management System

## Content Overview
This document defines the security invariants and testing strategy for the IPTV Management System Firestore database.

## 1. Data Invariants
- **Users**: 
  - Every user must have a unique profile at `/users/{uid}`.
  - Roles are immutable for non-admin users.
  - Credits can only be modified by admins or system-triggered payment completions.
- **Activations**:
  - An activation must be linked to a valid reseller (user with reseller/admin role).
  - MAC addresses must follow a standard format (12-17 characters).
  - Playlist URLs and Xtream credentials can only be set by the owning reseller or an admin.
  - Heartbeat fields (`last_connection`, `current_channel`) can be updated if the request validates as a system update.
- **Payments**:
  - Payments are immutable once set to 'SUCCESS' or 'FAILED'.
  - Users can only read their own payment history.

## 2. The "Dirty Dozen" Payloads (Attack Vectors)

| # | Attack Type | Target Path | Malicious Payload | Expected Result |
|---|-------------|-------------|-------------------|-----------------|
| 1 | Identity Spoofing | `/users/victim_uid` | `{ role: 'admin' }` | PERMISSION_DENIED |
| 2 | Credit Theft | `/users/attacker_uid` | `{ credits: 999999 }` | PERMISSION_DENIED |
| 3 | Orphaned Activation | `/activations/new` | `{ resellerId: 'non_existent' }` | PERMISSION_DENIED |
| 4 | MAC Poisoning | `/activations/new` | `{ target_mac: 'A'.repeat(500) }` | PERMISSION_DENIED |
| 5 | Cross-User Access | `/activations/victim_act` | `{ playlist_url: 'http://malicious.com' }` | PERMISSION_DENIED |
| 6 | System Field Injection | `/activations/act_id` | `{ resellerId: 'new_reseller' }` (Update) | PERMISSION_DENIED |
| 7 | Payment Forgery | `/payments/fake_pay` | `{ status: 'SUCCESS', userId: 'attacker' }` | PERMISSION_DENIED |
| 8 | Terminal State Bypass | `/payments/success_pay` | `{ amount: 0 }` | PERMISSION_DENIED |
| 9 | PII Leak (List) | `/users` | `allow list: if isSignedIn()` | REJECTED BY AUDIT |
| 10 | Shadow Update | `/activations/id` | `{ note: 'ok', ghost_field: true }` | PERMISSION_DENIED |
| 11 | ID Injection | `/activations/..%2F..%2Fsys` | Any payload | PERMISSION_DENIED |
| 12 | Bulk Read Scraping | `activations.get()` | Unauthorized UID fetch | PERMISSION_DENIED |

## 3. Implementation Plan
- [x] Create helper functions for `isValidId`, `isAdmin`, `isOwner`.
- [x] Implement `isValidUser`, `isValidActivation`, `isValidPayment` blueprints.
- [x] Apply `affectedKeysOnly` for tiered updates.
- [x] Restrict `list` operations to enforce owner filtering.
- [x] Verify server-side admin access via `FIREBASE_SERVICE_ACCOUNT`.
