package webrtc.sample.compose.ui.screens.door

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

data class DoorHistoryEntry(
  val state: String,
  val user: String?,
  val timestamp: String
)

@Composable
fun DoorHistoryScreen(
  accessToken: String,
  onBack: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var history by remember { mutableStateOf<List<DoorHistoryEntry>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var state by remember { mutableStateOf("") }
  var startDate by remember { mutableStateOf("") }
  var endDate by remember { mutableStateOf("") }
  var showStateDropdown by remember { mutableStateOf(false) }
  val calendar = Calendar.getInstance()

  // Date picker dialogs
  val context = androidx.compose.ui.platform.LocalContext.current
  val startDatePicker = DatePickerDialog(
    context,
    { _, year, month, day ->
      calendar.set(year, month, day)
      startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    },
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
  )
  val endDatePicker = DatePickerDialog(
    context,
    { _, year, month, day ->
      calendar.set(year, month, day)
      endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    },
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
  )

  fun performGetDoorHistory() {
    isLoading = true
    errorMessage = ""

    val client = OkHttpClient()
    val urlBuilder = StringBuilder("https://thientranduc.id.vn:444/api/door-history?")
    if (state.isNotEmpty()) urlBuilder.append("state=$state&")
    if (startDate.isNotEmpty()) urlBuilder.append("startDate=$startDate&")
    if (endDate.isNotEmpty()) urlBuilder.append("endDate=$endDate&")
    urlBuilder.append("limit=50&page=1")
    Log.d("DoorHistory", urlBuilder.toString())
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(urlBuilder.toString())
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("DoorHistory Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val historyArray = json.getJSONArray("history")
            val historyList = mutableListOf<DoorHistoryEntry>()
            for (i in 0 until historyArray.length()) {
              val entryJson = historyArray.getJSONObject(i)
              historyList.add(
                DoorHistoryEntry(
                  state = entryJson.getString("state"),
                  user = entryJson.optString("user", "No information"),
                  timestamp = entryJson.getString("time")
                )
              )
            }
            CoroutineScope(Dispatchers.Main).launch {
              history = historyList
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to fetch door history")
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            errorMessage = json.optString("message", "Server error")
            isLoading = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          errorMessage = "Network error: ${e.message}"
          isLoading = false
        }
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Door History", fontSize = 20.sp) },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    bottomBar = {
      Button(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 32.dp).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = MaterialTheme.colors.primary,
          contentColor = MaterialTheme.colors.onPrimary
        )
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = "Back", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // State dropdown and Fetch button column
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showStateDropdown = true }
            ) {
              OutlinedTextField(
                value = state.ifEmpty { "Select State" },
                onValueChange = {},
                label = { Text("State", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = MaterialTheme.typography.body2.copy(fontSize = 14.sp),
                trailingIcon = {
                  Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable { showStateDropdown = true }
                  )
                }
              )
              DropdownMenu(
                expanded = showStateDropdown,
                onDismissRequest = { showStateDropdown = false },
                modifier = Modifier.fillMaxWidth()
              ) {
                DropdownMenuItem(onClick = {
                  state = ""
                  showStateDropdown = false
                }) {
                  Text("All", fontSize = 14.sp)
                }
                DropdownMenuItem(onClick = {
                  state = "Locked"
                  showStateDropdown = false
                }) {
                  Text("Locked", fontSize = 14.sp)
                }
                DropdownMenuItem(onClick = {
                  state = "Unlocked"
                  showStateDropdown = false
                }) {
                  Text("Unlocked", fontSize = 14.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Button(
              onClick = { performGetDoorHistory() },
              modifier = Modifier.fillMaxWidth().height(56.dp),
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
              ),
              enabled = !isLoading
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = MaterialTheme.colors.onSecondary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
              } else {
                Text("Fetch History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Date pickers column
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Start Date picker
            Box {
              OutlinedTextField(
                value = startDate,
                onValueChange = {},
                label = { Text("Start Date", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = MaterialTheme.typography.body2.copy(fontSize = 14.sp)
              )
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .clickable {
                    Log.d("DoorHistory", "Start date field clicked")
                    startDatePicker.show()
                  }
              )
            }

            // End Date picker
            Box {
              OutlinedTextField(
                value = endDate,
                onValueChange = {},
                label = { Text("End Date", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = MaterialTheme.typography.body2.copy(fontSize = 14.sp)
              )
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .clickable {
                    Log.d("DoorHistory", "End date field clicked")
                    endDatePicker.show()
                  }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // History list
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colors.secondary,
            strokeWidth = 2.dp
          )
          Text(
            text = "Fetching history...",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
          )
        } else if (history.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
          ) {
            items(history) { entry ->
              Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                  Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = if (entry.state == "Locked") MaterialTheme.colors.primaryVariant else MaterialTheme.colors.error,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                      text = "State: ${entry.state}",
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                  Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Person,
                      contentDescription = "User",
                      modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                    Text(
                      text = entry.user ?: "N/A",
                      fontSize = 16.sp
                    )
                  }
                  Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Filled.AccessTime,
                      contentDescription = "Time",
                      modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                    Text(
                      text = SimpleDateFormat(
                        "HH:mm, dd MMM, yyyy",
                        Locale.getDefault()
                      ).format(
                        SimpleDateFormat(
                          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                          Locale.getDefault()
                        ).parse(entry.timestamp) ?: entry.timestamp
                      ),
                      fontSize = 14.sp
                    )
                  }
                }
              }
            }
          }
        } else if (errorMessage.isNotEmpty()) {
          Text(
            text = errorMessage,
            fontSize = 16.sp,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
    }
  )

  if (errorMessage.isNotEmpty()) {
    LaunchedEffect(errorMessage) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }
}