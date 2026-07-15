package com.polishmediahub.app.data.torrent

import android.util.Log
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentHttpServer @Inject constructor(
    private val engine: TorrentEngine
) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeClients = AtomicInteger(0)

    var port: Int = 0
        private set

    fun start(): Int = synchronized(this) {
        serverSocket?.let { return port }
        val socket = ServerSocket(0)
        port = socket.localPort
        serverSocket = socket
        scope.launch {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    activeClients.incrementAndGet()
                    scope.launch { handleClient(client) }
                } catch (e: Exception) {
                    if (!socket.isClosed) Log.w("TorrentHttpServer", "accept error: ${e.message}")
                }
            }
        }
        port
    }

    fun stop() = synchronized(this) {
        try {
            serverSocket?.close()
            scope.cancel()
        } catch (_: Exception) {
        }
        serverSocket = null
        port = 0
    }

    fun resolveUrl(infoHash: String, fileIndex: Int): String {
        if (port == 0) start()
        return "http://127.0.0.1:$port/stream?infoHash=$infoHash&file=$fileIndex"
    }

    private fun handleClient(client: Socket) {
        try {
            client.use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1]

                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        headers[line.substring(0, colon).trim().lowercase(Locale.getDefault())] =
                            line.substring(colon + 1).trim()
                    }
                }

                if (method != "GET" && method != "HEAD") {
                    writeResponse(socket, 405, "Method Not Allowed", emptyMap(), null)
                    return
                }

                val query = path.substringAfter('?', "")
                val params = parseQuery(query)
                val infoHash = params["infohash"] ?: params["infoHash"] ?: params["info_hash"]
                val fileIndex = params["file"]?.toIntOrNull() ?: 0

                if (infoHash.isNullOrBlank()) {
                    writeResponse(socket, 400, "Bad Request", emptyMap(), null)
                    return
                }

                val handle = engine.findHandle(infoHash)
                val fileInfo = engine.findFileInfo(infoHash)
                if (handle == null || fileInfo == null) {
                    writeResponse(socket, 404, "Not Found", emptyMap(), null)
                    return
                }

                val file = File(fileInfo.path)
                if (!file.exists() && fileInfo.size > 0) {
                    // File may be allocated later; wait briefly then try again
                    Thread.sleep(100)
                }

                val totalSize = fileInfo.size
                val rangeHeader = headers["range"]
                val (start, end, statusCode) = if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val rangeSpec = rangeHeader.substringAfter("bytes=").trim()
                    val (s, e) = rangeSpec.split('-')
                    val start = s.toLongOrNull() ?: 0L
                    val end = if (e.isNotBlank()) e.toLongOrNull() ?: (totalSize - 1) else (totalSize - 1)
                    Triple(start, end.coerceAtMost(totalSize - 1), 206)
                } else {
                    Triple(0L, totalSize - 1, 200)
                }

                if (start > end || start >= totalSize) {
                    writeResponse(socket, 416, "Range Not Satisfiable", emptyMap(), null)
                    return
                }

                val contentLength = end - start + 1
                val contentType = contentTypeForFile(fileInfo.name)
                val responseHeaders = mutableMapOf(
                    "Content-Type" to contentType,
                    "Content-Length" to contentLength.toString(),
                    "Accept-Ranges" to "bytes",
                    "Connection" to "close"
                )
                if (statusCode == 206) {
                    responseHeaders["Content-Range"] = "bytes $start-$end/$totalSize"
                }

                if (method == "HEAD") {
                    writeResponse(socket, statusCode, "OK", responseHeaders, null)
                    return
                }

                writeResponse(socket, statusCode, "OK", responseHeaders, null)
                val input = TorrentInputStream(handle, fileInfo, file, start, end)
                input.use { stream ->
                    val out = socket.getOutputStream()
                    val buffer = ByteArray(65536)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        if (read > 0) {
                            out.write(buffer, 0, read)
                            out.flush()
                        } else {
                            Thread.sleep(20)
                        }
                    }
                    out.flush()
                }
            }
        } catch (e: Exception) {
            Log.w("TorrentHttpServer", "client error: ${e.message}")
        } finally {
            activeClients.decrementAndGet()
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                key to value
            } else null
        }.toMap()
    }

    private fun writeResponse(socket: Socket, code: Int, status: String, headers: Map<String, String>, body: InputStream?) {
        val out = socket.getOutputStream()
        val builder = StringBuilder()
        builder.append("HTTP/1.1 $code $status\r\n")
        headers.forEach { (k, v) -> builder.append("$k: $v\r\n") }
        builder.append("\r\n")
        out.write(builder.toString().toByteArray())
        out.flush()
    }

    private fun contentTypeForFile(name: String): String {
        return when {
            name.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            name.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            name.endsWith(".webm", ignoreCase = true) -> "video/webm"
            name.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
            name.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            name.endsWith(".m4v", ignoreCase = true) -> "video/x-m4v"
            name.endsWith(".m3u8", ignoreCase = true) -> "application/vnd.apple.mpegurl"
            name.endsWith(".mpd", ignoreCase = true) -> "application/dash+xml"
            else -> "video/*"
        }
    }
}

private class TorrentInputStream(
    private val handle: TorrentHandle,
    private val fileInfo: TorrentFileInfo,
    private val file: File,
    private val start: Long,
    private val endInclusive: Long
) : InputStream() {

    private val raf = RandomAccessFile(file, "r")
    private var position = start

    init {
        raf.seek(fileInfo.offset + start)
    }

    override fun read(): Int {
        if (position > endInclusive) return -1
        waitForPiece(position)
        raf.seek(fileInfo.offset + position)
        val b = raf.read()
        if (b != -1) position++
        return b
    }

    override fun read(buffer: ByteArray, off: Int, len: Int): Int {
        if (position > endInclusive) return -1
        if (len == 0) return 0
        waitForPiece(position)
        val available = contiguousAvailableBytes(position, len)
        if (available <= 0) {
            // Should not happen after wait, but loop
            return 0
        }
        raf.seek(fileInfo.offset + position)
        val read = raf.read(buffer, off, available)
        if (read > 0) position += read
        return read
    }

    override fun close() {
        try {
            raf.close()
        } catch (_: Exception) {
        }
    }

    private fun contiguousAvailableBytes(from: Long, maxLen: Int): Int {
        var count = 0L
        var pos = from
        val end = (from + maxLen).coerceAtMost(endInclusive + 1)
        while (pos < end) {
            val piece = TorrentEngine.pieceForByte(fileInfo, pos)
            if (!handle.havePiece(piece)) break
            val pieceEnd = ((piece + 1) * fileInfo.pieceLength).coerceAtMost(fileInfo.offset + fileInfo.size)
            val pieceBytes = pieceEnd - (fileInfo.offset + pos)
            val canRead = (end - pos).coerceAtMost(pieceBytes)
            count += canRead
            pos += canRead
            if (count >= maxLen) break
        }
        return count.toInt().coerceAtLeast(0)
    }

    private fun waitForPiece(pos: Long) {
        while (true) {
            val piece = TorrentEngine.pieceForByte(fileInfo, pos)
            if (piece >= handle.torrentFile().numPieces()) return
            if (handle.havePiece(piece)) return
            handle.piecePriority(piece, Priority.SEVEN)
            handle.setPieceDeadline(piece, 0)
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                return
            }
        }
    }
}
