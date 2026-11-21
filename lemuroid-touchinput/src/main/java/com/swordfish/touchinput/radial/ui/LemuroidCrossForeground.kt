package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import gg.padkit.ui.DefaultCrossForeground

@Composable
fun LemuroidCrossForeground(
    allowDiagonals: Boolean,
    directionState: State<Offset>,
) {
    // ff-bgm1x: 스킨에 십자키 그림이 있으면 누른 방향의 그림을 그린다
    val skin = com.swordfish.touchinput.radial.custom.LocalFfSkinImages.current
    val image = skin?.cross(directionState.value)
    if (image != null) {
        com.swordfish.touchinput.radial.custom.FfSkinImage(image)
        return
    }
    DefaultCrossForeground(
        modifier = Modifier.fillMaxSize(),
        directionState = directionState,
        allowDiagonals = allowDiagonals,
        leftDial = {
            LemuroidCrossButton(it, Icons.Default.KeyboardArrowLeft)
        },
        rightDial = {
            LemuroidCrossButton(it, Icons.Default.KeyboardArrowRight)
        },
        topDial = {
            LemuroidCrossButton(it, Icons.Default.KeyboardArrowUp)
        },
        bottomDial = {
            LemuroidCrossButton(it, Icons.Default.KeyboardArrowDown)
        },
        foregroundComposite = {
            LemuroidCompositeForeground(it)
        },
    )
}

@Composable
private fun LemuroidCrossButton(
    pressedState: State<Boolean>,
    imageVector: ImageVector,
) {
    LemuroidButtonForeground(
        pressed = pressedState,
        label = { },
        icon = {
            Icon(
                modifier = Modifier.size(maxWidth * 0.5f, maxHeight * 0.5f),
                imageVector = imageVector,
                contentDescription = "",
                tint = LocalLemuroidPadTheme.current.icons(pressedState.value),
            )
        },
    )
}
