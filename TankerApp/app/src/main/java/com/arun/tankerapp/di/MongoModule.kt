            package com.arun.tankerapp.di

            import com.mongodb.kotlin.client.coroutine.MongoClient
            import com.mongodb.kotlin.client.coroutine.MongoDatabase
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            object MongoModule {

                // Use standard connection string to avoid Android JNDI (javax.naming) crash
        private const val CONNECTION_STRING = "mongodb://admin:ostraadmin@159.41.192.52:27017,159.41.192.72:27017,159.41.192.98:27017/?authSource=admin&replicaSet=atlas-430hbz-shard-0&tls=true&tlsAllowInvalidHostnames=true&w=majority&retryWrites=true&appName=Cluster0"
                private const val DATABASE_NAME = "tanker_db"

                @Provides
                @Singleton
                fun provideMongoClient(): MongoClient {
                    return MongoClient.create(CONNECTION_STRING)
                }

                @Provides
                @Singleton
                fun provideMongoDatabase(client: MongoClient): MongoDatabase {
                    return client.getDatabase(DATABASE_NAME)
                }
            }
