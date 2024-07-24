package uk.org.freeflight.canbellupload

import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import uk.org.freeflight.canbellupload.ui.theme.CanBellUploadTheme


class MyViewModel: ViewModel() {
    var txt by mutableStateOf("")

    fun append(data: String) {
        Log.d("FOO", data)
        txt += data
    }
}

class MainActivity : ComponentActivity(), SerialInputOutputManager.Listener {
    private lateinit var usbIoManager: SerialInputOutputManager
    private lateinit var usbSerialPort: UsbSerialPort

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            CanBellUploadTheme {
                CanBellScaffold { getData() }
            }
        }
    }

    override fun onNewData(bytes: ByteArray) {
        runOnUiThread {receive(bytes)}
    }

    override fun onRunError(e: Exception) {
        runOnUiThread {Log.d("FOO", "Run error")}
    }

    private fun receive(bytes: ByteArray) {
        val myViewModel: MyViewModel by viewModels()
        myViewModel.append(bytes.decodeToString())
    }

    private fun getData() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty())
            return

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)

        usbSerialPort = driver.ports[0]
        usbSerialPort.open(connection)
        usbSerialPort.dtr = true

        usbIoManager = SerialInputOutputManager(usbSerialPort, this)
        usbIoManager.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanBellScaffold(onClick: () -> Unit) {
    val myViewModel: MyViewModel = viewModel()
    val contents = myViewModel.txt

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(text="CAN-Bell")
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                //Button(onClick = { contents = "Goodbye"}) {
                Button(onClick = { onClick() }) {
                    Text(text ="Hello", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            Text(contents)
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6a")
@Composable
fun ScaffoldPreview() {
    CanBellUploadTheme {
        CanBellScaffold {}
    }
}