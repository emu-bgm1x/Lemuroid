package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

@Composable
fun LemuroidControlBackground(modifier: Modifier = Modifier) {
    // ff-bgm1x: 스킨이 있으면 버튼 바탕(동그란 판)을 그리지 않는다 (스킨 배경이 보이게)
    if (com.swordfish.touchinput.radial.custom.LocalFfSkinImages.current != null) return
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier.fillMaxSize(),
        fillColor = theme.level1Fill,
        shadowColor = theme.level1Shadow,
        shadowWidth = theme.level1ShadowWidth,
    )
}
