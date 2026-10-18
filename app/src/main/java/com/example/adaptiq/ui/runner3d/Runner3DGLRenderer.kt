package com.example.adaptiq.ui.runner3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.example.adaptiq.ui.screens.RunnerActionState
import com.example.adaptiq.ui.screens.RunnerCollisionEngine
import com.example.adaptiq.ui.screens.RunnerCollisionResult
import com.example.adaptiq.ui.screens.RunnerObstacle
import com.example.adaptiq.ui.screens.RunnerObstacleType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlin.random.Random

/**
 * High-Performance OpenGL ES 2.0 Real-Time 3D Renderer for Adapt-IQ Temple Explorer 3D.
 * Delivers an authentic Temple Run–style endless running experience:
 * - True 3D perspective camera following the runner from behind and slightly above
 * - Real 3D world coordinates (X=lateral lanes, Y=vertical elevation, Z=forward track depth)
 * - Directional sunlight, ambient jungle lighting, specular highlights, and atmospheric depth fog
 * - Infinite procedural 3D stone highway with carved lane dividers and glowing runes
 * - Ancient stepped Mayan pyramids, towering stone steles with ivy, and rainforest canopies
 * - Fully articulated 3D Explorer Runner character mesh with running gait, leaping jump, and crouching slide
 * - 3D obstacles (Fallen Mossy Log, Ruined Stone Archway, Giant Carved Idol Monolith)
 * - Deterministic collision detection matching RunnerCollisionEngine with zero false alarms
 */
