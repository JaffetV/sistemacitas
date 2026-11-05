package com.example.comunamadsistemacitas.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.comunamadsistemacitas.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MarketplaceApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String
) : IOException(message)

/**
 * Cliente HTTP del backend v1.
 * Las funciones síncronas deben llamarse desde un hilo IO; ejecutarAsync permite hacerlo desde UI.
 * En debug se admite HTTP para emulador/LAN. En release se exige HTTPS.
 */
class MarketplaceApiClient(
    context: Context,
    baseUrl: String,
    allowInsecureForDevelopment: Boolean = BuildConfig.DEBUG,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000
) {
    private val rootUrl = baseUrl.trimEnd('/')
    private val session = MarketplaceSessionStore(context.applicationContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(3)

    init {
        val parsedUrl = runCatching { URI(rootUrl) }.getOrNull()
        val scheme = parsedUrl?.scheme
        require(scheme == "https" || (scheme == "http" && BuildConfig.DEBUG && allowInsecureForDevelopment)) {
            "Usa HTTPS. HTTP solo está permitido en compilaciones debug para desarrollo local."
        }
        require(parsedUrl?.host?.isNotBlank() == true) { "Configura una URL válida para el servidor marketplace." }
    }

    fun iniciarSesion(email: String, password: String): JSONObject {
        val response = post("/auth/login", JSONObject().put("email", email).put("password", password), false)
        val data = response.getJSONObject("data")
        session.guardarToken(data.getString("token"))
        return data.getJSONObject("user")
    }

    fun registrarCliente(nombre: String, email: String, telefono: String, password: String): JSONObject {
        val body = JSONObject()
            .put("fullName", nombre)
            .put("email", email)
            .put("phone", telefono)
            .put("password", password)
        val data = post("/auth/register/customer", body, false).getJSONObject("data")
        session.guardarToken(data.getString("token"))
        return data.getJSONObject("user")
    }

    fun registrarNegocio(datos: JSONObject): JSONObject {
        val data = post("/auth/register/business", datos, false).getJSONObject("data")
        session.guardarToken(data.getString("token"))
        return data.getJSONObject("business")
    }

    /** Para un usuario que ya inició sesión y quiere abrir un segundo negocio. */
    fun crearOtroNegocio(datos: JSONObject): JSONObject =
        post("/businesses", datos, true).getJSONObject("data")

    fun obtenerMiPerfil(): JSONObject = get("/me", true).getJSONObject("data")

    fun obtenerMisNegocios(): JSONArray = get("/me/businesses", true).getJSONArray("data")

    fun obtenerCategorias(): JSONArray = get("/categories", false).getJSONArray("data")

    fun explorarNegocios(texto: String = "", categoriaId: String? = null, distrito: String = ""): JSONArray {
        val query = buildList {
            if (texto.isNotBlank()) add("q=${codificar(texto)}")
            if (!categoriaId.isNullOrBlank()) add("categoryId=${codificar(categoriaId)}")
            if (distrito.isNotBlank()) add("district=${codificar(distrito)}")
        }.joinToString("&")
        val suffix = if (query.isBlank()) "" else "?$query"
        return get("/businesses$suffix", false).getJSONArray("data")
    }

    fun obtenerNegocio(negocioId: String): JSONObject =
        get("/businesses/${codificarPath(negocioId)}", false).getJSONObject("data")

    fun obtenerPublicaciones(negocioId: String): JSONArray =
        get("/businesses/${codificarPath(negocioId)}/listings", false).getJSONArray("data")

    fun publicar(negocioId: String, publicacion: JSONObject): JSONObject =
        post("/businesses/${codificarPath(negocioId)}/listings", publicacion, true).getJSONObject("data")

    fun obtenerHorarios(negocioId: String): JSONArray =
        get("/businesses/${codificarPath(negocioId)}/hours", false).getJSONArray("data")

    fun configurarHorarios(negocioId: String, horarios: JSONArray): JSONObject =
        request("PUT", "/businesses/${codificarPath(negocioId)}/hours", JSONObject().put("hours", horarios), true)

    fun crearReserva(reserva: JSONObject): JSONObject =
        post("/me/reservations", reserva, true).getJSONObject("data")

    fun obtenerMisReservas(): JSONArray = get("/me/reservations", true).getJSONArray("data")

    fun cancelarMiReserva(reservaId: String): JSONObject =
        post("/me/reservations/${codificarPath(reservaId)}/cancel", JSONObject(), true).getJSONObject("data")

    fun obtenerReservasNegocio(negocioId: String): JSONArray =
        get("/businesses/${codificarPath(negocioId)}/reservations", true).getJSONArray("data")

    fun cambiarEstadoReserva(negocioId: String, reservaId: String, estado: String): JSONObject =
        request(
            "PATCH",
            "/businesses/${codificarPath(negocioId)}/reservations/${codificarPath(reservaId)}/status",
            JSONObject().put("status", estado),
            true
        ).getJSONObject("data")

    fun guardarFavorito(negocioId: String): JSONObject =
        post("/me/favorites", JSONObject().put("businessId", negocioId), true)

    fun obtenerFavoritos(): JSONArray = get("/me/favorites", true).getJSONArray("data")

    fun cerrarSesion() = session.cerrarSesion()

    /** Indica si el dispositivo conserva un token JWT legible y no corrupto. */
    fun haySesion(): Boolean = session.obtenerToken() != null

    fun <T> ejecutarAsync(operacion: () -> T, callback: (Result<T>) -> Unit) {
        ioExecutor.execute {
            val result = runCatching(operacion)
            mainHandler.post { callback(result) }
        }
    }

    fun liberar() {
        ioExecutor.shutdown()
    }

    private fun get(path: String, authenticated: Boolean): JSONObject = request("GET", path, null, authenticated)

    private fun post(path: String, body: JSONObject, authenticated: Boolean): JSONObject =
        request("POST", path, body, authenticated)

    private fun request(method: String, path: String, body: JSONObject?, authenticated: Boolean): JSONObject {
        val connection = (URI("$rootUrl/api/v1$path").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (authenticated) {
                val token = session.obtenerToken()
                    ?: throw MarketplaceApiException(401, "UNAUTHENTICATED", "Inicia sesión para continuar.")
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input -> BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText() }.orEmpty()
            val response = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse {
                throw IOException("El servidor devolvió una respuesta no válida.")
            }
            if (status !in 200..299) {
                if (status == 401) session.cerrarSesion()
                val error = response.optJSONObject("error") ?: JSONObject()
                throw MarketplaceApiException(
                    status,
                    error.optString("code", "HTTP_ERROR"),
                    error.optString("message", "No se pudo completar la solicitud.")
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun codificar(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun codificarPath(value: String): String = codificar(value).replace("+", "%20")
}
