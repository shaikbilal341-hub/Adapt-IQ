package com.example.adaptiq.ui.runner3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * Procedural Texture Generator and Manager for Adapt-IQ Temple Explorer 3D.
 * Generates rich, authentic ancient temple and adventurer textures entirely in-memory:
 * - Weathered stone flagstones with ancient carved golden runes and emerald moss
 * - Aztec/Mayan bas-relief stone carvings and pillar hieroglyphs
 * - Explorer adventurer clothing:
 *   * Adventurer jacket back with shoulder yoke, leather grain, harness straps, and utility belt
 *   * Adventurer hair with realistic dark locks, neck hairline, and crimson knotted bandana
 *   * Safari cargo pants with double stitching, pocket flaps, and knee patches
 *   * Trekking boots with deep rubber lug treads and laces
 *   * Fedora hat with sable felt and dark leather hatband
 *   * Warm sun-kissed explorer skin for arms and hands
 * - Detailed obstacle materials:
 *   * Mossy fibrous tree bark with cut-end tree rings
 *   * Ruined temple stone arch lintels with glowing golden glyphs
 *   * Menacing Aztec idol totems with glowing amber gemstone eyes
 * - Collectible 3D golden coins and animated temple torch braziers
 */
class Runner3DTextureManager {

    var roadTextureId = 0
        private set
    var pillarTextureId = 0
        private set
    var jacketBackTextureId = 0
        private set
    var hairTextureId = 0
        private set
    var pantsTextureId = 0
        private set
    var bootsTextureId = 0
        private set
    var hatTextureId = 0
        private set
    var skinTextureId = 0
        private set
    var barkTextureId = 0
        private set
    var archTextureId = 0
        private set
    var totemTextureId = 0
        private set
    var coinTextureId = 0
        private set
    var fireTextureId = 0
        private set
    var whiteTextureId = 0
        private set

    fun initTextures() {
        whiteTextureId = createSolidTexture(Color.WHITE)
        roadTextureId = uploadBitmap(createRoadBitmap())
        pillarTextureId = uploadBitmap(createPillarBitmap())
        jacketBackTextureId = uploadBitmap(createJacketBackBitmap())
        hairTextureId = uploadBitmap(createHairBitmap())
        pantsTextureId = uploadBitmap(createPantsBitmap())
        bootsTextureId = uploadBitmap(createBootsBitmap())
        hatTextureId = uploadBitmap(createHatBitmap())
        skinTextureId = uploadBitmap(createSkinBitmap())
        barkTextureId = uploadBitmap(createBarkBitmap())
        archTextureId = uploadBitmap(createArchBitmap())
        totemTextureId = uploadBitmap(createTotemBitmap())
        coinTextureId = uploadBitmap(createCoinBitmap())
        fireTextureId = uploadBitmap(createFireBitmap())
    }

    private fun uploadBitmap(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val texId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return texId
    }

    private fun createSolidTexture(color: Int): Int {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(color)
        return uploadBitmap(bmp)
    }

    // 1. Weathered Stone Highway with Carved Gold Runes & Emerald Moss
    private fun createRoadBitmap(): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Ancient Sandstone Flagstone Base
        paint.color = Color.rgb(148, 136, 120)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Flagstone tile mortar grid
        val tileRows = 4
        val tileCols = 3
        val rh = size.toFloat() / tileRows
        val cw = size.toFloat() / tileCols

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.rgb(65, 58, 48)

        for (r in 0..tileRows) {
            canvas.drawLine(0f, r * rh, size.toFloat(), r * rh, paint)
        }
        for (c in 0..tileCols) {
            for (r in 0 until tileRows) {
                val offset = if (r % 2 == 1) cw * 0.5f else 0f
                val x = (c * cw + offset) % size
                canvas.drawLine(x, r * rh, x, (r + 1) * rh, paint)
            }
        }

