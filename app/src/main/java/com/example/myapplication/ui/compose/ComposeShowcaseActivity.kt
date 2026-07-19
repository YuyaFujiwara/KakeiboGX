package com.example.myapplication.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ComposeShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF2F2F7) // Apple-like light gray background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background content
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Apple Design Showcase",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.padding(top = 80.dp, bottom = 16.dp)
                            )
                            Text(
                                "This screen demonstrates fluid, interruptible spring animations and translucency as described in skill.md.",
                                fontSize = 17.sp,
                                color = Color.DarkGray
                            )
                        }

                        // Fluid Card
                        FluidCard()

                        // Translucent Top Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.White.copy(alpha = 0.6f))
                                .blur(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "Fluid UI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FluidCard() {
    val coroutineScope = rememberCoroutineScope()

    // 1:1 tracking offset
    val animatedOffsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val animatedOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(240.dp, 320.dp)
                .offset { IntOffset(animatedOffsetX.value.roundToInt(), animatedOffsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                // Apple Design: Spring back with critically damped / bouncy spring
                                animatedOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f, // slight bounce
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                            coroutineScope.launch {
                                animatedOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { animatedOffsetX.animateTo(0f) }
                            coroutineScope.launch { animatedOffsetY.animateTo(0f) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            animatedOffsetX.snapTo(animatedOffsetX.value + dragAmount.x)
                            animatedOffsetY.snapTo(animatedOffsetY.value + dragAmount.y)
                        }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Drag me",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }
    }
}
