package imgui.internal.api

import imgui.COL32
import imgui.ImGui.getForegroundDrawList
import imgui.api.g

/** Debug Tools */
internal interface debugTools {

    fun debugDrawItemRect(col: Int = COL32(255, 0, 0, 255)) {
        val window = g.currentWindow!!
        getForegroundDrawList(window).addRect(window.dc.lastItemRect.min, window.dc.lastItemRect.max, col)
    }

    fun debugStartItemPicker() {
        g.debugItemPickerActive = true
    }
}