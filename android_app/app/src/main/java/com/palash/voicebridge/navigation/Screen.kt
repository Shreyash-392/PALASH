package com.palash.voicebridge.navigation

/**
 * All navigation routes in the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object VoiceTranslation : Screen("voice_translation")
    object LessonList : Screen("lesson_list")
    object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/$lessonId"
    }
    object Worksheets : Screen("worksheets")
    object VideoLessons : Screen("video_lessons")
    object WorksheetPreview : Screen("worksheet_preview/{worksheetId}") {
        fun createRoute(worksheetId: String) = "worksheet_preview/$worksheetId"
    }
    object Flashcards : Screen("flashcards")
    object Settings : Screen("settings")
    object DemoShowcase : Screen("demo_showcase")
}
