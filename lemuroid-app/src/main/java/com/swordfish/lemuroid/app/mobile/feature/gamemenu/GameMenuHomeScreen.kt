package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.alorma.compose.settings.storage.memory.rememberMemoryBooleanSettingState
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.tilt.TiltConfigurationMenuEntry
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.mobile.feature.settings.screenOrientationName
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (gameMenuRequest.coreConfig.statesSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_save)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_save),
                        contentDescription = stringResource(id = R.string.game_menu_save),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
            )

            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_load)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_load),
                        contentDescription = stringResource(id = R.string.game_menu_load),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
            )
        }

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_quit)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_quit),
                    contentDescription = stringResource(id = R.string.game_menu_quit),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_QUIT, true) }
            },
        )

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_restart)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_restart),
                    contentDescription = stringResource(id = R.string.game_menu_restart),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_RESET, true) }
            },
        )

        LemuroidSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.game_menu_mute_audio)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_mute),
                    contentDescription = stringResource(id = R.string.game_menu_mute_audio),
                )
            },
            state = rememberMemoryBooleanSettingState(!gameMenuRequest.audioEnabled),
            onCheckedChange = {
                onResult { putExtra(GameMenuContract.RESULT_ENABLE_AUDIO, !it) }
            },
        )

        if (gameMenuRequest.fastForwardSupported) {
            LemuroidSettingsSwitch(
                title = { Text(text = stringResource(id = R.string.game_menu_fast_forward)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_fast_forward),
                        contentDescription = stringResource(id = R.string.game_menu_fast_forward),
                    )
                },
                state = rememberMemoryBooleanSettingState(gameMenuRequest.fastForwardEnabled),
                onCheckedChange = {
                    onResult { putExtra(GameMenuContract.RESULT_ENABLE_FAST_FORWARD, it) }
                },
            )

            // ff-bgm1x: 이 게임의 빨리 감기 배수 (첫 항목 = 기본값)
            val speeds = listOf(0) + SettingsManager.FAST_FORWARD_SPEEDS
            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_fast_forward_speed)) },
                items =
                    speeds.map {
                        if (it == 0) {
                            stringResource(R.string.game_menu_fast_forward_speed_default, gameMenuRequest.defaultFastForwardSpeed)
                        } else {
                            stringResource(R.string.fast_forward_speed_value, it)
                        }
                    },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_fast_forward),
                        contentDescription = stringResource(id = R.string.game_menu_fast_forward_speed),
                    )
                },
                state = rememberMemoryIntSettingState(speeds.indexOf(gameMenuRequest.gameFastForwardSpeed).coerceAtLeast(0)),
                onItemSelected = { index, _ ->
                    onResult { putExtra(GameMenuContract.RESULT_CHANGE_GAME_FAST_FORWARD_SPEED, speeds[index]) }
                },
            )
        }

        if (gameMenuRequest.numDisks > 1) {
            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_change_disk_button)) },
                items = (1..gameMenuRequest.numDisks).map { stringResource(R.string.game_menu_change_disk_disk, it) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_disk),
                        contentDescription = stringResource(id = R.string.game_menu_change_disk_button),
                    )
                },
                state = rememberMemoryIntSettingState(gameMenuRequest.currentDisk),
                onItemSelected = { index, _ ->
                    onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, index) }
                },
            )
        }

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_edit_touch_controls)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_controls),
                    contentDescription = stringResource(id = R.string.game_menu_edit_touch_controls),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) }
            },
        )

        if (gameMenuRequest.advancedCoreOptions.isNotEmpty() || gameMenuRequest.coreOptions.isNotEmpty()) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_settings)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_settings),
                        contentDescription = stringResource(id = R.string.game_menu_settings),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.OPTIONS) },
            )
        }

        // ff-bgm1x: 이 게임의 화면 방향 (첫 항목 = 기본값)
        val orientations = listOf("") + SettingsManager.SCREEN_ORIENTATIONS
        LemuroidSettingsList(
            title = { Text(text = stringResource(id = R.string.game_menu_screen_orientation)) },
            items =
                orientations.map {
                    if (it.isEmpty()) {
                        stringResource(
                            R.string.screen_orientation_default,
                            screenOrientationName(gameMenuRequest.defaultScreenOrientation),
                        )
                    } else {
                        screenOrientationName(it)
                    }
                },
            icon = {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = stringResource(id = R.string.game_menu_screen_orientation),
                )
            },
            state = rememberMemoryIntSettingState(orientations.indexOf(gameMenuRequest.gameScreenOrientation).coerceAtLeast(0)),
            onItemSelected = { index, _ ->
                onResult { putExtra(GameMenuContract.RESULT_CHANGE_GAME_SCREEN_ORIENTATION, orientations[index]) }
            },
        )

        // ff-bgm1x: 치트
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_cheats)) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = stringResource(id = R.string.game_menu_cheats),
                )
            },
            onClick = { navController.navigateToRoute(GameMenuRoute.CHEATS) },
        )
        // ff-bgm1x 소리 분류 (1x Lab 앱)
        if (com.swordfish.lemuroid.BuildConfig.FF_LAB) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_sounds)) },
                icon = {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.MusicNote,
                        contentDescription = stringResource(id = R.string.game_menu_sounds),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.SOUNDS) },
            )
        }

        if (gameMenuRequest.allTiltConfigurations.isNotEmpty()) {
            val tiltConfigurationEntries =
                gameMenuRequest.allTiltConfigurations
                    .map { TiltConfigurationMenuEntry.fromTiltConfiguration(it) }

            val selectedIndex =
                gameMenuRequest.allTiltConfigurations
                    .indexOf(gameMenuRequest.currentTiltConfiguration)

            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_tilt_sensor)) },
                items = tiltConfigurationEntries.map { stringResource(it.descriptionId) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = stringResource(id = R.string.game_menu_tilt_sensor),
                    )
                },
                state = rememberMemoryIntSettingState(selectedIndex),
                onItemSelected = { index, _ ->
                    onResult {
                        putExtra(
                            GameMenuContract.RESULT_CHANGE_TILT_CONFIG,
                            tiltConfigurationEntries[index].configuration,
                        )
                    }
                },
            )
        }
    }
}
