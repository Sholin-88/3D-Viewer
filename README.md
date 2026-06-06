



 I chose SceneView (built on Google's Filament engine) for this project. • Why: It provides a Compose-native DSL, making it much easier to integrate 3D content into a modern Android UI than using raw OpenGL or standard Sceneform. Filament is the industry standard for high-fidelity, physically-based rendering (PBR) on mobile, ensuring the models look realistic.

Performance & Memory Optimizations

Shared Native Resources: The Filament Engine and ModelLoader are initialized once at the root. All windowed containers share these instances to avoid the massive memory overhead of multiple rendering contexts.

TextureSurface Rendering: We used SurfaceType.TextureSurface. While slightly more GPU-intensive than a SurfaceView, it allows 3D views to be treated as standard UI elements that can be clipped, scaled, and overlapped smoothly.

Async Asset Loading: Models are loaded using rememberModelInstance, which handles file I/O on Dispatchers.IO. This keeps the UI thread free and prevents "jank" when adding new models.

Gesture Isolation: By using an invisible Compose Box as a shield in Normal Mode, we prevent the 3D engine from processing unnecessary touch events, reducing CPU cycles during container movement.

Trade-offs

Performance vs. UI Flexibility: Using TextureSurface (TextureView) instead of SurfaceView was a deliberate trade-off. SurfaceView offers better raw performance but sits on a separate hardware plane, which would make the "floating window" effect and overlapping containers impossible to implement in Compose.

Independent Scenes: Each container has its own viewport state. While this allows for independent rotation/zoom per window, it increases the number of draw calls compared to a single scene containing multiple models.

Future Improvements

Z-Index Management: Implement a "Bring to Front" logic where tapping a container moves it to the top of the stack.

Draco Compression: Add support for Draco-compressed GLBs to significantly reduce memory footprint for complex meshes.

Environment Controls: Allow users to change the lighting (HDRIs) or background for each container.

Culling: Automatically pause rendering for containers that are off-screen or fully covered by others.

Known Bugs & Limitations

Compose Preview: SceneView depends on native OpenGL/EGL libraries that are not available in the Android Studio Layout Preview. A placeholder is shown instead.

Memory Limits: While optimized, loading 10+ high-poly models simultaneously may lead to thermal throttling or "Out of Memory" errors on low-end devices due to texture pressure.

Format Support: Currently restricted to .glb files to ensure stability. No support for .obj or .fbx without external conversion.
