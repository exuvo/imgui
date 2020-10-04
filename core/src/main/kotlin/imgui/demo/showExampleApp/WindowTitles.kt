package imgui.demo.showExampleApp

import glm_.i
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.frameCount
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.ImGui.time
import imgui.dsl.window

object WindowTitles {

    // Demonstrate using "##" and "###" in identifiers to manipulate ID generation.
    // This apply to all regular items as well.
    // Read FAQ section "How can I have multiple widgets with the same label?" for details.
    operator fun invoke() {
        /*  By default, Windows are uniquely identified by their title.
            You can use the "##" and "###" markers to manipulate the display/ID.
            Using "##" to display same title but have unique identifier.    */
        setNextWindowPos(Vec2(100), Cond.FirstUseEver)
        window("Same title as another window##1") {
            text("This is window 1.\nMy title is the same as window 2, but my identifier is unique.")
        }

        setNextWindowPos(Vec2(100, 200), Cond.FirstUseEver)
        window("Same title as another window##2") {
            text("This is window 2.\nMy title is the same as window 1, but my identifier is unique.")
        }

        // Using "###" to display a changing title but keep a static identifier "AnimatedTitle"
        val title = "Animated title ${"|/-\\"[(time / 0.25f).i and 3]} $frameCount###AnimatedTitle"
        setNextWindowPos(Vec2(100, 300), Cond.FirstUseEver)
        window(title) { text("This window has a changing title.") }
    }
}