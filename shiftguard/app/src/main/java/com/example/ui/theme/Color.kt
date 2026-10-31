package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// DESIGN TOKENS - Synced from Web Portal index.html :root
// Update here when web CSS :root changes, then rebuild Android.
// ============================================================

// --- Primary Brand (Quantum Lime / WhatsApp Green) ---
val Primary = Color(0xFFA6CE39)           // --primary: #a6ce39
val PrimaryHover = Color(0xFF90B828)      // --primary-hover: #90b828
val WaGreen = Color(0xFF25D366)           // --wa-green: #25d366
val WaGreenDark = Color(0xFF0E9F56)       // --wa-green-dark: #0e9f56
val WaGreenGlow = Color(0x3325D366)       // 20% opacity for glows

// --- Semantic Colors ---
val Success = Color(0xFF22C55E)           // --success: #22c55e
val Warning = Color(0xFFF59E0B)           // --warning: #f59e0b
val Danger = Color(0xFFEF4444)            // --danger: #ef4444

// --- Neutral / Surface Colors (Light Theme) ---
val Background = Color(0xFFF5F7FB)        // --bg: #f5f7fb
val Card = Color(0xFFFFFFFF)              // --card: #ffffff
val CardElevated = Color(0xFFF8FAFC)      // slight elevation
val Border = Color(0xFFE5E9F0)            // --border: #e5e9f0
val Line = Color(0xFFE5E9F0)              // --line: #e5e9f0

// --- Neutral / Surface Colors (Dark Theme) ---
val DarkBackground = Color(0xFF0B1220)    // --ink: #0f172a (approx)
val DarkSurface = Color(0xFF0F172A)       // --ink: #0f172a
val DarkSurfaceElevated = Color(0xFF1E293B)
val DarkBorder = Color(0xFF1E2D42)
val DarkLine = Color(0xFF24364D)

// --- Text Colors (Light Theme) ---
val TextPrimary = Color(0xFF1E293B)       // --text: #1e293b
val TextSecondary = Color(0xFF475569)     // --muted darker
val TextMuted = Color(0xFF64748B)         // --muted: #64748b
val TextOnPrimary = Color(0xFF04240F)     // --wa-green dark text

// --- Text Colors (Dark Theme) ---
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)
val DarkTextOnPrimary = Color(0xFF04240F)

// --- Aliases for backward compatibility ---
val QuantumLime = Primary
val QuantumLimeHover = PrimaryHover
val WhatsAppGreen = WaGreen
val WhatsAppGreenDark = WaGreenDark
val WhatsAppGreenGlow = WaGreenGlow
val AmberWarning = Warning
val AlertRed = Danger
val SuccessGreen = Success
val QuantumDark = DarkBackground
val QuantumNavy = DarkSurface
val QuantumSurface = DarkSurface
val QuantumElevated = DarkSurfaceElevated
val QuantumBorder = DarkBorder
val QuantumLine = DarkLine
val NavyDark = DarkBackground
val NavySurface = DarkSurface
val NavyElevated = DarkSurfaceElevated
val NavyBorder = DarkBorder
val TechCyan = WaGreen
val TechCyanGlow = WaGreenGlow
val ElectricBlue = Color(0xFF0284C7)
val LightBackground = Background
val LightSurface = Card
val LightSurfaceElevated = CardElevated
val LightBorder = Border
val LightTextPrimary = TextPrimary
val LightTextSecondary = TextSecondary

