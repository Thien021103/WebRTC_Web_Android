package io.getstream.webrtc.sample.compose.ui.screens.list

import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class Video(
  val id: String, // Cloudinary public_id
  val displayName: String,
  val url: String, // Cloudinary secure_url
  val thumbnailUrl: String? = null,
)

class VideoListViewModel(
  private val token: String
) : ViewModel() {

  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  init {
    fetchVideos()
  }

  private fun fetchVideos() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        try {
          val client = OkHttpClient()
          val getUrl = "https://thientranduc.id.vn:444/api/get-videos"

          val request = Request.Builder()
            .url(getUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

          val response = client.newCall(request).execute()
          if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: """{"videos":[]}"""
            val jsonObject = Json.decodeFromString<JsonObject>(responseBody)
            val videosArray = jsonObject["videos"]?.jsonArray ?: JsonArray(emptyList())
            val videos = videosArray.mapNotNull { json ->
              val obj = json.jsonObject
              Video(
                id = obj["public_id"]?.jsonPrimitive?.content ?: "",
                displayName = obj["name"]?.jsonPrimitive?.content ?: "",
                url = obj["secure_url"]?.jsonPrimitive?.content ?: "",
                thumbnailUrl = obj["secure_url"]?.jsonPrimitive?.content
                  ?.replace("/upload/", "/upload/so_0/")
                  ?.replace(".mp4", ".jpg")
              )
            }
            _videos.value = videos
            Log.d("VideoListViewModel", "Fetched ${videos.size} videos")
          } else {
            Log.e("VideoListViewModel", "Failed to fetch videos: ${response.code}")
          }
        } catch (e: Exception) {
          Log.e("VideoListViewModel", "Failed to fetch videos: ${e.message}")
        }
      }
    }
  }
  fun deleteVideo(
    video: Video,
    onDone: () -> Unit) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        try {
          val client = OkHttpClient()
          val deleteUrl = "https://thientranduc.id.vn:444/api/delete-videos"

          val body = JSONObject().apply {
            put("publicId", video.id)
          }.toString()
          val request = Request.Builder()
            .url(deleteUrl)
            .addHeader("Authorization", "Bearer $token")
            .delete(body.toRequestBody("application/json".toMediaType()))
            .build()

          val response = client.newCall(request).execute()
          if (response.isSuccessful) {
            Log.d("VideoListViewModel", "Deleted ${video.id.substringAfterLast("/")}")
            fetchVideos()
            onDone()
          } else {
            Log.e("VideoListViewModel", "Failed to delete ${video.displayName}: ${response.code}")
            onDone()
          }
        } catch (e: Exception) {
          Log.e("VideoListViewModel", "Error deleting ${video.displayName}: ${e.message}")
          onDone()
        }
      }
    }
  }

  fun downloadVideo(launcher: ManagedActivityResultLauncher<Intent,ActivityResult>, video: Video) {
    launcher.launch(
      Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "video/mp4"
        putExtra(Intent.EXTRA_TITLE, video.displayName)
      }
    )
  }
}