"""Copies a few icons from material-icons-extended (Apache 2.0) into VqIcons.kt.

Usage: python3 tools/icons/gen_icons.py <unzipped material-icons-extended-android sources>/commonMain/androidx/compose/material/icons \
           app/src/main/java/io/github/vitoosh/vitquest/core/designsystem/icon/VqIcons.kt
"""
import re, sys, os
base = sys.argv[1]
out = sys.argv[2]
icons = [("Walk","automirrored/filled/DirectionsWalk.kt"),("Restaurant","filled/Restaurant.kt"),
 ("FitnessCenter","filled/FitnessCenter.kt"),("Trophy","filled/EmojiEvents.kt"),("WaterDrop","filled/WaterDrop.kt"),
 ("MonitorWeight","filled/MonitorWeight.kt"),("Flag","filled/Flag.kt"),("Language","filled/Language.kt"),
 ("Palette","filled/Palette.kt"),("Shield","filled/Shield.kt"),("Bolt","filled/Bolt.kt"),("Remove","filled/Remove.kt"),("StarBorder","filled/StarBorder.kt"),
 ("Pause","filled/Pause.kt"),("SkipNext","filled/SkipNext.kt"),("SkipPrevious","filled/SkipPrevious.kt"),
 ("Timer","filled/Timer.kt"),("Insights","filled/Insights.kt"),("Checklist","filled/Checklist.kt"),
 ("Shuffle","filled/Shuffle.kt"),("Replay","filled/Replay.kt"),("Stop","filled/Stop.kt"),
 ("RadioButtonUnchecked","filled/RadioButtonUnchecked.kt"),("SelfImprovement","filled/SelfImprovement.kt"),
 ("History","filled/History.kt"),("Run","automirrored/filled/DirectionsRun.kt"),
 ("SaveToDevice","filled/SaveAlt.kt"),("Restore","filled/Restore.kt"),
 ("PhotoCamera","filled/PhotoCamera.kt"),("PhotoLibrary","filled/PhotoLibrary.kt"),("AutoAwesome","filled/AutoAwesome.kt")]
parts = []
for name, rel in icons:
    src = open(os.path.join(base, rel)).read()
    m = re.search(r'materialIcon\(name = "[^"]+"(,\s*autoMirror\s*=\s*true)?\)\s*\{', src)
    i = m.end(); depth = 1; j = i
    while depth:
        c = src[j]
        depth += (c == '{') - (c == '}')
        j += 1
    body = src[i:j-1]
    body = "\n".join(l.rstrip() for l in body.splitlines() if l.strip())
    mirror = ", autoMirror = true" if m.group(1) else ""
    parts.append(f'    val {name}: ImageVector by lazy {{\n        materialIcon(name = "VitQuest.{name}"{mirror}) {{\n{body}\n        }}\n    }}\n')
header = '''/*
 * Path data copied from androidx.compose.material:material-icons-extended 1.7.8
 * (Apache License 2.0) so the app does not ship the full extended icon library.
 */
package io.github.vitoosh.vitquest.core.designsystem.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons used by VitQuest that are not part of material-icons-core. */
object VqIcons {
'''
open(out, "w").write(header + "\n".join(parts) + "}\n")
