package com.auramusic.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.ui.component.LocalMenuState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.constants.EnableGoogleCastKey
import timber.log.Timber

@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    
    var castAvailable by remember { mutableStateOf(false) }
    var availableRoutes by remember { mutableStateOf<List<androidx.mediarouter.media.MediaRouter.RouteInfo>>(emptyList()) }
    
    val (enableGoogleCast) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
    )
    
    val isCasting by playerConnection?.service?.castConnectionHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    
    LaunchedEffect(enableGoogleCast) {
        if (!enableGoogleCast) {
            castAvailable = false
            availableRoutes = emptyList()
            return@LaunchedEffect
        }
        try {
            val mediaRouter = androidx.mediarouter.media.MediaRouter.getInstance(context)
            val routeSelector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                .addControlCategory(androidx.mediarouter.media.MediaRouteAction.ACTION_MEDIA_ROUTE_CONTROL)
                .build()
            
            val routes = mediaRouter.routes.filter { route ->
                route.matchesSelector(routeSelector) && !route.isDefault
            }
            availableRoutes = routes
            castAvailable = routes.isNotEmpty()
            Timber.d("Foss Cast: found ${routes.size} routes")
        } catch (e: Exception) {
            Timber.e(e, "Foss Cast: not available: ${e.message}")
            castAvailable = false
        }
    }
    
    if (enableGoogleCast && castAvailable) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    menuState.show {
                        com.auramusic.app.ui.component.CastPickerSheet(
                            routes = availableRoutes,
                            isConnecting = false,
                            currentlyConnectedRoute = null,
                            onRouteSelected = { route ->
                                // In foss mode, just show toast for now
                                Timber.d("Foss Cast: selected route ${route.name}")
                            },
                            onDisconnect = {
                                Timber.d("Foss Cast: disconnect")
                            }
                        )
                    }
                }
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    if (isCasting) com.auramusic.app.R.drawable.cast_connected else com.auramusic.app.R.drawable.cast
                ),
                contentDescription = if (isCasting) "Stop casting" else "Cast",
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    if (isCasting) MaterialTheme.colorScheme.primary else tintColor
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}