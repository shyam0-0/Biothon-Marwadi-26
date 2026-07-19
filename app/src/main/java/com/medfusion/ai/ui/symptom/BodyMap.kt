package com.medfusion.ai.ui.symptom

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.medfusion.ai.domain.model.BodyRegion
import com.medfusion.ai.domain.model.BodyView
import com.medfusion.ai.domain.model.SymptomLocalizationOptions
import com.medfusion.ai.domain.model.SymptomLocation
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.theme.Spacing

/**
 * Smart pain & symptom localization (Phase 5.6, visuals Phase 6.6): a simple
 * multi-view body map built from tappable predefined regions — deliberately
 * NOT an anatomy viewer, just an intuitive way to say where it hurts. The same
 * map renders read-only for the doctor's pre-read.
 *
 * Rendering: the four silhouettes are the actual traced contours of the
 * project's reference illustrations (front.svg / back.svg / left.svg /
 * right.svg), normalized to a 0.38-aspect canvas — so the on-screen figures
 * match the references, including the hands. Faint divider lines hint the
 * selectable regions; pressing a region brightens it lightly before selection.
 * Region mapping, tap targets and interaction logic are unchanged.
 */

/** One tappable region: normalized polygon used for the highlight + its tap bounds. */
private class RegionShape(
    val region: BodyRegion,
    val bounds: Rect,
    val part: String,
    val build: (Size) -> Path,
)

/** A faint internal line separating adjacent regions (open smooth polyline). */
private class Divider(val pts: List<Pair<Float, Float>>, val part: String)

/** One connected silhouette piece. Drawn in list order — later pieces sit on top. */
private class BodyPart(val key: String, val outline: List<Pair<Float, Float>>)

/** Everything needed to draw one viewpoint. */
private class BodyViewSpec(
    val parts: List<BodyPart>,
    val dividers: List<Divider>,
    val regions: List<RegionShape>,
)

private fun mirrorList(pts: List<Pair<Float, Float>>): List<Pair<Float, Float>> =
    pts.map { (1f - it.first) to it.second }

private fun mirror(pts: Array<Pair<Float, Float>>): Array<Pair<Float, Float>> =
    pts.map { (1f - it.first) to it.second }.toTypedArray()

private fun List<Pair<Float, Float>>.scaled(size: Size): List<Offset> =
    map { Offset(it.first * size.width, it.second * size.height) }

/** Unpacks x0,y0,x1,y1… literals produced from the traced reference contours. */
private fun tracedPts(a: FloatArray): List<Pair<Float, Float>> =
    List(a.size / 2) { a[it * 2] to a[it * 2 + 1] }

/** Closed Catmull-Rom spline: a smooth, organic outline through the anchors. */
private fun smoothClosed(pts: List<Offset>): Path {
    val path = Path()
    val n = pts.size
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until n) {
        val p0 = pts[(i - 1 + n) % n]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % n]
        val p3 = pts[(i + 2) % n]
        val c1 = p1 + (p2 - p0) * (1f / 6f)
        val c2 = p2 - (p3 - p1) * (1f / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    path.close()
    return path
}

