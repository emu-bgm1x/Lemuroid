package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.custom.CustomLayout
import com.swordfish.touchinput.radial.custom.CustomButtonFunction
import com.swordfish.touchinput.radial.custom.CustomLayoutKeys
import com.swordfish.touchinput.radial.custom.CustomLayoutState
import com.swordfish.touchinput.radial.custom.LocalCustomLayout
import com.swordfish.touchinput.radial.custom.customLayoutElement
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidControlBackground
import gg.padkit.controls.ControlButton
import gg.padkit.ids.Id
import androidx.compose.foundation.layout.size
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.GlassSurface
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = constraints.maxWidth > constraints.maxHeight

        LaunchedEffect(isLandscape) {
            val orientation =
                if (isLandscape) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                }
            viewModel.onScreenOrientationChanged(orientation)
        }

        val controllerConfigState = viewModel.getTouchControllerConfig().collectAsState(null)
        val touchControlsVisibleState = viewModel.isTouchControllerVisible().collectAsState(false)
        val touchControllerSettingsState =
            viewModel
                .getTouchControlsSettings(LocalDensity.current, WindowInsets.displayCutout)
                .collectAsState(null)

        val touchControllerSettings = touchControllerSettingsState.value
        val currentControllerConfig = controllerConfigState.value

        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }

        val touchGamePads = currentControllerConfig?.getTouchControllerConfig()
        val leftGamePad = touchGamePads?.leftComposable
        val rightGamePad = touchGamePads?.rightComposable

        val hapticFeedbackMode =
            viewModel
                .getTouchHapticFeedbackMode()
                .collectAsState(HapticFeedbackMode.NONE)

        val padHapticFeedback =
            when (hapticFeedbackMode.value) {
                HapticFeedbackMode.NONE -> HapticFeedbackType.NONE
                HapticFeedbackMode.PRESS -> HapticFeedbackType.PRESS
                HapticFeedbackMode.PRESS_RELEASE -> HapticFeedbackType.PRESS_RELEASE
            }

        // ff-bgm1x: 버튼·게임 화면 자유 배치 (저장된 배치가 없으면 레무로이드 원래 배치)
        val customLayoutState = remember { CustomLayoutState(CustomLayout()) }
        val loadedCustomLayout = viewModel.getCustomLayout().collectAsState(null)
        val customLayoutEditShown = viewModel.isCustomLayoutEditShown().collectAsState(false)
        customLayoutState.screenWidth = constraints.maxWidth.toFloat()
        customLayoutState.screenHeight = constraints.maxHeight.toFloat()
        LaunchedEffect(loadedCustomLayout.value, customLayoutEditShown.value) {
            customLayoutState.editing = customLayoutEditShown.value
            if (!customLayoutEditShown.value) {
                customLayoutState.selectOnly(null)
                loadedCustomLayout.value?.let { customLayoutState.layout = it.layout }
            }
        }

        // ff-bgm1x: 스킨 버튼 그림
        val skinDir = customLayoutState.layout.skinDir
        val skinImages =
            remember(skinDir) {
                skinDir?.let { java.io.File(it) }?.takeIf { it.isDirectory }?.let { com.swordfish.touchinput.radial.custom.FfSkinImages(it) }
            }
        CompositionLocalProvider(
            LocalCustomLayout provides customLayoutState,
            com.swordfish.touchinput.radial.custom.LocalFfSkinImages provides skinImages,
        ) {
        PadKit(
            modifier = Modifier.fillMaxSize(),
            onInputEvents = { viewModel.handleVirtualInputEvent(it) },
            hapticFeedbackType = padHapticFeedback,
            simulatedState = tiltSimulatedStates,
            simulatedControlIds = tiltSimulatedControls,
        ) {
            val localContext = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current

            val fullScreenPosition = remember { mutableStateOf<Rect?>(null) }
            val viewportPosition = remember { mutableStateOf<Rect?>(null) }

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { fullScreenPosition.value = it.boundsInRoot() },
                factory = {
                    viewModel.createRetroView(localContext, lifecycle)
                },
            )

            val fullPos = fullScreenPosition.value
            val viewPos = viewportPosition.value

            // ff-bgm1x: 스킨 배경. 게임 화면 자리는 비워 둔다 (게임 화면은 이 아래 층에 그려진다)
            val skinPath = if (isLandscape) customLayoutState.layout.skinLandscape else customLayoutState.layout.skinPortrait
            val skinImage =
                remember(skinPath) {
                    skinPath?.let { android.graphics.BitmapFactory.decodeFile(it)?.asImageBitmap() }
                }
            if (skinImage != null && fullPos != null) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val hole = androidx.compose.ui.graphics.Path()
                    viewPos?.let {
                        hole.addRect(
                            androidx.compose.ui.geometry.Rect(
                                it.left - fullPos.left, it.top - fullPos.top, it.right - fullPos.left, it.bottom - fullPos.top,
                            ),
                        )
                    }
                    clipPath(hole, androidx.compose.ui.graphics.ClipOp.Difference) {
                        drawImage(
                            image = skinImage,
                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }
                }
            }

            LaunchedEffect(fullPos, viewPos) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                if (fullPos == null || viewPos == null) return@LaunchedEffect
                val viewport =
                    RectF(
                        (viewPos.left - fullPos.left) / fullPos.width,
                        (viewPos.top - fullPos.top) / fullPos.height,
                        (viewPos.right - fullPos.left) / fullPos.width,
                        (viewPos.bottom - fullPos.top) / fullPos.height,
                    )
                gameView.viewport = viewport
            }

            // ff-bgm1x: 비율 유지를 끄면 게임 화면을 영역에 늘인다. 편집기가 쓸 게임 화면 비율도 읽어 둔다
            val keepAspect = customLayoutState.layout.keepAspect
            LaunchedEffect(keepAspect, customLayoutEditShown.value) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                gameView.stretch = !keepAspect
                customLayoutState.contentAspect = gameView.getContentAspectRatio()
            }

            ConstraintLayout(
                modifier = Modifier.fillMaxSize(),
                constraintSet =
                    GameScreenLayout.buildConstraintSet(
                        isLandscape,
                        currentControllerConfig?.allowTouchOverlay ?: true,
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            .customLayoutElement(CustomLayoutKeys.GAME_VIEW)
                            .onGloballyPositioned { viewportPosition.value = it.boundsInRoot() },
                )

                val isVisible =
                    touchControllerSettings != null &&
                        currentControllerConfig != null &&
                        touchControlsVisibleState.value

                if (isVisible) {
                    CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                        if (skinImages != null) {
                            // 스킨이 있으면 버튼 영역의 어두운 바탕을 깔지 않는다
                        } else if (!isLandscape) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_BOTTOM_CONTAINER),
                            )
                        } else if (!currentControllerConfig.allowTouchOverlay) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_CONTAINER),
                            )
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_CONTAINER),
                            )
                        }

                        leftGamePad?.invoke(
                            this,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD),
                            touchControllerSettings,
                        )
                        rightGamePad?.invoke(
                            this,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD),
                            touchControllerSettings,
                        )

                        GameScreenRunningCentralMenu(
                            modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_CONTAINER),
                            controllerConfig = currentControllerConfig,
                            touchControllerSettings = touchControllerSettings,
                            viewModel = viewModel,
                        )
                    }
                }
            }

            // ff-bgm1x: 화면 빨리 감기 버튼 (편집 화면에서 켜야 보인다, 기본 위치는 오른쪽 위)
            if (customLayoutState.layout.showFastForwardButton && touchControlsVisibleState.value) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.displayCutout)
                                .padding(16.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        ControlButton(
                            modifier =
                                Modifier
                                    .size(56.dp)
                                    .customLayoutElement(CustomLayoutKeys.FAST_FORWARD),
                            id = Id.Key(BaseGameScreenViewModel.FAST_FORWARD_BUTTON_KEY),
                            foreground = { LemuroidButtonForeground(pressed = it, label = ">>") },
                            background = { LemuroidControlBackground() },
                        )
                    }
                }
            }

            // ff-bgm1x: 사용자가 만든 기능 버튼 (처음 자리는 화면 가운데)
            if (customLayoutState.layout.customButtons.isNotEmpty() && touchControlsVisibleState.value) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        customLayoutState.layout.customButtons.forEach { button ->
                            key(button.uid) {
                                val label = customFunctionName(CustomButtonFunction.of(button.function))
                                ControlButton(
                                    modifier =
                                        Modifier
                                            .size(56.dp)
                                            .customLayoutElement(CustomLayoutKeys.custom(button.uid)),
                                    id = Id.Key(CustomButtonFunction.keyCode(button)),
                                    foreground = { LemuroidButtonForeground(pressed = it, label = label) },
                                    background = { LemuroidControlBackground() },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (customLayoutEditShown.value) {
            CustomLayoutEditor(
                state = customLayoutState,
                initialForGame = loadedCustomLayout.value?.isGameSpecific ?: false,
                onSave = { layout, forGame -> viewModel.saveCustomLayout(layout, forGame) },
                onClear = { forGame -> viewModel.clearCustomLayout(forGame) },
                onClose = { viewModel.showCustomLayoutEdit(false) },
            )
        }
        }

        val isLoading =
            viewModel.loadingState
                .collectAsState(true)
                .value

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier,
        cornerRadius = theme.level0CornerRadius,
        fillColor = theme.level0Fill,
        shadowColor = theme.level0Shadow,
        shadowWidth = theme.level0ShadowWidth,
    )
}

