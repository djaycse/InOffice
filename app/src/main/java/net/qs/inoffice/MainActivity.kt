package net.qs.inoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.qs.inoffice.data.WorkDataStore
import net.qs.inoffice.ui.theme.InOfficeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = WorkDataStore(this)
        setContent {
            val theme by store.appTheme.collectAsState(initial = AppTheme.DEFAULT)
            val useDarkTheme = when (theme) {
                AppTheme.DEFAULT -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            InOfficeTheme(darkTheme = useDarkTheme, dynamicColor = false) {
                InOfficeApp(store)
            }
        }
    }
}
