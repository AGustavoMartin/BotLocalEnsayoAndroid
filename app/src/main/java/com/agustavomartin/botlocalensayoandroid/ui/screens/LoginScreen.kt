package com.agustavomartin.botlocalensayoandroid.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agustavomartin.botlocalensayoandroid.data.auth.AppSession
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthStartResult
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

private enum class LoginStep {
    Identify,
    CreatePin,
    EnterPin,
    ResetPin
}

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onAuthenticated: (AppSession) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(LoginStep.Identify) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                val hintedPhone = Identity.getSignInClient(context).getPhoneNumberFromIntent(result.data)
                phone = hintedPhone.filter { it.isDigit() }
            }.onFailure {
                errorMessage = "No se pudo leer el numero sugerido por Android."
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        ScreenHeader(
            eyebrow = "BOT",
            title = "Acceso a la app",
            subtitle = "Solo los miembros del grupo pueden entrar. La primera vez crearas tu PIN y despues la sesion se mantendra automaticamente."
        )

        if (step == LoginStep.Identify) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() } },
                label = { Text("Telefono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )

            Button(
                onClick = {
                    errorMessage = null
                    val request = GetPhoneNumberHintIntentRequest.builder().build()
                    Identity.getSignInClient(context)
                        .getPhoneNumberHintIntent(request)
                        .addOnSuccessListener { pendingIntent ->
                            phoneHintLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                        }
                        .addOnFailureListener {
                            errorMessage = "Android no ha podido sugerir el numero automaticamente en este dispositivo."
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263342), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Usar mi numero del sistema")
            }

            Button(
                onClick = {
                    loading = true
                    errorMessage = null
                    infoMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.start(phone) }
                            .onSuccess { result ->
                                when (result) {
                                    is AuthStartResult.CreatePin -> {
                                        phone = result.phone
                                        memberName = result.memberName
                                        step = LoginStep.CreatePin
                                    }
                                    is AuthStartResult.EnterPin -> {
                                        phone = result.phone
                                        memberName = result.memberName
                                        step = LoginStep.EnterPin
                                    }
                                    AuthStartResult.NotAllowed -> {
                                        errorMessage = "Ese telefono no esta autorizado en la base del grupo."
                                    }
                                }
                            }
                            .onFailure {
                                errorMessage = "No se ha podido validar el telefono."
                            }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E948B), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Comprobando..." else "Continuar")
            }
        }

        if (step == LoginStep.CreatePin) {
            Text("Miembro detectado: $memberName", color = Color(0xFFF6F3EC), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text("Crea tu PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text("Repite tu PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = "El PIN debe tener al menos 4 digitos."
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "Los dos PIN no coinciden."
                        return@Button
                    }
                    loading = true
                    errorMessage = null
                    infoMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.registerPin(phone, pin, "android-app") }
                            .onSuccess(onAuthenticated)
                            .onFailure { errorMessage = it.message ?: "No se pudo registrar el PIN." }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E948B), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Creando sesion..." else "Guardar PIN y entrar")
            }
        }

        if (step == LoginStep.EnterPin) {
            Text("Miembro detectado: $memberName", color = Color(0xFFF6F3EC), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text("Introduce tu PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = {
                    loading = true
                    errorMessage = null
                    infoMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.login(phone, pin, "android-app") }
                            .onSuccess(onAuthenticated)
                            .onFailure { errorMessage = it.message ?: "No se pudo iniciar sesion." }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E948B), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Entrando..." else "Entrar")
            }

            OutlinedButton(
                onClick = {
                    loading = true
                    errorMessage = null
                    infoMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.requestPinReset(phone) }
                            .onSuccess { challenge ->
                                phone = challenge.phone
                                memberName = challenge.memberName
                                step = LoginStep.ResetPin
                                pin = ""
                                confirmPin = ""
                                resetCode = ""
                                infoMessage = "Te hemos enviado por WhatsApp un codigo temporal para recuperar tu PIN."
                            }
                            .onFailure { errorMessage = mapResetError(it.message) }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Solicitando codigo..." else "He olvidado mi PIN")
            }
        }

        if (step == LoginStep.ResetPin) {
            Text("Recuperacion para: $memberName", color = Color(0xFFF6F3EC), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = resetCode,
                onValueChange = { resetCode = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("Codigo temporal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text("Nuevo PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text("Repite el nuevo PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = {
                    if (resetCode.length != 6) {
                        errorMessage = "Introduce el codigo temporal de 6 digitos."
                        return@Button
                    }
                    if (pin.length < 4) {
                        errorMessage = "El PIN debe tener al menos 4 digitos."
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "Los dos PIN no coinciden."
                        return@Button
                    }
                    loading = true
                    errorMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.confirmPinReset(phone, resetCode, pin, "android-app") }
                            .onSuccess(onAuthenticated)
                            .onFailure { errorMessage = mapResetError(it.message) }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E948B), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Restableciendo..." else "Guardar PIN nuevo y entrar")
            }

            OutlinedButton(
                onClick = {
                    loading = true
                    errorMessage = null
                    infoMessage = null
                    coroutineScope.launch {
                        runCatching { authRepository.requestPinReset(phone) }
                            .onSuccess {
                                infoMessage = "Te hemos enviado un codigo nuevo por WhatsApp."
                            }
                            .onFailure { errorMessage = mapResetError(it.message) }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (loading) "Reenviando..." else "Reenviar codigo")
            }
        }

        if (step != LoginStep.Identify) {
            Button(
                onClick = {
                    step = LoginStep.Identify
                    pin = ""
                    confirmPin = ""
                    resetCode = ""
                    memberName = ""
                    errorMessage = null
                    infoMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263342), contentColor = Color(0xFFF6F3EC)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Volver")
            }
        }

        infoMessage?.let {
            Text(
                text = it,
                color = Color(0xFF8DE2C4),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                color = Color(0xFFF28B82),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun mapResetError(message: String?): String = when (message) {
    "PIN_NOT_REGISTERED" -> "Ese numero aun no tenia PIN registrado. Puedes entrar por el flujo normal y crearlo."
    "RESET_CODE_NOT_FOUND" -> "No hay un codigo activo para ese numero. Solicita uno nuevo."
    "RESET_CODE_EXPIRED" -> "El codigo ha caducado. Pide uno nuevo desde la app."
    "INVALID_RESET_CODE" -> "El codigo temporal no es correcto."
    "MEMBER_NOT_FOUND", "MEMBER_NOT_ALLOWED" -> "Ese telefono no esta autorizado en la base del grupo."
    "HTML_RESPONSE" -> "La recuperacion de PIN aun no esta desplegada en el servidor o la ruta no responde correctamente."
    else -> message ?: "No se pudo recuperar el PIN."
}