/** Open Catmull-Rom spline for divider lines. */
private fun smoothOpen(pts: List<Offset>): Path {
    val path = Path()
    if (pts.size < 2) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val p0 = pts[maxOf(i - 1, 0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[minOf(i + 2, pts.size - 1)]
        val c1 = p1 + (p2 - p0) * (1f / 6f)
        val c2 = p2 - (p3 - p1) * (1f / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** Softly-rounded polygon for a region highlight (its edges get clipped to its body part). */
private fun roundedPoly(pts: List<Offset>, radius: Float): Path {
    val path = Path()
    for (i in pts.indices) {
        val prev = pts[(i + pts.size - 1) % pts.size]
        val cur = pts[i]
        val next = pts[(i + 1) % pts.size]
        val fromPrev = cur - prev
        val toNext = next - cur
        val lenPrev = fromPrev.getDistance()
        val lenNext = toNext.getDistance()
        val start = cur - fromPrev * (minOf(radius, lenPrev / 2f) / lenPrev)
        val end = cur + toNext * (minOf(radius, lenNext / 2f) / lenNext)
        if (i == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        path.quadraticBezierTo(cur.x, cur.y, end.x, end.y)
    }
    path.close()
    return path
}

/** Region from normalized polygon points; [r] is corner rounding as a fraction of map width. */
private fun shape(region: BodyRegion, part: String, r: Float, vararg pts: Pair<Float, Float>): RegionShape {
    val bounds = Rect(
        pts.minOf { it.first }, pts.minOf { it.second },
        pts.maxOf { it.first }, pts.maxOf { it.second },
    )
    return RegionShape(region, bounds, part) { size ->
        roundedPoly(
            pts.map { Offset(it.first * size.width, it.second * size.height) },
            r * size.width,
        )
    }
}

/** Oval region (head), optionally cut horizontally so head/face can share one ellipse. */
private fun ellipseShape(
    region: BodyRegion,
    part: String,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    clipTop: Float = Float.NEGATIVE_INFINITY,
    clipBottom: Float = Float.POSITIVE_INFINITY,
): RegionShape {
    val boundsTop = maxOf(top, clipTop)
    val boundsBottom = minOf(bottom, clipBottom)
    return RegionShape(region, Rect(left, boundsTop, right, boundsBottom), part) { size ->
        val oval = Path().apply {
            addOval(Rect(left * size.width, top * size.height, right * size.width, bottom * size.height))
        }
        if (clipTop <= top && clipBottom >= bottom) {
            oval
        } else {
            val clip = Path().apply {
                addRect(Rect(0f, boundsTop * size.height, size.width, boundsBottom * size.height))
            }
            Path.combine(PathOperation.Intersect, oval, clip)
        }
    }
}

// ── Traced silhouette contours (from front/back/left/right.svg) ─────────────
// Coordinates are canvas-normalized: x for a 0.38 width/height aspect, y 0..1.

private val FRONT_OUTLINE_PTS = floatArrayOf(
    0.4942f, -0.0004f, 0.5429f, 0.0018f, 0.5758f, 0.0078f, 0.6079f, 0.02f, 0.6234f, 0.0333f,
    0.6274f, 0.0591f, 0.6411f, 0.0636f, 0.645f, 0.0724f, 0.6332f, 0.0894f, 0.6274f, 0.0939f,
    0.6079f, 0.0998f, 0.6003f, 0.1116f, 0.5924f, 0.116f, 0.5963f, 0.1449f, 0.6147f, 0.1504f,
    0.6984f, 0.1644f, 0.7937f, 0.1748f, 0.8316f, 0.1863f, 0.8589f, 0.204f, 0.8724f, 0.2247f,
    0.8745f, 0.2439f, 0.8687f, 0.2602f, 0.8842f, 0.2942f, 0.8882f, 0.3245f, 0.8997f, 0.3429f,
    0.9192f, 0.3622f, 0.9308f, 0.3858f, 0.9405f, 0.4826f, 0.9582f, 0.5262f, 0.9521f, 0.5425f,
    0.9289f, 0.5684f, 0.8813f, 0.5835f, 0.8658f, 0.5857f, 0.855f, 0.5824f, 0.855f, 0.575f,
    0.8861f, 0.5617f, 0.8918f, 0.5558f, 0.8918f, 0.5418f, 0.8871f, 0.5399f, 0.8821f, 0.5418f,
    0.8803f, 0.5521f, 0.8637f, 0.5599f, 0.8463f, 0.5591f, 0.8374f, 0.5521f, 0.8413f, 0.5122f,
    0.8626f, 0.4915f, 0.8568f, 0.4767f, 0.8337f, 0.4472f, 0.785f, 0.3984f, 0.7732f, 0.3769f,
    0.7713f, 0.3474f, 0.7403f, 0.3008f, 0.7353f, 0.299f, 0.7305f, 0.3008f, 0.7071f, 0.3363f,
    0.7013f, 0.3666f, 0.7129f, 0.4021f, 0.7111f, 0.4154f, 0.7345f, 0.4412f, 0.7539f, 0.4789f,
    0.7634f, 0.5218f, 0.7634f, 0.5558f, 0.75f, 0.6053f, 0.715f, 0.6674f, 0.7129f, 0.711f,
    0.7284f, 0.7517f, 0.7266f, 0.7805f, 0.68f, 0.8714f, 0.6703f, 0.9024f, 0.6779f, 0.9187f,
    0.6761f, 0.9342f, 0.7092f, 0.9557f, 0.7421f, 0.9697f, 0.75f, 0.9756f, 0.7518f, 0.9837f,
    0.7392f, 0.9908f, 0.7218f, 0.9945f, 0.7061f, 0.9952f, 0.6905f, 0.9989f, 0.6692f, 0.9974f,
    0.6555f, 1.0004f, 0.6382f, 1.0004f, 0.6147f, 0.9967f, 0.6003f, 0.9904f, 0.5884f, 0.9793f,
    0.5632f, 0.9704f, 0.5555f, 0.9623f, 0.5653f, 0.9372f, 0.5592f, 0.9187f, 0.5711f, 0.8995f,
    0.5729f, 0.8817f, 0.5711f, 0.8507f, 0.5653f, 0.833f, 0.5458f, 0.8012f, 0.5437f, 0.7731f,
    0.5653f, 0.7214f, 0.5418f, 0.68f, 0.5361f, 0.6275f, 0.5108f, 0.5713f, 0.5068f, 0.5551f,
    0.5087f, 0.5233f, 0.5039f, 0.5214f, 0.4892f, 0.5233f, 0.4913f, 0.558f, 0.4874f, 0.5743f,
    0.4639f, 0.626f, 0.4563f, 0.6822f, 0.4347f, 0.7184f, 0.4563f, 0.779f, 0.4542f, 0.8004f,
    0.4347f, 0.833f, 0.4271f, 0.8581f, 0.4271f, 0.8936f, 0.4408f, 0.9202f, 0.4347f, 0.9416f,
    0.4426f, 0.9645f, 0.4368f, 0.9704f, 0.4095f, 0.98f, 0.3979f, 0.9911f, 0.3676f, 0.9996f,
    0.3484f, 1.0004f, 0.3289f, 0.9974f, 0.3113f, 0.9989f, 0.2803f, 0.9952f, 0.2684f, 0.993f,
    0.2461f, 0.983f, 0.2558f, 0.9704f, 0.2947f, 0.9534f, 0.3221f, 0.935f, 0.32f, 0.9187f,
    0.3279f, 0.9047f, 0.3124f, 0.8551f, 0.2871f, 0.8137f, 0.2716f, 0.7775f, 0.2716f, 0.7472f,
    0.285f, 0.7162f, 0.2832f, 0.6644f, 0.2482f, 0.6031f, 0.2345f, 0.5455f, 0.2461f, 0.4789f,
    0.2579f, 0.4523f, 0.2889f, 0.4139f, 0.2871f, 0.3954f, 0.2987f, 0.3651f, 0.2947f, 0.3407f,
    0.2695f, 0.303f, 0.2626f, 0.3004f, 0.2579f, 0.3023f, 0.2287f, 0.3466f, 0.2268f, 0.3755f,
    0.215f, 0.3976f, 0.1645f, 0.4486f, 0.1374f, 0.4863f, 0.1392f, 0.4952f, 0.1547f, 0.507f,
    0.1605f, 0.5174f, 0.1566f, 0.5299f, 0.1605f, 0.5543f, 0.1566f, 0.558f, 0.1461f, 0.5606f,
    0.1324f, 0.5591f, 0.1197f, 0.5521f, 0.1158f, 0.5418f, 0.1111f, 0.5399f, 0.1061f, 0.5418f,
    0.11f, 0.5595f, 0.1392f, 0.5721f, 0.1471f, 0.5802f, 0.1403f, 0.5843f, 0.1247f, 0.585f,
    0.0732f, 0.5691f, 0.0613f, 0.561f, 0.0418f, 0.5344f, 0.0613f, 0.473f, 0.0692f, 0.3821f,
    0.0789f, 0.3644f, 0.1082f, 0.3333f, 0.1179f, 0.2868f, 0.1295f, 0.2661f, 0.1255f, 0.2313f,
    0.1334f, 0.2121f, 0.1605f, 0.1899f, 0.1966f, 0.177f, 0.2258f, 0.1718f, 0.2803f, 0.1674f,
    0.3405f, 0.1585f, 0.3853f, 0.1504f, 0.4058f, 0.1434f, 0.4076f, 0.116f, 0.3979f, 0.1094f,
    0.3921f, 0.099f, 0.3745f, 0.0946f, 0.3647f, 0.0872f, 0.355f, 0.0732f, 0.3571f, 0.0658f,
    0.3726f, 0.0591f, 0.3745f, 0.0362f, 0.39f, 0.0214f, 0.4124f, 0.0115f, 0.4534f, 0.0026f,
    0.4942f, -0.0004f,
)

private val BACK_OUTLINE_PTS = floatArrayOf(
    0.4982f, -0.0004f, 0.5534f, 0.004f, 0.5821f, 0.0105f, 0.6082f, 0.0218f, 0.6234f, 0.0356f,
    0.6271f, 0.0451f, 0.6234f, 0.0625f, 0.6405f, 0.0676f, 0.6424f, 0.0792f, 0.6271f, 0.0945f,
    0.6042f, 0.1017f, 0.5984f, 0.1156f, 0.6024f, 0.1374f, 0.6205f, 0.1435f, 0.7389f, 0.1653f,
    0.7734f, 0.1682f, 0.8155f, 0.1748f, 0.8529f, 0.1868f, 0.8758f, 0.202f, 0.8874f, 0.2202f,
    0.8853f, 0.25f, 0.9026f, 0.2776f, 0.9121f, 0.3219f, 0.9542f, 0.3874f, 0.9637f, 0.4717f,
    0.9905f, 0.5407f, 0.9637f, 0.5734f, 0.9379f, 0.5839f, 0.9055f, 0.5934f, 0.8903f, 0.5948f,
    0.8797f, 0.5908f, 0.8776f, 0.5858f, 0.9103f, 0.561f, 0.9103f, 0.5487f, 0.9055f, 0.5469f,
    0.9008f, 0.5487f, 0.8968f, 0.5589f, 0.8824f, 0.5658f, 0.8634f, 0.565f, 0.8566f, 0.5574f,
    0.8624f, 0.5429f, 0.8624f, 0.5182f, 0.8853f, 0.4985f, 0.8853f, 0.4898f, 0.8624f, 0.4615f,
    0.8068f, 0.4092f, 0.7937f, 0.3874f, 0.7821f, 0.3452f, 0.7534f, 0.3103f, 0.7487f, 0.3085f,
    0.7418f, 0.311f, 0.7153f, 0.3488f, 0.7132f, 0.3663f, 0.7247f, 0.3953f, 0.7247f, 0.4113f,
    0.7534f, 0.4491f, 0.7705f, 0.4869f, 0.7803f, 0.5429f, 0.7687f, 0.5974f, 0.7305f, 0.673f,
    0.7284f, 0.7202f, 0.7476f, 0.7624f, 0.7476f, 0.7972f, 0.7018f, 0.8757f, 0.6921f, 0.9281f,
    0.6997f, 0.939f, 0.6961f, 0.9506f, 0.7276f, 0.9626f, 0.7582f, 0.9684f, 0.7668f, 0.9724f,
    0.7687f, 0.9782f, 0.7563f, 0.9851f, 0.7334f, 0.9895f, 0.6989f, 0.9916f, 0.6453f, 1.0004f,
    0.5918f, 0.9975f, 0.5774f, 0.992f, 0.5718f, 0.9855f, 0.5795f, 0.9637f, 0.5795f, 0.9491f,
    0.5718f, 0.936f, 0.5832f, 0.915f, 0.5832f, 0.8837f, 0.5755f, 0.8517f, 0.5468f, 0.7958f,
    0.5487f, 0.7733f, 0.5621f, 0.7442f, 0.5639f, 0.7289f, 0.5411f, 0.6882f, 0.5334f, 0.6323f,
    0.5124f, 0.5901f, 0.5047f, 0.564f, 0.5047f, 0.5385f, 0.5105f, 0.5262f, 0.5f, 0.5222f,
    0.4895f, 0.5262f, 0.4934f, 0.5378f, 0.4913f, 0.5705f, 0.4626f, 0.6286f, 0.4532f, 0.6868f,
    0.4303f, 0.7246f, 0.4321f, 0.7456f, 0.4455f, 0.7783f, 0.4455f, 0.7994f, 0.4187f, 0.8496f,
    0.4111f, 0.875f, 0.4111f, 0.9201f, 0.4205f, 0.9317f, 0.4205f, 0.9404f, 0.4129f, 0.9491f,
    0.4129f, 0.9637f, 0.4205f, 0.976f, 0.4205f, 0.9876f, 0.415f, 0.9927f, 0.3968f, 0.9982f,
    0.3489f, 1.0004f, 0.3221f, 0.9975f, 0.2953f, 0.9916f, 0.2611f, 0.9895f, 0.2324f, 0.9836f,
    0.2237f, 0.9767f, 0.2274f, 0.9717f, 0.2361f, 0.9684f, 0.2687f, 0.9618f, 0.2982f, 0.9506f,
    0.2924f, 0.9397f, 0.3003f, 0.9288f, 0.3003f, 0.907f, 0.2905f, 0.8743f, 0.2447f, 0.7965f,
    0.2447f, 0.7631f, 0.2637f, 0.7224f, 0.2618f, 0.6693f, 0.2274f, 0.5996f, 0.2161f, 0.5531f,
    0.2179f, 0.5174f, 0.2295f, 0.4804f, 0.2484f, 0.4419f, 0.2734f, 0.4135f, 0.2734f, 0.3961f,
    0.2847f, 0.3706f, 0.2829f, 0.3488f, 0.2561f, 0.3089f, 0.2513f, 0.307f, 0.2447f, 0.3103f,
    0.2121f, 0.3517f, 0.2084f, 0.3815f, 0.1911f, 0.4099f, 0.1337f, 0.4637f, 0.1147f, 0.4891f,
    0.1147f, 0.5f, 0.1318f, 0.5116f, 0.1376f, 0.5203f, 0.1358f, 0.5407f, 0.1413f, 0.5603f,
    0.1329f, 0.5658f, 0.1137f, 0.565f, 0.1032f, 0.5596f, 0.0992f, 0.5465f, 0.0945f, 0.5447f,
    0.0897f, 0.5465f, 0.0897f, 0.5603f, 0.1203f, 0.5843f, 0.1203f, 0.5908f, 0.1118f, 0.5948f,
    0.0966f, 0.5941f, 0.0382f, 0.5741f, 0.0132f, 0.5465f, 0.0095f, 0.5349f, 0.0266f, 0.5007f,
    0.0363f, 0.4658f, 0.0458f, 0.3859f, 0.0861f, 0.3249f, 0.0955f, 0.2812f, 0.1147f, 0.2485f,
    0.1108f, 0.2267f, 0.1203f, 0.2057f, 0.1453f, 0.1875f, 0.1805f, 0.1755f, 0.2132f, 0.1697f,
    0.2687f, 0.1639f, 0.37f, 0.1457f, 0.3995f, 0.1374f, 0.4053f, 0.125f, 0.3976f, 0.101f,
    0.3787f, 0.0967f, 0.3576f, 0.0763f, 0.3613f, 0.0683f, 0.3787f, 0.0632f, 0.3747f, 0.0501f,
    0.3824f, 0.0312f, 0.3976f, 0.0196f, 0.4255f, 0.0091f, 0.4542f, 0.0033f, 0.4982f, -0.0004f,
)

private val LEFT_OUTLINE_PTS = floatArrayOf(
    0.5137f, -0.0006f, 0.5803f, 0.0006f, 0.6258f, 0.0075f, 0.6634f, 0.0207f, 0.6816f, 0.0334f,
    0.6908f, 0.046f, 0.6876f, 0.0713f, 0.6666f, 0.0898f, 0.6392f, 0.1024f, 0.6332f, 0.1093f,
    0.6332f, 0.1254f, 0.6424f, 0.1415f, 0.7089f, 0.1853f, 0.7211f, 0.2002f, 0.7303f, 0.2232f,
    0.7242f, 0.2624f, 0.6634f, 0.3498f, 0.6513f, 0.3947f, 0.6605f, 0.4166f, 0.6816f, 0.4304f,
    0.7061f, 0.4557f, 0.7121f, 0.4672f, 0.7121f, 0.4868f, 0.7029f, 0.5017f, 0.6605f, 0.5305f,
    0.6545f, 0.5397f, 0.6574f, 0.5961f, 0.6424f, 0.6421f, 0.6271f, 0.6697f, 0.6303f, 0.7054f,
    0.6695f, 0.7503f, 0.6847f, 0.7779f, 0.6847f, 0.8009f, 0.6605f, 0.847f, 0.6453f, 0.9137f,
    0.6484f, 0.9356f, 0.6695f, 0.9597f, 0.6755f, 0.977f, 0.6695f, 0.9885f, 0.6561f, 0.9937f,
    0.6347f, 0.9971f, 0.5076f, 0.996f, 0.4439f, 1.0006f, 0.3955f, 1.0006f, 0.3258f, 0.9983f,
    0.2955f, 0.9948f, 0.2729f, 0.9873f, 0.2697f, 0.9793f, 0.2834f, 0.9741f, 0.3197f, 0.973f,
    0.3682f, 0.9672f, 0.4742f, 0.9453f, 0.5061f, 0.931f, 0.5182f, 0.9125f, 0.5213f, 0.8918f,
    0.5121f, 0.8527f, 0.4879f, 0.8021f, 0.4787f, 0.7491f, 0.4395f, 0.7043f, 0.4395f, 0.6789f,
    0.4334f, 0.6663f, 0.3939f, 0.6203f, 0.3668f, 0.5685f, 0.3637f, 0.5155f, 0.3789f, 0.4776f,
    0.3789f, 0.4603f, 0.3547f, 0.4223f, 0.3547f, 0.3924f, 0.3608f, 0.3751f, 0.3426f, 0.3176f,
    0.3487f, 0.2808f, 0.3366f, 0.267f, 0.3395f, 0.252f, 0.3576f, 0.2325f, 0.3879f, 0.2117f,
    0.4666f, 0.1692f, 0.4697f, 0.1577f, 0.4576f, 0.1438f, 0.4545f, 0.1335f, 0.4471f, 0.1306f,
    0.3955f, 0.1318f, 0.3789f, 0.1277f, 0.3637f, 0.1001f, 0.3668f, 0.0967f, 0.3426f, 0.0886f,
    0.3426f, 0.0817f, 0.3637f, 0.0679f, 0.3758f, 0.0391f, 0.3971f, 0.023f, 0.4379f, 0.0086f,
    0.4803f, 0.0017f, 0.5137f, -0.0006f,
)

private val RIGHT_OUTLINE_PTS = floatArrayOf(
    0.4297f, -0.0006f, 0.4816f, -0.0006f, 0.5153f, 0.0029f, 0.5518f, 0.0099f, 0.5839f, 0.022f,
    0.6053f, 0.0394f, 0.6113f, 0.0615f, 0.6389f, 0.08f, 0.6358f, 0.0847f, 0.6145f, 0.0905f,
    0.6176f, 0.0963f, 0.6113f, 0.1009f, 0.6053f, 0.1218f, 0.5884f, 0.1259f, 0.5397f, 0.1247f,
    0.5289f, 0.1288f, 0.5168f, 0.152f, 0.5229f, 0.1659f, 0.5871f, 0.2019f, 0.6205f, 0.2251f,
    0.6389f, 0.2436f, 0.645f, 0.2587f, 0.6329f, 0.2749f, 0.6358f, 0.3248f, 0.6237f, 0.3631f,
    0.6297f, 0.4153f, 0.6084f, 0.4443f, 0.6024f, 0.4652f, 0.6053f, 0.4849f, 0.6176f, 0.5116f,
    0.6176f, 0.558f, 0.5992f, 0.6032f, 0.5474f, 0.6705f, 0.5474f, 0.6995f, 0.5076f, 0.7459f,
    0.4955f, 0.8074f, 0.4771f, 0.8445f, 0.465f, 0.8852f, 0.4711f, 0.9188f, 0.4892f, 0.9374f,
    0.5061f, 0.9437f, 0.6253f, 0.9681f, 0.6647f, 0.9727f, 0.7076f, 0.9739f, 0.7305f, 0.978f,
    0.7334f, 0.9861f, 0.7105f, 0.9948f, 0.6253f, 1.0006f, 0.5397f, 1.0006f, 0.4634f, 0.9959f,
    0.3595f, 0.9983f, 0.3261f, 0.9948f, 0.3092f, 0.9838f, 0.3124f, 0.9629f, 0.3366f, 0.9374f,
    0.3397f, 0.9153f, 0.3245f, 0.8445f, 0.3032f, 0.8074f, 0.3f, 0.7842f, 0.3184f, 0.7471f,
    0.355f, 0.7042f, 0.3582f, 0.6694f, 0.3305f, 0.609f, 0.3305f, 0.536f, 0.3245f, 0.5267f,
    0.2908f, 0.5058f, 0.2755f, 0.4838f, 0.2755f, 0.4675f, 0.2847f, 0.4501f, 0.3366f, 0.4014f,
    0.3366f, 0.3677f, 0.3213f, 0.3329f, 0.2726f, 0.2599f, 0.2666f, 0.2436f, 0.2666f, 0.2158f,
    0.2847f, 0.1821f, 0.3061f, 0.1636f, 0.3397f, 0.145f, 0.3582f, 0.1265f, 0.355f, 0.0998f,
    0.3245f, 0.0835f, 0.3061f, 0.0661f, 0.3061f, 0.0441f, 0.3124f, 0.0348f, 0.3337f, 0.0186f,
    0.3566f, 0.0099f, 0.3839f, 0.0041f, 0.4297f, -0.0006f,
)

// ── Region highlight bands (fitted to the traced anatomy's landmarks) ───────
// Measured on the references: neck base y≈0.168, shoulders 0.155–0.235,
// waist≈0.345, hips 0.41–0.475, crotch≈0.53, wrists≈0.485, hands to 0.60,
// knees 0.66–0.72, ankles≈0.92.

private val SHOULDER_PTS = arrayOf(.105f to .155f, .37f to .155f, .37f to .235f, .105f to .235f)
private val ARM_PTS = arrayOf(.03f to .235f, .27f to .235f, .27f to .485f, .03f to .485f)
private val HAND_PTS = arrayOf(.02f to .485f, .28f to .485f, .28f to .60f, .02f to .60f)
private val LEG_UPPER_PTS = arrayOf(.19f to .478f, .50f to .478f, .50f to .66f, .19f to .66f)
private val KNEE_PTS = arrayOf(.21f to .66f, .49f to .66f, .49f to .72f, .21f to .72f)
private val LEG_LOWER_PTS = arrayOf(.22f to .72f, .48f to .72f, .48f to .92f, .22f to .92f)
private val FOOT_PTS = arrayOf(.21f to .92f, .50f to .92f, .50f to 1.0f, .21f to 1.0f)
private val HIP_PTS = arrayOf(.20f to .41f, .42f to .41f, .43f to .478f, .28f to .478f, .20f to .45f)
private val BACK_HIP_PTS = arrayOf(.20f to .41f, .50f to .41f, .50f to .53f, .28f to .52f, .20f to .45f)
private val BACK_LEG_PTS = arrayOf(.19f to .478f, .50f to .478f, .50f to .92f, .19f to .92f)

// ── Divider lines ────────────────────────────────────────────────────────────

private val LIMB_DIVIDERS: List<Divider> = buildList {
    val shoulderArm = listOf(.12f to .233f, .20f to .241f, .27f to .233f)
    add(Divider(shoulderArm, "BODY")); add(Divider(mirrorList(shoulderArm), "BODY"))
    val wrist = listOf(.06f to .483f, .15f to .49f, .245f to .48f)
    add(Divider(wrist, "BODY")); add(Divider(mirrorList(wrist), "BODY"))
    val hipLeg = listOf(.21f to .475f, .35f to .488f, .485f to .482f)
    add(Divider(hipLeg, "BODY")); add(Divider(mirrorList(hipLeg), "BODY"))
    val ankle = listOf(.25f to .92f, .35f to .927f, .44f to .917f)
    add(Divider(ankle, "BODY")); add(Divider(mirrorList(ankle), "BODY"))
}

private val FRONT_DIVIDERS: List<Divider> = buildList {
    add(Divider(listOf(.40f to .065f, .500f to .069f, .60f to .065f), "BODY"))    // head | face
    add(Divider(listOf(.42f to .122f, .500f to .127f, .58f to .122f), "BODY"))    // face | neck
    add(Divider(listOf(.40f to .168f, .500f to .173f, .60f to .168f), "BODY"))    // neck | chest
    val shoulderChest = listOf(.43f to .172f, .36f to .19f, .30f to .233f)
    add(Divider(shoulderChest, "BODY")); add(Divider(mirrorList(shoulderChest), "BODY"))
    add(Divider(listOf(.26f to .28f, .500f to .287f, .74f to .28f), "BODY"))      // chest | upper abd
    add(Divider(listOf(.27f to .345f, .500f to .352f, .73f to .345f), "BODY"))    // upper | lower abd
    add(Divider(listOf(.24f to .41f, .500f to .417f, .76f to .41f), "BODY"))      // abdomen | hips
    val hipPelvis = listOf(.42f to .412f, .425f to .445f, .43f to .475f)
    add(Divider(hipPelvis, "BODY")); add(Divider(mirrorList(hipPelvis), "BODY"))
    val kneeTop = listOf(.23f to .66f, .35f to .668f, .475f to .66f)
    add(Divider(kneeTop, "BODY")); add(Divider(mirrorList(kneeTop), "BODY"))
    val kneeBot = listOf(.24f to .72f, .35f to .727f, .465f to .72f)
    add(Divider(kneeBot, "BODY")); add(Divider(mirrorList(kneeBot), "BODY"))
    addAll(LIMB_DIVIDERS)
}

private val BACK_DIVIDERS: List<Divider> = buildList {
    add(Divider(listOf(.42f to .122f, .500f to .127f, .58f to .122f), "BODY"))    // head | neck
    add(Divider(listOf(.40f to .168f, .500f to .173f, .60f to .168f), "BODY"))    // neck | back
    val shoulderBack = listOf(.43f to .172f, .36f to .19f, .30f to .233f)
    add(Divider(shoulderBack, "BODY")); add(Divider(mirrorList(shoulderBack), "BODY"))
    add(Divider(listOf(.26f to .35f, .500f to .357f, .74f to .35f), "BODY"))      // back | lower back
    add(Divider(listOf(.24f to .41f, .500f to .417f, .76f to .41f), "BODY"))      // lower back | hips
    add(Divider(listOf(.500f to .175f, .496f to .29f, .500f to .408f), "BODY"))   // spine centerline
    add(Divider(listOf(.500f to .412f, .500f to .53f), "BODY"))                   // hip split
    addAll(LIMB_DIVIDERS)
}

// ── Regions per view (order also resolves overlapping tap targets: later wins) ──

private val FRONT_REGIONS = listOf(
    ellipseShape(BodyRegion.HEAD, "BODY", .355f, -.005f, .645f, .135f, clipBottom = .065f),
    ellipseShape(BodyRegion.FACE, "BODY", .355f, -.005f, .645f, .135f, clipTop = .065f),
    shape(BodyRegion.NECK, "BODY", .02f, .382f to .115f, .618f to .115f, .625f to .168f, .375f to .168f),
    shape(BodyRegion.CHEST, "BODY", .03f, .25f to .168f, .75f to .168f, .75f to .28f, .25f to .28f),
    shape(BodyRegion.RIGHT_SHOULDER, "BODY", .025f, *SHOULDER_PTS),
    shape(BodyRegion.LEFT_SHOULDER, "BODY", .025f, *mirror(SHOULDER_PTS)),
    shape(BodyRegion.UPPER_ABDOMEN, "BODY", .03f, .26f to .28f, .74f to .28f, .74f to .345f, .26f to .345f),
    shape(BodyRegion.LOWER_ABDOMEN, "BODY", .03f, .26f to .345f, .74f to .345f, .74f to .41f, .26f to .41f),
    shape(BodyRegion.RIGHT_HIP, "BODY", .025f, *HIP_PTS),
    shape(BodyRegion.LEFT_HIP, "BODY", .025f, *mirror(HIP_PTS)),
    shape(
        BodyRegion.PELVIS, "BODY", .03f,
        .42f to .41f, .58f to .41f, .56f to .48f, .50f to .535f, .44f to .48f,
    ),
    shape(BodyRegion.RIGHT_ARM, "BODY", .025f, *ARM_PTS),
    shape(BodyRegion.LEFT_ARM, "BODY", .025f, *mirror(ARM_PTS)),
    shape(BodyRegion.RIGHT_HAND, "BODY", .03f, *HAND_PTS),
    shape(BodyRegion.LEFT_HAND, "BODY", .03f, *mirror(HAND_PTS)),
    shape(BodyRegion.RIGHT_LEG, "BODY", .025f, *LEG_UPPER_PTS),
    shape(BodyRegion.LEFT_LEG, "BODY", .025f, *mirror(LEG_UPPER_PTS)),
    shape(BodyRegion.RIGHT_KNEE, "BODY", .02f, *KNEE_PTS),
    shape(BodyRegion.LEFT_KNEE, "BODY", .02f, *mirror(KNEE_PTS)),
    shape(BodyRegion.RIGHT_LEG, "BODY", .025f, *LEG_LOWER_PTS),
    shape(BodyRegion.LEFT_LEG, "BODY", .025f, *mirror(LEG_LOWER_PTS)),
    shape(BodyRegion.RIGHT_FOOT, "BODY", .025f, *FOOT_PTS),
    shape(BodyRegion.LEFT_FOOT, "BODY", .025f, *mirror(FOOT_PTS)),
)

private val BACK_REGIONS = listOf(
    ellipseShape(BodyRegion.HEAD, "BODY", .355f, -.005f, .645f, .135f),
    shape(BodyRegion.NECK, "BODY", .02f, .382f to .112f, .618f to .112f, .625f to .168f, .375f to .168f),
    shape(BodyRegion.BACK_UPPER, "BODY", .03f, .24f to .168f, .76f to .168f, .76f to .35f, .24f to .35f),
    shape(BodyRegion.LEFT_SHOULDER, "BODY", .025f, *SHOULDER_PTS),
    shape(BodyRegion.RIGHT_SHOULDER, "BODY", .025f, *mirror(SHOULDER_PTS)),
    shape(BodyRegion.LOWER_BACK, "BODY", .03f, .26f to .35f, .74f to .35f, .74f to .41f, .26f to .41f),
    shape(BodyRegion.LEFT_HIP, "BODY", .03f, *BACK_HIP_PTS),
    shape(BodyRegion.RIGHT_HIP, "BODY", .03f, *mirror(BACK_HIP_PTS)),
    shape(BodyRegion.LEFT_ARM, "BODY", .025f, *ARM_PTS),
    shape(BodyRegion.RIGHT_ARM, "BODY", .025f, *mirror(ARM_PTS)),
    shape(BodyRegion.LEFT_HAND, "BODY", .03f, *HAND_PTS),
    shape(BodyRegion.RIGHT_HAND, "BODY", .03f, *mirror(HAND_PTS)),
    shape(BodyRegion.LEFT_LEG, "BODY", .025f, *BACK_LEG_PTS),
    shape(BodyRegion.RIGHT_LEG, "BODY", .025f, *mirror(BACK_LEG_PTS)),
    shape(BodyRegion.LEFT_FOOT, "BODY", .025f, *FOOT_PTS),
    shape(BodyRegion.RIGHT_FOOT, "BODY", .025f, *mirror(FOOT_PTS)),
)

private val FRONT_SPEC = BodyViewSpec(
    parts = listOf(BodyPart("BODY", tracedPts(FRONT_OUTLINE_PTS))),
    dividers = FRONT_DIVIDERS,
    regions = FRONT_REGIONS,
)
private val BACK_SPEC = BodyViewSpec(
    parts = listOf(BodyPart("BODY", tracedPts(BACK_OUTLINE_PTS))),
    dividers = BACK_DIVIDERS,
    regions = BACK_REGIONS,
)

private fun sideSpec(left: Boolean): BodyViewSpec {
    fun pts(vararg raw: Pair<Float, Float>): Array<Pair<Float, Float>> =
        (if (left) raw.toList() else mirrorList(raw.toList())).toTypedArray()
    fun line(vararg raw: Pair<Float, Float>): List<Pair<Float, Float>> =
        if (left) raw.toList() else mirrorList(raw.toList())
    fun r(l: BodyRegion, rr: BodyRegion) = if (left) l else rr

    val outline = tracedPts(if (left) LEFT_OUTLINE_PTS else RIGHT_OUTLINE_PTS)

    // The near arm from the reference, drawn as a hint line inside the body.
    val armHint = line(
        .50f to .19f, .44f to .28f, .415f to .36f, .41f to .43f, .42f to .49f,
        .44f to .55f, .46f to .585f, .50f to .575f, .52f to .52f, .535f to .45f,
        .55f to .37f, .565f to .28f, .575f to .22f,
    )

    val dividers = listOf(
        Divider(line(.40f to .128f, .50f to .134f, .60f to .128f), "BODY"),   // head | neck
        Divider(line(.40f to .178f, .52f to .184f, .64f to .178f), "BODY"),   // neck | shoulder
        Divider(line(.38f to .41f, .52f to .417f, .68f to .41f), "BODY"),     // waist | hip
        Divider(line(.40f to .52f, .53f to .527f, .67f to .518f), "BODY"),    // hip | thigh
        Divider(line(.40f to .66f, .51f to .667f, .64f to .658f), "BODY"),    // thigh | knee
        Divider(line(.41f to .72f, .51f to .726f, .63f to .717f), "BODY"),    // knee | calf
        Divider(line(.44f to .92f, .52f to .926f, .60f to .915f), "BODY"),    // ankle
        Divider(armHint, "BODY"),                                             // near arm
        Divider(line(.42f to .49f, .47f to .496f, .52f to .488f), "BODY"),    // wrist
    )

    val headLeft = if (left) .33f else 1f - .70f
    val headRight = if (left) .70f else 1f - .33f
    val regions = listOf(
        ellipseShape(BodyRegion.HEAD, "BODY", headLeft, -.005f, headRight, .13f),
        shape(BodyRegion.NECK, "BODY", .02f, *pts(.39f to .128f, .63f to .128f, .63f to .178f, .39f to .178f)),
        shape(
            r(BodyRegion.LEFT_SHOULDER, BodyRegion.RIGHT_SHOULDER), "BODY", .03f,
            *pts(.36f to .178f, .72f to .178f, .72f to .248f, .36f to .248f),
        ),
        shape(
            r(BodyRegion.LEFT_HIP, BodyRegion.RIGHT_HIP), "BODY", .03f,
            *pts(.34f to .41f, .72f to .41f, .72f to .52f, .34f to .52f),
        ),
        shape(r(BodyRegion.LEFT_LEG, BodyRegion.RIGHT_LEG), "BODY", .025f, *pts(.35f to .52f, .70f to .52f, .66f to .66f, .37f to .66f)),
        shape(r(BodyRegion.LEFT_KNEE, BodyRegion.RIGHT_KNEE), "BODY", .02f, *pts(.37f to .66f, .66f to .66f, .64f to .72f, .38f to .72f)),
        shape(r(BodyRegion.LEFT_LEG, BodyRegion.RIGHT_LEG), "BODY", .025f, *pts(.38f to .72f, .64f to .72f, .62f to .92f, .40f to .92f)),
        shape(r(BodyRegion.LEFT_FOOT, BodyRegion.RIGHT_FOOT), "BODY", .025f, *pts(.26f to .92f, .70f to .92f, .70f to 1.0f, .26f to 1.0f)),
        shape(r(BodyRegion.LEFT_ARM, BodyRegion.RIGHT_ARM), "BODY", .025f, *pts(.40f to .248f, .60f to .248f, .60f to .49f, .40f to .49f)),
        shape(r(BodyRegion.LEFT_HAND, BodyRegion.RIGHT_HAND), "BODY", .03f, *pts(.40f to .49f, .58f to .49f, .58f to .60f, .40f to .60f)),
    )

    return BodyViewSpec(
        parts = listOf(BodyPart("BODY", outline)),
        dividers = dividers,
        regions = regions,
    )
}

private val LEFT_SPEC = sideSpec(left = true)
private val RIGHT_SPEC = sideSpec(left = false)

private fun specFor(view: BodyView): BodyViewSpec = when (view) {
    BodyView.FRONT -> FRONT_SPEC
    BodyView.BACK -> BACK_SPEC
    BodyView.LEFT -> LEFT_SPEC
    BodyView.RIGHT -> RIGHT_SPEC
}

/** The tappable body figure for one viewpoint. */
@Composable
fun BodyMap(
    view: BodyView,
    selectedRegions: Set<BodyRegion>,
    onRegionTap: ((BodyRegion) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spec = specFor(view)
    val bodyFill = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val dividerColor = outlineColor.copy(alpha = .55f)
    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(0.38f),
    ) {
        val w: Dp = maxWidth
        val h: Dp = maxHeight

        // One interaction source per region, shared by the tap target and the
        // press-feedback animation below.
        val interactions = spec.regions.mapIndexed { index, _ ->
            key(view, index) { remember { MutableInteractionSource() } }
        }

        // 0 → not selected, 1 → selected; eases the highlight in and out.
        val fractions = spec.regions.mapIndexed { index, s ->
            val isSelected = s.region in selectedRegions
            key(view, index) {
                animateFloatAsState(
                    if (isSelected) 1f else 0f,
                    tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ).value
            }
        }

        // Light press feedback before selection: "this area is interactive".
        val pressFractions = spec.regions.mapIndexed { index, s ->
            key(view, index) {
                val pressed by interactions[index].collectIsPressedAsState()
                animateFloatAsState(
                    if (pressed && s.region !in selectedRegions) 1f else 0f,
                    tween(durationMillis = 120),
                ).value
            }
        }

        Canvas(Modifier.matchParentSize()) {
            val thin = 1.dp.toPx()
            val outlineWidth = 1.4.dp.toPx()
            val press = 1.2.dp.toPx()
            val thick = 2.dp.toPx()

            val partPaths = spec.parts.associate { it.key to smoothClosed(it.outline.scaled(size)) }

            spec.parts.forEach { part ->
                val path = partPaths.getValue(part.key)
                drawPath(path, bodyFill)
                clipPath(path) {
                    spec.dividers.forEach { d ->
                        if (d.part != part.key) return@forEach
                        drawPath(
                            smoothOpen(d.pts.scaled(size)), dividerColor,
                            style = Stroke(thin, cap = StrokeCap.Round),
                        )
                    }
                }
                drawPath(path, outlineColor, style = Stroke(outlineWidth))
            }

            // Press feedback (unselected regions only), clipped to the body.
            spec.regions.forEachIndexed { index, s ->
                val fraction = pressFractions[index]
                if (fraction < .01f) return@forEachIndexed
                val target = partPaths[s.part] ?: return@forEachIndexed
                val highlight = Path.combine(PathOperation.Intersect, s.build(size), target)
                drawPath(highlight, primary.copy(alpha = .12f * fraction))
                drawPath(highlight, primary.copy(alpha = .45f * fraction), style = Stroke(width = press))
            }

            // Selection highlights, clipped to their body part so they follow its contour.
            spec.regions.forEachIndexed { index, s ->
                val fraction = fractions[index]
                if (fraction < .01f) return@forEachIndexed
                val target = partPaths[s.part] ?: return@forEachIndexed
                val highlight = Path.combine(PathOperation.Intersect, s.build(size), target)
                drawPath(highlight, primary.copy(alpha = .30f * fraction))
                drawPath(highlight, primary.copy(alpha = fraction), style = Stroke(width = thick))
            }
        }

        if (onRegionTap != null) {
            spec.regions.forEachIndexed { index, s ->
                key(view, index) {
                    val isSelected = s.region in selectedRegions
                    Box(
                        modifier = Modifier
                            .offset(x = w * s.bounds.left, y = h * s.bounds.top)
                            .size(width = w * s.bounds.width, height = h * s.bounds.height)
                            .semantics {
                                contentDescription =
                                    if (isSelected) "${s.region.label}, selected" else s.region.label
                            }
                            .clickable(interactionSource = interactions[index], indication = null) {
                                onRegionTap(s.region)
                            },
                    )
                }
            }
        }
    }
}

/** ← view switcher → shared by the patient input and the doctor pre-read. */
@Composable
fun BodyViewSwitcher(view: BodyView, onViewChange: (BodyView) -> Unit) {
    val views = BodyView.entries
    val index = views.indexOf(view)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onViewChange(views[(index - 1 + views.size) % views.size]) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous view")
        }
        Text(view.label, style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { onViewChange(views[(index + 1) % views.size]) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next view")
        }
    }
}

/**
 * Patient-side localization card: pick regions on the map, answer the follow-up
 * questions, review/remove selections. Entirely optional.
 */
@Composable
fun SymptomLocalizationSection(
    locations: List<SymptomLocation>,
    onAdd: (SymptomLocation) -> Unit,
    onRemove: (BodyRegion) -> Unit,
) {
    var view by remember { mutableStateOf(BodyView.FRONT) }
    var pendingRegion by remember { mutableStateOf<BodyRegion?>(null) }
    val selected = locations.map { it.region }.toSet()

    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Where do you feel it? (optional)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap the body areas where your symptoms occur. Tap a highlighted area to remove it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.sm))
        BodyViewSwitcher(view = view, onViewChange = { view = it })
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BodyMap(
                view = view,
                selectedRegions = selected,
                onRegionTap = { region ->
                    if (region in selected) onRemove(region) else pendingRegion = region
                },
            )
        }
        if (locations.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            locations.forEach { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        location.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemove(location.region) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove ${location.region.label}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    pendingRegion?.let { region ->
        SymptomDetailDialog(
            region = region,
            onConfirm = { location ->
                onAdd(location)
                pendingRegion = null
            },
            onDismiss = { pendingRegion = null },
        )
    }
}

/**
 * The follow-up questions asked right after a region is selected (Phase 6.6:
 * presentation polish only — questions, options and stored values unchanged).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomDetailDialog(
    region: BodyRegion,
    onConfirm: (SymptomLocation) -> Unit,
    onDismiss: () -> Unit,
) {
    var descriptor by remember { mutableStateOf("Pain") }
    var severity by remember { mutableIntStateOf(5) }
    var duration by remember { mutableStateOf("Several days") }
    var progression by remember { mutableStateOf("Same") }

    Dialog(onDismissRequest = onDismiss) {
        MedFusionCard(contentPadding = Spacing.lg) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column {
                    Text(
                        region.label,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "Tell us a little more about this area",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column {
                    Text("What best describes this symptom?", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.xs),
                    ) {
                        SymptomLocalizationOptions.descriptors.forEach { option ->
                            FilterChip(
                                selected = descriptor == option,
                                onClick = { descriptor = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("How severe is it?", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$severity/10",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = severity.toFloat(),
                        onValueChange = { severity = it.toInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }

                Column {
                    Text("When did it begin?", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.xs),
                    ) {
                        SymptomLocalizationOptions.durations.forEach { option ->
                            FilterChip(
                                selected = duration == option,
                                onClick = { duration = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }

                Column {
                    Text("Is it getting…", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.xs),
                    ) {
                        SymptomLocalizationOptions.progressions.forEach { option ->
                            FilterChip(
                                selected = progression == option,
                                onClick = { progression = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }

                Column {
                    PrimaryButton(
                        text = "Add symptom",
                        onClick = {
                            onConfirm(SymptomLocation(region, descriptor, severity, duration, progression))
                        },
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

/** Read-only body map + details for the doctor's pre-read (Phase 5.6). */
@Composable
fun SymptomMapSummary(locations: List<SymptomLocation>) {
    if (locations.isEmpty()) return
    val selected = locations.map { it.region }.toSet()
    // Open on the first view that shows a marked region.
    var view by remember {
        mutableStateOf(BodyView.entries.firstOrNull { v -> locations.any { v in it.region.views } }
            ?: BodyView.FRONT)
    }
    Column {
        Text("Symptom locations", style = MaterialTheme.typography.labelLarge)
        BodyViewSwitcher(view = view, onViewChange = { view = it })
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BodyMap(view = view, selectedRegions = selected, onRegionTap = null)
        }
        Spacer(Modifier.height(Spacing.xs))
        locations.forEach {
            Text("•  ${it.summary()}", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}
