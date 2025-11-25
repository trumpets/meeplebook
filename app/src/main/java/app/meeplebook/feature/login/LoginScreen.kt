package app.meeplebook.feature.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import app.meeplebook.R
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState().value

    // Navigate on successful login
    if (uiState.isLoggedIn) {
        LaunchedEffect(Unit) {
            onLoginSuccess()
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { viewModel.login() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit
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
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                uiState.errorMessageResId?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = onLoginClick,
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview_Default() {
    MeepleBookTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Default / Empty", style = MaterialTheme.typography.titleMedium)
            LoginScreenContent(
                uiState = LoginUiState(),
                onUsernameChange = {},
                onPasswordChange = {},
                onLoginClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview_Filled() {
    MeepleBookTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Filled Credentials", style = MaterialTheme.typography.titleMedium)
            LoginScreenContent(
                uiState = LoginUiState(username = "user123", password = "password"),
                onUsernameChange = {},
                onPasswordChange = {},
                onLoginClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview_Loading() {
    MeepleBookTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Loading", style = MaterialTheme.typography.titleMedium)
            LoginScreenContent(
                uiState = LoginUiState(
                    username = "loadingUser",
                    password = "********",
                    isLoading = true
                ),
                onUsernameChange = {},
                onPasswordChange = {},
                onLoginClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview_Error() {
    MeepleBookTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Error", style = MaterialTheme.typography.titleMedium)
            LoginScreenContent(
                uiState = LoginUiState(
                    username = "wrongUser",
                    password = "1234",
                    errorMessageResId = R.string.msg_invalid_credentials_error
                ),
                onUsernameChange = {},
                onPasswordChange = {},
                onLoginClick = {}
            )
        }
    }
}


@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreview_DefaultDark() {
    LoginScreenPreview_Default()
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreview_FilledDark() {
    LoginScreenPreview_Filled()
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreview_LoadingDark() {
    LoginScreenPreview_Loading()
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreview_ErrorDark() {
    LoginScreenPreview_Error()
}