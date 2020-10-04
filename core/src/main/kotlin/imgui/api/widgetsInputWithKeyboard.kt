package imgui.api

import gli_.hasnt
import glm_.func.common.max
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemWidth
import imgui.ImGui.currentWindow
import imgui.ImGui.dataTypeApplyOp
import imgui.ImGui.dataTypeApplyOpFromText
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.format
import imgui.ImGui.frameHeight
import imgui.ImGui.inputTextEx
import imgui.ImGui.io
import imgui.ImGui.markItemEdited
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.internal.sections.or
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.internal.sections.ButtonFlag as Bf

@Suppress("UNCHECKED_CAST")

/** Widgets: Input with Keyboard
 *  - If you want to use InputText() with std::string or any custom dynamic string type, see cpp/imgui_stdlib.h and comments in imgui_demo.cpp.
 *  - Most of the ImGuiInputTextFlags flags are only useful for InputText() and not for InputFloatX, InputIntX, InputDouble etc. */
interface widgetsInputWithKeyboard {

    /** String overload */
    fun inputText(label: String, buf: String, flags: InputTextFlags = Itf.None.i,
                  callback: InputTextCallback? = null, userData: Any? = null): Boolean =
            inputText(label, buf.toByteArray(), flags, callback, userData)

    fun inputText(label: String, buf: ByteArray, flags: InputTextFlags = Itf.None.i,
                  callback: InputTextCallback? = null, userData: Any? = null): Boolean {
        assert(flags hasnt Itf._Multiline) { "call InputTextMultiline()" }
        return inputTextEx(label, null, buf, Vec2(), flags, callback, userData)
    }

    /** String overload */
    fun inputTextMultiline(label: String, buf: String, size: Vec2 = Vec2(), flags: InputTextFlags = Itf.None.i,
                           callback: InputTextCallback? = null, userData: Any? = null): Boolean =
            inputTextEx(label, null, buf.toByteArray(), size, flags or Itf._Multiline, callback, userData)

    fun inputTextMultiline(label: String, buf: ByteArray, size: Vec2 = Vec2(), flags: InputTextFlags = Itf.None.i,
                           callback: InputTextCallback? = null, userData: Any? = null): Boolean =
            inputTextEx(label, null, buf, size, flags or Itf._Multiline, callback, userData)

    /** String overload */
    fun inputTextWithHint(label: String, hint: String, buf: String, flags: InputTextFlags = Itf.None.i,
                          callback: InputTextCallback? = null, userData: Any? = null): Boolean =
            inputTextWithHint(label, hint, buf.toByteArray(), flags)