@Composable
private fun GameScreenRunningCentralMenu(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
    controllerConfig: ControllerConfig,
) {
    val menuPressed = viewModel.isMenuPressed().collectAsState(false)
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        LemuroidButtonPressFeedback(
            pressed = menuPressed.value,
            animationDurationMillis = MENU_LOADING_ANIMATION_MILLIS,
            icon = R.drawable.button_menu,
        )
        MenuEditTouchControls(viewModel, controllerConfig, touchControllerSettings)
    }
}

@Composable
private fun MenuEditTouchControls(
    viewModel: BaseGameScreenViewModel,
    controllerConfig: ControllerConfig,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
) {
    val showEditControls = viewModel.isEditControlShown().collectAsState(false)
    if (!showEditControls.value) return

    Dialog(onDismissRequest = { viewModel.showEditControls(false) }) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuEditTouchControlRow(Icons.Default.OpenInFull, "Scale", 0f) {
                    Slider(
                        value = touchControllerSettings.scale,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(scale = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Horizontal Margin", 90f) {
                    Slider(
                        value = touchControllerSettings.marginX,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginX = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Vertical Margin", 0f) {
                    Slider(
                        value = touchControllerSettings.marginY,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginY = it),
                            )
                        },
                    )
                }
                if (controllerConfig.allowTouchRotation) {
                    MenuEditTouchControlRow(Icons.Default.RotateLeft, "Rotate", 0f) {
                        Slider(
                            value = touchControllerSettings.rotation,
                            onValueChange = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(rotation = it),
                                )
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // ff-bgm1x: 버튼 하나씩 자유 배치
                    TextButton(
                        onClick = {
                            viewModel.showEditControls(false)
                            viewModel.showCustomLayoutEdit(true)
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(com.swordfish.lemuroid.R.string.custom_layout_open))
                    }
                    TextButton(
                        onClick = { viewModel.resetTouchControls() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_reset))
                    }
                    TextButton(
                        onClick = { viewModel.showEditControls(false) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditTouchControlRow(
    icon: ImageVector,
    label: String,
    rotation: Float,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = icon,
            contentDescription = label,
        )
        slider()
    }
}
