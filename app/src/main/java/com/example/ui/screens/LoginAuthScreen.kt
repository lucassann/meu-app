package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CineDarkGray
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineTextGray
import com.example.ui.theme.CineTextWhite
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginAuthScreen(
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CineDarkGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Acesso Premium",
                    color = CineGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Faça login com a conta fornecida pelo administrador para liberar o catálogo VIP.",
                    color = CineTextGray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail Premium", color = CineTextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha", color = CineTextGray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    val user = task.result?.user
                                    if (user != null) {
                                        Toast.makeText(context, "Bem-vindo VIP!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(user.uid)
                                    }
                                } else {
                                    Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Entrar", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Continuar Grátis", color = CineTextGray)
                }
            }
        }
    }
}
