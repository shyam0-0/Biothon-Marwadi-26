package com.medfusion.ai.ui.symptom

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
 * Smart pain & symptom localization (Phase 5.6): a simple multi-view body map
 * built from tappable predefined regions — deliberately NOT an anatomy viewer,
 * just an intuitive way to say where it hurts. The same map renders read-only
 * for the doctor's pre-read.
 *
 * Rendering: each view is ONE continuous closed silhouette (a smooth spline
 * whose outline runs shoulder → arm → hand → armpit notch → torso → leg →
 * foot, exactly like a standard medical body-diagram asset), plus a near-arm
 * overlay on side views. Faint internal divider lines hint the selectable
 * regions. All coordinates are normalized to the canvas; x-widths are derived
 * from real height-relative body proportions divided by the canvas aspect
 * ratio (0.45), so the figure renders with correct proportions on screen.
 * Region mapping, tap targets and interaction are unchanged.
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

// ── Silhouette outlines (normalized 0..1 anchors, y grows downward) ─────────
// Proportions taken from the reference asset (canvas aspect 0.45, so an
// on-screen width of w·H maps to w/0.45 in x units): head ≈ .09H wide,
// shoulders ≈ .25H, waist ≈ .15H, hips ≈ .19H, thigh ≈ .07H, knee ≈ .06H,
// calf ≈ .06H, ankle ≈ .03H, upper arm ≈ .04H. Arms hang close to the torso
// with a thin sliver gap from the armpit notch; legs nearly touch.

// Front/back: one closed outline. Screen-left half from crown → head → neck →
// trapezius → shoulder → outer arm → hand → inner arm → armpit notch → torso
// side → hip → outer leg → foot → inner leg; crotch joins the mirrored half.
private val FRONT_LEFT = listOf(
    .500f to .000f, .452f to .008f, .413f to .033f, .402f to .062f, .414f to .088f,
    .434f to .103f, .452f to .112f, .450f to .126f, .442f to .140f, .390f to .150f,
    .300f to .161f, .245f to .172f, .226f to .196f, .225f to .250f, .224f to .305f,
    .222f to .360f, .221f to .412f, .208f to .432f, .206f to .465f, .228f to .488f,
    .258f to .482f, .270f to .450f, .272f to .418f, .281f to .360f, .291f to .300f,
    .301f to .250f, .311f to .213f, .316f to .240f, .323f to .278f, .330f to .310f,
    .318f to .350f, .293f to .388f, .295f to .425f, .303f to .465f, .316f to .545f,
    .335f to .615f, .329f to .652f, .323f to .700f, .343f to .800f, .368f to .885f,
    .352f to .915f, .318f to .945f, .332f to .958f, .428f to .956f, .437f to .898f,
    .452f to .750f, .463f to .635f, .472f to .530f,
)
private val FRONT_OUTLINE: List<Pair<Float, Float>> =
    FRONT_LEFT + (.500f to .448f) + mirrorList(FRONT_LEFT.drop(1)).reversed()

// Side profile, facing viewer-left: one closed body outline (forehead, nose
// hint, chin, throat, subtle chest/belly curve, front leg, foot, calf bulge,
// buttock, back curve, nape, skull), plus the near arm drawn as an overlay.
private val SIDE_BODY = listOf(
    .500f to .000f, .435f to .020f, .412f to .045f, .400f to .060f, .412f to .075f,
    .428f to .092f, .448f to .108f, .462f to .126f, .452f to .145f, .426f to .170f,
    .412f to .235f, .418f to .295f, .428f to .355f, .448f to .415f, .462f to .470f,
    .468f to .530f, .474f to .620f, .480f to .720f, .487f to .880f, .430f to .916f,
    .396f to .940f, .398f to .958f, .500f to .960f, .560f to .952f, .566f to .916f,
    .536f to .882f, .552f to .800f, .590f to .700f, .570f to .630f, .590f to .550f,
    .605f to .485f, .628f to .408f, .596f to .338f, .610f to .270f, .628f to .210f,
    .615f to .155f, .572f to .120f, .580f to .098f, .612f to .068f, .604f to .035f,
    .565f to .010f,
)
private val SIDE_ARM = listOf(
    .470f to .158f, .592f to .152f, .608f to .195f, .600f to .260f, .588f to .330f,
    .575f to .395f, .580f to .430f, .560f to .472f, .524f to .470f, .508f to .430f,
    .512f to .395f, .498f to .330f, .486f to .260f, .476f to .200f,
)

// ── Region highlight bands (front/back; viewer-left, mirrored for the right) ──

