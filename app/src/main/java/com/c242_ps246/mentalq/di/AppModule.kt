package com.c242_ps246.mentalq.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.c242_ps246.mentalq.BuildConfig
import com.c242_ps246.mentalq.data.local.room.AnalysisDao
import com.c242_ps246.mentalq.data.local.room.MentalQDatabase
import com.c242_ps246.mentalq.data.local.room.NoteDao
import com.c242_ps246.mentalq.data.local.room.UserDao
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.data.remote.retrofit.AnalysisApiService
import com.c242_ps246.mentalq.data.remote.retrofit.AuthApiService
import com.c242_ps246.mentalq.data.remote.retrofit.GeminiApiService
import com.c242_ps246.mentalq.data.remote.retrofit.MidtransApiService
import com.c242_ps246.mentalq.data.remote.retrofit.NoteApiService
import com.c242_ps246.mentalq.data.remote.retrofit.PsychologistApiService
import com.c242_ps246.mentalq.data.remote.retrofit.UserApiService
import com.c242_ps246.mentalq.data.repository.AnalysisRepository
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.repository.MidtransRepository
import com.c242_ps246.mentalq.data.repository.NoteRepository
import com.c242_ps246.mentalq.data.repository.PsychologistRepository
import com.c242_ps246.mentalq.data.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val GEMINI_BASE_URL = BuildConfig.GEMINI_BASE_URL


    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideWorkManager(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMentalQAppPreferences(context: Context): MentalQAppPreferences {
        return MentalQAppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseApp(context: Context): FirebaseApp =
        FirebaseApp.initializeApp(context)
            ?: throw IllegalStateException("FirebaseApp initialization failed")

    @Provides
    @Singleton
    fun provideNoteApiService(preferencesManager: MentalQAppPreferences): NoteApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = runBlocking {
                withContext(Dispatchers.IO) {
                    val token = preferencesManager.getToken().first()

                    originalRequest.newBuilder()
                        .apply {
                            if (token.isNotEmpty()) {
                                addHeader("Authorization", "Bearer $token")
                            }
                        }
                        .build()
                }
            }

            chain.proceed(newRequest)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NoteApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiService(preferencesManager: MentalQAppPreferences): UserApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = runBlocking {
                withContext(Dispatchers.IO) {
                    val token = preferencesManager.getToken().first()

                    originalRequest.newBuilder()
                        .apply {
                            if (token.isNotEmpty()) {
                                addHeader("Authorization", "Bearer $token")
                            }
                        }
                        .build()
                }
            }

            chain.proceed(newRequest)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnalysisApiService(preferencesManager: MentalQAppPreferences): AnalysisApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = runBlocking {
                withContext(Dispatchers.IO) {
                    val token = preferencesManager.getToken().first()

                    originalRequest.newBuilder()
                        .apply {
                            if (token.isNotEmpty()) {
                                addHeader("Authorization", "Bearer $token")
                            }
                        }
                        .build()
                }
            }

            chain.proceed(newRequest)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnalysisApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePsychologistApiService(): PsychologistApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PsychologistApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMidtransApiService(): MidtransApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MidtransApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMentalQDatabase(application: Application): MentalQDatabase {
        return Room.databaseBuilder(
            application,
            MentalQDatabase::class.java,
            "mentalq_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(mentalQDatabase: MentalQDatabase): NoteDao {
        return mentalQDatabase.noteDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(mentalQDatabase: MentalQDatabase): UserDao {
        return mentalQDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideAnalysisDao(mentalQDatabase: MentalQDatabase): AnalysisDao {
        return mentalQDatabase.analysisDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        noteApiService: NoteApiService,
        geminiApiService: GeminiApiService
    ): NoteRepository {
        return NoteRepository(noteDao, noteApiService, geminiApiService)
    }

    @Provides
    @Singleton
    fun provideMidtransRepository(
        midtransApiService: MidtransApiService
    ): MidtransRepository {
        return MidtransRepository(midtransApiService)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        userDao: UserDao,
        noteDao: NoteDao,
        analysisDao: AnalysisDao,
        preferencesManager: MentalQAppPreferences
    ): AuthRepository {
        return AuthRepository(authApiService, userDao, noteDao, analysisDao, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userApiService: UserApiService,
        userDao: UserDao
    ): UserRepository {
        return UserRepository(userApiService, userDao)
    }

    @Provides
    @Singleton
    fun provideAnalysisRepository(
        analysisApiService: AnalysisApiService,
        analysisDao: AnalysisDao
    ): AnalysisRepository {
        return AnalysisRepository(analysisApiService, analysisDao)
    }

    @Provides
    @Singleton
    fun providePsychologistRepository(
        psychologistApiService: PsychologistApiService
    ): PsychologistRepository {
        return PsychologistRepository(psychologistApiService)
    }
}
