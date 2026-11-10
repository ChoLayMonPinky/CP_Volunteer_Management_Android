package dev.tintwym.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.tintwym.BuildConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object BackendClient {
  private val tokenRef = AtomicReference<String?>(null)

  fun setToken(token: String?) {
    tokenRef.set(token)
  }

  val isConfigured: Boolean
    get() = BuildConfig.API_BASE_URL.isNotBlank()

  private val baseUrl: String
    get() = BuildConfig.API_BASE_URL.trim().trimEnd('/') + "/"

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }

  private val authInterceptor = Interceptor { chain ->
    val token = tokenRef.get()
    val req = if (!token.isNullOrBlank()) {
      chain.request().newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
    } else {
      chain.request()
    }
    chain.proceed(req)
  }

  private val okHttp: OkHttpClient by lazy {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }
    OkHttpClient.Builder()
      // Render free tier cold starts are often ~45–60s.
      .connectTimeout(90, TimeUnit.SECONDS)
      .readTimeout(120, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .addInterceptor(authInterceptor)
      .addInterceptor(logging)
      .build()
  }

  /** Fire-and-forget health ping so the API can wake during splash/boot. */
  fun warmup() {
    if (!isConfigured) return
    Thread {
      runCatching {
        okHttp.newCall(
          okhttp3.Request.Builder()
            .url(baseUrl.trimEnd('/') + "/actuator/health/liveness")
            .get()
            .build(),
        ).execute().close()
      }
    }.apply { isDaemon = true; name = "cg-api-warmup"; start() }
  }

  val api: CommonGroundApi by lazy {
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttp)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(CommonGroundApi::class.java)
  }
}