class Runner3DGLRenderer(
    private val onEventCollision: (obstacle: RunnerObstacle) -> Unit,
    private val onEventAvoided: (obstacle: RunnerObstacle, latencyMs: Long) -> Unit,
    private val onLaneChanged: (newLane: Int) -> Unit
) : GLSurfaceView.Renderer {

    // Gameplay State
    var isRunning = false
    var isPaused = false
    var isPractice = true

    // Player 3D Spatial State
    var targetLane = 1 // 0=Left (-1.8m), 1=Center (0.0m), 2=Right (+1.8m)
    var currentLaneX = 0f
    var playerY = 0f
    var playerActionState = RunnerActionState.RUNNING
    var isJumping = false
    var jumpProgress = 0f
    var isSliding = false
    var slideProgress = 0f
    var runCycle = 0f
    var collisionFlash = false

    // World & Camera
    private var worldScrollZ = 0f
    private val runningSpeed = 22.0f // meters per second
    private var lastFrameTimeNanos = 0L

    // Active 3D Obstacles
    val activeObstacles = mutableListOf<RunnerObstacle>()
    private var obstacleIdCounter = 1
    private var lastObstacleSpawnTime = 0L

    // Stats
    var score = 0
    var metersRun = 0f
    var obstaclesEncountered = 0
    var obstaclesAvoided = 0
    var obstaclesCollided = 0

    // Matrix buffers
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // Shader Handles
    private var programHandle = 0
    private var uMVPMatrixHandle = 0
    private var uModelMatrixHandle = 0
    private var uLightPosHandle = 0
    private var uCameraPosHandle = 0
    private var uFogColorHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0
    private var aColorHandle = 0

    // Lighting and Fog Constants
    private val lightPos = floatArrayOf(25.0f, 40.0f, -20.0f) // Warm high sun
    private val fogColor = floatArrayOf(0.08f, 0.12f, 0.10f, 1.0f) // Tropical jungle canopy haze

    // 3D Geometry Meshes
    private var roadChunkMesh: GLMesh? = null
    private var curbMesh: GLMesh? = null
    private var runeLineMesh: GLMesh? = null
    private var pillarMesh: GLMesh? = null
    private var pyramidMesh: GLMesh? = null
    private var treeTrunkMesh: GLMesh? = null
    private var treeFoliageMesh: GLMesh? = null

    // Character Meshes
    private var charTorsoMesh: GLMesh? = null
    private var charBackpackMesh: GLMesh? = null
    private var charBedrollMesh: GLMesh? = null
    private var charHeadMesh: GLMesh? = null
    private var charLimbMesh: GLMesh? = null
    private var charBootMesh: GLMesh? = null
    private var charShadowMesh: GLMesh? = null

    // Obstacle Meshes
    private var logObstacleMesh: GLMesh? = null
    private var archPillarMesh: GLMesh? = null
    private var archBeamMesh: GLMesh? = null
    private var totemObstacleMesh: GLMesh? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        initShaders()
        buildMeshes()
        lastFrameTimeNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1f, height.toFloat())
        // True 3D Perspective Projection (56 degree FOV)
        Matrix.perspectiveM(projectionMatrix, 0, 56.0f, aspect, 0.4f, 160.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val deltaSeconds = if (lastFrameTimeNanos > 0L) {
            ((now - lastFrameTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        } else {
            0.016f
        }
        lastFrameTimeNanos = now

        updateSimulation(deltaSeconds)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programHandle)

        // Set Light & Fog Uniforms
        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform4fv(uFogColorHandle, 1, fogColor, 0)

        // Setup 3D Third-Person Chase Camera
        val camX = currentLaneX * 0.35f
        val camY = 3.2f
        val camZ = -5.0f

        val lookX = currentLaneX * 0.70f
        val lookY = 1.6f
        val lookZ = 12.0f

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        GLES20.glUniform3f(uCameraPosHandle, camX, camY, camZ)

        // 1. Draw Infinite 3D Stone Highway Track Chunks
        renderTrack(worldScrollZ)

        // 2. Draw Flanking Jungle Ruins (Stepped Pyramids, Stone Pillars, Trees)
        renderEnvironment(worldScrollZ)

        // 3. Draw 3D Approaching Obstacles
        renderObstacles()

        // 4. Draw 3D Articulated Adventurer Runner Character
        renderCharacter()
    }

    private fun updateSimulation(dt: Float) {
        if (!isRunning || isPaused) return

        // Forward Progression
        worldScrollZ = (worldScrollZ + runningSpeed * dt) % 16.0f
        metersRun += runningSpeed * dt
        score += (runningSpeed * dt * 2.5f).toInt()
        runCycle += dt * 14.0f

        // Smooth Lateral Lane Movement
        val targetX = (targetLane - 1) * 1.8f
        currentLaneX += (targetX - currentLaneX) * (14.0f * dt).coerceAtMost(1.0f)

        // Jump & Slide State Updates
        if (isJumping) {
            jumpProgress += dt * 1.45f
            if (jumpProgress >= 1.0f) {
                isJumping = false
                jumpProgress = 0f
                playerY = 0f
                playerActionState = RunnerActionState.RUNNING
            } else {
                playerY = sin(jumpProgress * Math.PI.toFloat()) * 1.45f
                playerActionState = RunnerActionState.JUMPING
            }
        } else if (isSliding) {
            slideProgress += dt * 1.55f
            if (slideProgress >= 1.0f) {
                isSliding = false
                slideProgress = 0f
                playerY = 0f
                playerActionState = RunnerActionState.RUNNING
            } else {
                playerY = 0.15f
                playerActionState = RunnerActionState.SLIDING
            }
        } else {
            playerY = abs(sin(runCycle)) * 0.08f // Vertical stride bobbing
            playerActionState = RunnerActionState.RUNNING
        }

        // Spawn New 3D Obstacles
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastObstacleSpawnTime > 2100L) {
            lastObstacleSpawnTime = elapsed
            val lane = Random.nextInt(0, 3)
            val type = when (Random.nextInt(0, 3)) {
                0 -> RunnerObstacleType.JUMP_HURDLE
                1 -> RunnerObstacleType.SLIDE_ARCH
                else -> RunnerObstacleType.DODGE_ROADBLOCK
            }
            activeObstacles.add(
                RunnerObstacle(
                    obstacleId = obstacleIdCounter++,
                    lane = lane,
                    type = type,
                    depth = 0.0f,
                    onsetElapsedRealtime = elapsed
                )
            )
        }

        // Advance & Evaluate Obstacles along Z axis
        val spawnDistance = 65.0f
        val iterator = activeObstacles.listIterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            // Forward movement along Z
            val approachSpeed = 0.016f + (runningSpeed / 1200f)
            obs.depth += approachSpeed

            // Collision Evaluation
            if (obs.depth >= RunnerCollisionEngine.COLLISION_ZONE_START && !obs.evaluated) {
                obs.evaluated = true
                obstaclesEncountered++

                // Map lateral position to display lane (0.0 to 2.0)
                val displayLane = (currentLaneX / 1.8f) + 1.0f
                val eval = RunnerCollisionEngine.evaluate(displayLane, playerActionState, obs)

                if (eval.hasCollided) {
                    obs.hasCollided = true
                    obstaclesCollided++
                    collisionFlash = true
                    onEventCollision(obs)
                } else {
                    obs.isAvoided = true
                    obstaclesAvoided++
                    score += 50
                    val latency = SystemClock.elapsedRealtime() - obs.onsetElapsedRealtime
                    onEventAvoided(obs, latency)
                }
            } else if (obs.depth > 1.20f) {
                iterator.remove()
            }
        }
    }

    // Input Gestures
    fun swipeLeft() {
        if (targetLane > 0) {
            targetLane--
            onLaneChanged(targetLane)
        }
    }

    fun swipeRight() {
        if (targetLane < 2) {
            targetLane++
            onLaneChanged(targetLane)
        }
    }

    fun jump() {
        if (!isJumping && !isSliding) {
            isJumping = true
            jumpProgress = 0f
            playerActionState = RunnerActionState.JUMPING
        }
    }

    fun slide() {
        if (!isSliding && !isJumping) {
            isSliding = true
            slideProgress = 0f
            playerActionState = RunnerActionState.SLIDING
        }
    }

    fun resetTrial() {
        activeObstacles.clear()
        targetLane = 1
        currentLaneX = 0f
        playerY = 0f
        isJumping = false
        jumpProgress = 0f
        isSliding = false
        slideProgress = 0f
        score = 0
        metersRun = 0f
        obstaclesEncountered = 0
        obstaclesAvoided = 0
        obstaclesCollided = 0
        collisionFlash = false
    }

    // =========================================================================
    // 3D RENDERING PIPELINE
    // =========================================================================

    private fun renderTrack(scrollZ: Float) {
        val chunkLength = 16.0f
        val numChunks = 6

        for (i in -1 until numChunks) {
            val zPos = i * chunkLength - scrollZ

            // Stone Highway Slab
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0f, 0f, zPos + chunkLength / 2f)
            drawMesh(roadChunkMesh, modelMatrix)

            // Left & Right Raised Curbstones
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, -2.85f, 0.12f, zPos + chunkLength / 2f)
            drawMesh(curbMesh, modelMatrix)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 2.85f, 0.12f, zPos + chunkLength / 2f)
            drawMesh(curbMesh, modelMatrix)

            // Lane Divider Glow Lines
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, -0.90f, 0.02f, zPos + chunkLength / 2f)
            drawMesh(runeLineMesh, modelMatrix)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.90f, 0.02f, zPos + chunkLength / 2f)
            drawMesh(runeLineMesh, modelMatrix)
        }
    }

    private fun renderEnvironment(scrollZ: Float) {
        val spacing = 12.0f
        val numDecor = 7

        for (i in -1 until numDecor) {
            val zPos = i * spacing - (scrollZ * 0.75f)

            // Left & Right Ancient Stone Pillars with Ivy
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, -3.8f, 1.8f, zPos)
            drawMesh(pillarMesh, modelMatrix)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 3.8f, 1.8f, zPos + spacing * 0.5f)
            drawMesh(pillarMesh, modelMatrix)

            // Distant Stepped Pyramids
            if (i % 2 == 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, -18.0f, 4.5f, zPos + 10f)
                drawMesh(pyramidMesh, modelMatrix)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, 19.0f, 4.5f, zPos + 18f)
                drawMesh(pyramidMesh, modelMatrix)
            }

            // Rainforest Jungle Trees
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, -7.5f, 3.5f, zPos + 3f)
            drawMesh(treeTrunkMesh, modelMatrix)
            Matrix.translateM(modelMatrix, 0, 0f, 4.2f, 0f)
            drawMesh(treeFoliageMesh, modelMatrix)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 7.8f, 3.5f, zPos + 8f)
            drawMesh(treeTrunkMesh, modelMatrix)
            Matrix.translateM(modelMatrix, 0, 0f, 4.2f, 0f)
            drawMesh(treeFoliageMesh, modelMatrix)
        }
    }

    private fun renderObstacles() {
        val spawnDistance = 65.0f

        for (obs in activeObstacles) {
            // Map normalized depth (0.0 to 1.0) to 3D world Z coordinate
            val obsZ = spawnDistance * (1.0f - obs.depth)
            val obsX = (obs.lane - 1) * 1.8f

            when (obs.type) {
                RunnerObstacleType.JUMP_HURDLE -> {
                    // Fallen Mossy Tree Trunk Log (Low hurdle: Jump over!)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obsX, 0.35f, obsZ)
                    drawMesh(logObstacleMesh, modelMatrix)
                }
                RunnerObstacleType.SLIDE_ARCH -> {
                    // Ancient Ruined Stone Archway (Overhead beam: Slide under!)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obsX - 0.85f, 1.4f, obsZ)
                    drawMesh(archPillarMesh, modelMatrix)

                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obsX + 0.85f, 1.4f, obsZ)
                    drawMesh(archPillarMesh, modelMatrix)

                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obsX, 2.3f, obsZ)
                    drawMesh(archBeamMesh, modelMatrix)
                }
                RunnerObstacleType.DODGE_ROADBLOCK -> {
                    // Giant Carved Totem Idol Monolith (Dodge lane!)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obsX, 1.45f, obsZ)
                    drawMesh(totemObstacleMesh, modelMatrix)
                }
            }
        }
    }

    private fun renderCharacter() {
        val charZ = 0.0f
        val charX = currentLaneX
        val charY = playerY + 0.95f

        // Lateral Banking Roll Angle during lane transitions
        val bankingAngle = (targetLane - 1 - (currentLaneX / 1.8f)) * -14.0f

        // 1. Dynamic 3D Decoupled Ground Shadow
        val shadowScale = if (isJumping) (1.0f - (playerY / 1.8f) * 0.5f).coerceAtLeast(0.4f) else if (isSliding) 1.35f else 1.0f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, charX, 0.02f, charZ)
        Matrix.scaleM(modelMatrix, 0, shadowScale, 1.0f, shadowScale)
        drawMesh(charShadowMesh, modelMatrix)

        // 2. Character Mesh Hierarchy
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, charX, charY, charZ)
        Matrix.rotateM(modelMatrix, 0, bankingAngle, 0f, 0f, 1f)

        if (isSliding) {
            // Low athletic sliding duck
            Matrix.rotateM(modelMatrix, 0, -68f, 1f, 0f, 0f)
            Matrix.translateM(modelMatrix, 0, 0f, -0.4f, 0f)
        }

        // Torso & Weathered Leather Vest
        drawMesh(charTorsoMesh, modelMatrix)

        // Mounted Expedition Backpack (Facing chase camera on -Z side!)
        Matrix.translateM(modelMatrix, 0, 0f, 0.05f, -0.25f)
        drawMesh(charBackpackMesh, modelMatrix)

        // Lashed Bedroll on Top of Backpack
        Matrix.translateM(modelMatrix, 0, 0f, 0.28f, 0f)
        drawMesh(charBedrollMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.33f, 0.25f) // Reset backpack offset

        // Head with Crimson Bandana
        Matrix.translateM(modelMatrix, 0, 0f, 0.55f, 0f)
        drawMesh(charHeadMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.55f, 0f) // Reset head offset

        // Stride Articulation: Swinging Arms & Legs
        val legSwing = if (isJumping) 0f else sin(runCycle) * 35.0f
        val armSwing = if (isJumping) 0f else -sin(runCycle) * 40.0f

        // Left Leg
        Matrix.translateM(modelMatrix, 0, -0.22f, -0.45f, 0f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) -45f else legSwing, 1f, 0f, 0f)
        drawMesh(charLimbMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0.08f)
        drawMesh(charBootMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, 0.42f, -0.08f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) 45f else -legSwing, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, 0.22f, 0.45f, 0f)

        // Right Leg
        Matrix.translateM(modelMatrix, 0, 0.22f, -0.45f, 0f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) -45f else -legSwing, 1f, 0f, 0f)
        drawMesh(charLimbMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0.08f)
        drawMesh(charBootMesh, modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, 0.42f, -0.08f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) 45f else legSwing, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, -0.22f, 0.45f, 0f)

        // Left Arm
        Matrix.translateM(modelMatrix, 0, -0.40f, 0.15f, 0f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) 55f else armSwing, 1f, 0f, 0f)
        drawMesh(charLimbMesh, modelMatrix)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) -55f else -armSwing, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, 0.40f, -0.15f, 0f)

        // Right Arm
        Matrix.translateM(modelMatrix, 0, 0.40f, 0.15f, 0f)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) 55f else -armSwing, 1f, 0f, 0f)
        drawMesh(charLimbMesh, modelMatrix)
        Matrix.rotateM(modelMatrix, 0, if (isJumping) -55f else armSwing, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, -0.40f, -0.15f, 0f)
    }

    private fun drawMesh(mesh: GLMesh?, modelMat: FloatArray) {
        if (mesh == null) return

        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMat, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMat, 0)
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, tempMatrix, 0)

        mesh.draw(aPositionHandle, aNormalHandle, aColorHandle)
    }

    // =========================================================================
    // SHADERS & GEOMETRY GENERATION
    // =========================================================================

    private fun initShaders() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uLightPos;
            uniform vec3 uCameraPos;
            
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            
            varying vec4 vColor;
            varying float vFogFactor;
            
            void main() {
                vec4 worldPos = uModelMatrix * aPosition;
                gl_Position = uMVPMatrix * aPosition;
                
                // Directional Sunlight + Ambient + Specular
                vec3 normal = normalize(mat3(uModelMatrix) * aNormal);
                vec3 lightDir = normalize(uLightPos - worldPos.xyz);
                float diff = max(dot(normal, lightDir), 0.0);
                
                vec3 ambient = vec3(0.35, 0.32, 0.28);
                vec3 diffuse = vec3(0.95, 0.88, 0.75) * diff * 0.85;
                
                vec3 viewDir = normalize(uCameraPos - worldPos.xyz);
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 12.0);
                vec3 specular = vec3(0.35, 0.30, 0.18) * spec;
                
                vec3 litColor = aColor.rgb * (ambient + diffuse) + specular;
                vColor = vec4(litColor, aColor.a);
                
                // Exponential Atmospheric Depth Fog
                float dist = length(worldPos.xyz - uCameraPos);
                float fogDensity = 0.015;
                vFogFactor = clamp(exp(-fogDensity * dist), 0.0, 1.0);
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec4 vColor;
            varying float vFogFactor;
            uniform vec4 uFogColor;
            
            void main() {
                // Blend with distance fog for convincing atmospheric jungle horizon
                gl_FragColor = mix(uFogColor, vColor, vFogFactor);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programHandle = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uModelMatrix")
        uLightPosHandle = GLES20.glGetUniformLocation(programHandle, "uLightPos")
        uCameraPosHandle = GLES20.glGetUniformLocation(programHandle, "uCameraPos")
        uFogColorHandle = GLES20.glGetUniformLocation(programHandle, "uFogColor")

        aPositionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(programHandle, "aNormal")
        aColorHandle = GLES20.glGetAttribLocation(programHandle, "aColor")
    }

    private fun buildMeshes() {
        // Highway Stone Slab (5.4m wide x 16m long)
        roadChunkMesh = MeshBuilder.createBox(
            width = 5.4f, height = 0.15f, depth = 16.0f,
            r = 0.22f, g = 0.20f, b = 0.18f, a = 1.0f
        )

        // Raised Mossy Stone Curbstone (0.4m wide x 0.25m high x 16m long)
        curbMesh = MeshBuilder.createBox(
            width = 0.45f, height = 0.28f, depth = 16.0f,
            r = 0.18f, g = 0.28f, b = 0.18f, a = 1.0f
        )

        // Carved Amber Rune Lane Divider Strip (0.08m wide x 0.02m high x 16m long)
        runeLineMesh = MeshBuilder.createBox(
            width = 0.08f, height = 0.02f, depth = 16.0f,
            r = 0.95f, g = 0.65f, b = 0.15f, a = 0.9f
        )

        // Ancient Carved Stone Pillar (0.8m x 3.6m x 0.8m)
        pillarMesh = MeshBuilder.createBox(
            width = 0.85f, height = 3.6f, depth = 0.85f,
            r = 0.32f, g = 0.34f, b = 0.32f, a = 1.0f
        )

        // Mayan Stepped Pyramid Tier (14m x 9m x 14m)
        pyramidMesh = MeshBuilder.createPyramidTier(
            baseWidth = 14.0f, height = 9.0f,
            r = 0.26f, g = 0.24f, b = 0.22f, a = 1.0f
        )

        // Rainforest Tree Trunk & Canopy
        treeTrunkMesh = MeshBuilder.createCylinder(
            radius = 0.65f, height = 7.0f, segments = 10,
            r = 0.28f, g = 0.18f, b = 0.12f, a = 1.0f
        )
        treeFoliageMesh = MeshBuilder.createCone(
            radius = 3.2f, height = 5.5f, segments = 10,
            r = 0.12f, g = 0.38f, b = 0.14f, a = 1.0f
        )

        // Explorer Adventurer Character Components
        charTorsoMesh = MeshBuilder.createBox(
            width = 0.62f, height = 0.70f, depth = 0.34f,
            r = 0.48f, g = 0.28f, b = 0.12f, a = 1.0f // Leather vest brown
        )
        charBackpackMesh = MeshBuilder.createBox(
            width = 0.46f, height = 0.54f, depth = 0.24f,
            r = 0.26f, g = 0.16f, b = 0.10f, a = 1.0f // Heavy canvas rucksack
        )
        charBedrollMesh = MeshBuilder.createCylinder(
            radius = 0.12f, height = 0.58f, segments = 8,
            r = 0.15f, g = 0.32f, b = 0.15f, a = 1.0f // Green rolled bedroll
        )
        charHeadMesh = MeshBuilder.createBox(
            width = 0.34f, height = 0.36f, depth = 0.34f,
            r = 0.85f, g = 0.15f, b = 0.15f, a = 1.0f // Crimson adventurer bandana
        )
        charLimbMesh = MeshBuilder.createCylinder(
            radius = 0.11f, height = 0.52f, segments = 8,
            r = 0.25f, g = 0.30f, b = 0.22f, a = 1.0f // Olive safari cargo pants
        )
        charBootMesh = MeshBuilder.createBox(
            width = 0.20f, height = 0.16f, depth = 0.30f,
            r = 0.15f, g = 0.10f, b = 0.08f, a = 1.0f // Dark trekking boots
        )
        charShadowMesh = MeshBuilder.createCylinder(
            radius = 0.65f, height = 0.02f, segments = 12,
            r = 0.0f, g = 0.0f, b = 0.0f, a = 0.55f // Ground shadow
        )

        // Obstacles
        logObstacleMesh = MeshBuilder.createCylinder(
            radius = 0.32f, height = 1.70f, segments = 10,
            r = 0.34f, g = 0.20f, b = 0.12f, a = 1.0f // Fallen tree log
        )
        archPillarMesh = MeshBuilder.createBox(
            width = 0.36f, height = 2.6f, depth = 0.36f,
            r = 0.36f, g = 0.38f, b = 0.36f, a = 1.0f // Ruined stone arch pillars
        )
        archBeamMesh = MeshBuilder.createBox(
            width = 2.10f, height = 0.44f, depth = 0.44f,
            r = 0.42f, g = 0.44f, b = 0.40f, a = 1.0f // Overhead arch beam
        )
        totemObstacleMesh = MeshBuilder.createBox(
            width = 1.50f, height = 2.80f, depth = 1.10f,
            r = 0.30f, g = 0.32f, b = 0.35f, a = 1.0f // Carved totem idol monolith
        )
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}

