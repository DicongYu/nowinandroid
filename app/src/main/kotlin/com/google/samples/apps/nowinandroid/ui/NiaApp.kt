/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaGradientBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationSuiteScaffold
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopAppBar
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.GradientColors
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalGradientColors
import com.google.samples.apps.nowinandroid.feature.settings.SettingsDialog
import com.google.samples.apps.nowinandroid.navigation.NiaNavHost
import com.google.samples.apps.nowinandroid.navigation.TopLevelDestination
import kotlin.reflect.KClass
import com.google.samples.apps.nowinandroid.feature.settings.R as settingsR

@Composable//任何用 @Composable 注解的函数都可以作为 UI 组件,它们描述了 UI 的外观和行为 Composable 函数可以相互嵌套，形成 UI 树
fun NiaApp(//这是一个包装函数，主要负责处理全局的背景、离线状态提示，并管理设置对话框的显示状态
    appState: NiaAppState,
    modifier: Modifier = Modifier,//几乎所有 Compose UI 组件都会接受一个 Modifier 参数
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val shouldShowGradientBackground =//声明只读变量（不可重新赋值）
        appState.currentTopLevelDestination == TopLevelDestination.FOR_YOU
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }//声明可变变量（可以重新赋值） rememberSaveable类似于 remember，但它会在屏幕旋转、进程终止等情况下保留状态。它内部使用了 SavedStateHandle
                        //：by关键字 (属性委托)这是一种 Kotlin 的高级特性，允许将属性的 getter 和 setter 逻辑委托给另一个对象。在 Compose 中，它简化了状态管理
    NiaBackground(modifier = modifier) {//NiaBackground 和 NiaGradientBackground用于统一应用程序的背景样式。它们可能封装了 Surface 或其他基础 Composable 来应用颜色或渐变
        NiaGradientBackground(
            gradientColors = if (shouldShowGradientBackground) {
                LocalGradientColors.current
            } else {
                GradientColors()
            },
        ) {
            val snackbarHostState = remember { SnackbarHostState() }//状态管理 (State Management):在 Composable 函数重组时保留一个值，避免每次重组都重新创建对象

            val isOffline by appState.isOffline.collectAsStateWithLifecycle()//collectAsStateWithLifecycle()它用于将 Kotlin Flow 或 StateFlow 发出的数据作为 Compose 状态收集
                                                                            //它只在 Composable 处于活动状态（started 或 resumed）时收集数据，并在 Composable 停止时暂停收集，优化了资源使用。
            // If user is not connected to the internet show a snack bar to inform them.
            val notConnectedMessage = stringResource(R.string.not_connected)//Compose 中获取 Android 资源文件 strings.xml 中定义的字符串的方法。
            /*••用于在 Composable 函数的生命周期内执行副作用 (Side Effect)，例如启动协程、发送网络请求、显示 Snackbar 等。
            •它接受一个或多个 key 参数。当 key 的值发生变化时，LaunchedEffect 会取消当前的协程并重新启动一个新的协程。
            如果 key 保持不变，协程会继续运行直到 Composable 退出组合树。
            •这里的 key 是 isOffline，意味着当 isOffline 状态改变时，会重新评估并可能显示 Snackbar。*/
            LaunchedEffect(isOffline) {
                if (isOffline) {
                    snackbarHostState.showSnackbar(
                        message = notConnectedMessage,
                        duration = Indefinite,
                    )
                }
            }

            NiaApp(//实际构建应用程序骨架的函数，包括 Scaffold、NiaNavigationSuiteScaffold、TopAppBar、Snackbar 和 NiaNavHost
                appState = appState,
                snackbarHostState = snackbarHostState,
                showSettingsDialog = showSettingsDialog,
                onSettingsDismissed = { showSettingsDialog = false },
                onTopAppBarActionClick = { showSettingsDialog = true },
                windowAdaptiveInfo = windowAdaptiveInfo,
            )
        }
    }
}

