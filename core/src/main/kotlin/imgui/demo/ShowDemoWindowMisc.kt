package imgui.demo

import glm_.i
import imgui.*
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.captureKeyboardFromApp
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.inputText
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isKeyPressed
import imgui.ImGui.isKeyReleased
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isMouseReleased
import imgui.ImGui.mouseCursor
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.sliderFloat3
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.TextFilter
import imgui.dsl.collapsingHeader
import imgui.dsl.treeNode

object ShowDemoWindowMisc {

    /* Tabbing */
    var buf0 = "dummy".toByteArray(32)


    /* Focus from code */
    var buf1 = "click on a button to set focus".toByteArray(128)
    val f3 = FloatArray(3)
    var float3 = 0f

    /** Focused & Hovered Test */
    var embedAllInsideAchildWindow = false

    operator fun invoke() {

        collapsingHeader("Filtering") {
            // Helper class to easy setup a text filter.
            // You may want to implement a more feature-full filtering scheme in your own application.
            val filter = TextFilter()
            text("Filter usage:\n" +
                    "  \"\"         display all lines\n" +
                    "  \"xxx\"      display lines containing \"xxx\"\n" +
                    "  \"xxx,yyy\"  display lines containing \"xxx\" or \"yyy\"\n" +
                    "  \"-xxx\"     hide lines containing \"xxx\"")
            filter.draw()
            val lines = arrayListOf("aaa1.c", "bbb1.c", "ccc1.c", "aaa2.cpp", "bbb2.cpp", "ccc2.cpp", "abc.h", "hello, world")
            lines.stream().filter { filter.passFilter(it) }.forEach { bulletText(it) }
        }

        collapsingHeader("Inputs, Navigation & Focus") {

            // Display ImGuiIO output flags
            text("WantCaptureMouse: ${io.wantCaptureMouse}")
            text("WantCaptureKeyboard: ${io.wantCaptureKeyboard}")
            text("WantTextInput: ${io.wantTextInput}")
            text("WantMoveMouse: ${io.wantSetMousePos}")
            text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")

            treeNode("Keyboard, Mouse & Navigation State") {
                if (isMousePosValid()) text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                else text("Mouse pos: <INVALID>")
                text("Mouse delta: (%g, %g)", io.mouseDelta.x, io.mouseDelta.y)
                text("Mouse down:")
                for (i in 0 until io.mouseDown.size)
                    if (io.mouseDownDuration[i] >= 0f) {
                        sameLine()
                        text("b$i (%.02f secs)", io.mouseDownDuration[i])
                    }
                text("Mouse clicked:")
                for (i in io.mouseDown.indices)
                    if (isMouseClicked(MouseButton of i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse dblclick:")
                for (i in io.mouseDown.indices)
                    if (isMouseDoubleClicked(MouseButton of i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse released:")
                for (i in io.mouseDown.indices)
                    if (isMouseReleased(MouseButton of i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse wheel: %.1f", io.mouseWheel)

                text("Keys down:")
                for (i in io.keysDown.indices)
                    if (io.keysDownDuration[i] >= 0f) {
                        sameLine()
                        text("$i (0x%X) (%.02f secs)", i, io.keysDownDuration[i])
                    }
                text("Keys pressed:")
                for (i in io.keysDown.indices)
                    if (isKeyPressed(i)) {
                        sameLine()
                        text("$i (0x%X)", i)
                    }
                text("Keys release:")
                for (i in io.keysDown.indices)
                    if (isKeyReleased(i)) {
                        sameLine()
                        text("$i (0x%X)", i)
                    }
                val ctrl = if (io.keyCtrl) "CTRL " else ""
                val shift = if (io.keyShift) "SHIFT " else ""
                val alt = if (io.keyAlt) "ALT " else ""
                val super_ = if (io.keySuper) "SUPER " else ""
                text("Keys mods: $ctrl$shift$alt$super_")
                text("Chars queue:")
                io.inputQueueCharacters.forEach { c ->
                    // UTF-8 will represent some characters using multiple bytes, so we join them here
                    // example: 'ç' becomes "0xC3, 0xA7"
                    val bytes = c.toString().toByteArray().joinToString { "0x%X".format(it) }
                    sameLine(); text("\'%c\' (%s)", if (c > ' ' && c.i <= 255) c else '?', bytes)
                }

                text("NavInputs down:")
                io.navInputs.filter { it > 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }
                text("NavInputs pressed:")
                io.navInputsDownDuration.filter { it == 0f }.forEachIndexed { i, _ -> sameLine(); text("[$i]") }
                text("NavInputs duration:")
                io.navInputsDownDuration.filter { it >= 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }

                button("Hovering me sets the\nkeyboard capture flag")
                if (isItemHovered()) captureKeyboardFromApp(true)
                sameLine()
                button("Holding me clears the\nthe keyboard capture flag")
                if (isItemActive) captureKeyboardFromApp(false)
            }

            treeNode("Tabbing") {
                text("Use TAB/SHIFT+TAB to cycle through keyboard editable fields.")
                inputText("1", buf0)
                inputText("2", buf0)
                inputText("3", buf0)
                pushAllowKeyboardFocus(false)
                inputText("4 (tab skip)", buf0)
                //SameLine(); ShowHelperMarker("Use PushAllowKeyboardFocus(bool) to disable tabbing through certain widgets.");
                popAllowKeyboardFocus()
                inputText("5", buf0)
            }

            treeNode("Focus from code") {
                val focus1 = button("Focus on 1"); sameLine()
                val focus2 = button("Focus on 2"); sameLine()
                val focus3 = button("Focus on 3")
                var hasFocus = 0

                if (focus1) setKeyboardFocusHere()
                inputText("1", buf1)
                if (isItemActive) hasFocus = 1

                if (focus2) setKeyboardFocusHere()
                inputText("2", buf1)
                if (isItemActive) hasFocus = 2

                pushAllowKeyboardFocus(false)
                if (focus3) setKeyboardFocusHere()
                inputText("3 (tab skip)", buf1)
                if (isItemActive) hasFocus = 3
                popAllowKeyboardFocus()

                text("Item with focus: ${if (hasFocus != 0) "$hasFocus" else "<none>"}")

                // Use >= 0 parameter to SetKeyboardFocusHere() to focus an upcoming item
                var focusAhead = -1
                if (button("Focus on X")) focusAhead = 0; sameLine()
                if (button("Focus on Y")) focusAhead = 1; sameLine()
                if (button("Focus on Z")) focusAhead = 2
                if (focusAhead != -1) setKeyboardFocusHere(focusAhead)
                sliderFloat3("Float3", f3, 0f, 1f)

                textWrapped("NB: Cursor & selection are preserved when refocusing last used item in code.")
            }

            treeNode("Dragging") {
                textWrapped("You can use getMouseDragDelta(0) to query for the dragged amount on any widget.")
                for(button in MouseButton.values())
                    if (button != MouseButton.None) {
                        text("IsMouseDragging(${button.i}):")
                        text("  w/ default threshold: ${isMouseDragging(button).i},")
                        text("  w/ zero threshold: ${isMouseDragging(button, 0f).i},")
                        text("  w/ large threshold: ${isMouseDragging(button, 20f)},")
                    }
                button("Drag Me")
                if (isItemActive)
                    foregroundDrawList.addLine(io.mouseClickedPos[0], io.mousePos, Col.Button.u32, 4f) // Draw a line between the button and the mouse cursor

                // Drag operations gets "unlocked" when the mouse has moved past a certain threshold
                // (the default threshold is stored in io.MouseDragThreshold). You can request a lower or higher
                // threshold using the second parameter of IsMouseDragging() and GetMouseDragDelta().
                val valueRaw = getMouseDragDelta(MouseButton.Left, 0f)
                val valueWithLockThreshold = getMouseDragDelta(MouseButton.Left)
                val mouseDelta = io.mouseDelta
                text("GetMouseDragDelta(0):")
                text("  w/ default threshold: (%.1f, %.1f)", valueWithLockThreshold.x, valueWithLockThreshold.y)
                text("  w/ zero threshold: (%.1f, %.1f)", valueRaw.x, valueRaw.y)
                text("io.MouseDelta: (%.1f, %.1f)", mouseDelta.x, mouseDelta.y)
            }

            treeNode("Mouse cursors") {
                val current = mouseCursor
                text("Current mouse cursor = ${current.i}: $current")
                text("Hover to see mouse cursors:")
                sameLine(); helpMarker(
                    "Your application can render a different mouse cursor based on what ImGui::GetMouseCursor() returns. " +
                    "If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, " +
                    "otherwise your backend needs to handle it.")
                for (i in 0 until MouseCursor.COUNT) {
                    bullet(); selectable("Mouse cursor $i: ${MouseCursor.of(i)}", false)
                    if (isItemHovered() || isItemFocused)
                        mouseCursor = MouseCursor.of(i)
                }
            }
        }
    }
}