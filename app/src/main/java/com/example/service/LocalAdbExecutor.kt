package com.example.service

import android.content.Context
import android.util.Log
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.callbackFlow
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * Executes local ADB shell commands by using a bundled native ADB binary.
 * 
 * NOTE: For this to work in production without Shizuku, you must bundle an ARM-compiled 
 * "adb" binary into your app's src/main/assets/ directory.
 * Apps like LADB use this exact approach.
 */
import kotlinx.coroutines.channels.awaitClose

class LocalAdbExecutor(private val context: Context) {
    
    private val adbBinaryFile = File(context.applicationInfo.nativeLibraryDir, "libadb.so")

    init {
        if (!adbBinaryFile.exists()) {
            Log.e("LocalAdb", "libadb.so not found in nativeLibraryDir!")
        } else {
            // Ensure it has executable permissions
            adbBinaryFile.setExecutable(true)
        }
    }

    /**
     * Executes the pairing command: adb pair 127.0.0.1:PORT CODE
     */
    suspend fun pairWireless(ip: String, port: Int, code: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val command = listOf(adbBinaryFile.absolutePath, "pair", "$ip:$port", code)
        val output = executeShellCommand(command)
        val success = output.contains("Successfully paired") || output.contains("Success") || output.contains("successfully paired")
        return@withContext Pair(success, output)
    }

    /**
     * Executes the connect command: adb connect IP:PORT
     */
    suspend fun connectWireless(ip: String, port: Int): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val command = listOf(adbBinaryFile.absolutePath, "connect", "$ip:$port")
        val output = executeShellCommand(command)
        val success = output.contains("connected to") || output.contains("already connected")
        return@withContext Pair(success, output)
    }

    /**
     * Executes an arbitrary ADB shell command once connected.
     */
    suspend fun executeAdbShell(shellCommand: String, ip: String = "127.0.0.1", port: Int? = null): String = withContext(Dispatchers.IO) {
        val commandList = mutableListOf(adbBinaryFile.absolutePath)
        if (port != null) {
            commandList.add("-s")
            commandList.add("$ip:$port")
        }
        commandList.add("shell")
        commandList.addAll(shellCommand.split(" "))
        return@withContext executeShellCommand(commandList)
    }

    fun startLogcat(ip: String = "127.0.0.1", port: Int? = null): Flow<String> = callbackFlow {
        val commandList = mutableListOf(adbBinaryFile.absolutePath)
        if (port != null) {
            commandList.add("-s")
            commandList.add("$ip:$port")
        }
        commandList.add("logcat")

        var process: Process? = null
        try {
            val processBuilder = ProcessBuilder(commandList)
            processBuilder.environment()["HOME"] = context.filesDir.absolutePath
            processBuilder.environment()["TMPDIR"] = context.cacheDir.absolutePath
            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)
            
            process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String? = null
            while (isActive && reader.readLine().also { line = it } != null) {
                line?.let { trySend(it) }
            }
        } catch (e: Exception) {
            Log.e("LocalAdb", "Failed to execute logcat", e)
            trySend("Error: ${e.message}")
        } finally {
            process?.destroy()
            close()
        }
        
        awaitClose {
            process?.destroy()
        }
    }

    private fun executeShellCommand(command: List<String>): String {
        return try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.environment()["HOME"] = context.filesDir.absolutePath
            processBuilder.environment()["TMPDIR"] = context.cacheDir.absolutePath
            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
                Log.d("LocalAdb", "Output: $line")
            }
            
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            Log.e("LocalAdb", "Failed to execute command: $command", e)
            e.message ?: "Unknown error"
        }
    }
}
