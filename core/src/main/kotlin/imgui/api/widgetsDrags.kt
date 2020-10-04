package imgui.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.dragBehavior
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.focusableItemUnregister
import imgui.ImGui.format
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.sameLine
import imgui.ImGui.setActiveID
import imgui.ImGui.setFocusID
import imgui.ImGui.style
import imgui.ImGui.tempInputIsActive
import imgui.ImGui.tempInputScalar
import imgui.ImGui.textEx
import imgui.internal.sections.DragFlag
import imgui.internal.classes.Rect
import imgui.static.patchFormatStringFloatToInt
import uno.kotlin.getValue
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Widgets: Drags
 *  - CTRL+Click on any drag box to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function argument
 *      is the same as 'float* v', the array syntax is just a way to document the number of elements that are expected to be
 *      accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
 *  - Speed are per-pixel of mouse movement (v_speed=0.2f: mouse needs to move by 5 pixels to increase value by 1).
 *      For gamepad/keyboard navigation, minimum speed is Max(v_speed, minimum_step_at_given_precision).
 *  - Use v_min < v_max to clamp edits to given limits. Note that CTRL+Click manual input can override those limits.
 *  - Use v_min > v_max to lock edits.  */
interface widgetsDrags {

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: KMutableProperty0<Float>, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                  format: String? = "%.3f", power: Float = 1f): Boolean =
            dragScalar(label, DataType.Float, v, vSpeed, vMin, vMax, format, power)

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: FloatArray, ptr: Int, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                  format: String = "%.3f", power: Float = 1f): Boolean =
            withFloat(v, ptr) { dragScalar(label, DataType.Float, it, vSpeed, vMin, vMax, format, power) }

    fun dragFloat2(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", power: Float = 1f) =
            dragScalarN(label, DataType.Float, v, 2, vSpeed, vMin, vMax, format, power)

    fun dragVec2(label: String, v: Vec2, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 2, vSpeed, vMin, vMax, format, power)
                    .also { v put _fa }

    fun dragFloat3(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                   power: Float = 1f) = dragScalarN(label, DataType.Float, v, 3, vSpeed, vMin, vMax, format, power)

    fun dragVec3(label: String, v: Vec3, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                 power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 3, vSpeed, vMin, vMax, format, power)
                    .also { v put _fa }

    fun dragFloat4(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                   power: Float = 1f): Boolean = dragScalarN(label, DataType.Float, v, 4, vSpeed, vMin, vMax, format, power)

    fun dragVec4(label: String, v: Vec4, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                 power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 4, vSpeed, vMin, vMax, format, power)
                    .also { v put _fa }

    fun dragFloatRange2(label: String, vCurrentMinPtr: KMutableProperty0<Float>, vCurrentMaxPtr: KMutableProperty0<Float>,
                        vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", formatMax: String = format,
                        power: Float = 1f): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        var min = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        var valueChanged = dragFloat("##min", vCurrentMinPtr, vSpeed, min, max, format, power)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)
        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Float.MAX_VALUE else vMax
        valueChanged = dragFloat("##max", vCurrentMaxPtr, vSpeed, min, max, formatMax, power) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popID()
        return valueChanged
    }

    /** If v_min >= v_max we have no bound
     *
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun dragInt(label: String, v: IntArray, ptr: Int, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            withInt(v, ptr) { dragInt(label, it, vSpeed, vMin, vMax, format) }

    fun dragInt(label: String, v: KMutableProperty0<Int>, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d"): Boolean = dragScalar(label, DataType.Int, v, vSpeed, vMin, vMax, format)

    fun dragInt2(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 2, vSpeed, vMin, vMax, format)

    fun dragVec2i(label: String, v: Vec2i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 2, vSpeed, vMin, vMax, format)
                    .also { v put _ia }

    fun dragInt3(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 3, vSpeed, vMin, vMax, format)

    fun dragVec3i(label: String, v: Vec3i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 3, vSpeed, vMin, vMax, format)
                    .also { v put _ia }

    fun dragInt4(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 4, vSpeed, vMin, vMax, format)

    fun dragVec4i(label: String, v: Vec4i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 4, vSpeed, vMin, vMax, format)
                    .also { v put _ia }

    fun dragIntRange2(label: String, vCurrentMinPtr: KMutableProperty0<Int>, vCurrentMaxPtr: KMutableProperty0<Int>, vSpeed: Float = 1f,
                      vMin: Int = 0, vMax: Int = 0, format: String = "%d", formatMax: String = format): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        var min = if (vMin >= vMax) Int.MIN_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        var valueChanged = dragInt("##min", vCurrentMinPtr, vSpeed, min, max, format)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)
        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Int.MAX_VALUE else vMax
        valueChanged = dragInt("##max", vCurrentMaxPtr, vSpeed, min, max, formatMax) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popID()
        return valueChanged
    }

    /** For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function
     *  argument is the same as 'float* v', the array syntax is just a way to document the number of elements that are
     *  expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
     *  Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
     *  e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
     *  Speed are per-pixel of mouse movement (vSpeed = 0.2f: mouse needs to move by 5 pixels to increase value by 1).
     *  For gamepad/keyboard navigation, minimum speed is Max(vSpeed, minimumStepAtGivenPrecision). */
    fun dragScalar(label: String, pData: FloatArray, vSpeed: Float, pMin: Float? = null, pMax: Float? = null, format: String? = null,
                   power: Float = 1f): Boolean = dragScalar(label, pData, 0, vSpeed, pMin, pMax, format, power)

    /** If vMin >= vMax we have no bound  */
    fun dragScalar(label: String, pData: FloatArray, ptr: Int = 0, vSpeed: Float, pMin: Float? = null,
                   pMax: Float? = null, format: String? = null, power: Float = 1f): Boolean =
            withFloat(pData, ptr) { dragScalar(label, DataType.Float, it, vSpeed, pMin, pMax, format, power) }

    fun <N> dragScalar(label: String, dataType: DataType,
                       pData: KMutableProperty0<N>, vSpeed: Float,
                       pMin: N? = null, pMax: N? = null,
                       format_: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        if (power != 1f)
            assert(pMin != null && pMax != null) { "When using a power curve the drag needs to have known bounds" }

        val id = window.getID(label)
        val w = calcItemWidth()
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash =  true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, frameBb))
            return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }

        // Tabbing or CTRL-clicking on Drag turns it into an input box
        val hovered = itemHoverable(frameBb, id)
        val tempInputIsActive = tempInputIsActive(id)
        var tempInputStart = false
        if (!tempInputIsActive) {
            val focusRequested = focusableItemRegister(window, id)
            val clicked = hovered && io.mouseClicked[0]
            val doubleClicked = hovered && io.mouseDoubleClicked[0]
            if (focusRequested || clicked || doubleClicked || g.navActivateId == id || g.navInputId == id) {
                setActiveID(id, window)
                setFocusID(id, window)
                focusWindow(window)
                g.activeIdUsingNavDirMask  = (1 shl Dir.Left) or (1 shl Dir.Right)
                if (focusRequested || (clicked && io.keyCtrl) || doubleClicked || g.navInputId == id) {
                    tempInputStart = true
                    focusableItemUnregister(window)
                }
            }
        }

        // Our current specs do NOT clamp when using CTRL+Click manual input, but we should eventually add a flag for that..
        if (tempInputIsActive || tempInputStart)
            return tempInputScalar(frameBb, id, label, dataType, pData, format) // , p_min, p_max)

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(frameBb, id)
        renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, style.frameRounding)

        // Drag behavior
        val valueChanged = dragBehavior(id, dataType, pData, vSpeed, pMin, pMax, format, power, DragFlag.None)
        if (valueChanged)
            markItemEdited(id)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = pData.format(dataType, format)
        renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return valueChanged
    }

    fun <N> dragScalarN(label: String, dataType: DataType, v: Any, components: Int, vSpeed: Float, vMin: N? = null, vMax: N? = null,
                        format: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushID(label)
        pushMultiItemsWidths(components, calcItemWidth())
        for (i in 0 until components) {
            pushID(i)
            if (i > 0)
                sameLine(0f, style.itemInnerSpacing.x)
            when (dataType) {
                DataType.Int -> withInt(v as IntArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power) or valueChanged
                }
                DataType.Float -> withFloat(v as FloatArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power) or valueChanged
                }
                else -> error("invalid")
            }
            popID()
            popItemWidth()
        }
        popID()

        val labelEnd = findRenderedTextEnd(label)
        if (0 != labelEnd)        {
            sameLine(0f, style.itemInnerSpacing.x)
            textEx(label, labelEnd)
        }

        endGroup()
        return valueChanged
    }
}