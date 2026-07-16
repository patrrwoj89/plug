package com.polishmediahub.app.data.plugin

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.source.MediaSource
import dalvik.system.DexClassLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicPluginLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {

    private val loadedPlugins = mutableMapOf<String, Pair<MediaSource, DexClassLoader>>()
    private val lock = Any()

    private val optimizedDexDir: File
        get() = File(context.codeCacheDir, "plugins_dex").apply { mkdirs() }

    /**
     * Loads a binary plugin (`.cs3`, `.cs4` or `.apk`) from [pluginFile] into RAM using
     * [DexClassLoader]. The class named [mainClassName] is instantiated and cast to
     * [MediaSource]. If the loaded class does not implement [MediaSource] directly,
     * a reflective adapter is used as a fallback.
     */
    fun loadPlugin(pluginFile: File, mainClassName: String): MediaSource? = synchronized(lock) {
        val cacheKey = pluginFile.absolutePath
        loadedPlugins[cacheKey]?.first?.let { return it }

        if (!pluginFile.exists()) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Plugin file does not exist: ${pluginFile.absolutePath}")
            return null
        }

        val pluginDexDir = File(optimizedDexDir, pluginFile.name).apply {
            deleteRecursively()
            mkdirs()
        }

        return try {
            val classLoader = DexClassLoader(
                pluginFile.absolutePath,
                pluginDexDir.absolutePath,
                null,
                context.classLoader
            )
            val clazz = classLoader.loadClass(mainClassName)
            val instance = instantiate(clazz, classLoader)

            val source = when (instance) {
                is MediaSource -> instance
                else -> if (instance != null) {
                    ReflectiveMediaSource(
                        id = "dex:${pluginFile.name}",
                        name = pluginFile.nameWithoutExtension,
                        pluginInstance = instance,
                        client = client
                    )
                } else null
            }

            if (source != null) {
                loadedPlugins[cacheKey] = source to classLoader
            } else {
                if (BuildConfig.DEBUG) Log.w(TAG, "Could not cast ${clazz.name} to MediaSource; class must implement ${MediaSource::class.java.name}")
            }
            source
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Failed to load plugin ${pluginFile.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Unloads a previously loaded plugin and deletes its optimized DEX cache.
     */
    fun unloadPlugin(pluginFile: File) = synchronized(lock) {
        val cacheKey = pluginFile.absolutePath
        loadedPlugins.remove(cacheKey)
        File(optimizedDexDir, pluginFile.name).deleteRecursively()
    }

    /**
     * Clears every optimized DEX cache. Call when refreshing or disabling all binary plugins.
     */
    fun clearDexCache() = synchronized(lock) {
        loadedPlugins.clear()
        optimizedDexDir.deleteRecursively()
    }

    private fun instantiate(clazz: Class<*>, classLoader: DexClassLoader): Any? {
        val constructors = clazz.declaredConstructors

        // Prefer a constructor that receives a Context and OkHttpClient.
        constructors.find { it.parameterTypes.contentEquals(arrayOf(Context::class.java, OkHttpClient::class.java)) }?.let {
            it.isAccessible = true
            return it.newInstance(context, client)
        }

        constructors.find { it.parameterTypes.contentEquals(arrayOf(OkHttpClient::class.java)) }?.let {
            it.isAccessible = true
            return it.newInstance(client)
        }

        constructors.find { it.parameterTypes.contentEquals(arrayOf(Context::class.java)) }?.let {
            it.isAccessible = true
            return it.newInstance(context)
        }

        constructors.find { it.getParameterTypes().size == 0 }?.let {
            it.isAccessible = true
            val instance = it.newInstance()
            injectDependencies(instance)
            return instance
        }

        // Last resort: try the first declared constructor and inject dependencies afterwards.
        constructors.firstOrNull()?.let {
            try {
                it.isAccessible = true
                val instance = it.newInstance(*Array(it.getParameterTypes().size) { null })
                injectDependencies(instance)
                return instance
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "constructor fallback failed: ${e.message}", e)
            }
        }

        return null
    }

    private fun injectDependencies(instance: Any) {
        val clazz = instance.javaClass
        clazz.methods.find { it.name == "setOkHttpClient" && it.getParameterTypes().size == 1 }
            ?.invokeSafe(instance, client)
        clazz.methods.find { it.name == "setContext" && it.getParameterTypes().size == 1 }
            ?.invokeSafe(instance, context)
    }

    private fun java.lang.reflect.Method.invokeSafe(instance: Any, value: Any?) {
        try {
            isAccessible = true
            invoke(instance, value)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "invokeSafe failed: ${e.message}", e)
        }
    }

    private companion object {
        private const val TAG = "DynamicPluginLoader"
    }
}
