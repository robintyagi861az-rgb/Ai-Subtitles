package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class CaptionPreset(
    val id: String,
    val name: String,
    val textColor: Color,
    val outlineColor: Color = Color.Transparent,
    val outlineWidth: Float = 0f,
    val backgroundColor: Color = Color.Transparent,
    val fontSize: TextUnit = 24.sp,
    val isUppercase: Boolean = false,
    val fontStyle: FontStyle = FontStyle.Normal,
    val fontWeight: FontWeight = FontWeight.Bold,
    val letterSpacing: TextUnit = 0.sp,
    val description: String,
    val textAlignment: Int = android.view.Gravity.CENTER,
    val paddingDp: Int = 8
) {
    companion object {
        val list = listOf(
            CaptionPreset(
                id = "mrbeast",
                name = "MrBeast Yellow",
                textColor = Color(0xFFFFE600),
                outlineColor = Color.Black,
                outlineWidth = 6f,
                fontSize = 32.sp,
                isUppercase = true,
                fontWeight = FontWeight.ExtraBold,
                description = "Bold uppercase energetic text with heavy outline"
            ),
            CaptionPreset(
                id = "tiktok",
                name = "TikTok Classic",
                textColor = Color.White,
                backgroundColor = Color.Black.copy(alpha = 0.7f),
                fontSize = 22.sp,
                isUppercase = false,
                fontWeight = FontWeight.Bold,
                description = "White text in rounded dark background bar"
            ),
            CaptionPreset(
                id = "reels_glow",
                name = "Instagram Reels Lime",
                textColor = Color(0xFF00FF66),
                outlineColor = Color(0xFF003311),
                outlineWidth = 2f,
                fontSize = 26.sp,
                isUppercase = false,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                description = "Energetic neon lime italic glow text"
            ),
            CaptionPreset(
                id = "cinematic",
                name = "Cinematic Georgia",
                textColor = Color(0xFFF5F5F5),
                fontSize = 18.sp,
                isUppercase = false,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                description = "Elegant and clean bottom-placed cinematic typography"
            ),
            CaptionPreset(
                id = "cyberpunk",
                name = "Cyberpunk Neon",
                textColor = Color(0xFF00FFFF),
                outlineColor = Color(0xFFFF00FF),
                outlineWidth = 3f,
                fontSize = 28.sp,
                isUppercase = true,
                fontWeight = FontWeight.Bold,
                description = "Cyan text with magenta outline for sci-fi look"
            ),
            CaptionPreset(
                id = "karaoke",
                name = "Karaoke Pop",
                textColor = Color(0xFFFF007F), // Active word color
                outlineColor = Color.White,
                outlineWidth = 2f,
                fontSize = 26.sp,
                isUppercase = false,
                fontWeight = FontWeight.Bold,
                description = "Deep hot pink active word highlight styling"
            ),
            CaptionPreset(
                id = "news_banner",
                name = "News Banner",
                textColor = Color.White,
                backgroundColor = Color(0xFFD32F2F),
                fontSize = 20.sp,
                isUppercase = true,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                description = "High contrast white text on top of solid red bar"
            ),
            CaptionPreset(
                id = "comic_pop",
                name = "Comic Book Pop",
                textColor = Color.Black,
                backgroundColor = Color.White,
                outlineColor = Color.Black,
                outlineWidth = 4f,
                fontSize = 24.sp,
                isUppercase = true,
                fontWeight = FontWeight.ExtraBold,
                description = "Black outline caps on friendly white comic board"
            ),
            CaptionPreset(
                id = "retro_vhs",
                name = "Retro VHS",
                textColor = Color(0xFF22FF22),
                fontSize = 20.sp,
                isUppercase = true,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp,
                description = "Green monospace electric tape VCR overlay style"
            ),
            CaptionPreset(
                id = "bold_impact",
                name = "Bold Impact",
                textColor = Color(0xFFFF3D00),
                outlineColor = Color.Black,
                outlineWidth = 5f,
                fontSize = 34.sp,
                isUppercase = true,
                fontWeight = FontWeight.ExtraBold,
                description = "Extra-large blazing orange outline caps"
            )
        )

        fun getById(id: String): CaptionPreset {
            return list.firstOrNull { it.id == id } ?: list[0]
        }
    }
}
