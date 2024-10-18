package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    // Pantalla principal
                    composable("home") {
                        MainScreen(navController)
                    }
                    // Pantalla de la pregunta
                    composable("question/{uid}") { backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("uid") ?: ""
                        QuestionScreen(navController, uid)
                    }
                }
            }
        }
    }
}


@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    // Obtén el nombre de usuario fuera del bloque `remember`
    val nombreGuardado = getUserName(context) ?: ""

    // Usa `remember` solo para gestionar el estado mutable
    var nombreUsuario by remember { mutableStateOf(nombreGuardado) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (nombreUsuario.isEmpty()) {
                // Si el nombre no está guardado, pide al usuario que lo ingrese
                TextField(
                    value = nombreUsuario,
                    onValueChange = { nombreUsuario = it },
                    label = { Text("Nombre de usuario") }
                )
            } else {
                // Si el nombre está guardado, muestra el saludo
                Text(text = "Hola $nombreUsuario")
            }
            Spacer(modifier = Modifier.height(16.dp))
            FilledButtonExample {
                if (nombreUsuario.isNotEmpty()) {
                    // Guardar el nombre del usuario
                    saveUserName(context, nombreUsuario)
                    val usuario = UsuarioRequest(nombreUsuario)
                    RetroFitClient.instance.crearPartida(usuario).enqueue(object : Callback<PartidaResponse> {
                        override fun onResponse(call: Call<PartidaResponse>, response: Response<PartidaResponse>) {
                            if (response.isSuccessful) {
                                val partida = response.body()
                                val uid = partida?.uid ?: ""
                                navController.navigate("question/$uid")
                            } else {
                                Log.e("MainScreen", "Error al crear la partida: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<PartidaResponse>, t: Throwable) {
                            Log.e("MainScreen", "Error al crear la partida", t)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun QuestionScreen(navController: NavController, uid: String) {
    var preguntes by remember { mutableStateOf<List<Pregunta>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val respuestasSeleccionadas = remember { mutableStateListOf<Respuesta>() }
    var respuestasEnviadas by remember { mutableStateOf(false) }

    // Cargar las preguntas cuando se inicie la pantalla
    LaunchedEffect(Unit) {
        RetroFitClient.instance.getPreguntes().enqueue(object : Callback<List<Pregunta>> {
            override fun onResponse(call: Call<List<Pregunta>>, response: Response<List<Pregunta>>) {
                if (response.isSuccessful) {
                    val allPreguntes = response.body() ?: emptyList()
                    preguntes = allPreguntes.shuffled().take(10) // Tomar solo 10 preguntas
                    Log.d("QuestionScreen", "Preguntas recibidas: $preguntes")
                }
            }

            override fun onFailure(call: Call<List<Pregunta>>, t: Throwable) {
                Log.e("QuestionScreen", "Error al obtener las preguntas", t)
            }
        })
    }

    // UI de la pantalla
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preguntes.isNotEmpty() && currentQuestionIndex < preguntes.size) {
                val pregunta = preguntes[currentQuestionIndex]
                Text(text = pregunta.pregunta)

                Spacer(modifier = Modifier.height(16.dp))

                pregunta.respostes.forEach { resposta ->
                    AnswerButton(text = resposta, onClick = {
                        // Verificar si la respuesta es un número o un texto
                        val respuestaValor = try {
                            RespuestaValor.Num(resposta.toInt())
                        } catch (e: NumberFormatException) {
                            RespuestaValor.Text(resposta)
                        }

                        respuestasSeleccionadas.add(Respuesta(pregunta.id, respuestaValor))
                        currentQuestionIndex++

                        // Enviar respuestas al servidor cuando se termine el quiz
                        if (currentQuestionIndex >= preguntes.size && !respuestasEnviadas) {
                            Log.d("QuestionScreen", "Enviando respuestas al servidor...")
                            enviarRespuestasAlServidor(uid, respuestasSeleccionadas) {
                                respuestasEnviadas = true
                                Log.d("QuestionScreen", "Respuestas enviadas, estado actualizado a $respuestasEnviadas")
                            }
                        }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // Mostrar mensaje de fin de preguntas y botón de reinicio
                if (preguntes.isEmpty()) {
                    Text(text = "No hay preguntas disponibles.")
                } else {
                    Text(text = "Has terminado las preguntas.")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para reiniciar el quiz y regresar a la pantalla principal
                    Button(onClick = {
                        // Navegar a la pantalla principal sin solicitar el nombre nuevamente
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true } // Regresar eliminando el stack de navegación
                        }
                    }) {
                        Text(text = "Reiniciar Quiz")
                    }
                }
            }
        }
    }
}


fun enviarRespuestasAlServidor(uid: String, respuestas: List<Respuesta>, onSent: () -> Unit) {
    val request = RespuestaRequest(uid, respuestas)
    RetroFitClient.instance.enviarRespuestas(request).enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) {
                Log.d("QuestionScreen", "Respuestas enviadas exitosamente")
                onSent() // Llama al callback para marcar que las respuestas fueron enviadas
            } else {
                Log.e("QuestionScreen", "Error al enviar respuestas: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            Log.e("QuestionScreen", "Error al enviar respuestas", t)
        }
    })
}

fun saveUserName(context: Context, name: String) {
    val sharedPref: SharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("userName", name)
        apply()
    }
}

fun getUserName(context: Context): String? {
    val sharedPref: SharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    return sharedPref.getString("userName", null)
}

@Composable
fun AnswerButton(text: String, onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(text)
    }
}

@Composable
fun FilledButtonExample(onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text("Start Quiz")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        val navController = rememberNavController()
        MainScreen(navController = navController)
    }
}