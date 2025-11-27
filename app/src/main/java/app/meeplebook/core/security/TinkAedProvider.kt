package app.meeplebook.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class TinkAeadProvider(context: Context) {

    companion object {
        private const val KEYSET_NAME = "meeplebook_master_keyset"
        private const val PREF_FILE = "meeplebook_master_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://meeplebook_aead_master_key"
    }

    private val keysetHandle: KeysetHandle

    init {
        // required once
        AeadConfig.register()

        keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
    }

    fun getAead(): Aead {
        return keysetHandle.getPrimitive(RegistryConfiguration.get(),Aead::class.java)
    }
}