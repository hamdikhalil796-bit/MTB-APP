package com.mtb.mtb.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.MessageDigest
import java.security.SecureRandom

enum class ProfileError { INVALID_LOGIN, DISABLED, USERNAME_INVALID, USERNAME_TAKEN, PASSWORD_SHORT, NETWORK, OTHER }

/**
 * Simple in-app login (no Firebase Authentication), mirroring the website's src/lib/profiles.ts —
 * same Firestore documents and the same password hashing, so profiles work in both apps:
 *   profiles/{username}  username, displayName, isSupervisor, active, permissions, salt, passwordHash
 *   settings/guest       permissions
 *   settings/setup       exists once the first supervisor was created
 * passwordHash = SHA-256 hex of (salt + password), so passwords are never stored readable.
 * The database itself can't tell who is logged in, so permissions are applied by the apps.
 */
object ProfileRepository {
    private const val PROFILES = "profiles"
    private const val SETTINGS = "settings"
    const val MIN_PASSWORD_LENGTH = 6
    private val USERNAME_PATTERN = Regex("^[a-z0-9._-]{3,20}$")

    private val db get() = FirebaseFirestore.getInstance()

    fun normalizeUsername(username: String): String = username.trim().lowercase()

    private fun toProfile(snap: DocumentSnapshot): Profile = Profile(
        username = snap.getString("username") ?: snap.id,
        displayName = snap.getString("displayName") ?: "",
        isSupervisor = snap.getBoolean("isSupervisor") == true,
        active = snap.getBoolean("active") != false,
        permissions = Permissions.fromMap(snap.get("permissions"))
    )

    private fun profileFields(displayName: String, isSupervisor: Boolean, active: Boolean, permissions: Permissions): Map<String, Any> =
        mapOf(
            "displayName" to displayName.trim(),
            "isSupervisor" to isSupervisor,
            "active" to active,
            "permissions" to permissions.toMap()
        )

    fun hashPassword(salt: String, password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun passwordFields(password: String): Map<String, Any> {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = bytes.joinToString("") { "%02x".format(it) }
        return mapOf("salt" to salt, "passwordHash" to hashPassword(salt, password))
    }

    /** Checks the password; on success returns the profile, otherwise the error. */
    fun checkLogin(username: String, password: String, onResult: (Profile?, ProfileError?) -> Unit) {
        val name = normalizeUsername(username)
        if (!USERNAME_PATTERN.matches(name) || password.isEmpty()) return onResult(null, ProfileError.INVALID_LOGIN)
        db.collection(PROFILES).document(name).get()
            .addOnSuccessListener { snap ->
                val salt = snap.getString("salt") ?: ""
                if (!snap.exists() || hashPassword(salt, password) != snap.getString("passwordHash")) {
                    return@addOnSuccessListener onResult(null, ProfileError.INVALID_LOGIN)
                }
                val profile = toProfile(snap)
                if (!profile.active) onResult(null, ProfileError.DISABLED) else onResult(profile, null)
            }
            .addOnFailureListener { onResult(null, ProfileError.NETWORK) }
    }

    fun listenProfile(username: String, onChange: (Profile?) -> Unit): ListenerRegistration =
        db.collection(PROFILES).document(username).addSnapshotListener { snap, error ->
            if (error != null) {
                onChange(null)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            // A cache-only "missing" just means the server hasn't answered yet; treating it as
            // "profile deleted" would log the user straight back out.
            if (!snap.exists() && snap.metadata.isFromCache) return@addSnapshotListener
            onChange(if (snap.exists()) toProfile(snap) else null)
        }

    fun listenProfiles(onChange: (List<Profile>) -> Unit): ListenerRegistration =
        db.collection(PROFILES).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            onChange(snap.documents.map(::toProfile).sortedBy { it.username })
        }

    fun listenGuestPermissions(onChange: (Permissions) -> Unit): ListenerRegistration =
        db.collection(SETTINGS).document("guest").addSnapshotListener { snap, _ ->
            onChange(if (snap != null && snap.exists()) Permissions.fromMap(snap.get("permissions")) else Permissions.DEFAULT_GUEST)
        }

    fun saveGuestPermissions(permissions: Permissions, onResult: (ProfileError?) -> Unit) {
        db.collection(SETTINGS).document("guest").set(mapOf("permissions" to permissions.toMap()))
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(ProfileError.OTHER) }
    }

    fun checkFirstSetupDone(onResult: (Boolean) -> Unit) {
        db.collection(SETTINGS).document("setup").get()
            .addOnSuccessListener { onResult(it.exists()) }
            // Offline: show the normal login form rather than the setup form.
            .addOnFailureListener { onResult(true) }
    }

    /** One-time setup on the login screen: creates the first supervisor. */
    fun createFirstSupervisor(displayName: String, username: String, password: String, onResult: (ProfileError?) -> Unit) {
        val name = normalizeUsername(username)
        createProfile(name, password, displayName, isSupervisor = true, permissions = Permissions()) { error ->
            if (error != null) return@createProfile onResult(error)
            db.runBatch { batch ->
                batch.set(db.collection(SETTINGS).document("guest"), mapOf("permissions" to Permissions.DEFAULT_GUEST.toMap()))
                batch.set(db.collection(SETTINGS).document("setup"), mapOf("doneBy" to name))
            }.addOnSuccessListener { onResult(null) }
                .addOnFailureListener { onResult(ProfileError.NETWORK) }
        }
    }

    fun createProfile(
        username: String,
        password: String,
        displayName: String,
        isSupervisor: Boolean,
        permissions: Permissions,
        onResult: (ProfileError?) -> Unit
    ) {
        val name = normalizeUsername(username)
        if (!USERNAME_PATTERN.matches(name)) return onResult(ProfileError.USERNAME_INVALID)
        if (password.length < MIN_PASSWORD_LENGTH) return onResult(ProfileError.PASSWORD_SHORT)
        val ref = db.collection(PROFILES).document(name)
        ref.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) return@addOnSuccessListener onResult(ProfileError.USERNAME_TAKEN)
                val data = profileFields(displayName, isSupervisor, true, permissions) +
                    mapOf("username" to name) + passwordFields(password)
                ref.set(data)
                    .addOnSuccessListener { onResult(null) }
                    .addOnFailureListener { onResult(ProfileError.NETWORK) }
            }
            .addOnFailureListener { onResult(ProfileError.NETWORK) }
    }

    fun updateProfile(profile: Profile, displayName: String, isSupervisor: Boolean, active: Boolean, permissions: Permissions, onResult: (ProfileError?) -> Unit) {
        db.collection(PROFILES).document(profile.username)
            .update(profileFields(displayName, isSupervisor, active, permissions))
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(ProfileError.NETWORK) }
    }

    fun resetPassword(profile: Profile, newPassword: String, onResult: (ProfileError?) -> Unit) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) return onResult(ProfileError.PASSWORD_SHORT)
        db.collection(PROFILES).document(profile.username)
            .update(passwordFields(newPassword))
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(ProfileError.NETWORK) }
    }

    fun deleteProfile(profile: Profile, onResult: (ProfileError?) -> Unit) {
        db.collection(PROFILES).document(profile.username).delete()
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(ProfileError.NETWORK) }
    }
}
