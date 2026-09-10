package com.akil.glyphlife
import kotlin.random.Random
fun main() {
    val n=13; val g=ShooterGame(n, Random(5))
    // fast-forward to a missile, then dump frames through the wipe
    var t=0; while (g.missile()==null && !g.isWiping() && t<2000){ g.step(); t++ }
    // find detonation
    while (!g.isWiping() && t<3000){ g.step(); t++ }
    repeat(6) {
        println("── wipeRow=${g.wipeRow()} alive=${g.aliveCount()} ──")
        val f=ShooterRenderer.toFrame(g)
        for (y in 0 until n){ val sb=StringBuilder(); for(x in 0 until n){val v=f[y*n+x]; sb.append(if(v==0)" ." else if(v>=255)" @" else " o")}; println(sb) }
        g.step()
    }
}
