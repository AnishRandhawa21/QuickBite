package com.anish.quickbite.ui.screens.student

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.ui.theme.AcidLime
import com.anish.quickbite.ui.theme.CoralPunch
import com.anish.quickbite.ui.theme.SunflowerGold
import com.anish.quickbite.viewmodel.StudentPassViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun formatExpiryTime(expiresAtIso: String?): String {
    if (expiresAtIso.isNullOrBlank()) return "10 Mins"
    return try {
        val instant = Instant.parse(expiresAtIso)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        "10 Mins"
    }
}

@Composable
fun StudentPassScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentPassViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ENFORCE STRICT FLAG_SECURE: Blocks screenshots and screen recording on device
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Direct Box using the Global Scaffold from MainScreen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.currentOrder != null -> {
                val order = uiState.currentOrder!!
                ActivePassContent(
                    order = order,
                    orders = uiState.orders,
                    isOffline = uiState.isOffline,
                    formattedTime = uiState.formattedTime,
                    progress = uiState.progress,
                    isExpired = uiState.isExpired,
                    isActionInProgress = uiState.isActionInProgress,
                    onActivatePass = { viewModel.activatePass(order.id) },
                    onSelectOrder = { viewModel.selectOrder(it) },
                    onReorder = { orderToReorder -> viewModel.reorder(orderToReorder) { onNavigateBack() } }
                )
            }
            else -> {
                EmptyPassContent(
                    orders = uiState.orders,
                    isOffline = uiState.isOffline,
                    isActionInProgress = uiState.isActionInProgress,
                    errorMessage = uiState.errorMessage,
                    onCreateDemo = { viewModel.createDemoOrder() },
                    onExploreMenu = onNavigateBack,
                    onSelectOrder = { viewModel.selectOrder(it) },
                    onReorder = { orderToReorder -> viewModel.reorder(orderToReorder) { onNavigateBack() } }
                )
            }
        }
    }
}

