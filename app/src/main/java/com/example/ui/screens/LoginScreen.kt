package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.QualityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: QualityViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = IndLightBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Branding QC)
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(IndBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Shield Logo",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Auditoría de Control",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = IndDarkBg,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sistema de Gestión de Calidad e ISO 9001",
                            fontSize = 13.sp,
                            color = IndSteelSlate,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Main Form Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, IndCargoGrey),
                        colors = CardDefaults.cardColors(containerColor = IndLightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // MODE SEGMENTED CONTROL (Sign In / Sign Up)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isSignUpMode) IndBluePrimary else Color.Transparent)
                                        .clickable { isSignUpMode = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Iniciar Sesión",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isSignUpMode) Color.White else IndSteelSlate
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSignUpMode) IndBluePrimary else Color.Transparent)
                                        .clickable { isSignUpMode = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Registrarse",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSignUpMode) Color.White else IndSteelSlate
                                    )
                                }
                            }

                            Text(
                                text = if (isSignUpMode) "Crea una cuenta local de auditor" else "Ingresa tus credenciales oficiales",
                                fontSize = 12.sp,
                                color = IndSteelSlate,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            // Name Field (Only in sign up mode)
                            AnimatedVisibility(
                                visible = isSignUpMode,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nombre Completo") },
                                    placeholder = { Text("Ej. Juan Pérez") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_name_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = IndSteelSlate) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IndBluePrimary,
                                        unfocusedBorderColor = IndCargoGrey,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Correo Electrónico") },
                                placeholder = { Text("usuario@empresa.com") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = IndSteelSlate) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndBluePrimary,
                                    unfocusedBorderColor = IndCargoGrey,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = IndSteelSlate) },
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = "Ver Contraseña")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndBluePrimary,
                                    unfocusedBorderColor = IndCargoGrey,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action Button
                            Button(
                                onClick = {
                                    if (isSignUpMode) {
                                        viewModel.signUpUser(
                                            email = email,
                                            name = name,
                                            password = password,
                                            onSuccess = {
                                                Toast.makeText(context, "Cuenta creada y conectado correctamente", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        viewModel.signInUser(
                                            email = email,
                                            password = password,
                                            onSuccess = {
                                                Toast.makeText(context, "¡Sesión iniciada correctamente!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auth_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSignUpMode) "REGISTRARSE" else "INICIAR SESIÓN",
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Default local auditor bypass card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IndBlueLight),
                        border = BorderStroke(1.dp, IndBluePrimary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "info",
                                    tint = IndBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Acceso Rápido Offline-First",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = IndBluePrimary
                                )
                            }
                            Text(
                                text = "Como requerimiento offline, puedes ingresar instantáneamente con las credenciales precargadas:\n• Correo: supervisor@qc.com\n• Contraseña: 123",
                                fontSize = 11.sp,
                                color = IndSteelSlate,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Firebase sync indicator note
                item {
                    Text(
                        text = "Soporta sincronización en la nube con Firebase backend de forma automática al detectar conexión.",
                        fontSize = 11.sp,
                        color = IndSteelSlate.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