        // Stone texture specks & aged chisel fissures
        paint.style = Paint.Style.FILL
        val rnd = java.util.Random(42)
        for (i in 0..800) {
            val sx = rnd.nextFloat() * size
            val sy = rnd.nextFloat() * size
            val sr = 1.5f + rnd.nextFloat() * 3.5f
            paint.color = if (rnd.nextBoolean()) Color.argb(55, 230, 222, 205) else Color.argb(65, 45, 40, 32)
            canvas.drawCircle(sx, sy, sr, paint)
        }

        // Emerald Moss Creepers along stone grooves
        paint.color = Color.argb(175, 42, 85, 38)
        for (i in 0..60) {
            val mx = rnd.nextFloat() * size
            val my = rnd.nextFloat() * size
            canvas.drawCircle(mx, my, 5f + rnd.nextFloat() * 12f, paint)
        }

        // Glowing Golden Inlay Rune Border Strips (Lane Margins)
        paint.color = Color.rgb(240, 185, 45)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawLine(24f, 0f, 24f, size.toFloat(), paint)
        canvas.drawLine(size - 24f, 0f, size - 24f, size.toFloat(), paint)

        // Ancient Carved Rune Sigils along borders
        paint.style = Paint.Style.FILL
        for (y in 24 until size step 48) {
            canvas.drawRect(16f, y.toFloat(), 32f, (y + 20).toFloat(), paint)
            canvas.drawRect((size - 32).toFloat(), y.toFloat(), (size - 16).toFloat(), (y + 20).toFloat(), paint)
        }

