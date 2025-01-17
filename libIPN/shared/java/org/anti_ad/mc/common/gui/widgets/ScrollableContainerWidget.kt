/*
 * Inventory Profiles Next
 *
 *   Copyright (c) 2019-2020 jsnimda <7615255+jsnimda@users.noreply.github.com>
 *   Copyright (c) 2021-2022 Plamen K. Kosseff <p.kosseff@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anti_ad.mc.common.gui.widgets

import org.anti_ad.mc.common.gui.NativeContext
import org.anti_ad.mc.common.gui.layout.AnchorStyles
import org.anti_ad.mc.common.gui.layout.Overflow.HIDDEN
import org.anti_ad.mc.common.math2d.Rectangle
import org.anti_ad.mc.common.vanilla.render.glue.rDrawOutline
import org.anti_ad.mc.common.vanilla.render.glue.rFillRect
import kotlin.math.roundToInt

// private static final int COLOR_WHITE              = 0xFFFFFFFF;
private const val COLOR_BORDER = -0x666667
private const val COLOR_SCROLLBAR_BG = -0x80000000 // 0xFF000000; ref: EntryListWidget.render
private const val COLOR_SCROLLBAR_SHADOW = -0x676768 // 0xFF808080;
private const val COLOR_SCROLLBAR = -0x272728 // 0xFFC0C0C0;
private const val COLOR_SCROLLBAR_HOVER_SHADOW = -0x3f3f40
private const val COLOR_SCROLLBAR_HOVER = -0x1

class ScrollableContainerWidget(scrollbarW: Int = 6) : Widget() {

    private var scrollbarWidth: Int = scrollbarW

    private val padding = 3

    val viewport = Widget().apply {
        overflow = HIDDEN
        anchor = AnchorStyles.all
        this@ScrollableContainerWidget.addChild(this)
        top = padding
        left = padding
        bottom = padding
        right = padding + scrollbarWidth
    }

    private val _contentContainer = object : Widget() {
        override fun render(context: NativeContext,
                            mouseX: Int,
                            mouseY: Int,
                            partialTicks: Float) {
            contentCustomRenderer(context,
                                  mouseX,
                                  mouseY,
                                  partialTicks)
        }

        fun superRender(context: NativeContext,
                        mouseX: Int,
                        mouseY: Int,
                        partialTicks: Float) {
            super.render(context,
                         mouseX,
                         mouseY,
                         partialTicks)
        }

        override var absoluteBounds: Rectangle
            get() = parent?.absoluteBounds!!
            set(value) {
                parent?.absoluteBounds = value
            }

    }.apply {
        anchor = AnchorStyles.noBottom
        viewport.addChild(this)
        top = 0
        left = 0
        right = 0
        parent = this@ScrollableContainerWidget
        sizeChanged += {
            scrollY = scrollY // update scrollY
        }
    }

    val contentContainer
        get() = _contentContainer
    val contentCustomRendererDefault: Widget.(NativeContext, Int, Int, Float) -> Unit =
        {context: NativeContext, mouseX: Int, mouseY: Int, partialTicks: Float ->
            _contentContainer.superRender(context,
                                          mouseX,
                                          mouseY,
                                          partialTicks)
        }
    var contentCustomRenderer = contentCustomRendererDefault

    var contentHeight: Int
        get() = contentContainer.height
        set(value) {
            contentContainer.height = value
        }
    var scrollY: Int
        get() = -contentContainer.top
        set(value) {
            contentContainer.top = -value.coerceIn(0,
                                                   scrollYMax)
        }
    val scrollYMax: Int
        get() = (contentHeight - viewport.height).coerceAtLeast(0)

    var renderBorder = false
    var borderColor = COLOR_BORDER

    var scrollWheelAmount: Int = 20

    //region Scrollbar

    val scrollbar = Scrollbar() // not using "= object { ... }" for public accessing

    inner class Scrollbar {
        val visible: Boolean
            get() = scrollYMax > 0
        val trackHeight: Int
            get() = viewport.height
        val thumbHeight: Int
            get() {
                return if (visible) trackHeight * trackHeight / contentHeight else trackHeight
            }
        val yMax: Int
            get() = trackHeight - thumbHeight
        var y: Int
            get() = toY(scrollY)
            set(value) {
                scrollY = toScrollY(value)
            }

        fun toScrollY(y: Int): Int = map(yMax,
                                         scrollYMax,
                                         y)

        fun toY(scrollY: Int): Int = map(scrollYMax,
                                         yMax,
                                         scrollY)

        val trackAbsoluteBounds: Rectangle
            get() = viewport.absoluteBounds.run {
                copy(x = right,
                     width = scrollbarWidth)
            }
        val thumbAbsoluteBounds: Rectangle
            get() = trackAbsoluteBounds.let {
                it.copy(y = it.y + y,
                        height = thumbHeight)
            }
    }

    private fun map(inputMax: Int,
                    outputMax: Int,
                    input: Int): Int = // min = 0
        if (inputMax == 0) 0 else (1.0 * input * outputMax / inputMax).roundToInt()

    //endregion

    override fun render(context: NativeContext,
                        mouseX: Int,
                        mouseY: Int,
                        partialTicks: Float) {
        if (renderBorder) {
            rDrawOutline(context,
                         absoluteBounds,
                         borderColor)
        }
        // render scrollbar, ref: EntryListWidget.render
        if (scrollbar.visible) {
            rFillRect(context,
                      scrollbar.trackAbsoluteBounds,
                      COLOR_SCROLLBAR_BG)
            val hover = scrollbar.thumbAbsoluteBounds.contains(mouseX,
                                                               mouseY) || draggingScrollbar
            rFillRect(context,
                      scrollbar.thumbAbsoluteBounds,
                      if (hover) COLOR_SCROLLBAR_HOVER_SHADOW else COLOR_SCROLLBAR_SHADOW)
            rFillRect(context,
                      scrollbar.thumbAbsoluteBounds.run {
                copy(width = width - 1,
                     height = height - 1)
            },
                      if (hover) COLOR_SCROLLBAR_HOVER else COLOR_SCROLLBAR)
        }
        this.overflow = HIDDEN
        super.render(context,
                     mouseX,
                     mouseY,
                     partialTicks)
    }

    // scrolling logic / ui events
    private var draggingScrollbar = false
    private var draggingInitMouseY = 0
    private var draggingInitScrollbarY = 0
    override fun mouseClicked(x: Int,
                              y: Int,
                              button: Int): Boolean =
        super.mouseClicked(x,
                           y,
                           button) || if (button == 0 && scrollbar.visible && scrollbar.trackAbsoluteBounds.contains(x, y)) {
            if (!scrollbar.thumbAbsoluteBounds.contains(x, y)) {
                scrollbar.y = y - viewport.screenY - scrollbar.thumbHeight / 2 // e = y1 + yoffset + sh/2
            }
            draggingScrollbar = true
            draggingInitMouseY = y
            draggingInitScrollbarY = scrollbar.y
            true
        } else false

    override fun mouseReleased(x: Int,
                               y: Int,
                               button: Int): Boolean =
        false.also { draggingScrollbar = false }

    override fun mouseDragged(x: Double,
                              y: Double,
                              button: Int,
                              dx: Double,
                              dy: Double): Boolean =
        super.mouseDragged(x,
                           y,
                           button,
                           dx,
                           dy) || if (draggingScrollbar) {
            val shiftY = (y - draggingInitMouseY).toInt()
            scrollbar.y = draggingInitScrollbarY + shiftY
            true
        } else false

    override fun mouseScrolled(x: Int,
                               y: Int,
                               amount: Double): Boolean = // f = 1 or -1
        super.mouseScrolled(x,
                            y,
                            amount) || if (scrollbar.visible) {
            scrollY -= (amount * scrollWheelAmount).toInt()
            true
        } else false

    fun isOutOfContainer(entry: Widget) = scrollY > entry.bounds.bottom || scrollY + viewport.height < entry.top
}
