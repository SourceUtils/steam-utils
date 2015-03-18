package com.timepath.steam.net

import com.timepath.DateUtils
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.steam.io.VDFNode
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
class ServerTest : JPanel() {

    {
        val server = JTextArea()
        val favorites = JTextArea()
        val history = JTextArea()
        val internet = JTextArea()
        val ss = SourceServer("27.50.71.201", 27016)
        setLayout(BorderLayout())
        add(with(JSplitPane()) {
            setLeftComponent(JScrollPane(server))
            setRightComponent(with(JTabbedPane()) {
                add("Favorites", JScrollPane(favorites))
                add("History", JScrollPane(history))
                add("Internet", JScrollPane(internet))
                this
            })
            Thread(object : Runnable {
                override fun run() {
                    server.append("Server: ${ss.address}\n")
                    try {
                        log.info("Getting info ...")
                        ss.getInfo(object : ServerListener {
                            override fun inform(update: String) {
                                server.append("${update}\n")
                            }
                        })
                        log.info("Getting rules ...")
                        server.append("Rules: \n")
                        ss.getRules(object : ServerListener {
                            override fun inform(update: String) {
                                server.append("${update}\n")
                            }
                        })
                    } catch (ex: IOException) {
                        log.log(Level.WARNING, null, ex)
                    }

                }
            }).start()
            Thread(object : Runnable {
                override fun run() {
                    var vdf: VDFNode? = null
                    try {
                        vdf = VDF.load(File(SteamUtils.getUserData(), "7/remote/serverbrowser_hist.vdf"))
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    val filters = vdf!!["Filters"]
                    val map = HashMap<String, JTextArea>(2)
                    map.put("Favorites", favorites)
                    map.put("History", history)
                    for (e in map.entrySet()) {
                        val k = e.getKey()
                        val v = e.getValue()
                        for (n in filters[k]!!.getNodes()) {
                            v.append("Favorite: ${n.getCustom()}\n")
                            v.append("Name: ${n.getValue("name")}\n")
                            v.append("Address: ${n.getValue("address")}\n")
                            v.append("\n")
                            v.append("Last Played: ${DateUtils.parse(java.lang.Long.parseLong(n.getValue("LastPlayed") as String))}\n")
                        }
                    }
                }
            }).start()
            Thread(object : Runnable {
                override fun run() {
                    log.info("Querying master ...")
                    try {
                        MasterServer.SOURCE.query(MasterServer.Region.AUSTRALIA, "\\gamedir\\tf", object : ServerListener {

                            override fun inform(update: String) {
                                internet.append("$update\n")
                            }
                        })
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }).start()
            this
        })
    }

    class object {

        val log = Logger.getLogger(javaClass<ServerTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            log.log(Level.INFO, "Started")

            SwingUtilities.invokeLater(object : Runnable {
                override fun run() {
                    log.log(Level.INFO, "EDT")
                    val d = JDialog()
                    d.setTitle("Server browser")
                    d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                    d.setPreferredSize(Dimension(800, 600))
                    d.add(ServerTest())
                    d.pack()
                    d.setLocationRelativeTo(null)
                    d.setVisible(true)
                    log.log(Level.INFO, "Visible")
                }
            })
        }
    }
}
