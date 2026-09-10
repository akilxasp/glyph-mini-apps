package com.akil.glyphlife

import kotlin.random.Random

/**
 * Engine self-check. Run: `kotlinc FlowerSprites.kt GardenScene.kt GardenRenderer.kt GardenSceneCheck.kt -include-runtime -d t.jar && java -jar t.jar`.
 * Verifies for every flower: frames stay in range over a long run, rain eventually starts and
 * stops, and wilt terminates to a blank finished state.
 */
fun main() {
    val n = 13
    for (flower in FlowerSprites.ALL) for (style in FlowerSprites.Style.entries) {
        val s = GardenScene(n, flower, style, Random(11))
        var sawRain = false; var sawRainEnd = false; var wasRaining = false

        repeat(6000) {
            s.step()
            val f = GardenRenderer.toFrame(s)
            check(f.size == n * n) { "${flower.key}: bad frame size" }
            check(f.all { v -> v in 0..255 }) { "${flower.key}/$style: brightness out of range" }
            check(f.any { v -> v > 0 }) { "${flower.key}/$style: went blank while playing" }
            if (s.raining) sawRain = true
            if (wasRaining && !s.raining) sawRainEnd = true
            wasRaining = s.raining
        }
        check(sawRain) { "${flower.key}/$style: rain never started in 6000 ticks" }
        check(sawRainEnd) { "${flower.key}/$style: rain never stopped" }

        // Wilt must finish and end blank.
        s.wiltNow()
        var guard = 0
        while (s.mode != GardenScene.Mode.FINISHED && guard++ < 500) s.step()
        check(s.mode == GardenScene.Mode.FINISHED) { "${flower.key}/$style: wilt never finished" }
        check(GardenRenderer.toFrame(s).all { v -> v == 0 }) { "${flower.key}/$style: finished frame not blank" }
    }
    // Clouds: present by default, dim, and drift + wrap without leaving the count.
    val cs = GardenScene(n, FlowerSprites.TULIP, FlowerSprites.Style.DETAILED, Random(5))
    check(cs.clouds.isNotEmpty()) { "clouds missing by default" }
    val cloudCount = cs.clouds.size
    for (c in cs.clouds) for (p in c.px) check(p.b <= GardenScene.CLOUD_MAX_B) { "cloud pixel too bright: ${p.b}" }
    repeat(2000) {
        cs.step()
        check(cs.clouds.size == cloudCount) { "cloud lost during drift" }
        for (c in cs.clouds) check(c.x in -5.0..n.toDouble() + 1) { "cloud drifted out of wrap range: ${c.x}" }
    }
    // Picker path: no clouds.
    val noCloud = GardenScene(n, FlowerSprites.TULIP, FlowerSprites.Style.DETAILED, Random(5), showClouds = false)
    check(noCloud.clouds.isEmpty()) { "showClouds=false should have no clouds" }

    println("OK: ${FlowerSprites.ALL.size} flowers × ${FlowerSprites.Style.entries.size} styles — frames valid, rain cycles, wilt ends blank, clouds drift dim")
}