/**
 * Procedural 3D Mesh Representation
 */
class GLMesh(
    private val vertexBuffer: FloatBuffer,
    private val indexBuffer: ShortBuffer,
    private val indexCount: Int
) {
    fun draw(posHandle: Int, normalHandle: Int, colorHandle: Int) {
        val stride = 10 * 4 // 10 floats (pos:3, norm:3, col:4) * 4 bytes

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posHandle)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(normalHandle)

        vertexBuffer.position(6)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(colorHandle)

        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
}

/**
 * Geometric 3D Mesh Generator for OpenGL ES Vertex/Index Buffers
 */
object MeshBuilder {

    fun createBox(width: Float, height: Float, depth: Float, r: Float, g: Float, b: Float, a: Float): GLMesh {
        val w = width / 2f
        val h = height / 2f
        val d = depth / 2f

        // 6 Faces x 4 Vertices = 24 Vertices (pos:3, norm:3, col:4)
        val vertices = floatArrayOf(
            // Front (+Z)
            -w, -h,  d,  0f, 0f, 1f,  r, g, b, a,
             w, -h,  d,  0f, 0f, 1f,  r, g, b, a,
             w,  h,  d,  0f, 0f, 1f,  r, g, b, a,
            -w,  h,  d,  0f, 0f, 1f,  r, g, b, a,
            // Back (-Z)
             w, -h, -d,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
            -w, -h, -d,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
            -w,  h, -d,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
             w,  h, -d,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
            // Top (+Y)
            -w,  h,  d,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
             w,  h,  d,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
             w,  h, -d,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
            -w,  h, -d,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
            // Bottom (-Y)
            -w, -h, -d,  0f, -1f, 0f, r * 0.70f, g * 0.70f, b * 0.70f, a,
             w, -h, -d,  0f, -1f, 0f, r * 0.70f, g * 0.70f, b * 0.70f, a,
             w, -h,  d,  0f, -1f, 0f, r * 0.70f, g * 0.70f, b * 0.70f, a,
            -w, -h,  d,  0f, -1f, 0f, r * 0.70f, g * 0.70f, b * 0.70f, a,
            // Left (-X)
            -w, -h, -d, -1f, 0f, 0f,  r * 0.80f, g * 0.80f, b * 0.80f, a,
            -w, -h,  d, -1f, 0f, 0f,  r * 0.80f, g * 0.80f, b * 0.80f, a,
            -w,  h,  d, -1f, 0f, 0f,  r * 0.80f, g * 0.80f, b * 0.80f, a,
            -w,  h, -d, -1f, 0f, 0f,  r * 0.80f, g * 0.80f, b * 0.80f, a,
            // Right (+X)
             w, -h,  d,  1f, 0f, 0f,  r * 0.90f, g * 0.90f, b * 0.90f, a,
             w, -h, -d,  1f, 0f, 0f,  r * 0.90f, g * 0.90f, b * 0.90f, a,
             w,  h, -d,  1f, 0f, 0f,  r * 0.90f, g * 0.90f, b * 0.90f, a,
             w,  h,  d,  1f, 0f, 0f,  r * 0.90f, g * 0.90f, b * 0.90f, a
        )

        val indices = shortArrayOf(
            0, 1, 2,  0, 2, 3,    // Front
            4, 5, 6,  4, 6, 7,    // Back
            8, 9, 10, 8, 10, 11,  // Top
            12, 13, 14, 12, 14, 15, // Bottom
            16, 17, 18, 16, 18, 19, // Left
            20, 21, 22, 20, 22, 23  // Right
        )

        return createMesh(vertices, indices)
    }

