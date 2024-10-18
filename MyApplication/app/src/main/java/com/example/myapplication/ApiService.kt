package com.example.myapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class Pregunta(
    val id: Int,
    val pregunta: String,
    val respostes: List<String>,
    val resposta_correcta: Int
)

sealed class RespuestaValor {
    data class Text(val valor: String) : RespuestaValor()
    data class Num(val valor: Int) : RespuestaValor()
}

data class Respuesta(
    val id_pregunta: Int,
    val resposta: RespuestaValor
)

data class RespuestaRequest(
    val uid: String,
    val respuestas: List<Respuesta>
)

data class UsuarioRequest(val usuario: String)
data class PartidaResponse(val uid: String, val preguntas: List<Pregunta>)

interface ApiService {
    @GET("/preguntes")
    fun getPreguntes(): Call<List<Pregunta>>

    @POST("/partida")
    fun crearPartida(@Body usuario: UsuarioRequest): Call<PartidaResponse>

    @POST("/respuestas")
    fun enviarRespuestas(@Body respuestas: RespuestaRequest): Call<Void>
}