private val SHOULDER_PTS = arrayOf(.210f to .135f, .385f to .135f, .385f to .215f, .210f to .215f)
private val ARM_PTS = arrayOf(.205f to .215f, .320f to .215f, .320f to .412f, .205f to .412f)
private val HAND_PTS = arrayOf(.195f to .412f, .290f to .412f, .290f to .498f, .195f to .498f)
private val LEG_UPPER_PTS = arrayOf(.285f to .440f, .498f to .440f, .498f to .600f, .285f to .600f)
private val KNEE_PTS = arrayOf(.285f to .600f, .498f to .600f, .498f to .655f, .285f to .655f)
private val LEG_LOWER_PTS = arrayOf(.290f to .655f, .480f to .655f, .480f to .885f, .290f to .885f)
private val FOOT_PTS = arrayOf(.295f to .885f, .455f to .885f, .455f to .965f, .295f to .965f)
private val BACK_LEG_PTS = arrayOf(.285f to .440f, .498f to .440f, .490f to .885f, .290f to .885f)

// ── Divider lines ────────────────────────────────────────────────────────────

private val LIMB_DIVIDERS: List<Divider> = buildList {
    val shoulderArm = listOf(.228f to .208f, .268f to .216f, .306f to .208f)
    add(Divider(shoulderArm, "BODY")); add(Divider(mirrorList(shoulderArm), "BODY"))
    val wrist = listOf(.224f to .415f, .248f to .421f, .271f to .414f)
    add(Divider(wrist, "BODY")); add(Divider(mirrorList(wrist), "BODY"))
    val hipLeg = listOf(.298f to .435f, .400f to .450f, .492f to .446f)
    add(Divider(hipLeg, "BODY")); add(Divider(mirrorList(hipLeg), "BODY"))
    val ankle = listOf(.370f to .882f, .405f to .888f, .436f to .880f)
    add(Divider(ankle, "BODY")); add(Divider(mirrorList(ankle), "BODY"))
}

private val FRONT_DIVIDERS: List<Divider> = buildList {
    add(Divider(listOf(.410f to .055f, .500f to .060f, .590f to .055f), "BODY"))   // head | face
    add(Divider(listOf(.430f to .105f, .500f to .110f, .570f to .105f), "BODY"))   // face | neck
    add(Divider(listOf(.418f to .142f, .500f to .148f, .582f to .142f), "BODY"))   // neck | chest
    val shoulderChest = listOf(.435f to .150f, .360f to .170f, .318f to .210f)
    add(Divider(shoulderChest, "BODY")); add(Divider(mirrorList(shoulderChest), "BODY"))
    add(Divider(listOf(.305f to .238f, .500f to .246f, .695f to .238f), "BODY"))   // chest | upper abd
    add(Divider(listOf(.310f to .300f, .500f to .307f, .690f to .300f), "BODY"))   // upper | lower abd
    add(Divider(listOf(.295f to .356f, .500f to .364f, .705f to .356f), "BODY"))   // abdomen | hips
    val hipPelvis = listOf(.418f to .358f, .424f to .400f, .430f to .440f)
    add(Divider(hipPelvis, "BODY")); add(Divider(mirrorList(hipPelvis), "BODY"))
    val kneeTop = listOf(.338f to .603f, .400f to .611f, .460f to .603f)
    add(Divider(kneeTop, "BODY")); add(Divider(mirrorList(kneeTop), "BODY"))
    val kneeBot = listOf(.332f to .650f, .400f to .656f, .458f to .648f)
    add(Divider(kneeBot, "BODY")); add(Divider(mirrorList(kneeBot), "BODY"))
    addAll(LIMB_DIVIDERS)
}

private val BACK_DIVIDERS: List<Divider> = buildList {
    add(Divider(listOf(.430f to .104f, .500f to .110f, .570f to .104f), "BODY"))   // head | neck
    add(Divider(listOf(.418f to .142f, .500f to .148f, .582f to .142f), "BODY"))   // neck | back
    val shoulderBack = listOf(.435f to .150f, .360f to .170f, .318f to .210f)
    add(Divider(shoulderBack, "BODY")); add(Divider(mirrorList(shoulderBack), "BODY"))
    add(Divider(listOf(.305f to .300f, .500f to .308f, .695f to .300f), "BODY"))   // back | lower back
    add(Divider(listOf(.295f to .356f, .500f to .364f, .705f to .356f), "BODY"))   // lower back | hips
    add(Divider(listOf(.500f to .150f, .496f to .255f, .500f to .356f), "BODY"))   // spine centerline
    add(Divider(listOf(.500f to .360f, .500f to .450f), "BODY"))                   // hip split
    addAll(LIMB_DIVIDERS)
}

// ── Regions per view (order also resolves overlapping tap targets: later wins) ──

