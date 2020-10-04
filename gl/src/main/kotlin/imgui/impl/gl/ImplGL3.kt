package imgui.impl.gl

import glm_.L
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4ub
import gln.*
import gln.glf.semantic
import gln.identifiers.GlBuffers
import gln.identifiers.GlProgram
import gln.identifiers.GlVertexArray
import gln.uniform.glUniform
import gln.vertexArray.glVertexAttribPointer
import imgui.BackendFlag
import imgui.DEBUG
import imgui.ImGui.io
import imgui.impl.mat
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import imgui.or
import kool.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL32C.glDrawElementsBaseVertex
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL33C.glBindSampler
import org.lwjgl.opengl.GL45C.GL_CLIP_ORIGIN
import org.lwjgl.system.Platform


class ImplGL3 : GLInterface {

    var program = GlProgram(0)
    var matUL = -1

    val buffers = GlBuffers<Buffer>()
    var vao = GlVertexArray()

    init {

        // query for GL version
        glVersion = when {
            !OPENGL_ES2 -> glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10
            else -> 200 // GLES 2
        }

        // Setup back-end capabilities flags
        io.backendRendererName = "imgui_impl_opengl3"

        if (MAY_HAVE_DRAW_WITH_BASE_VERTEX)
            if (glVersion >= 320)
                io.backendFlags = io.backendFlags or BackendFlag.RendererHasVtxOffset  // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.
    }

    override fun shutdown() = destroyDeviceObjects()

    override fun newFrame() {
        if (program.isInvalid)
            createDeviceObjects()
    }

    fun setupRenderState(drawData: DrawData, fbWidth: Int, fbHeight: Int) {

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_SCISSOR_TEST)
        if (POLYGON_MODE)
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Support for GL 4.5 rarely used glClipControl(GL_UPPER_LEFT)
        val clipOriginLowerLeft = when {
            CLIP_ORIGIN && Platform.get() != Platform.MACOSX -> glGetInteger(GL_CLIP_ORIGIN) != GL_UPPER_LEFT
            else -> true
        }

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayPos is (0,0) for single viewport apps.
        glViewport(0, 0, fbWidth, fbHeight)
        val L = drawData.displayPos.x
        val R = drawData.displayPos.x + drawData.displaySize.x
        var T = drawData.displayPos.y
        var B = drawData.displayPos.y + drawData.displaySize.y
        if (!clipOriginLowerLeft) {
            val tmp = T; T = B; B = tmp; } // Swap top and bottom if origin is upper left
        val orthoProjection = glm.ortho(L, R, B, T, mat)
        glUseProgram(program.name)
        glUniform(matUL, orthoProjection)
        if (SAMPLER_BINDING)
            glBindSampler(0, 0) // We use combined texture/sampler state. Applications using GL 3.3 may set that otherwise.

        vao.bind()

