package io.github.vitoosh.vitquest.testing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.unit.dp

/**
 * Checks every visible control a screen reader user can activate:
 * - it has a label (its own text or content description, or one merged from its children);
 * - its touch area is at least 48 × 48 dp;
 * - its touch area does not overlap a neighbouring control's, which would make a
 *   small-looking control effectively smaller than 48 dp (taps land on the wrong one).
 *
 * Google's Accessibility Test Framework does not evaluate Compose under Robolectric,
 * so these checks run here instead. Off-screen nodes are skipped, so use a tall test
 * window to cover a whole screen. Problems are returned as readable lines.
 */
fun SemanticsNodeInteractionsProvider.accessibilityProblems(): List<String> {
    val root = onNode(isRoot()).fetchSemanticsNode()
    val density = root.layoutInfo.density
    val minTouchPx = with(density) { 48.dp.toPx() } - 0.5f
    val problems = mutableListOf<String>()
    val controls = mutableListOf<Pair<String, Rect>>()
    fun dp(px: Float) = with(density) { px.toDp().value.toInt() }
    fun visit(node: SemanticsNode) {
        val config = node.config
        val actionable = SemanticsActions.OnClick in config || SemanticsActions.SetText in config
        val visible = node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f
        if (actionable && visible && SemanticsProperties.Disabled !in config) {
            val label = buildList {
                config.getOrNull(SemanticsProperties.ContentDescription)?.let(::addAll)
                config.getOrNull(SemanticsProperties.Text)?.map { it.text }?.let(::addAll)
                config.getOrNull(SemanticsProperties.EditableText)?.text?.let(::add)
            }.joinToString(" ").trim()
            val name = label.ifEmpty { "(unlabelled ${config.getOrNull(SemanticsProperties.Role) ?: "control"})" }
            val touch = node.touchBoundsInRoot
            if (label.isEmpty()) problems += "No label: $name"
            if (touch.width < minTouchPx || touch.height < minTouchPx) {
                problems += "Touch area ${dp(touch.width)}×${dp(touch.height)} dp is below 48 dp: $name"
            }
            controls += name to touch
        }
        node.children.forEach(::visit)
    }
    visit(root)
    val slack = with(density) { 1.dp.toPx() }
    for (i in controls.indices) for (j in i + 1 until controls.size) {
        val (a, ra) = controls[i]
        val (b, rb) = controls[j]
        val overlap = ra.intersect(rb)
        // A control inside another (a switch in a list row) is fine; side-by-side overlap is not.
        val nested = ra.contains(rb) || rb.contains(ra)
        if (!nested && overlap.width > slack && overlap.height > slack) {
            problems += "Touch areas overlap by ${dp(overlap.width)}×${dp(overlap.height)} dp: $a / $b"
        }
    }
    return problems
}

private fun Rect.contains(other: Rect) =
    left <= other.left + 0.5f && top <= other.top + 0.5f && right >= other.right - 0.5f && bottom >= other.bottom - 0.5f

/** Fails with every problem [accessibilityProblems] finds on the current screen. */
fun SemanticsNodeInteractionsProvider.assertAccessible() {
    val problems = accessibilityProblems()
    check(problems.isEmpty()) { "Accessibility problems:\n" + problems.joinToString("\n") }
}