private val FRONT_REGIONS = listOf(
    ellipseShape(BodyRegion.HEAD, "BODY", .400f, -.008f, .600f, .108f, clipBottom = .056f),
    ellipseShape(BodyRegion.FACE, "BODY", .400f, -.008f, .600f, .108f, clipTop = .056f),
    shape(BodyRegion.NECK, "BODY", .02f, .428f to .108f, .572f to .108f, .582f to .144f, .418f to .144f),
    shape(BodyRegion.CHEST, "BODY", .03f, .295f to .144f, .705f to .144f, .705f to .240f, .295f to .240f),
    shape(BodyRegion.RIGHT_SHOULDER, "BODY", .025f, *SHOULDER_PTS),
    shape(BodyRegion.LEFT_SHOULDER, "BODY", .025f, *mirror(SHOULDER_PTS)),
    shape(BodyRegion.UPPER_ABDOMEN, "BODY", .03f, .300f to .240f, .700f to .240f, .700f to .302f, .300f to .302f),
    shape(BodyRegion.LOWER_ABDOMEN, "BODY", .03f, .295f to .302f, .705f to .302f, .705f to .358f, .295f to .358f),
    shape(BodyRegion.RIGHT_HIP, "BODY", .025f, .265f to .358f, .425f to .358f, .415f to .428f, .300f to .440f, .265f to .400f),
    shape(BodyRegion.LEFT_HIP, "BODY", .025f, .735f to .358f, .575f to .358f, .585f to .428f, .700f to .440f, .735f to .400f),
    shape(BodyRegion.PELVIS, "BODY", .03f, .420f to .358f, .580f to .358f, .572f to .425f, .500f to .452f, .428f to .425f),
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
    ellipseShape(BodyRegion.HEAD, "BODY", .400f, -.008f, .600f, .108f),
    shape(BodyRegion.NECK, "BODY", .02f, .428f to .106f, .572f to .106f, .582f to .144f, .418f to .144f),
    shape(BodyRegion.BACK_UPPER, "BODY", .03f, .295f to .144f, .705f to .144f, .705f to .302f, .295f to .302f),
    shape(BodyRegion.LEFT_SHOULDER, "BODY", .025f, *SHOULDER_PTS),
    shape(BodyRegion.RIGHT_SHOULDER, "BODY", .025f, *mirror(SHOULDER_PTS)),
    shape(BodyRegion.LOWER_BACK, "BODY", .03f, .295f to .302f, .705f to .302f, .705f to .358f, .295f to .358f),
    shape(BodyRegion.LEFT_HIP, "BODY", .03f, .265f to .358f, .500f to .358f, .500f to .450f, .300f to .440f, .265f to .400f),
    shape(BodyRegion.RIGHT_HIP, "BODY", .03f, .735f to .358f, .500f to .358f, .500f to .450f, .700f to .440f, .735f to .400f),
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
    parts = listOf(BodyPart("BODY", FRONT_OUTLINE)),
    dividers = FRONT_DIVIDERS,
    regions = FRONT_REGIONS,
)
private val BACK_SPEC = BodyViewSpec(
    parts = listOf(BodyPart("BODY", FRONT_OUTLINE)),
    dividers = BACK_DIVIDERS,
    regions = BACK_REGIONS,
)

