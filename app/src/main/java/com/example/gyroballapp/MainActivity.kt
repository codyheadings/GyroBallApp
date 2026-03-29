package com.example.gyroballapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import com.example.gyroballapp.ui.theme.GyroBallAppTheme
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    // Borrowed sensor setup from class 7 examples
    private var _x by mutableStateOf(0f)
    private var _y by mutableStateOf(0f)
    private var _z by mutableStateOf(0f)
    private var _acc by mutableStateOf("Unknown")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager;
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        setContent {
            GyroBallAppTheme {
                BallCanvas(xRotation = _x, yRotation = _y)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _acc = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
            else -> "Unknown"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            _x = it.values[0]
            _y = it.values[1]
            _z = it.values[2]
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}

data class Obstacle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color = Color(0xFF1A602C),
    val cornerRadius: Float = 12f
)

data class CollisionResult(
    val ballX: Float, val ballY: Float,
    val xVelocity: Float, val yVelocity: Float
)

fun borderObstacles(screenWidth: Float, screenHeight: Float): List<Obstacle> {
    val width = 24f
    return listOf(
        Obstacle(0f, 0f, screenWidth, width),
        Obstacle(0f, screenHeight - width, screenWidth, width),
        Obstacle(0f, 0f, width, screenHeight),
        Obstacle(screenWidth - width, 0f, width, screenHeight)
    )
}

fun mazeObstacles(screenWidth: Float, screenHeight: Float): List<Obstacle> {
    return listOf(
        Obstacle(screenWidth * 0.2f, screenHeight * 0.2f, screenWidth * 0.5f, 32f),
        Obstacle(screenWidth * 0.4f, screenHeight * 0.1f, screenWidth * 0.6f, 32f),
        Obstacle(screenWidth * 0.5f, screenHeight * 0.55f, screenWidth * 0.5f, 32f),
        Obstacle(screenWidth * 0.6f, screenHeight * 0.85f, screenWidth * 0.5f, 32f),
        Obstacle(screenWidth * 0.65f, screenHeight * 0.35f, 140f, 140f, cornerRadius = 50f),
        Obstacle(screenWidth * 0.25f, screenHeight * 0.4f, 150f, 150f, cornerRadius = 75f),
        Obstacle(screenWidth * 0.85f, screenHeight * 0.8f, 180f, 240f, cornerRadius = 50f),
        Obstacle(screenWidth * 0.5f, screenHeight * 0.9f, 100f, 300f, cornerRadius = 50f),
        Obstacle(0f, screenHeight * 0.7f, screenWidth * 0.6f, 32f),
    )
}

fun handleCollision(
    ballX: Float, ballY: Float, ballRadius: Float,
    xVelocity: Float, yVelocity: Float,
    obstacle: Obstacle,
    bounce: Float
): CollisionResult? {

    // Find the closest point on the rectangle to the ball center
    val closestX = ballX.coerceIn(obstacle.x, obstacle.x + obstacle.width)
    val closestY = ballY.coerceIn(obstacle.y, obstacle.y + obstacle.height)

    val distX = ballX - closestX
    val distY = ballY - closestY
    val distSquared = distX * distX + distY * distY

    if (distSquared >= ballRadius * ballRadius) return null

    // Push ball out of obstacle
    val dist = sqrt(distSquared)
    val overlap = ballRadius - dist

    // Avoid divide-by-zero if ball is exactly on corner
    val normalX = if (dist > 0) distX / dist else 1f
    val normalY = if (dist > 0) distY / dist else 0f

    val newBallX = ballX + normalX * overlap
    val newBallY = ballY + normalY * overlap

    // Reflect velocity along the collision normal
    val dot = xVelocity * normalX + yVelocity * normalY
    val newXVelocity = (xVelocity - 2 * dot * normalX) * bounce
    val newYVelocity = (yVelocity - 2 * dot * normalY) * bounce

    return CollisionResult(newBallX, newBallY, newXVelocity, newYVelocity)
}

@Composable
fun BallCanvas (xRotation: Float, yRotation: Float) {
    var obstacles by remember { mutableStateOf(emptyList<Obstacle>()) }
    var ballX by remember { mutableStateOf(0f) }
    var ballY by remember { mutableStateOf(0f) }
    var xVelocity by remember { mutableStateOf(0f) }
    var yVelocity by remember { mutableStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    val ballRadius = 30f
    val friction = 0.95f
    val bounce = 0.25f

    // Every time the gyroscope has a new value, move the ball
    LaunchedEffect(xRotation, yRotation) {
        if (initialized) {
            xVelocity += yRotation
            yVelocity += xRotation

            // Apply friction to lower velocity
            xVelocity *= friction
            yVelocity *= friction

            // Move ball by current velocity
            ballX += xVelocity
            ballY += yVelocity

            // Check obstacles
            for (obstacle in obstacles) {
                val result = handleCollision(
                    ballX, ballY, ballRadius,
                    xVelocity, yVelocity, obstacle, bounce
                )
                result?.let {
                    ballX = it.ballX
                    ballY = it.ballY
                    xVelocity = it.xVelocity
                    yVelocity = it.yVelocity
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF457746))
            .onSizeChanged { size ->
                val screenWidth = size.width.toFloat()
                val screenHeight = size.height.toFloat()
                if (!initialized) {
                    ballX = size.width / 2f
                    ballY = size.height / 2f
                    obstacles = borderObstacles(screenWidth, screenHeight) + mazeObstacles(screenWidth, screenHeight)
                    initialized = true
                }

                // Keep ball inside screen
                ballX = ballX.coerceIn(ballRadius, size.width - ballRadius)
                ballY = ballY.coerceIn(ballRadius, size.height - ballRadius)
            }
    ) {
        ballX = ballX.coerceIn(ballRadius, size.width - ballRadius)
        ballY = ballY.coerceIn(ballRadius, size.height - ballRadius)

        obstacles.forEach { obs ->
            drawRoundRect(
                color = obs.color,
                topLeft = Offset(obs.x, obs.y),
                size = Size(obs.width, obs.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(obs.cornerRadius)
            )
        }

        // Ball
        drawCircle(
            color = Color(0xFFD3A51A),
            radius = ballRadius,
            center = Offset(ballX, ballY)
        )
    }

}