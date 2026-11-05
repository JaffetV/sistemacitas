package com.example.comunamadsistemacitas.data.remote

import android.content.Context
import com.example.comunamadsistemacitas.BuildConfig

/**
 * Punto único de configuración del servidor.
 *
 * Debug usa por defecto 10.0.2.2 (host visto desde el emulador Android).
 * Para un teléfono físico, configura marketplaceApiBaseUrl con la IP LAN
 * del equipo que ejecuta Docker/API. En release, MarketplaceApiClient exige HTTPS.
 */
object MarketplaceApiConfig {
    fun crearCliente(context: Context): MarketplaceApiClient =
        MarketplaceApiClient(
            context = context.applicationContext,
            baseUrl = BuildConfig.MARKETPLACE_API_BASE_URL
        )
}
