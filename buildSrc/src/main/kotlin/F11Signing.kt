import java.io.File
import java.util.Properties
import org.gradle.api.Project

/** Release signing shared by F11 and the F11 Shield plugin; both must use the same key. */
data class F11Signing(val storeFile: File, val storePassword: String?, val keyAlias: String?, val keyPassword: String?)

/**
 * Reads keystore.properties (git-ignored) or F11_* environment variables.
 * Returns null when neither is set, so release builds come out unsigned.
 */
fun f11ReleaseSigning(root: Project): F11Signing? {
    val props = Properties().apply {
        val file = root.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    fun value(key: String, env: String): String? = props.getProperty(key) ?: System.getenv(env)
    val store = value("storeFile", "F11_STORE_FILE") ?: return null
    return F11Signing(
        storeFile = root.file(store),
        storePassword = value("storePassword", "F11_STORE_PASSWORD"),
        keyAlias = value("keyAlias", "F11_KEY_ALIAS"),
        keyPassword = value("keyPassword", "F11_KEY_PASSWORD"),
    )
}
