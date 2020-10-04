package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.COL32
import imgui.ImGui.backgroundDrawList
import imgui.ImGui.begin
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.calcItemWidth
import imgui.ImGui.checkbox
import imgui.ImGui.colorEdit4
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.fontSize
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDown
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.sliderInt
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowPos
import imgui.ImGui.windowSize
import imgui.MouseButton
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.dsl.button
import imgui.internal.sections.DrawCornerFlag
import imgui.u32
import kotlin.reflect.KMutableProperty0

object CustomRendering {

    var sz = 36f
    var thickness = 3f
    var gradientSteps = 16
    var ngonSides = 6
    var circleSegmentsOverride = false
    var circleSegmentsOverrideV = 12
    val colf = Vec4(1.0f, 1.0f, 0.4f, 1.0f)

    var addingLine = false
    val points = ArrayList<Vec2>()

    var drawBg = true
    var drawFg = true

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (!begin("Example: Custom rendering", open)) {
            end()
            return
        }

        // Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and benefit of
        // overloaded operators, etc. Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your
        // types and ImVec2/ImVec4. Dear ImGui defines overloaded operators but they are internal to imgui.cpp and not
        // exposed outside (to avoid messing with your types) In this example we are not using the maths operators!
        val drawList = windowDrawList

        if (beginTabBar("##TabBar")) {

            if (beginTabItem("Primitives")) {

                pushItemWidth(-fontSize * 10)

                // Draw gradients
                // (note that those are currently exacerbating our sRGB/Linear issues)
                text("Gradients")
                val gradientSize = Vec2(calcItemWidth(), frameHeight)
                run {
                    val p0 = cursorScreenPos
                    val p1 = p0 + gradientSize
                    val colA = Vec4(0f, 0f, 0f, 1f).u32
                    val colB = Vec4(1f).u32
                    drawList.addRectFilledMultiColor(p0, p1, colA, colB, colB, colA)
                    invisibleButton("##gradient1", gradientSize)
                }
                run {
                    val p0 = cursorScreenPos
                    val p1 = p0 + gradientSize
                    val colA = Vec4(0f, 1f, 0f, 1f).u32
                    val colB = Vec4(1f, 0f, 0f, 1f).u32
                    drawList.addRectFilledMultiColor(p0, p1, colA, colB, colB, colA)
                    invisibleButton("##gradient2", gradientSize)
                }

                // Draw a bunch of primitives
                text("All primitives")
                dragFloat("Size", ::sz, 0.2f, 2.0f, 72.0f, "%.0f")
                dragFloat("Thickness", ::thickness, 0.05f, 1.0f, 8.0f, "%.02f")
                sliderInt("N-gon sides", ::ngonSides, 3, 12)
                checkbox("##circlesegmentoverride", ::circleSegmentsOverride)
                sameLine(0f, style.itemInnerSpacing.x)
                if (sliderInt("Circle segments", ::circleSegmentsOverrideV, 3, 40))
                    circleSegmentsOverride = true
                colorEdit4("Color", colf)
                val p = cursorScreenPos
                val col = getColorU32(colf)
                val spacing = 10f
                val cornersNone = DrawCornerFlag.None.i
                val cornersAll = DrawCornerFlag.All.i
                val cornersTlBr = DrawCornerFlag.TopLeft or DrawCornerFlag.BotRight
                val circleSegments = if (circleSegmentsOverride) circleSegmentsOverrideV else 0
                var (x, y) = p + 4f
                for (n in 0 until 2) {
                    // First line uses a thickness of 1.0f, second line uses the configurable thickness
                    val th = if (n == 0) 1.0f else thickness
                    drawList.apply {
                        addNgon(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, ngonSides, th); x += sz + spacing  // N-gon
                        addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, circleSegments, th); x += sz + spacing  // Circle
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 0.0f, cornersNone, th); x += sz + spacing  // Square
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersAll, th); x += sz + spacing  // Square with all rounded corners
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersTlBr, th); x += sz + spacing  // Square with two rounded corners
                        addTriangle(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col, th); x += sz + spacing      // Triangle
                        addTriangle(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col, th); x += sz * 0.4f + spacing // Thin triangle
                        addLine(Vec2(x, y), Vec2(x + sz, y), col, th); x += sz + spacing  // Horizontal line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x, y + sz), col, th); x += spacing       // Vertical line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x + sz, y + sz), col, th); x += sz + spacing  // Diagonal line
                        addBezierCurve(Vec2(x, y), Vec2(x + sz * 1.3f, y + sz * 0.3f), Vec2(x + sz - sz * 1.3f, y + sz - sz * 0.3f), Vec2(x + sz, y + sz), col, th)
                    }
                    x = p.x + 4
                    y += sz + spacing
                }
                drawList.apply {
                    addNgonFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, ngonSides); x += sz + spacing  // N-gon
                    addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, circleSegments); x += sz + spacing  // Circle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col); x += sz + spacing  // Square
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f); x += sz + spacing  // Square with all rounded corners
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersTlBr); x += sz + spacing  // Square with two rounded corners
                    addTriangleFilled(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col); x += sz + spacing      // Triangle
                    addTriangleFilled(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col); x += sz * 0.4f + spacing // Thin triangle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + thickness), col); x += sz + spacing  // Horizontal line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + thickness, y + sz), col); x += spacing * 2f  // Vertical line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + 1, y + 1), col); x += sz            // Pixel (faster than AddLine)
                }
                dummy(Vec2((sz + spacing) * 9.8f, (sz + spacing) * 3))

                popItemWidth()
                endTabItem()
            }

            if (beginTabItem("Canvas")) {

                button("Clear") { points.clear() }
                if (points.size >= 2) {
                    sameLine()
                    button("Undo") {
                        points.removeAt(points.lastIndex)
                        points.removeAt(points.lastIndex)
                    }
                }
                text("Left-click and drag to add lines,\nRight-click to undo")

                // Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use
                // IsItemHovered(). But you can also draw directly and poll mouse/keyboard by yourself.
                // You can manipulate the cursor using GetCursorPos() and SetCursorPos().
                // If you only use the ImDrawList API, you can notify the owner window of its extends with SetCursorPos(max).
                val canvasP = Vec2(cursorScreenPos)       // ImDrawList API uses screen coordinates!
                val canvasSz = Vec2(contentRegionAvail)   // Resize canvas to what's available
                if (canvasSz.x < 50f) canvasSz.x = 50f
                if (canvasSz.y < 50f) canvasSz.y = 50f
                drawList.addRectFilledMultiColor(canvasP, canvasP + canvasSz, getColorU32(50, 50, 50, 255), getColorU32(50, 50, 60, 255), getColorU32(60, 60, 70, 255), getColorU32(50, 50, 60, 255))
                drawList.addRect(canvasP, canvasP + canvasSz, getColorU32(255, 255, 255, 255))

                var addingPreview = false
                invisibleButton("canvas", canvasSz)
                val mousePosGlobal = io.mousePos
                val mousePosCanvas = mousePosGlobal - canvasP
                if (addingLine) {
                    addingPreview = true
                    points += mousePosCanvas
                    if (!isMouseDown(MouseButton.Left)) {
                        addingLine = false
                        addingPreview = false
                    }
                }
                if (isItemHovered()) {
                    if (!addingLine and isMouseClicked(MouseButton.Left)) {
                        points += mousePosCanvas
                        addingLine = true
                    }
                    if (isMouseClicked(MouseButton.Right) and points.isNotEmpty()) {
                        addingLine = false
                        addingPreview = false
                        points.removeAt(points.lastIndex)
                        points.removeAt(points.lastIndex)
                    }
                }

                // Draw all lines in the canvas (with a clipping rectangle so they don't stray out of it).
                drawList.pushClipRect(canvasP, canvasP + canvasSz, true)
                for (i in 0 until points.lastIndex step 2)
                    drawList.addLine(canvasP + points[i], canvasP + points[i + 1], getColorU32(255, 255, 0, 255), 2f)
                drawList.popClipRect()
                if (addingPreview)
                    points.removeAt(points.lastIndex)
                endTabItem()
            }

            if (beginTabItem("BG/FG draw lists")) {
                checkbox("Draw in Background draw list", ::drawBg)
                sameLine(); helpMarker("The Background draw list will be rendered below every Dear ImGui windows.")
                checkbox("Draw in Foreground draw list", ::drawFg)
                sameLine(); helpMarker("The Foreground draw list will be rendered over every Dear ImGui windows.")
                val windowCenter = Vec2(windowPos.x + windowSize.x * 0.5f, windowPos.y + windowSize.y * 0.5f)
                if (drawBg)
                    backgroundDrawList.addCircle(windowCenter, windowSize.x * 0.6f, COL32(255, 0, 0, 200), 0, 10f + 4)
                if (drawFg)
                    foregroundDrawList.addCircle(windowCenter, windowSize.y * 0.6f, COL32(0, 255, 0, 200), 0, 10f)
                endTabItem()
            }
            endTabBar()
        }

        end()
    }
}