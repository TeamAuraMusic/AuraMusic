package com.auramusic.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.EnableGoogleCastKey
import com.auramusic.app.utils.rememberPreference
import timber.log.Timber

@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    
    var castAvailable by remember { mutableStateOf(false) }
    var mediaRouter by remember { mutableStateOf<MediaRouter?>(null) }
    var routeSelector by remember { mutableStateOf<MediaRouteSelector?>(null) }
    var availableRoutes by remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }
    
    val (enableGoogleCast) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
    )
    
    val castHandler = playerConnection?.service?.castConnectionHandler
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val isConnecting by castHandler?.isConnecting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsState() ?: remember { mutableStateOf(null) }
    
    val currentMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(enableGoogleCast) {
        if (!enableGoogleCast) {
            if (isCasting) {
                playerConnection?.service?.castConnectionHandler?.disconnect()
            }
            castAvailable = false
            mediaRouter = null
            routeSelector = null
            availableRoutes = emptyList()
            return@LaunchedEffect
        }
        try {
            CastContext.getSharedInstance(context)
            mediaRouter = MediaRouter.getInstance(context)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build()
            playerConnection?.service?.castConnectionHandler?.initialize()
            castAvailable = true
        } catch (e: Exception) {
            Timber.d("Cast not available: ${e.message}")
            castAvailable = false
        }
    }
    
    DisposableEffect(mediaRouter, routeSelector) {
        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router, routeSelector) { availableRoutes = it }
            }
            
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router, routeSelector) { availableRoutes = it }
            }
            
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router, routeSelector) { availableRoutes = it }
            }
        }
        
        routeSelector?.let { selector ->
            mediaRouter?.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
            updateRoutes(mediaRouter, selector) { availableRoutes = it }
        }
        
        onDispose {
            mediaRouter?.removeCallback(callback)
        }
    }

    if (enableGoogleCast && castAvailable) {
        Box(
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val currentRoute = if (isCasting) {
                            mediaRouter?.routes?.find { route ->
                                routeSelector?.let { selector -> 
                                    route.matchesSelector(selector) && route.isSelected
                                } == true
                            }
                        } else null

                        showCastPickerDialog(
                            routes = availableRoutes,
                            isConnecting = isConnecting,
                            currentlyConnectedRoute = currentRoute,
                            onRouteSelected = { route ->
                                castHandler?.connectToRoute(route)
                            },
                            onDisconnect = {
                                castHandler?.disconnect()
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(
                        if (isCasting) R.drawable.cast_connected else R.drawable.cast
                    ),
                    contentDescription = if (isCasting) "Stop casting" else "Cast",
                    colorFilter = ColorFilter.tint(
                        if (isCasting) MaterialTheme.colorScheme.primary else tintColor
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun updateRoutes(
    router: MediaRouter?,
    selector: MediaRouteSelector?,
    onUpdate: (List<MediaRouter.RouteInfo>) -> Unit
) {
    if (router == null || selector == null) {
        onUpdate(emptyList())
        return
    }
    val routes = router.routes.filter { route ->
        route.matchesSelector(selector) && !route.isDefault
    }
    onUpdate(routes)
}

private fun showCastPickerDialog(
    routes: List<MediaRouter.RouteInfo>,
    isConnecting: Boolean,
    currentlyConnectedRoute: MediaRouter.RouteInfo?,
    onRouteSelected: (MediaRouter.RouteInfo) -> Unit,
    onDisconnect: () -> Unit
) {
    // Simple implementation - connect to first available route or disconnect if already connected
    if (currentlyConnectedRoute != null) {
        onDisconnect()
    } else if (routes.isNotEmpty()) {
        onRouteSelected(routes.first())
    }
}