@Composable
@OptIn(//用于标记使用了实验性 API 的代码块。Google 会将一些功能标记为实验性，表示它们可能在未来的版本中发生变化。使用 @OptIn 是一种明确的选择，表示开发者知晓其风险
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
internal fun NiaApp(//internal fun ...:只能在当前模块内部访问（在这里是 app 模块）
    appState: NiaAppState,
    snackbarHostState: SnackbarHostState,
    showSettingsDialog: Boolean,
    onSettingsDismissed: () -> Unit,
    onTopAppBarActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val unreadDestinations by appState.topLevelDestinationsWithUnreadResources
        .collectAsStateWithLifecycle()
    val currentDestination = appState.currentDestination

    if (showSettingsDialog) {
        SettingsDialog(//一个自定义的对话框 Composable，用于显示应用程序的设置界面
            onDismiss = { onSettingsDismissed() },
        )
    }
/*这是一个自定义的自适应导航组件，它结合了 Material 3 的 adaptive 库
* navigationSuiteItems = { ... }: 接受一个 NavigationSuiteScope 的 lambda，用于定义导航项（可能根据屏幕大小自动切换 NavigationBar、NavigationRail 或 PermanentNavigationDrawer）
* windowAdaptiveInfo: 用于获取当前窗口的自适应信息（如窗口宽度类别），以便组件可以根据不同的屏幕尺寸和布局做出响应。 */
    NiaNavigationSuiteScaffold(
        navigationSuiteItems = {
            appState.topLevelDestinations.forEach { destination ->
                val hasUnread = unreadDestinations.contains(destination)
                val selected = currentDestination
                    .isRouteInHierarchy(destination.baseRoute)
                item(
                    selected = selected,
                    onClick = { appState.navigateToTopLevelDestination(destination) },//Lambda 表达式：一种匿名函数，通常用花括号 { } 包裹，用于传递代码块作为参数
                    icon = {
                        Icon(
                            imageVector = destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = destination.selectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.iconTextId)) },
                    modifier = Modifier
                        .testTag("NiaNavItem")
                        .then(if (hasUnread) Modifier.notificationDot() else Modifier),
                )
            }
        },
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        /*•Scaffold 是 Material Design 中一个非常重要的布局组件，它提供了一个标准的布局结构，
        可以轻松地添加 TopAppBar、BottomAppBar、FloatingActionButton、Snackbar 等。
        •contentWindowInsets = WindowInsets(0, 0, 0, 0): 禁用 Scaffold 默认的窗口边距处理，通常是为了更精细地手动控制。
        •snackbarHost = { SnackbarHost(...) }: 指定用于显示 Snackbar 的主机。*/
        Scaffold(
            modifier = modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime,
                        ),
                    ),
                )
            },
        ) { padding ->
            Column(//Jetpack Compose 中最基本的布局容器 Column: 元素垂直排列
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                // Show the top app bar on top level destinations.
                val destination = appState.currentTopLevelDestination
                var shouldShowTopAppBar = false

                if (destination != null) {
                    shouldShowTopAppBar = true
                    NiaTopAppBar(//这是自定义的顶部应用栏，可能封装了 Material 3 的 TopAppBar 或 CenterAlignedTopAppBar，并添加了 Now in Android 特定的样式和行为。
                        titleRes = destination.titleTextId,// 接受字符串资源 ID 作为标题。
                        navigationIcon = NiaIcons.Search,//用于顶部栏的图标
                        navigationIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_top_app_bar_navigation_icon_description,
                        ),
                        actionIcon = NiaIcons.Settings,
                        actionIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_top_app_bar_action_icon_description,
                        ),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        onActionClick = { onTopAppBarActionClick() },// 回调函数，处理图标点击事件
                        onNavigationClick = { appState.navigateToSearch() },
                    )
                }

                Box(//Jetpack Compose 中最基本的布局容器 Box: 元素可以堆叠在彼此之上
                    // Workaround for https://issuetracker.google.com/338478720
                    modifier = Modifier.consumeWindowInsets(
                        if (shouldShowTopAppBar) {
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        } else {
                            WindowInsets(0, 0, 0, 0)
                        },
                    ),
                ) {
                    NiaNavHost(//这是自定义的导航宿主。它通常是 androidx.navigation.compose.NavHost 的一个封装，负责定义应用程序的导航图，并显示当前导航目标对应的 Composable。
                        appState = appState,
                        onShowSnackbar = { message, action ->//onShowSnackbar: 作为参数传递给 NiaNavHost 的回调函数，允许内部的导航目标（屏幕）请求显示 Snackbar。
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = Short,
                            ) == ActionPerformed
                        },
                    )
                }

                // TODO: We may want to add padding or spacer when the snackbar is shown so that
                //  content doesn't display behind it.
            }
        }
    }
}
//扩展函数 (Extension Functions) 允许你在不修改现有类源代码的情况下，为其添加新的函数
private fun Modifier.notificationDot(): Modifier =//private fun ...: 只能在当前文件内部访问
    composed {
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        drawWithContent {
            drawContent()
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                // This is based on the dimensions of the NavigationBar's "indicator pill";
                // however, its parameters are private, so we must depend on them implicitly
                // (NavigationBarTokens.ActiveIndicatorWidth = 64.dp)
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =//KClass<*>:代表一个 Kotlin 类类型。* 是星投影，表示任何类型参数。这里用于泛型地表示一个类的类型，通常用于反射或与路由系统结合，以类的类型作为路由标识。
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false
