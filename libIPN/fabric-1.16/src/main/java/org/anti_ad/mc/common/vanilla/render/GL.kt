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

package org.anti_ad.mc.common.vanilla.render

import org.anti_ad.mc.common.gui.NativeContext
import org.anti_ad.mc.common.math2d.Rectangle
import org.anti_ad.mc.common.math2d.intersect
import org.anti_ad.mc.common.vanilla.alias.DiffuseLighting
import org.anti_ad.mc.common.vanilla.alias.DstFactor
import org.anti_ad.mc.common.vanilla.alias.MatrixStack
import org.anti_ad.mc.common.vanilla.alias.RenderSystem
import org.anti_ad.mc.common.vanilla.alias.SrcFactor
import org.anti_ad.mc.common.vanilla.render.glue.rFillRect
import org.anti_ad.mc.common.vanilla.render.glue.rScreenBounds
import org.lwjgl.opengl.GL11

// ============
// api
// ============
// at Screen.render()
// do: rStandardGlState(); rClearDepth()
fun rStandardGlState() { // reset to standard state (for screen rendering)
    rEnableBlend()
    gDisableDiffuse()
    gEnableAlphaTest()
    gEnableDepthTest()
    RenderSystem.depthMask(true)
}

// ============
// depth
// ============

fun rClearDepth(context: NativeContext) {
    gEnableDepthTest()
    RenderSystem.depthMask(true)
    RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,
                       false)
    rOverwriteDepth(context,
                    rScreenBounds)
    depthBounds.clear() // added this
}

inline fun rDepthMask(context: NativeContext,
                      bounds: Rectangle,
                      block: () -> Unit) {
    //rDrawOutline(bounds, -6710887)
    rCreateDepthMask(context,
                     bounds)
    block()
    rRemoveDepthMask(context)
}

private val depthBounds = mutableListOf<Rectangle>()

//https://stackoverflow.com/questions/13742556/best-approach-to-draw-clipped-ui-elements-in-opengl
// can it be done without stencil?
// (maybe yes, if rectangle mask only)
fun rCreateDepthMask(context: NativeContext,
                     bounds: Rectangle) {
    rStandardGlState() // added this
    if (depthBounds.isEmpty()) {
        rCreateDepthMaskNoCheck(context,
                                bounds)
    } else {
        //rCreateDepthMaskNoCheck(depthBounds.last().intersect(bounds))
        rCreateDepthMaskNoCheck(context,
                                depthBounds.last().intersect(bounds))
    }
}

private fun rCreateDepthMaskNoCheck(context: NativeContext,
                                    bounds: Rectangle) {
    depthBounds.add(bounds)
    gPushMatrix()
    gTranslatef(0f,
                0f,
                -400.0f)
    rOverwriteDepth(context,
                    bounds)
}

fun rRemoveDepthMask(context: NativeContext) {
    //rStandardGlState() // added this
    gPopMatrix()
    rOverwriteDepth(context,
                    depthBounds.removeLast())
}

private fun rOverwriteDepth(context: NativeContext,
                            bounds: Rectangle) {
//  rEnableDepth()
    gDepthFunc(GL11.GL_ALWAYS)
    gDisableAlphaTest()
    rFillRect(context,
              bounds,
              0)
    gEnableAlphaTest()
    gDepthFunc(GL11.GL_LEQUAL)
}

fun rDisableDepth() { // todo see if same with disableDepthTest (?)
    gDepthFunc(GL11.GL_ALWAYS)
    RenderSystem.depthMask(false)
}

fun rEnableDepth() {
    RenderSystem.depthMask(true)
    gDepthFunc(GL11.GL_LEQUAL)
}

// ============
// matrix
// ============


@SuppressWarnings("Deprecated")
fun gPushMatrix() = RenderSystem.pushMatrix()

@SuppressWarnings("Deprecated")
fun gPopMatrix() = RenderSystem.popMatrix()

//fun gLoadIdentity() = RenderSystem.loadIdentity()
fun gTranslatef(x: Float,
                y: Float,
                z: Float) = RenderSystem.translatef(x,
                                                    y,
                                                    z)

// ============
// internal
// ============
private fun rEnableBlend() {
    // ref: AbstractButtonWidget.renderButton()
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    RenderSystem.blendFunc(SrcFactor.SRC_ALPHA,
                           DstFactor.ONE_MINUS_SRC_ALPHA)
    RenderSystem.color4f(1f,
                         1f,
                         1f,
                         1f)

}

// ============
// GlStateManager
// ============

private fun gDisableDiffuse() = DiffuseLighting.disable()
private fun gDisableAlphaTest() = RenderSystem.disableAlphaTest()
private fun gEnableAlphaTest() = RenderSystem.enableAlphaTest()
private fun gDisableDepthTest() = RenderSystem.disableDepthTest()
private fun gEnableDepthTest() = RenderSystem.enableDepthTest()
private fun gDepthFunc(value: Int) { // default = GL_LEQUAL = 515
    RenderSystem.depthFunc(value)
}