        // Bind vertex/index buffers and setup attributes for ImDrawVert
        glBindBuffer(GL_ARRAY_BUFFER, buffers[Buffer.Vertex].name)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[Buffer.Element].name)
        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
        glEnableVertexAttribArray(semantic.attr.COLOR)
        glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, DrawVert.size, 0)
        glVertexAttribPointer(semantic.attr.TEX_COORD, Vec2.length, GL_FLOAT, false, DrawVert.size, Vec2.size)
        glVertexAttribPointer(semantic.attr.COLOR, Vec4ub.length, GL_UNSIGNED_BYTE, true, DrawVert.size, Vec2.size * 2)
    }

    /** OpenGL3 Render function.
     *  (this used to be set in io.renderDrawListsFn and called by ImGui::render(), but you can now call this directly
     *  from your main loop)
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL
     *  state explicitly, in order to be able to run within any OpenGL engine that doesn't do so.   */
    override fun renderDrawData(drawData: DrawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = when {
            SAMPLER_BINDING -> glGetInteger(GL33C.GL_SAMPLER_BINDING)
            else -> 0
        }
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastPolygonMode = glGetVec2i(GL_POLYGON_MODE)[0]
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val lastEnableBlend = glIsEnabled(GL_BLEND)
        val lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
        val lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)

        // Setup desired GL state
        setupRenderState(drawData, fbWidth, fbHeight)

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (cmdList in drawData.cmdLists) {

            var idxBufferOffset = 0L

            // Upload vertex/index buffers
            nglBufferData(GL_ARRAY_BUFFER, cmdList.vtxBuffer.data.lim.L, cmdList.vtxBuffer.data.adr, GL_STREAM_DRAW)
            nglBufferData(GL_ELEMENT_ARRAY_BUFFER, cmdList.idxBuffer.lim * DrawIdx.BYTES.L, cmdList.idxBuffer.adr, GL_STREAM_DRAW)

            for (cmd in cmdList.cmdBuffer) {

                val userCB = cmd.userCallback
                if (userCB != null) {
                    // User callback, registered via ImDrawList::AddCallback()
                    // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                    if (cmd.resetRenderState)
                        setupRenderState(drawData, fbWidth, fbHeight)
                    else
                        userCB(cmdList, cmd)
                } else {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
                    val clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
                    val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
                    val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y

                    if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
                        // Apply scissor/clipping rectangle
                        glScissor(clipRectX.i, (fbHeight - clipRectW).i, (clipRectZ - clipRectX).i, (clipRectW - clipRectY).i)

                        // Bind texture, Draw
                        glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                        if (MAY_HAVE_DRAW_WITH_BASE_VERTEX && glVersion >= 320)
                            glDrawElementsBaseVertex(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, cmd.idxOffset.L * DrawIdx.BYTES, cmd.vtxOffset)
                        else
                            glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, cmd.idxOffset.L * DrawIdx.BYTES)
                    }
                }
                idxBufferOffset += cmd.elemCount * DrawIdx.BYTES
            }
        }

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        if (SAMPLER_BINDING)
            glBindSampler(0, lastSampler)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
        if (lastEnableBlend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        if (lastEnableCullFace) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
        if (POLYGON_MODE)
            glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode)
        glViewport(lastViewport)
        glScissor(lastScissorBox)
    }

    /** Build texture atlas */
    override fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bit (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        glGenTextures(fontTexture)
        glBindTexture(GL_TEXTURE_2D, fontTexture[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        if (UNPACK_ROW_LENGTH)
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

        // Store our identifier
        io.fonts.texID = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return when {
            DEBUG -> checkError("mainLoop")
            else -> true
        }
    }

    override fun destroyFontsTexture() {
        if (fontTexture[0] != 0) {
            glDeleteTextures(fontTexture)
            io.fonts.texID = 0
            fontTexture[0] = 0
        }
    }

    override fun createDeviceObjects(): Boolean {

        // Backup GL state [JVM] we have to save also program since we do the uniform mat and texture setup once here
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

        program = createProgram()
        program.use {
            matUL = "ProjMtx".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

        // Create buffers
        buffers.gen()

        createFontsTexture()

        vao = GlVertexArray.gen()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)

        return when {
            DEBUG -> checkError("mainLoop")
            else -> true
        }
    }

    override fun destroyDeviceObjects() {

        vao.delete()
        buffers.delete()

        if (program.isValid) program.delete()

        destroyFontsTexture()
    }

    companion object {

        var OPENGL_ES2 = false

        var CLIP_ORIGIN = false && Platform.get() != Platform.MACOSX

        var POLYGON_MODE = true
        var SAMPLER_BINDING = GL.getCapabilities().OpenGL33
            set(value) {
                //prevent crashes
                field = value and GL.getCapabilities().OpenGL33
            }
        var UNPACK_ROW_LENGTH = true
        var SINGLE_GL_CONTEXT = true

        // #if defined(IMGUI_IMPL_OPENGL_ES2) || defined(IMGUI_IMPL_OPENGL_ES3) || !defined(GL_VERSION_3_2) -> false
        var MAY_HAVE_DRAW_WITH_BASE_VERTEX = true
    }

    /*private fun debugSave(fbWidth: Int, fbHeight: Int) {
        if (g.frameCount % 60 == 0) {

            glReadBuffer(GL11C.GL_BACK)
            // no more alignment problems by texture updating
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)  // upload
            glPixelStorei(GL_PACK_ALIGNMENT, 1)  // download

            val colorBufferImg = BufferedImage(fbWidth, fbHeight, BufferedImage.TYPE_INT_ARGB)
            val graphicsColor = colorBufferImg.graphics

            val buffer = ByteBuffer(fbWidth * fbHeight * 4)
            glReadPixels(0, 0, fbWidth, fbHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

            var i = 0
            for (h in 0 until fbHeight) {
                for (w in 0 until fbWidth) {

                    val iR = buffer[i + 0].toUInt()
                    val iG = buffer[i + 1].toUInt()
                    val iB = buffer[i + 2].toUInt()
                    val iA = buffer[i + 3].toInt() and 0xff

                    graphicsColor.color = Color(iR, iG, iB, iA)
                    graphicsColor.fillRect(w, fbHeight - h - 1, 1, 1) // height - h is for flipping the image
                    i += 4
                }
            }

            val imgNameColor = "whate_(${System.currentTimeMillis()}).png"
            ImageIO.write(colorBufferImg, "png", File("""C:\Users\gbarbieri\Pictures\$imgNameColor"""))
            graphicsColor.dispose()
            buffer.free()
        }
    }*/
}