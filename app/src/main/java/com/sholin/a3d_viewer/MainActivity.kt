package com.sholin.a3d_viewer

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import com.sholin.a3d_viewer.ui.theme._3DViewerTheme
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.roundToInt

data class ModelItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val offset: Offset = Offset(200f, 400f),
    val size: Dp = 250.dp,
    val isInteracting: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _3DViewerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ThreeDView(innerPadding)
                }
            }
        }
    }
}

@Composable
fun ThreeDView(innerPaddingValues: PaddingValues) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val modelItems = remember { mutableStateListOf<ModelItem>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            val isGlb = uri.toString().lowercase().endsWith(".glb")

            if (isGlb) {
                modelItems.add(ModelItem(uri = uri))
            } else {
                Toast.makeText(
                    context,
                    "Invalid format: Only .glb files are supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPaddingValues)
    ) {
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("SceneView Placeholder", color = Color.White)
            }
        } else {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)

            // Background / Base Layer
            Column {
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Select 3D Models")
                }
            }

            // Render all model containers
            modelItems.forEach { item ->
                key(item.id) {
                    ModelContainer(
                        item = item,
                        engine = engine,
                        modelLoader = modelLoader,
                        onUpdate = { updatedItem ->
                            val index = modelItems.indexOfFirst { it.id == item.id }
                            if (index != -1) modelItems[index] = updatedItem
                        },
                        onRemove = {
                            modelItems.removeAll { it.id == item.id }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelContainer(
    item: ModelItem,
    engine: Engine,
    modelLoader: ModelLoader,
    onUpdate: (ModelItem) -> Unit,
    onRemove: () -> Unit
) {
    val currentItem by androidx.compose.runtime.rememberUpdatedState(item)
    val currentOnUpdate by androidx.compose.runtime.rememberUpdatedState(onUpdate)

    val cameraManipulator = rememberCameraManipulator()
    val gestureListener = rememberOnGestureListener()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    currentItem.offset.x.roundToInt(),
                    currentItem.offset.y.roundToInt()
                )
            }
            .size(currentItem.size)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (currentItem.isInteracting) Color.Green else Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // SceneView
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            surfaceType = SurfaceType.TextureSurface,
            cameraManipulator = if (currentItem.isInteracting) cameraManipulator else null,
            onGestureListener = if (currentItem.isInteracting) gestureListener else null
        ) {
            rememberModelInstance(
                modelLoader = modelLoader,
                fileLocation = currentItem.uri.toString()
            )?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.5f,
                    centerOrigin = Position(x = 0f, y = 0f, z = 0f)
                )
            }
        }

        // Blocking overlay & Gesture handler for Normal Mode
        if (!currentItem.isInteracting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Using a nearly invisible color to ensure hit-testing works perfectly
                    .background(Color.White.copy(alpha = 0.001f))
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newSize = (currentItem.size * zoom).coerceIn(150.dp, 600.dp)
                            currentOnUpdate(
                                currentItem.copy(
                                    offset = currentItem.offset + pan,
                                    size = newSize
                                )
                            )
                        }
                    }
            )
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onUpdate(item.copy(isInteracting = !item.isInteracting)) },
                modifier = Modifier.padding(2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (item.isInteracting) "Done" else "Interact",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                )
            }
            Button(
                onClick = onRemove,
                modifier = Modifier.padding(2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Close", style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    _3DViewerTheme {
        ThreeDView(innerPaddingValues = PaddingValues())
    }
}