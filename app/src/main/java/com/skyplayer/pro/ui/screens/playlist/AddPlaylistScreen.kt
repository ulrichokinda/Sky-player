package com.skyplayer.pro.ui.screens.playlist

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.ui.theme.GradientStart
import kotlinx.coroutines.delay

/**
 * Écran d'ajout de playlist
 * Supporte M3U URL et Xtream Codes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    onBackClick: () -> Unit,
    onPlaylistAdded: () -> Unit,
    viewModel: AddPlaylistViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSuccess by viewModel.isSuccess.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Focus initial sur le premier champ du formulaire pour la télécommande TV
    // (le clavier reste accessible au doigt sur mobile — pas de focus forcé).
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val formFocusRequester = remember { FocusRequester() }
    LaunchedEffect(selectedTab) {
        if (isTV) {
            delay(150)
            formFocusRequester.requestFocus()
        }
    }
    
    // Champs M3U
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    
    // Champs Xtream
    var xtreamName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    
    if (isSuccess) {
        onPlaylistAdded()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajouter une playlist",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GradientStart
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("M3U URL") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Xtream Codes") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Formulaire selon le tab sélectionné
            when (selectedTab) {
                0 -> M3UForm(
                    name = m3uName,
                    url = m3uUrl,
                    onNameChange = { m3uName = it },
                    onUrlChange = { m3uUrl = it },
                    isLoading = isLoading,
                    focusRequester = formFocusRequester,
                    onSubmit = {
                        viewModel.addM3UPlaylist(m3uName, m3uUrl)
                    }
                )
                1 -> XtreamForm(
                    name = xtreamName,
                    username = username,
                    password = password,
                    serverUrl = serverUrl,
                    onNameChange = { xtreamName = it },
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onServerUrlChange = { serverUrl = it },
                    isLoading = isLoading,
                    focusRequester = formFocusRequester,
                    onSubmit = {
                        viewModel.addXtreamPlaylist(xtreamName, username, password, serverUrl)
                    }
                )
            }
            
            // Message d'erreur
            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun M3UForm(
    name: String,
    url: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nom de la playlist") },
            placeholder = { Text("Ex: Ma Playlist IPTV") },
            leadingIcon = {
                Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("URL de la playlist M3U") },
            placeholder = { Text("http://exemple.com/playlist.m3u") },
            leadingIcon = {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && url.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = GradientStart
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Ajouter la playlist")
            }
        }
    }
}

@Composable
private fun XtreamForm(
    name: String,
    username: String,
    password: String,
    serverUrl: String,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onServerUrlChange: (String) -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nom de la playlist") },
            leadingIcon = {
                Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true
        )
        
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Nom d'utilisateur") },
            leadingIcon = {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mot de passe") },
            leadingIcon = {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        
        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("URL du serveur") },
            placeholder = { Text("http://serveur.com:8080") },
            leadingIcon = {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && username.isNotBlank() && 
                     password.isNotBlank() && serverUrl.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = GradientStart
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Se connecter")
            }
        }
    }
}
