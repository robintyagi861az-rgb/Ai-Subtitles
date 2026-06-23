package com.example.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

object SmtpSender {
    private const val TAG = "SmtpSender"

    suspend fun sendMail(
        host: String,
        port: Int,
        user: String,
        pass: String,
        to: String,
        subject: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to SMTP server: $host:$port")
            val socket: Socket = if (port == 465) {
                (SSLSocketFactory.getDefault() as javax.net.ssl.SSLSocketFactory).createSocket(host, port)
            } else {
                Socket(host, port)
            }

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

            fun readResponse(): String {
                val line = reader.readLine()
                Log.d(TAG, "SMTP Server: $line")
                return line ?: ""
            }

            readResponse() // Banner 220

            writer.println("EHLO localhost")
            readResponse()
            while (reader.ready()) {
                readResponse()
            }

            if (port == 587) {
                writer.println("STARTTLS")
                val startTlsResp = readResponse()
                if (startTlsResp.startsWith("220")) {
                    val sslSocket = (SSLSocketFactory.getDefault() as javax.net.ssl.SSLSocketFactory).createSocket(socket, host, port, true)
                    val sslReader = BufferedReader(InputStreamReader(sslSocket.getInputStream()))
                    val sslWriter = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream()), true)

                    sslWriter.println("EHLO localhost")
                    sslReader.readLine()
                    while (sslReader.ready()) {
                        sslReader.readLine()
                    }

                    sslWriter.println("AUTH LOGIN")
                    sslReader.readLine()

                    val b64User = Base64.encodeToString(user.toByteArray(), Base64.NO_WRAP)
                    sslWriter.println(b64User)
                    sslReader.readLine()

                    val b64Pass = Base64.encodeToString(pass.toByteArray(), Base64.NO_WRAP)
                    sslWriter.println(b64Pass)
                    val authResp = sslReader.readLine()
                    if (!authResp.startsWith("235")) {
                        Log.e(TAG, "SMTP Authentication Failed: $authResp")
                        sslSocket.close()
                        return@withContext false
                    }

                    sslWriter.println("MAIL FROM:<$user>")
                    sslReader.readLine()

                    sslWriter.println("RCPT TO:<$to>")
                    sslReader.readLine()

                    sslWriter.println("DATA")
                    sslReader.readLine()

                    sslWriter.println("Subject: $subject")
                    sslWriter.println("From: $user")
                    sslWriter.println("To: $to")
                    sslWriter.println("Content-Type: text/plain; charset=UTF-8")
                    sslWriter.println()
                    sslWriter.println(body)
                    sslWriter.println(".")
                    sslReader.readLine()

                    sslWriter.println("QUIT")
                    sslReader.readLine()
                    sslSocket.close()
                    return@withContext true
                } else {
                    Log.e(TAG, "STARTTLS failed with: $startTlsResp")
                    socket.close()
                    return@withContext false
                }
            } else {
                writer.println("AUTH LOGIN")
                readResponse()

                val b64User = Base64.encodeToString(user.toByteArray(), Base64.NO_WRAP)
                writer.println(b64User)
                readResponse()

                val b64Pass = Base64.encodeToString(pass.toByteArray(), Base64.NO_WRAP)
                writer.println(b64Pass)
                val authResp = readResponse()
                if (!authResp.startsWith("235")) {
                    Log.e(TAG, "SMTP Authentication Failed: $authResp")
                    socket.close()
                    return@withContext false
                }

                writer.println("MAIL FROM:<$user>")
                readResponse()

                writer.println("RCPT TO:<$to>")
                readResponse()

                writer.println("DATA")
                readResponse()

                writer.println("Subject: $subject")
                writer.println("From: $user")
                writer.println("To: $to")
                writer.println("Content-Type: text/plain; charset=UTF-8")
                writer.println()
                writer.println(body)
                writer.println(".")
                readResponse()

                writer.println("QUIT")
                readResponse()
                socket.close()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMTP Connection Error", e)
            false
        }
    }
}
