package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.BuildConfig
import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.mobile.feature.game.GameService
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.shared.cheats.CheatsManager
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelCustomLayout
import com.swordfish.touchinput.radial.custom.CustomLayout
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelInput
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSaves
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSideEffects
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTilt
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.rumble.RumbleManager
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.common.longAnimationDuration
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.lemuroid.R
import gg.padkit.inputevents.InputEvent
import com.swordfish.libretrodroid.Variable
import com.swordfish.touchinput.radial.custom.CustomButtonFunction
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class BaseGameScreenViewModel(
    private val appContext: Context,
    private val game: Game,
    private val settingsManager: SettingsManager,
    inputDeviceManager: InputDeviceManager,
    controllerConfigsManager: ControllerConfigsManager,
    system: GameSystem,
    private val systemCoreConfig: SystemCoreConfig,
    private val sharedPreferences: SharedPreferences,
    savesManager: SavesManager,
    statesManager: StatesManager,
    statesPreviewManager: StatesPreviewManager,
    coreVariablesManager: CoreVariablesManager,
    rumbleManager: RumbleManager,
) : ViewModel(), DefaultLifecycleObserver {
    class Factory(
        private val appContext: Context,
        private val game: Game,
        private val settingsManager: SettingsManager,
        private val inputDeviceManager: InputDeviceManager,
        private val controllerConfigsManager: ControllerConfigsManager,
        private val system: GameSystem,
        private val systemCoreConfig: SystemCoreConfig,
        private val sharedPreferences: SharedPreferences,
        private val savesManager: SavesManager,
        private val statesManager: StatesManager,
        private val statesPreviewManager: StatesPreviewManager,
        private val coreVariablesManager: CoreVariablesManager,
        private val rumbleManager: RumbleManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BaseGameScreenViewModel(
                appContext,
                game,
                settingsManager,
                inputDeviceManager,
                controllerConfigsManager,
                system,
                systemCoreConfig,
                sharedPreferences,
                savesManager,
                statesManager,
                statesPreviewManager,
                coreVariablesManager,
                rumbleManager,
            ) as T
        }
    }

    private val sideEffects = GameViewModelSideEffects(viewModelScope)
    val retroGameView =
        GameViewModelRetroGameView(
            appContext,
            system,
            systemCoreConfig,
            settingsManager,
            coreVariablesManager,
            sideEffects,
            rumbleManager,
            viewModelScope,
        )
    private val tilt = GameViewModelTilt(appContext, settingsManager)
    private val inputs =
        GameViewModelInput(
            appContext,
            system,
            systemCoreConfig,
            inputDeviceManager,
            controllerConfigsManager,
            retroGameView,
            tilt,
            sideEffects,
            viewModelScope,
        )
    private val touchControls =
        GameViewModelTouchControls(
            settingsManager,
            TouchControllerSettingsManager(sharedPreferences),
            retroGameView,
            inputs,
            tilt,
            sideEffects,
            viewModelScope,
        )
    private val saves =
        GameViewModelSaves(
            appContext,
            system,
            game,
            systemCoreConfig,
            retroGameView,
            settingsManager,
            savesManager,
            statesManager,
            statesPreviewManager,
            sideEffects,
        )

    val loadingState = MutableStateFlow(false)

    private inline fun withLoading(block: () -> Unit) {
        loadingState.value = true
        block()
        loadingState.value = false
    }

    fun getGameState(): Flow<GameViewModelRetroGameView.GameState> {
        return retroGameView.getGameState()
    }

    fun getSideEffects(): Flow<GameViewModelSideEffects.UiEffect> {
        return sideEffects.getUiEffects()
    }

    fun getTiltConfiguration(): Flow<TiltConfiguration> {
        return tilt.getTiltConfiguration()
    }

    fun getSimulatedTiltEvents(): Flow<InputState> {
        return tilt.getSimulatedTiltEvents()
    }

    fun getTouchControlsSettings(
        density: Density,
        insets: WindowInsets,
    ): Flow<TouchControllerSettingsManager.Settings?> {
        return touchControls.getTouchControlsSettings(density, insets)
    }

    fun getTouchHapticFeedbackMode(): Flow<HapticFeedbackMode> {
        return touchControls.getTouchHapticFeedbackMode()
    }

    fun createRetroView(
        context: Context,
        lifecycle: LifecycleOwner,
    ): GLRetroView {
        val (gameData, result) = retroGameView.createRetroView(context, lifecycle)
        viewModelScope.launch {
            gameData.quickSaveData?.let {
                saves.restoreAutoSaveAsync(it)
            }
        }
        return result
    }

    suspend fun loadGame(
        applicationContext: Context,
        game: Game,
        systemCoreConfig: SystemCoreConfig,
        gameLoader: GameLoader,
        requestLoadSave: Boolean,
    ) {
        Timber.i("Calling load game: $game")
        retroGameView.initialize(applicationContext, game, systemCoreConfig, gameLoader, requestLoadSave)
    }

    fun showEditControls(show: Boolean) {
        touchControls.showEditControls(show)
    }

    fun isEditControlShown(): Flow<Boolean> {
        return touchControls.isEditControlsShown()
    }

    fun updateTouchControllerSettings(touchControllerSettings: TouchControllerSettingsManager.Settings) {
        touchControls.updateTouchControllerSettings(touchControllerSettings)
    }

    fun resetTouchControls() {
        touchControls.resetTouchControls()
    }

    fun onScreenOrientationChanged(orientation: TouchControllerSettingsManager.Orientation) {
        touchControls.updateScreenOrientation(orientation)
    }

    fun isTouchControllerVisible(): Flow<Boolean> {
        return touchControls.isTouchControllerVisible()
    }

    fun getTouchControllerConfig(): Flow<ControllerConfig> {
        return touchControls.getTouchControllerConfig()
    }

    fun changeTiltConfiguration(tiltConfig: TiltConfiguration) {
        tilt.changeTiltConfiguration(tiltConfig)
    }

    fun isMenuPressed(): Flow<Boolean> {
        return touchControls.isMenuPressed()
    }

    suspend fun saveSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            saves.saveSlot(index)
        }
    }

    suspend fun loadSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            saves.loadSlot(index)
        }
    }

    fun saveQuickSave() {
        Timber.d("Saving quick save")
        if (loadingState.value) return
        withLoading {
            saves.saveQuickSave()
        }
    }

    fun loadQuickSave() {
        Timber.d("Loading quick save")
        if (loadingState.value) return
        withLoading {
            saves.loadQuickSave()
        }
    }

    // ff-bgm1x: 빨리 감기 배수. 게임별 값(0 이면 없음)이 있으면 그것, 없으면 설정의 기본값
    private var defaultFastForwardSpeed = SettingsManager.DEFAULT_FAST_FORWARD_SPEED

    // ff-bgm1x: 게임별 치트
    private val cheatsManager = CheatsManager(appContext)

    init {
        viewModelScope.launch {
            defaultFastForwardSpeed = settingsManager.fastForwardSpeed()
        }
        viewModelScope.launch {
            retroGameView.waitGLEvent<GLRetroView.GLRetroEvents.FrameRendered>()
            applyCheats()
            // ff-bgm1x 소리 분류(1x Lab 앱): 게임별 분류 파일 폴더를 코어에 알린다 (지정이 게임 처음부터 적용되게)
            if (BuildConfig.FF_LAB) {
                runCatching {
                    com.swordfish.libretrodroid.LibretroDroid.catalogCommand(
                        "dir " + java.io.File(appContext.filesDir, "ff-bgm1x-sounds").absolutePath,
                    )
                }
            }
        }
        CustomButtonFunction.soundCatalogAvailable = BuildConfig.FF_LAB
    }

    // 켜진 치트만 다시 건다 (하나를 끄면 전부 지운 뒤 나머지를 다시 거는 방식)
    fun applyCheats() {
        retroGameView.retroGameView?.applyCheats(CheatsManager.enabledCodes(cheatsManager.load(game)))
    }

    private fun gameFastForwardSpeedKey() = "ff_bgm1x_fast_forward_speed_game_${game.id}"

    fun gameFastForwardSpeed(): Int = sharedPreferences.getInt(gameFastForwardSpeedKey(), 0)

    fun defaultFastForwardSpeed(): Int = defaultFastForwardSpeed

    // ff-bgm1x: 게임 화면 방향. 이 게임 값("" 이면 없음)이 있으면 그것, 없으면 설정의 기본값
    private fun gameScreenOrientationKey() = "ff_bgm1x_screen_orientation_game_${game.id}"

    fun gameScreenOrientation(): String = sharedPreferences.getString(gameScreenOrientationKey(), "") ?: ""

    fun setGameScreenOrientation(value: String) {
        sharedPreferences.edit().putString(gameScreenOrientationKey(), value).apply()
    }

    suspend fun defaultScreenOrientation(): String = settingsManager.screenOrientation()

    suspend fun screenOrientation(): String = gameScreenOrientation().ifEmpty { defaultScreenOrientation() }

    fun fastForwardSpeed(): Int = gameFastForwardSpeed().takeIf { it > 0 } ?: defaultFastForwardSpeed

    fun setGameFastForwardSpeed(speed: Int) {
        sharedPreferences.edit().putInt(gameFastForwardSpeedKey(), speed).apply()
        retroGameView.retroGameView?.apply {
            if (frameSpeed > 1) frameSpeed = fastForwardSpeed()
        }
    }

    fun setFastForward(enabled: Boolean) {
        retroGameView.retroGameView?.apply {
            frameSpeed = if (enabled) fastForwardSpeed() else 1
        }
    }

    fun toggleFastForward() {
        retroGameView.retroGameView?.apply {
            frameSpeed = if (frameSpeed == 1) fastForwardSpeed() else 1
        }
    }

    suspend fun reset() =
        withLoading {
            try {
                delay(appContext.longAnimationDuration().toLong())
                retroGameView.retroGameViewFlow().reset()
            } catch (e: Throwable) {
                Timber.e(e, "Error in reset")
            }
        }

    fun requestFinish() {
        if (loadingState.value) return
        viewModelScope.launch {
            withLoading {
                val snapshot = saves.captureSaveSnapshot(true) ?: return@launch
                saves.writeSaveSnapshot(snapshot)
                sideEffects.requestSuccessfulFinish()
            }
        }
    }

    fun requestBackgroundSave() {
        if (loadingState.value) return
        GameService.schedule {
            val snapshot = saves.captureSaveSnapshot(false)
            saves.writeSaveSnapshot(snapshot)
        }
    }

    // ff-bgm1x: 1배속 옵션 이름은 코어마다 앞부분이 다르다 (GB/GBC 는 Gambatte)
    private fun ffBgm1xKey(suffix: String = ""): String {
        val prefix =
            when (systemCoreConfig.coreID) {
                CoreID.GAMBATTE -> "gambatte"
                CoreID.VBAM -> "vbam"
                else -> "mgba"
            }
        return "${prefix}_ff_bgm1x$suffix"
    }

    // ff-bgm1x: 코어 옵션(enabled/disabled)을 바꿔 설정에 저장하고 바로 코어에 알린다
    private fun toggleCoreOption(
        key: String,
        defaultEnabled: Boolean,
        nameRes: Int,
    ) {
        val prefKey = CoreVariablesManager.computeSharedPreferenceKey(key, game.systemId)
        val current = sharedPreferences.getString(prefKey, if (defaultEnabled) "enabled" else "disabled")
        val enabled = current != "enabled"
        val value = if (enabled) "enabled" else "disabled"
        sharedPreferences.edit().putString(prefKey, value).apply()
        retroGameView.retroGameView?.updateVariables(Variable(key, value))
        val state = appContext.getString(if (enabled) R.string.custom_func_state_on else R.string.custom_func_state_off)
        sideEffects.showToast("${appContext.getString(nameRes)}: $state")
    }

    fun handleVirtualInputEvent(events: List<InputEvent>) {
        // ff-bgm1x: 화면의 빨리 감기 버튼은 게임에 보내지 않고 빨리 감기를 켜고 끈다
        val (fastForward, others) =
            events.partition { it is InputEvent.Button && it.id == FAST_FORWARD_BUTTON_KEY }
        if (fastForward.any { (it as InputEvent.Button).pressed }) {
            toggleFastForward()
        }
        // ff-bgm1x: 사용자가 만든 기능 버튼도 게임에 보내지 않고 그 기능을 실행한다
        val (custom, rest) =
            others.partition { it is InputEvent.Button && CustomButtonFunction.fromKeyCode(it.id) != null }
        custom.forEach { event ->
            event as InputEvent.Button
            when (CustomButtonFunction.fromKeyCode(event.id)) {
                CustomButtonFunction.MENU ->
                    touchControls.handleVirtualInputEvent(listOf(InputEvent.Button(KeyEvent.KEYCODE_BUTTON_MODE, event.pressed)))
                CustomButtonFunction.FAST_FORWARD -> if (event.pressed) toggleFastForward()
                CustomButtonFunction.QUICK_SAVE -> if (event.pressed) sideEffects.saveQuickSave()
                CustomButtonFunction.QUICK_LOAD -> if (event.pressed) sideEffects.loadQuickSave()
                CustomButtonFunction.BGM_1X -> if (event.pressed) toggleCoreOption(ffBgm1xKey(), true, R.string.custom_func_bgm_1x)
                CustomButtonFunction.SFX_1X -> if (event.pressed) toggleCoreOption(ffBgm1xKey("_sfx"), true, R.string.custom_func_sfx_1x)
                CustomButtonFunction.FF_CUTSCENES ->
                    if (event.pressed) toggleCoreOption(ffBgm1xKey("_cutscenes"), false, R.string.custom_func_ff_cutscenes)
                CustomButtonFunction.SOUND_CATALOG ->
                    if (BuildConfig.FF_LAB) {
                        if (event.pressed) GameMenuContract.pendingStartRoute = GameMenuContract.ROUTE_SOUNDS
                        touchControls.handleVirtualInputEvent(listOf(InputEvent.Button(KeyEvent.KEYCODE_BUTTON_MODE, event.pressed)))
                    }
                CustomButtonFunction.NONE, null -> {}
            }
        }
        touchControls.handleVirtualInputEvent(rest)
    }

    // ff-bgm1x: 버튼·게임 화면 자유 배치 (기본 / 이 게임)
    private val customLayout = GameViewModelCustomLayout(sharedPreferences, game)

    fun getCustomLayout(): Flow<GameViewModelCustomLayout.Loaded> =
        customLayout.observe(touchControls.touchControllerIdFlow(), touchControls.screenOrientationFlow())

    fun saveCustomLayout(
        layout: CustomLayout,
        forGame: Boolean,
    ) {
        customLayout.save(touchControls.currentTouchControllerId(), touchControls.currentScreenOrientation(), layout, forGame)
    }

    fun clearCustomLayout(forGame: Boolean) {
        customLayout.clear(touchControls.currentTouchControllerId(), touchControls.currentScreenOrientation(), forGame)
    }

    // 자유 배치 편집 화면 표시
    private val editCustomLayout = MutableStateFlow(false)

    fun isCustomLayoutEditShown(): Flow<Boolean> = editCustomLayout

    fun showCustomLayoutEdit(show: Boolean) {
        editCustomLayout.value = show
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        owner.lifecycle.addObserver(tilt)
        owner.lifecycle.addObserver(inputs)
        owner.lifecycle.addObserver(retroGameView)
        owner.lifecycle.addObserver(touchControls)
    }

    fun sendKeyEvent(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return inputs.sendKeyEvent(keyCode, event)
    }

    fun sendMotionEvent(event: MotionEvent): Boolean {
        return inputs.sendMotionEvent(event)
    }

    companion object {
        // ff-bgm1x: 화면 빨리 감기 버튼의 가상 키 (게임에는 보내지 않는다)
        const val FAST_FORWARD_BUTTON_KEY = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    }
}