        return bmp
    }

    // 2. Mayan Pillar Bas-Relief Carving Texture
    private fun createPillarBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(108, 118, 108)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Horizontal Block Courses
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(42, 50, 42)
        for (y in 0..size step 64) {
            canvas.drawLine(0f, y.toFloat(), size.toFloat(), y.toFloat(), paint)
        }

        // Ancient Carved Tribal Glyphs
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.rgb(160, 175, 160)
        for (cy in 32 until size step 64) {
            canvas.drawRect(55f, (cy - 18).toFloat(), 201f, (cy + 18).toFloat(), paint)
            canvas.drawCircle(128f, cy.toFloat(), 12f, paint)
            canvas.drawLine(90f, cy.toFloat(), 166f, cy.toFloat(), paint)
        }

        // Hanging jungle vines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.rgb(32, 68, 30)
        val vinePath = Path()
        vinePath.moveTo(24f, 0f)
        vinePath.quadTo(38f, 65f, 20f, 130f)
        vinePath.quadTo(42f, 195f, 26f, 256f)
        canvas.drawPath(vinePath, paint)

        return bmp
    }

    // 3. Adventurer Explorer Leather Jacket (Viewed from Behind)
    private fun createJacketBackBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Rich Weathered Saddle Brown Leather with Shading Gradient
        paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), Color.rgb(135, 78, 36), Color.rgb(75, 42, 18), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        // Shoulder Yoke Seam (Across Upper Back)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(45, 24, 10)
        val yokePath = Path()
        yokePath.moveTo(0f, 50f)
        yokePath.quadTo(128f, 75f, 256f, 50f)
        canvas.drawPath(yokePath, paint)

        // Double stitching on yoke
        paint.strokeWidth = 2f
        paint.color = Color.rgb(180, 120, 60)
        canvas.drawPath(yokePath, paint)

        // Central Spine Seam
        paint.strokeWidth = 4f
        paint.color = Color.rgb(45, 24, 10)
        canvas.drawLine(128f, 65f, 128f, 200f, paint)

        // Dual Explorer Backpack Harness Straps
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(38, 22, 10) // Dark brown harness webbing
        canvas.drawRect(45f, 0f, 75f, 205f, paint)
        canvas.drawRect(181f, 0f, 211f, 205f, paint)

        // Brass buckles on harness straps
        paint.color = Color.rgb(215, 180, 65)
        canvas.drawRect(42f, 90f, 78f, 110f, paint)
        canvas.drawRect(178f, 90f, 214f, 110f, paint)

        // Bottom Heavy Leather Utility Belt with Pouches
        paint.color = Color.rgb(32, 18, 8)
        canvas.drawRect(0f, 200f, size.toFloat(), 250f, paint)

        // Leather ammo/gear pouches on belt
        paint.color = Color.rgb(55, 32, 14)
        canvas.drawRoundRect(RectF(15f, 205f, 65f, 245f), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(191f, 205f, 241f, 245f), 6f, 6f, paint)

        // Brass snap buttons on pouches
        paint.color = Color.rgb(230, 195, 70)
        canvas.drawCircle(40f, 225f, 4f, paint)
        canvas.drawCircle(216f, 225f, 4f, paint)

        return bmp
    }

    // 4. Adventurer Hair & Neck (Viewed from Behind)
    private fun createHairBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Rich Dark Brown/Black Adventurer Hair
        paint.color = Color.rgb(34, 22, 14)
        canvas.drawRect(0f, 0f, size.toFloat(), 85f, paint)

        // Hair strand highlights
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.rgb(58, 38, 24)
        for (x in 10..118 step 12) {
            canvas.drawLine(x.toFloat(), 10f, (x + 4).toFloat(), 80f, paint)
        }

        // Tanned Explorer Neckline
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(205, 155, 115)
        canvas.drawRect(25f, 85f, 103f, 105f, paint)

        // Crimson Explorer Bandana Knot at the Back of Neck
        paint.color = Color.rgb(195, 25, 25)
        canvas.drawRoundRect(RectF(15f, 100f, 113f, 128f), 6f, 6f, paint)

        // Bandana knot tails hanging down
        val knotPath = Path()
        knotPath.moveTo(64f, 105f)
        knotPath.lineTo(48f, 128f)
        knotPath.lineTo(64f, 122f)
        knotPath.lineTo(80f, 128f)
        knotPath.close()
        paint.color = Color.rgb(165, 20, 20)
        canvas.drawPath(knotPath, paint)

        // White paisley dots
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawCircle(35f, 112f, 2f, paint)
        canvas.drawCircle(93f, 112f, 2f, paint)
        canvas.drawCircle(64f, 114f, 2.5f, paint)

        return bmp
    }

    // 5. Safari Cargo Pants Texture
    private fun createPantsBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sturdy Khaki/Olive Safari Canvas
        paint.color = Color.rgb(122, 115, 88)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Fabric texture & seams
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.rgb(78, 72, 52)

        // Side seam
        canvas.drawLine(15f, 0f, 15f, size.toFloat(), paint)
        canvas.drawLine(size - 15f, 0f, size - 15f, size.toFloat(), paint)

        // Cargo Pocket Flap with Button
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(105, 98, 74)
        canvas.drawRoundRect(RectF(25f, 40f, 103f, 85f), 6f, 6f, paint)

        // Brass pocket snap
        paint.color = Color.rgb(205, 175, 65)
        canvas.drawCircle(64f, 55f, 3.5f, paint)

        // Knee Reinforcement Stitching
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(65, 60, 42)
        canvas.drawRect(25f, 95f, 103f, 125f, paint)

        return bmp
    }

    // 6. Leather Trekking Boots Texture
    private fun createBootsBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Dark Cordovan Trekking Leather
        paint.color = Color.rgb(52, 32, 18)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Deep Treaded Lug Sole at Bottom
        paint.color = Color.rgb(18, 18, 18)
        canvas.drawRect(0f, 92f, size.toFloat(), size.toFloat(), paint)

        // Sole grip lugs
        paint.color = Color.rgb(35, 35, 35)
        for (x in 10..118 step 20) {
            canvas.drawRect(x.toFloat(), 100f, (x + 12).toFloat(), 120f, paint)
        }

        // Laces and brass eyelets
        paint.color = Color.rgb(220, 180, 65)
        paint.style = Paint.Style.FILL
        for (y in 20..75 step 18) {
            canvas.drawCircle(35f, y.toFloat(), 3.5f, paint)
            canvas.drawCircle(93f, y.toFloat(), 3.5f, paint)
        }

        // Criss-cross gold laces
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.rgb(185, 140, 50)
        canvas.drawLine(35f, 20f, 93f, 38f, paint)
        canvas.drawLine(93f, 20f, 35f, 38f, paint)
        canvas.drawLine(35f, 38f, 93f, 56f, paint)
        canvas.drawLine(93f, 38f, 35f, 56f, paint)

        return bmp
    }

    // 7. Adventurer Fedora Hat Texture (Dark Sable Felt with Leather Band)
    private fun createHatBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Rich Sable Brown Felt
        paint.color = Color.rgb(72, 42, 22)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Felt texture gradient
        paint.shader = RadialGradient(64f, 64f, 64f, Color.rgb(95, 55, 30), Color.rgb(55, 30, 14), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        // Dark Leather Hatband around base
        paint.color = Color.rgb(28, 16, 8)
        canvas.drawRect(0f, 85f, size.toFloat(), 115f, paint)

        // Small Brass Hatband Buckle
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.rgb(225, 190, 70)
        canvas.drawRect(50f, 90f, 78f, 110f, paint)

        return bmp
    }

    // 8. Warm Sun-Kissed Adventurer Skin (Arms, Hands & Neck)
    private fun createSkinBitmap(): Bitmap {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Warm Tanned Adventurer Skin
        paint.color = Color.rgb(215, 168, 128)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Subtle anatomical muscle shading
        paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), Color.rgb(228, 180, 140), Color.rgb(195, 145, 105), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        return bmp
    }

    // 9. Mossy Ancient Jungle Tree Bark
    private fun createBarkBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Deep Rich Fibrous Bark
        paint.color = Color.rgb(78, 52, 32)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Longitudinal bark fissures
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(40, 24, 12)
        for (x in 16..size step 32) {
            val path = Path()
            path.moveTo(x.toFloat(), 0f)
            path.quadTo((x + 8).toFloat(), 64f, (x - 4).toFloat(), 128f)
            path.quadTo((x + 6).toFloat(), 192f, x.toFloat(), 256f)
            canvas.drawPath(path, paint)
        }

        // Emerald jungle moss veins
        paint.color = Color.argb(160, 45, 95, 38)
        paint.strokeWidth = 6f
        for (x in 32..size step 64) {
            canvas.drawLine(x.toFloat(), 0f, (x + 12).toFloat(), size.toFloat(), paint)
        }

        return bmp
    }

    // 10. Ruined Stone Archway Lintel with Glowing Golden Glyphs
    private fun createArchBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Weathered Ancient Stone
        paint.color = Color.rgb(105, 115, 108)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Stone borders
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.rgb(45, 52, 45)
        canvas.drawRect(6f, 6f, size - 6f, size - 6f, paint)

        // Glowing Ancient Golden Glyphs across lintel
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(245, 195, 45)
        for (x in 24..size - 48 step 48) {
            canvas.drawRect(x.toFloat(), 96f, (x + 32).toFloat(), 160f, paint)
            // Cutout eye in glyph
            paint.color = Color.rgb(45, 52, 45)
            canvas.drawCircle((x + 16).toFloat(), 128f, 6f, paint)
            paint.color = Color.rgb(245, 195, 45)
        }

        return bmp
    }

    // 11. Giant Aztec Totem Idol Monolith with Glowing Amber Gemstone Eyes
    private fun createTotemBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Dark Volcanic Basalt Stone
        paint.color = Color.rgb(62, 58, 54)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Feathered headdress relief
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(145, 130, 95)
        for (x in 20..size - 20 step 24) {
            val path = Path()
            path.moveTo(x.toFloat(), 60f)
            path.lineTo((x + 12).toFloat(), 15f)
            path.lineTo((x + 24).toFloat(), 60f)
            canvas.drawPath(path, paint)
        }

        // Menacing Idol Facial Relief
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.rgb(32, 28, 25)
        // Heavy brow
        canvas.drawLine(40f, 85f, 216f, 85f, paint)

        // Glowing Amber Gemstone Eyes
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 160, 20)
        canvas.drawRoundRect(RectF(55f, 95f, 105f, 125f), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(151f, 95f, 201f, 125f), 6f, 6f, paint)

        // Inner glowing slit pupils
        paint.color = Color.rgb(255, 240, 160)
        canvas.drawCircle(80f, 110f, 6f, paint)
        canvas.drawCircle(176f, 110f, 6f, paint)

        // Broad Aztec Stone Nose & Gritted Teeth Fangs
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.rgb(32, 28, 25)
        canvas.drawRect(110f, 115f, 146f, 165f, paint)

        // Open menacing stone mouth with fangs
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(25, 20, 18)
        canvas.drawRect(60f, 180f, 196f, 235f, paint)

        // Chiseled Stone Fangs
        paint.color = Color.rgb(205, 195, 180)
        for (x in 70..170 step 26) {
            val f = Path()
            f.moveTo(x.toFloat(), 180f)
            f.lineTo((x + 12).toFloat(), 180f)
            f.lineTo((x + 6).toFloat(), 205f)
            f.close()
            canvas.drawPath(f, paint)
        }

        return bmp
    }

    // 12. Golden Temple Collectible Coin with Ancient Sun Sigil
    private fun createCoinBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Brilliant Burnished Gold Base
        paint.shader = RadialGradient(64f, 64f, 60f, Color.rgb(255, 235, 120), Color.rgb(205, 145, 25), Shader.TileMode.CLAMP)
        canvas.drawCircle(64f, 64f, 60f, paint)
        paint.shader = null

        // Raised Coin Rim
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(175, 115, 15)
        canvas.drawCircle(64f, 64f, 54f, paint)

        // Ancient Mayan Sun Sigil in Center
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 215, 60)
        canvas.drawCircle(64f, 64f, 22f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.color = Color.rgb(160, 100, 10)
        for (deg in 0 until 360 step 45) {
            val rad = Math.toRadians(deg.toDouble())
            val x1 = (64 + Math.cos(rad) * 26).toFloat()
            val y1 = (64 + Math.sin(rad) * 26).toFloat()
            val x2 = (64 + Math.cos(rad) * 44).toFloat()
            val y2 = (64 + Math.sin(rad) * 44).toFloat()
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        return bmp
    }

    // 13. Temple Torch Fire Flame
    private fun createFireBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Transparent background
        canvas.drawColor(Color.TRANSPARENT)

        // Outer Flame Corona (Orange-Red)
        val flamePath = Path()
        flamePath.moveTo(64f, 10f)
        flamePath.quadTo(110f, 65f, 95f, 115f)
        flamePath.quadTo(64f, 125f, 33f, 115f)
        flamePath.quadTo(18f, 65f, 64f, 10f)

        paint.shader = RadialGradient(64f, 85f, 55f, Color.argb(240, 255, 95, 10), Color.argb(0, 220, 30, 0), Shader.TileMode.CLAMP)
        canvas.drawPath(flamePath, paint)

        // Inner Core (Blazing White-Yellow)
        val innerPath = Path()
        innerPath.moveTo(64f, 35f)
        innerPath.quadTo(88f, 75f, 80f, 115f)
        innerPath.quadTo(64f, 122f, 48f, 115f)
        innerPath.quadTo(40f, 75f, 64f, 35f)

        paint.shader = RadialGradient(64f, 90f, 35f, Color.argb(255, 255, 255, 210), Color.argb(210, 255, 180, 20), Shader.TileMode.CLAMP)
        canvas.drawPath(innerPath, paint)

        return bmp
    }
}