    fun inputTextWithHint(label: String, hint: String, buf: ByteArray, flags: InputTextFlags = 0,
                          callback: InputTextCallback? = null, userData: Any? = null): Boolean {
        assert(flags hasnt Itf._Multiline) { "call InputTextMultiline()" }
        return inputTextEx(label, hint, buf, Vec2(), flags, callback, userData)
    }


    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f,
                   format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputFloat(label, v, 0, step, stepFast, format, flags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f,
                   format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            withFloat(v, ptr) { inputFloat(label, it, step, stepFast, format, flags) }

    fun inputFloat(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f,
                   format: String = "%.3f", flags_: InputTextFlags = Itf.None.i): Boolean {
        val flags = flags_ or Itf.CharsScientific
        return inputScalar(label, DataType.Float, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }


    fun inputFloat2(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v, 2, null, null, format, flags)

    fun inputVec2(label: String, v: Vec2, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v to _fa, Vec2.length, null, null, format, flags)
                    .also { v put _fa }

    fun inputFloat3(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v, 3, null, null, format, flags)

    fun inputVec3(label: String, v: Vec3, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v to _fa, Vec3.length, null, null, format, flags)
                    .also { v put _fa }

    fun inputFloat4(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v, 4, null, null, format, flags)

    fun inputVec4(label: String, v: Vec4, format: String = "%.3f", flags: InputTextFlags = Itf.None.i): Boolean =
            inputScalarN<Float>(label, DataType.Float, v to _fa, Vec4.length, null, null, format, flags)
                    .also { v put _fa }

    fun inputInt(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, flags: InputTextFlags = 0): Boolean {
        /*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val format = if (flags has Itf.CharsHexadecimal) "%08X" else "%d"
        return inputScalar(label, DataType.Int, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun inputInt2(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v, 2, null, null, "%d", flags)

    fun inputVec2i(label: String, v: Vec2i, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v to _ia, Vec2i.length, null, null, "%d", flags)
                    .also { v put _ia }

    fun inputInt3(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v, 3, null, null, "%d", flags)

    fun inputVec3i(label: String, v: Vec3i, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v to _ia, Vec3i.length, null, null, "%d", flags)
                    .also { v put _ia }

    fun inputInt4(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v, 4, null, null, "%d", flags)

    fun inputVec4i(label: String, v: Vec4i, flags: InputTextFlags = 0): Boolean =
            inputScalarN<Int>(label, DataType.Int, v to _ia, Vec4i.length, null, null, "%d", flags)
                    .also { v put _ia }

    fun inputDouble(label: String, v: KMutableProperty0<Double>, step: Double = 0.0, stepFast: Double = 0.0,
                    format: String? = "%.6f", flags_: InputTextFlags = Itf.None.i): Boolean {
        val flags = flags_ or Itf.CharsScientific
        /*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        return inputScalar(label, DataType.Double, v, step.takeIf { it > 0.0 }, stepFast.takeIf { it > 0.0 }, format, flags)
    }

    fun <N> inputScalar(label: String, dataType: DataType, pData: IntArray, step: Int?, stepFast: Int?,
                        format: String? = null, flags: InputTextFlags = Itf.None.i)
            : Boolean where N : Number, N : Comparable<N> =
            withInt(pData) { inputScalar(label, dataType, it, step, stepFast, format, flags) }

    fun <N> inputScalar(label: String, dataType: DataType,
                        pData: KMutableProperty0<N>,
                        step: N? = null, stepFast: N? = null,
                        format_: String? = null, flags_: InputTextFlags = Itf.None.i)
            : Boolean where N : Number, N : Comparable<N> {

        var data by pData
        val window = currentWindow
        if (window.skipItems) return false

        val format = when (format_) {
            null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d"
            }
            else -> format_
        }

        val buf = pData.format(dataType, format/*, 64*/).toByteArray(64)

        var valueChanged = false
        var flags = flags_
        if (flags hasnt (Itf.CharsHexadecimal or Itf.CharsScientific))
            flags = flags or Itf.CharsDecimal
        flags = flags or Itf.AutoSelectAll
        flags = flags or Itf._NoMarkEdited  // We call MarkItemEdited() ourselves by comparing the actual data rather than the string.

        if (step != null) {
            val buttonSize = frameHeight

            beginGroup() // The only purpose of the group here is to allow the caller to query item data e.g. IsItemActive()
            pushID(label)
            setNextItemWidth(1f max (calcItemWidth() - (buttonSize + style.itemInnerSpacing.x) * 2))

            if (inputText("", buf, flags)) // PushId(label) + "" gives us the expected ID from outside point of view
                valueChanged = dataTypeApplyOpFromText(buf.cStr, g.inputTextState.initialTextA, dataType, pData, format)

            // Step buttons
            val backupFramePadding = Vec2(style.framePadding)
            style.framePadding.x = style.framePadding.y
            var buttonFlags = Bf.Repeat or Bf.DontClosePopups
            if (flags has Itf.ReadOnly)
                buttonFlags = buttonFlags or Bf.Disabled
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("-", Vec2(buttonSize), buttonFlags)) {
                data = dataTypeApplyOp(dataType, '-', data, stepFast?.takeIf { io.keyCtrl } ?: step)
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("+", Vec2(buttonSize), buttonFlags)) {
                data = dataTypeApplyOp(dataType, '+', data, stepFast?.takeIf { io.keyCtrl } ?: step)
                valueChanged = true
            }

            val labelEnd = findRenderedTextEnd(label)
            if (0 != labelEnd) {
                sameLine(0f, style.itemInnerSpacing.x)
                textEx(label, labelEnd)
            }
            style.framePadding put backupFramePadding

            popID()
            endGroup()
        } else if (inputText(label, buf, flags))
            valueChanged = dataTypeApplyOpFromText(buf.cStr, g.inputTextState.initialTextA, dataType, pData, format)

        if (valueChanged)
            markItemEdited(window.dc.lastItemId)

        return valueChanged
    }

    fun <N> inputScalarN(label: String, dataType: DataType, v: Any, components: Int, step: N? = null, stepFast: N? = null,
                         format: String? = null, flags: InputTextFlags = 0): Boolean where N : Number, N : Comparable<N> {

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
            valueChanged = when (dataType) {
                DataType.Float -> withFloat(v as FloatArray, i) { inputScalar("", dataType, it as KMutableProperty0<N>, step, stepFast, format, flags) }
                DataType.Int -> withInt(v as IntArray, i) { inputScalar("", dataType, it as KMutableProperty0<N>, step, stepFast, format, flags) }
                else -> error("invalid")
            } || valueChanged
            sameLine(0f, style.itemInnerSpacing.x)
            popID()
            popItemWidth()
        }
        popID()

        val labelEnd = findRenderedTextEnd(label)
        if (0 != labelEnd) {
            sameLine(0f, style.itemInnerSpacing.x)
            textEx(label, labelEnd)
        }

        endGroup()
        return valueChanged
    }
}
