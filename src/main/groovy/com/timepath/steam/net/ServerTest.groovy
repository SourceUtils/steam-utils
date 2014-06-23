package com.timepath.steam.net

import com.timepath.DateUtils
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.steam.io.VDFNode
import groovy.swing.SwingBuilder
import groovy.util.logging.Log

import javax.swing.*
import java.awt.*
import java.util.logging.Level

/**
 * TODO: @CompileStatic, @TypeChecked
 * @author TimePath
 */
@Log
class ServerTest extends JPanel {

    static void main(String[] args) {
        log.log(Level.INFO, "Started")
        new SwingBuilder().edt {
            log.log(Level.INFO, "EDT")
            dialog(title: "Server browser",
                    defaultCloseOperation: JDialog.DISPOSE_ON_CLOSE,
                    preferredSize: [800, 600] as Dimension) {
                widget(new ServerTest())
            }.with {
                pack()
                locationRelativeTo = null
                visible = true
                ServerTest.log.log(Level.INFO, "Visible")
            }
        }
    }

    ServerTest() {
        JTextArea server, favorites, history, internet
        SourceServer ss = new SourceServer("27.50.71.201", 27016)
        layout = new BorderLayout()
        SwingBuilder sb = new SwingBuilder()
        sb.build {
            add splitPane {
                scrollPane {
                    server = textArea()
                }
                doOutside {
                    server.append("Server: ${ss.getAddress()}\n")
                    try {
                        log.info("Getting info ...")
                        ss.getInfo(new ServerListener() {
                            @Override
                            void inform(String update) {
                                server.append("${update}\n")
                            }
                        })
                        log.info("Getting rules ...")
                        server.append("Rules: \n")
                        ss.getRules(new ServerListener() {
                            @Override
                            void inform(String update) {
                                server.append("${update}\n")
                            }
                        })
                    } catch (IOException ex) {
                        log.log(Level.WARNING, null, ex)
                    }
                }
                tabbedPane {
                    scrollPane(title: "Favorites") {
                        favorites = textArea()
                    }
                    scrollPane(title: "History") {
                        history = textArea()
                    }
                    doOutside {
                        VDFNode vdf = VDF.load(new File(SteamUtils.getUserData(), "7/remote/serverbrowser_hist.vdf"))
                        VDFNode filters = vdf.get("Filters")
                        HashMap<String, JTextArea> map = [Favorites: favorites, History: history] as HashMap<String, JTextArea>
                        map.collect { k, v ->
                            for (VDFNode n : filters.get(k).getNodes()) {
                                v.append """\
                                        Favorite: ${n.getCustom()}
                                        Name: ${n.getValue("name")}
                                        Address: ${n.getValue("address")}
                                        Last Played: ${
                                    DateUtils.parse(Long.parseLong((String) n.getValue("LastPlayed")))
                                }\n""".stripIndent()
                            }
                        }
                    }
                    scrollPane(title: "Internet") {
                        internet = textArea()
                    }
                    doOutside {
                        log.info("Querying master ...")
                        MasterServer.SOURCE.query(MasterServer.Region.AUSTRALIA, $/\gamedir\tf/$, new ServerListener() {
                            @Override
                            void inform(String update) {
                                internet.append(update + '\n')
                            }
                        })
                    }
                }
            }
        }
    }
}
