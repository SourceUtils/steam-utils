package com.timepath.steam.io

import com.timepath.steam.io.storage.ACF
import com.timepath.vfs.VFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.logging.Logger
import kotlin.platform.platformStatic

public class DiffGen private constructor() {
    companion object {

        private val LOG = Logger.getLogger(javaClass<DiffGen>().getName())
        private val blacklist = arrayOf(".*/bin", ".*/cache", ".*tf/custom", ".*tf/download", ".*tf/replay", ".*tf/screenshots", ".*sounds?\\.cache", ".*cfg/config\\.cfg", ".*media/viewed\\.res", ".*tf/console\\.log", ".*tf/condump.*\\.txt", ".*tf/demoheader\\.tmp", ".*tf/voice_ban\\.dt", ".*tf/trainingprogress\\.txt")
        private val K = 1024
        private val M = K * K
        private val G = M * K

        private fun check(f: File): Boolean {
            val path = f.getPath()
            for (r in blacklist) {
                if (path.matches(r.toRegex())) {
                    LOG.info(path)
                    return false
                }
            }
            return true
        }

        throws(IOException::class)
        private fun <V : VFile<V>> extract(v: V, dir: File) {
            val out = File(dir, v.name)
            if (!check(out)) {
                return
            }
            if (v.isDirectory) {
                out.mkdir()
                for (e in v.list()) {
                    extract(e, out)
                }
            } else {
                out.createNewFile()
                v.openStream()!!.copyTo(FileOutputStream(out).buffered())
            }
        }

        throws(IOException::class)
        public platformStatic fun main(args: Array<String>) {
            val apps = intArrayOf(440)
            val container = File(System.getProperty("user.home"), "steamtracker")
            for (i in apps) {
                val repo = File(container, i.toString())
                repo.mkdirs()
                val acf = ACF.fromManifest(i)
                val files = acf.list()
                for (v in files) {
                    extract(v, repo)
                }
            }
            LOG.info("EXTRACTED")
        }
    }
}
