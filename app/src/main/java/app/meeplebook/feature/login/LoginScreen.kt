package app.meeplebook.feature.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.ui.isNotEmpty
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.login.effect.LoginUiEffect
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                LoginUiEffect.LoginSucceeded -> onLoginSuccess()
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.login_title)) }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = uiState.username,
                    onValueChange = { onEvent(LoginEvent.UsernameChanged(it)) },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("usernameField")
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = uiState.password,
                    onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("passwordField")
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (uiState.errorMessage.isNotEmpty()) {
                    UiTextText(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = { onEvent(LoginEvent.Submit) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.login))
                }
            }
        }
    }
}

/**
 * Provides preview parameter states for [LoginScreenContent]:
 * 1. Default/Empty state
 * 2. Filled credentials
 * 3. Loading state
 * 4. Error state with invalid credentials
 */
class LoginUiStatePreviewParameterProvider : PreviewParameterProvider<LoginUiState> {
    override val values: Sequence<LoginUiState> = sequenceOf(
        LoginUiState(),
        LoginUiState(username = "user123", password = "password"),
        LoginUiState(
            username = "loadingUser",
            password = "********",
            isLoading = true
        ),
        LoginUiState(
            username = "wrongUser",
            password = "1234",
            errorMessage = uiTextRes(R.string.msg_invalid_credentials_error)
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginScreenPreview(
    @PreviewParameter(LoginUiStatePreviewParameterProvider::class) uiState: LoginUiState
) {
    MeepleBookTheme {
        LoginScreenContent(
            uiState = uiState,
            onEvent = {}
        )
    }
}
