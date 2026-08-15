package com.c242_ps246.mentalq.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.c242_ps246.mentalq.BuildConfig
import com.c242_ps246.mentalq.data.local.room.AnalysisDao
import com.c242_ps246.mentalq.data.local.room.MIGRATION_12_13
import com.c242_ps246.mentalq.data.local.room.LEGACY_CACHE_MIGRATIONS
import com.c242_ps246.mentalq.data.local.room.MentalQDatabase
import com.c242_ps246.mentalq.data.local.room.NoteDao
import com.c242_ps246.mentalq.data.local.room.UserDao
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.data.remote.retrofit.AnalysisApiService
import com.c242_ps246.mentalq.data.remote.retrofit.AuthApiService
import com.c242_ps246.mentalq.data.remote.retrofit.MidtransApiService
import com.c242_ps246.mentalq.data.remote.retrofit.NoteApiService
import com.c242_ps246.mentalq.data.remote.retrofit.PsychologistApiService
import com.c242_ps246.mentalq.data.remote.retrofit.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(preferences: MentalQAppPreferences): Interceptor = Interceptor { chain ->
        val token = preferences.currentToken()
        val request = chain.request().newBuilder().apply {
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @PublicApi
    fun providePublicClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @AuthenticatedApi
    fun provideAuthenticatedClient(
        logging: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @PublicApi
    fun providePublicRetrofit(@PublicApi client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @AuthenticatedApi
    fun provideAuthenticatedRetrofit(@AuthenticatedApi client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApiService(@PublicApi retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideNoteApiService(@AuthenticatedApi retrofit: Retrofit): NoteApiService =
        retrofit.create(NoteApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApiService(@AuthenticatedApi retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideAnalysisApiService(@AuthenticatedApi retrofit: Retrofit): AnalysisApiService =
        retrofit.create(AnalysisApiService::class.java)

    @Provides
    @Singleton
    fun providePsychologistApiService(@AuthenticatedApi retrofit: Retrofit): PsychologistApiService =
        retrofit.create(PsychologistApiService::class.java)

    @Provides
    @Singleton
    fun provideMidtransApiService(@AuthenticatedApi retrofit: Retrofit): MidtransApiService =
        retrofit.create(MidtransApiService::class.java)

    @Provides
    @Singleton
    fun provideMentalQDatabase(@ApplicationContext context: Context): MentalQDatabase =
        Room.databaseBuilder(context, MentalQDatabase::class.java, "mentalq_database")
            .addMigrations(MIGRATION_12_13)
            .addMigrations(*LEGACY_CACHE_MIGRATIONS.toTypedArray())
            .build()

    @Provides
    fun provideNoteDao(database: MentalQDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideUserDao(database: MentalQDatabase): UserDao = database.userDao()

    @Provides
    fun provideAnalysisDao(database: MentalQDatabase): AnalysisDao = database.analysisDao()
}