private fun sideSpec(left: Boolean): BodyViewSpec {
    fun pts(vararg raw: Pair<Float, Float>): Array<Pair<Float, Float>> =
        (if (left) raw.toList() else mirrorList(raw.toList())).toTypedArray()
    fun line(vararg raw: Pair<Float, Float>): List<Pair<Float, Float>> =
        if (left) raw.toList() else mirrorList(raw.toList())
    fun r(l: BodyRegion, rr: BodyRegion) = if (left) l else rr

    val body = if (left) SIDE_BODY else mirrorList(SIDE_BODY)
    val arm = if (left) SIDE_ARM else mirrorList(SIDE_ARM)

    val dividers = listOf(
        Divider(line(.445f to .104f, .510f to .111f, .572f to .104f), "BODY"),   // head | neck
        Divider(line(.446f to .146f, .520f to .152f, .596f to .146f), "BODY"),   // neck | shoulder
        Divider(line(.446f to .358f, .530f to .365f, .618f to .358f), "BODY"),   // waist | hip
        Divider(line(.458f to .452f, .530f to .459f, .605f to .450f), "BODY"),   // hip | thigh
        Divider(line(.468f to .602f, .520f to .610f, .560f to .602f), "BODY"),   // thigh | knee
        Divider(line(.470f to .652f, .520f to .658f, .565f to .650f), "BODY"),   // knee | calf
        Divider(line(.482f to .878f, .510f to .884f, .538f to .876f), "BODY"),   // ankle
        Divider(line(.478f to .205f, .540f to .212f, .604f to .205f), "ARM"),    // shoulder | arm
        Divider(line(.508f to .408f, .540f to .415f, .578f to .406f), "ARM"),    // arm | hand
    )

    val headLeft = if (left) .390f else 1f - .640f
    val headRight = if (left) .640f else 1f - .390f
    val regions = listOf(
        ellipseShape(BodyRegion.HEAD, "BODY", headLeft, -.008f, headRight, .106f),
        shape(BodyRegion.NECK, "BODY", .02f, *pts(.440f to .104f, .580f to .104f, .596f to .148f, .446f to .148f)),
        shape(
            r(BodyRegion.LEFT_SHOULDER, BodyRegion.RIGHT_SHOULDER), "BODY", .03f,
            *pts(.420f to .135f, .640f to .135f, .640f to .212f, .420f to .212f),
        ),
        shape(
            r(BodyRegion.LEFT_HIP, BodyRegion.RIGHT_HIP), "BODY", .03f,
            *pts(.430f to .355f, .660f to .355f, .650f to .455f, .445f to .455f),
        ),
        shape(r(BodyRegion.LEFT_LEG, BodyRegion.RIGHT_LEG), "BODY", .025f, *pts(.430f to .455f, .640f to .455f, .630f to .602f, .440f to .602f)),
        shape(r(BodyRegion.LEFT_KNEE, BodyRegion.RIGHT_KNEE), "BODY", .02f, *pts(.440f to .602f, .620f to .602f, .610f to .652f, .445f to .652f)),
        shape(r(BodyRegion.LEFT_LEG, BodyRegion.RIGHT_LEG), "BODY", .025f, *pts(.445f to .652f, .610f to .652f, .590f to .878f, .455f to .878f)),
        shape(r(BodyRegion.LEFT_FOOT, BodyRegion.RIGHT_FOOT), "BODY", .025f, *pts(.370f to .878f, .600f to .878f, .600f to .968f, .370f to .968f)),
        shape(r(BodyRegion.LEFT_ARM, BodyRegion.RIGHT_ARM), "ARM", .025f, *pts(.455f to .212f, .620f to .212f, .610f to .408f, .480f to .408f)),
        shape(r(BodyRegion.LEFT_HAND, BodyRegion.RIGHT_HAND), "ARM", .03f, *pts(.490f to .408f, .600f to .408f, .590f to .480f, .495f to .480f)),
    )

    return BodyViewSpec(
        parts = listOf(BodyPart("BODY", body), BodyPart("ARM", arm)),
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
            .aspectRatio(0.45f),
    ) {
        val w: Dp = maxWidth
        val h: Dp = maxHeight

        // 0 → not selected, 1 → selected; fades the highlight in and out.
        val fractions = spec.regions.mapIndexed { index, s ->
            val isSelected = s.region in selectedRegions
            key(view, index) {
                animateFloatAsState(if (isSelected) 1f else 0f, tween(durationMillis = 250)).value
            }
        }

        Canvas(Modifier.matchParentSize()) {
            val thin = 1.dp.toPx()
            val outlineWidth = 1.4.dp.toPx()
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

            // Highlights, clipped to their body part so they follow its contour.
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
                    val interaction = remember { MutableInteractionSource() }
                    val isSelected = s.region in selectedRegions
                    Box(
                        modifier = Modifier
                            .offset(x = w * s.bounds.left, y = h * s.bounds.top)
                            .size(width = w * s.bounds.width, height = h * s.bounds.height)
                            .semantics {
                                contentDescription =
                                    if (isSelected) "${s.region.label}, selected" else s.region.label
                            }
                            .clickable(interactionSource = interaction, indication = null) {
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

/** The follow-up questions asked right after a region is selected. */
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
            Text(region.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(Spacing.sm))

            Text("What best describes this symptom?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SymptomLocalizationOptions.descriptors.forEach { option ->
                    FilterChip(
                        selected = descriptor == option,
                        onClick = { descriptor = option },
                        label = { Text(option) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            Text("How severe is it?  $severity/10", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = severity.toFloat(),
                onValueChange = { severity = it.toInt() },
                valueRange = 0f..10f,
                steps = 9,
            )

            Text("When did it begin?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SymptomLocalizationOptions.durations.forEach { option ->
                    FilterChip(
                        selected = duration == option,
                        onClick = { duration = option },
                        label = { Text(option) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            Text("Is it getting…", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SymptomLocalizationOptions.progressions.forEach { option ->
                    FilterChip(
                        selected = progression == option,
                        onClick = { progression = option },
                        label = { Text(option) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))

            PrimaryButton(
                text = "Add symptom",
                onClick = {
                    onConfirm(SymptomLocation(region, descriptor, severity, duration, progression))
                },
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel")
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
