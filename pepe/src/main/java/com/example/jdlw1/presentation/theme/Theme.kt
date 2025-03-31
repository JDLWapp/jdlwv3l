package com.example.jdlw1.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color // <importacion de colores>
import com.example.jdlw1clock.presentation.theme.Blue1
import com.example.jdlw1clock.presentation.theme.Blue2
import com.example.jdlw1clock.presentation.theme.Blue3
import com.example.jdlw1clock.presentation.theme.Blue4
import com.example.jdlw1clock.presentation.theme.Blue5
import com.example.jdlw1clock.presentation.theme.DarkBlue1
import com.example.jdlw1clock.presentation.theme.DarkBlue2
import com.example.jdlw1clock.presentation.theme.DarkBlue3
import com.example.jdlw1clock.presentation.theme.Typography

private val LightColorScheme = lightColorScheme(
    primary = Blue5,
    secondary = Blue3,
    tertiary = Blue4,
    background = Blue1,
    surface = Blue2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Blue5,
    onSurface = Blue5
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue2,
    secondary = DarkBlue3,
    tertiary = Blue4,
    background = DarkBlue1,
    surface = DarkBlue2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun JDLW1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}