@Composable
fun ActivePassContent(
    order: Order,
    orders: List<Order>,
    isOffline: Boolean,
    formattedTime: String,
    progress: Float,
    isExpired: Boolean,
    isActionInProgress: Boolean,
    onActivatePass: () -> Unit,
    onSelectOrder: (Order) -> Unit,
    onReorder: (Order) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = activity as? LifecycleOwner

    // Accessibility check: respect user's system reduced motion preferences
    val isReducedMotion = remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    // Maximum subtle 3D tilt angle (strictly ±6.5 degrees for premium clarity)
    val maxTiltDeg = 6.5f

    // User touch / mouse / trackpad hover interaction state
    var touchTiltX by remember { mutableFloatStateOf(0f) }
    var touchTiltY by remember { mutableFloatStateOf(0f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Device sensor orientation state
    var sensorTiltX by remember { mutableFloatStateOf(0f) }
    var sensorTiltY by remember { mutableFloatStateOf(0f) }

    // Sensor setup: Lifecycle-aware listener for Gyroscope / Rotation Vector
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val rotationSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    DisposableEffect(lifecycleOwner, sensorManager, rotationSensor, isReducedMotion) {
        if (isReducedMotion || sensorManager == null || rotationSensor == null || lifecycleOwner == null) {
            return@DisposableEffect onDispose {}
        }

        val sensorListener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            private var smoothedPitch = 0f
            private var smoothedRoll = 0f

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        // Pitch in degrees around X-axis (held in hand ~ -50°)
                        val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        // Roll in degrees around Y-axis (tilt left / right)
                        val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        val targetPitch = ((rawPitch + 50f) * 0.25f).coerceIn(-maxTiltDeg, maxTiltDeg)
                        val targetRoll = (-rawRoll * 0.25f).coerceIn(-maxTiltDeg, maxTiltDeg)

                        // Low-pass exponential smoothing filter for clean, jitter-free tilt
                        smoothedPitch += 0.12f * (targetPitch - smoothedPitch)
                        smoothedRoll += 0.12f * (targetRoll - smoothedRoll)

                        sensorTiltX = smoothedPitch
                        sensorTiltY = smoothedRoll
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val targetRoll = (-x / 9.8f * maxTiltDeg * 1.4f).coerceIn(-maxTiltDeg, maxTiltDeg)
                        val targetPitch = ((y - 6.5f) / 9.8f * maxTiltDeg * 1.4f).coerceIn(-maxTiltDeg, maxTiltDeg)

                        smoothedPitch += 0.12f * (targetPitch - smoothedPitch)
                        smoothedRoll += 0.12f * (targetRoll - smoothedRoll)

                        sensorTiltX = smoothedPitch
                        sensorTiltY = smoothedRoll
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sensorManager.registerListener(
                        sensorListener,
                        rotationSensor,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(sensorListener)
                    sensorTiltX = 0f
                    sensorTiltY = 0f
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            sensorManager.registerListener(
                sensorListener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorListener)
            sensorTiltX = 0f
            sensorTiltY = 0f
        }
    }

    // Determine target rotation (touch/hover takes active precedence, then subtle sensor tilt, or neutral)
    val targetTiltX = when {
        isReducedMotion -> 0f
        isUserInteracting -> touchTiltX
        else -> sensorTiltX
    }
    val targetTiltY = when {
        isReducedMotion -> 0f
        isUserInteracting -> touchTiltY
        else -> sensorTiltY
    }

    // Smooth spring physics for natural return to neutral without sudden snaps
    val animatedTiltX by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardTiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardTiltY"
    )

    // Infinite Transitions for Anti-Counterfeit Motion
    val infiniteTransition = rememberInfiniteTransition(label = "ticketMovement")

    // 1. Holographic diagonal security sheen sweep across the ticket surface
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    // 2. Pulsing radar wave behind the checkmark
    val pulseRadarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadarAlpha"
    )
    val pulseRadarScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadarScale"
    )

    // Extract first item or fallback
    val primaryItem = order.items.firstOrNull()
    val itemName = primaryItem?.menuItem?.name ?: "Campus Meal"
    val itemCategory = primaryItem?.menuItem?.category ?: "Special"
    val itemQuantity = primaryItem?.quantity ?: 1
    val itemImageUrl = primaryItem?.menuItem?.imageUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offline Cache Indicator Banner
        if (isOffline) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1A14),
                border = BorderStroke(1.dp, SunflowerGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Offline",
                        tint = SunflowerGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Mode: Showing cached pass & order history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunflowerGold
                    )
                }
            }
        }

        // Privacy Security Indicator (FLAG_SECURE Active)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141419),
            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = AcidLime,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NO SCREENSHOT • NO RECORDING • ANTI-FRAUD VERIFIED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AcidLime,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // --- MAIN TICKET PASS CARD WITH 3D TILT & PARALLAX ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationX = animatedTiltX
                    rotationY = animatedTiltY
                    cameraDistance = 16f * density
                }
                .pointerInput(isReducedMotion) {
                    if (isReducedMotion) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed || event.type == PointerEventType.Move) {
                                    val normX = ((change.position.x - size.width / 2f) / (size.width / 2f)).coerceIn(-1f, 1f)
                                    val normY = ((change.position.y - size.height / 2f) / (size.height / 2f)).coerceIn(-1f, 1f)
                                    touchTiltX = -normY * maxTiltDeg
                                    touchTiltY = normX * maxTiltDeg
                                    isUserInteracting = true
                                } else if (event.type == PointerEventType.Release || event.type == PointerEventType.Exit) {
                                    if (event.changes.none { it.pressed }) {
                                        isUserInteracting = false
                                        touchTiltX = 0f
                                        touchTiltY = 0f
                                    }
                                }
                            }
                        }
                    }
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF121217))
                .border(BorderStroke(1.dp, Color(0xFF262632)), RoundedCornerShape(28.dp))
        ) {
            // Animated Holographic Sheen Sweep across Card Surface (reacts dynamically to tilt)
            Canvas(modifier = Modifier.matchParentSize()) {
                val dynamicSheenOffset = shimmerTranslate + (animatedTiltY * 20f)
                val sheenBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    start = Offset(dynamicSheenOffset, 0f),
                    end = Offset(dynamicSheenOffset + 250f, size.height)
                )
                drawRect(brush = sheenBrush)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. TOP HERO SECTION (Gradient Glow, QuickBite Logo, Item Info, and Food Plate)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Subtle reverse parallax for background depth
                            translationX = -animatedTiltY * 0.7f
                            translationY = animatedTiltX * 0.7f
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2C1623), // Deep subtle warm dark gradient
                                    Color(0xFF19141D),
                                    Color(0xFF121217)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header Bar: QuickBite Logo & Pickup Pass Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: QuickBite Branding
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CoralPunch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Restaurant,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "QuickBite",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            // Right: Frosted "Pickup Pass" Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x33FFFFFF),
                                border = BorderStroke(1.dp, Color(0x28FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pickup Pass",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Hero Item Details & Plate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Details
                            Column(modifier = Modifier.weight(1f)) {
                                // Category Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SunflowerGold.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, SunflowerGold.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = SunflowerGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = itemCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SunflowerGold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = itemName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "× $itemQuantity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC0C0CC)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Right Food Plate with 3D Parallax floating elevation
                            Box(
                                modifier = Modifier
                                    .size(118.dp)
                                    .graphicsLayer {
                                        // Foreground parallax shift: floats forward in perspective
                                        translationX = animatedTiltY * 1.5f
                                        translationY = -animatedTiltX * 1.5f
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E26))
                                    .border(BorderStroke(2.dp, Color(0x33FFFFFF)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!itemImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = itemImageUrl,
                                        contentDescription = itemName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Stylized Plate Graphic with Thali aesthetic
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF322830), Color(0xFF1A1A22))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restaurant,
                                            contentDescription = null,
                                            tint = SunflowerGold,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. TICKET NOTCHES & DASHED CUT LINE (The classic boarding pass cut)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Inward Semi-Circle Notch
                    Box(
                        modifier = Modifier
                            .offset(x = (-1).dp)
                            .size(width = 14.dp, height = 28.dp)
                            .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, Color(0xFF262632)), RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                    )

                    // Dashed Line across the ticket
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
                        drawLine(
                            color = Color(0xFF2C2C38),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }

                    // Right Inward Semi-Circle Notch
                    Box(
                        modifier = Modifier
                            .offset(x = 1.dp)
                            .size(width = 14.dp, height = 28.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, Color(0xFF262632)), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    )
                }

                // 3. ORDER ID & CANTEEN ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val orderDisplayId = if (!order.recoveryCode.isNullOrBlank()) {
                        "QB${order.recoveryCode}"
                    } else {
                        order.id.takeLast(6).uppercase()
                    }
                    Text(
                        text = "Order #$orderDisplayId",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color(0xFFA0A0AB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.canteen?.name ?: "Campus Canteen",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0D0DA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. MIDDLE LIVE STATUS CARD (Circular Progress Timer + Pickup Active)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF16161E),
                    border = BorderStroke(1.dp, Color(0xFF2A2A38))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Circular Countdown Timer with Parallax
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    translationX = animatedTiltY * 0.9f
                                    translationY = -animatedTiltX * 0.9f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular Ring Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                // Background Track
                                drawCircle(
                                    color = Color(0xFF242432),
                                    style = Stroke(width = strokeWidth)
                                )
                                // Active Progress Arc
                                val sweep = if (isExpired) 0f else (progress * 360f)
                                drawArc(
                                    color = if (isExpired) CoralPunch else AcidLime,
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            // Inner Time Text
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isExpired) "00:00" else formattedTime,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isExpired) "EXPIRED" else "remaining",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isExpired) CoralPunch else Color(0xFFA0A0AB)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Status Description
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center) {
                                    if ((order.status == "READY" || order.status == "PICKUP_ACTIVE") && !isExpired) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(pulseRadarScale)
                                                .clip(CircleShape)
                                                .background(AcidLime.copy(alpha = pulseRadarAlpha))
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isExpired) CoralPunch else AcidLime,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isExpired) "Pass Expired" else "Pickup Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isExpired) CoralPunch else AcidLime
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isExpired)
                                    "Your 10-minute window ended. Staff can reopen once via your recovery code."
                                else
                                    "Show this pass at the counter to collect your order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA0A0AB),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. BOTTOM DETAILS SECTION (Valid Until & Payment)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF14141B),
                    border = BorderStroke(1.dp, Color(0xFF22222E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Valid Until
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                tint = AcidLime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Valid Until",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888896)
                                )
                                Text(
                                    text = formatExpiryTime(order.expiresAt),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color(0xFF2A2A38))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Payment
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = null,
                                tint = SunflowerGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Payment",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888896)
                                )
                                Text(
                                    text = "Pay at Canteen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- STAFF RECOVERY CODE DRAWER ---
        if (!order.recoveryCode.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SunflowerGold.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141310))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = SunflowerGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STAFF RECOVERY CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SunflowerGold
                            )
                        }
                        Text(
                            text = "Reopens: ${order.reopenCount}/1",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA0A0AB)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C1A14),
                        border = BorderStroke(1.dp, SunflowerGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = order.recoveryCode,
                            modifier = Modifier.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = SunflowerGold,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "If pass expires before collection, staff will enter this recovery code on the canteen POS to reopen your pickup window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888894),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Manual Activate Button (if status is PLACED / PREPARING / ACTIVE and not yet READY)
        if (order.status in listOf("PLACED", "PREPARING", "ACTIVE")) {
            Button(
                onClick = onActivatePass,
                enabled = !isActionInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidLime,
                    contentColor = Color.Black
                )
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activate Live 10-Minute Pass",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Order History Section (Cached locally in Room)
        if (orders.size > 1) {
            OrderHistoryListCard(
                orders = orders,
                currentOrderId = order.id,
                onSelectOrder = onSelectOrder,
                onReorder = onReorder
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EmptyPassContent(
    orders: List<Order>,
    isOffline: Boolean,
    isActionInProgress: Boolean,
    errorMessage: String? = null,
    onCreateDemo: () -> Unit,
    onExploreMenu: () -> Unit,
    onSelectOrder: (Order) -> Unit,
    onReorder: (Order) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isOffline) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1A14),
                border = BorderStroke(1.dp, SunflowerGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Offline",
                        tint = SunflowerGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Mode: Showing cached order history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunflowerGold
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CoralPunch.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CoralPunch),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = CoralPunch,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF141418)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ConfirmationNumber,
                contentDescription = null,
                tint = AcidLime,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Active Pickup Pass",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "When you pre-order meals from any campus canteen, your live 10-minute pickup pass will appear right here with a live countdown and recovery code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExploreMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AcidLime,
                contentColor = Color.Black
            )
        ) {
            Text("Browse Canteen Menus", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCreateDemo,
            enabled = !isActionInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E26),
                contentColor = Color.White
            )
        ) {
            if (isActionInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Generate Test Pickup Pass", fontWeight = FontWeight.SemiBold)
            }
        }

        // Cached Orders History if any exist
        if (orders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            OrderHistoryListCard(
                orders = orders,
                currentOrderId = null,
                onSelectOrder = onSelectOrder,
                onReorder = onReorder
            )
        }
    }
}

