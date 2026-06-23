package com.example.data

import java.util.Locale

object SubtitleExporter {

    fun exportToSrt(lines: List<SubtitleLine>): String {
        val sb = StringBuilder()
        lines.forEachIndexed { index, line ->
            sb.append(index + 1).append("\n")
            sb.append(formatTimeSrt(line.startMs)).append(" --> ").append(formatTimeSrt(line.endMs)).append("\n")
            sb.append(line.text).append("\n\n")
        }
        return sb.toString()
    }

    fun exportToVtt(lines: List<SubtitleLine>): String {
        val sb = StringBuilder()
        sb.append("WEBVTT\n\n")
        lines.forEachIndexed { index, line ->
            sb.append(index + 1).append("\n")
            sb.append(formatTimeVtt(line.startMs)).append(" --> ").append(formatTimeVtt(line.endMs)).append("\n")
            sb.append(line.text).append("\n\n")
        }
        return sb.toString()
    }

    private fun formatTimeSrt(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val milliseconds = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }

    private fun formatTimeVtt(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val milliseconds = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }
}