    fun createCylinder(radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float, a: Float): GLMesh {
        val h = height / 2f
        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        // Side vertices
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            val x = cosA * radius
            val z = sinA * radius

            // Bottom vertex
            vertexList.addAll(listOf(x, -h, z, cosA, 0f, sinA, r, g, b, a))
            // Top vertex
            vertexList.addAll(listOf(x, h, z, cosA, 0f, sinA, r * 1.1f, g * 1.1f, b * 1.1f, a))
        }

        // Side indices
        for (i in 0 until segments) {
            val idx = (i * 2).toShort()
            val nextIdx = ((i + 1) * 2).toShort()

            indexList.addAll(listOf(idx, (idx + 1).toShort(), nextIdx))
            indexList.addAll(listOf(nextIdx, (idx + 1).toShort(), (nextIdx + 1).toShort()))
        }

        return createMesh(vertexList.toFloatArray(), indexList.toShortArray())
    }

    fun createCone(radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float, a: Float): GLMesh {
        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        // Apex vertex
        vertexList.addAll(listOf(0f, height, 0f, 0f, 1f, 0f, r * 1.2f, g * 1.2f, b * 1.2f, a))

        // Perimeter base vertices
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)
            vertexList.addAll(listOf(cosA * radius, 0f, sinA * radius, cosA, 0.4f, sinA, r, g, b, a))
        }

        for (i in 1..segments) {
            indexList.addAll(listOf(0.toShort(), (i + 1).toShort(), i.toShort()))
        }

        return createMesh(vertexList.toFloatArray(), indexList.toShortArray())
    }

    fun createPyramidTier(baseWidth: Float, height: Float, r: Float, g: Float, b: Float, a: Float): GLMesh {
        // Multi-tiered stepped ancient stone pyramid
        val w1 = baseWidth / 2f
        val w2 = baseWidth * 0.35f
        val h = height

        val vertices = floatArrayOf(
            // Bottom face
            -w1, 0f, -w1,  0f, -1f, 0f,  r, g, b, a,
             w1, 0f, -w1,  0f, -1f, 0f,  r, g, b, a,
             w1, 0f,  w1,  0f, -1f, 0f,  r, g, b, a,
            -w1, 0f,  w1,  0f, -1f, 0f,  r, g, b, a,

            // Top plateau
            -w2, h, -w2,  0f, 1f, 0f,  r * 1.2f, g * 1.2f, b * 1.2f, a,
             w2, h, -w2,  0f, 1f, 0f,  r * 1.2f, g * 1.2f, b * 1.2f, a,
             w2, h,  w2,  0f, 1f, 0f,  r * 1.2f, g * 1.2f, b * 1.2f, a,
            -w2, h,  w2,  0f, 1f, 0f,  r * 1.2f, g * 1.2f, b * 1.2f, a
        )

        val indices = shortArrayOf(
            // Bottom
            0, 1, 2,  0, 2, 3,
            // Top
            4, 5, 6,  4, 6, 7,
            // Sloped Sides
            0, 4, 5,  0, 5, 1, // Back
            1, 5, 6,  1, 6, 2, // Right
            2, 6, 7,  2, 7, 3, // Front
            3, 7, 4,  3, 4, 0  // Left
        )

        return createMesh(vertices, indices)
    }

    private fun createMesh(vertices: FloatArray, indices: ShortArray): GLMesh {
        val vBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vBuffer.position(0)

        val iBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
        iBuffer.position(0)

        return GLMesh(vBuffer, iBuffer, indices.size)
    }
}