@Composable
fun OrderHistoryListCard(
    orders: List<Order>,
    currentOrderId: String?,
    onSelectOrder: (Order) -> Unit,
    onReorder: (Order) -> Unit = {}
) {
    if (orders.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF22222E)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = AcidLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ORDER HISTORY (CACHED)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "${orders.size} order${if (orders.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA0A0AB)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            orders.forEachIndexed { index, order ->
                val isSelected = order.id == currentOrderId
                val canteenTitle = order.canteen?.name ?: "Campus Canteen"
                val itemsSummary = order.items.joinToString(", ") { "${it.menuItem?.name ?: "Item"} x${it.quantity}" }
                    .ifBlank { "Meal Order" }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF1C1C26) else Color(0xFF0F0F14),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AcidLime.copy(alpha = 0.5f) else Color(0xFF22222C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOrder(order) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = canteenTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Status Pill
                                val statusColor = when (order.status) {
                                    "READY", "PICKUP_ACTIVE" -> AcidLime
                                    "PLACED", "ACTIVE", "PREPARING" -> SunflowerGold
                                    "COMPLETED" -> Color(0xFF4CAF50)
                                    else -> Color(0xFF888894)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = order.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA0A0AB),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${order.totalAmount.toInt()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = AcidLime
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isSelected) {
                                Text(
                                    text = "Viewing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AcidLime,
                                    fontSize = 10.sp
                                )
                            } else if (order.items.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AcidLime.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { onReorder(order) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = "Reorder",
                                            tint = AcidLime,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Reorder",
                                            color = AcidLime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (index < orders.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
