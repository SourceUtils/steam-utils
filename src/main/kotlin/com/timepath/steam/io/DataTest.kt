package com.timepath.steam.io

import com.timepath.DataUtils
import com.timepath.plaf.OS
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.blob.Blob
import com.timepath.steam.io.bvdf.BVDF

import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.text.MessageFormat
import java.util.StringTokenizer
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger
import java.awt.Dimension
import java.awt.BorderLayout
import java.awt.EventQueue

/**
 * @author TimePath
 */
SuppressWarnings("serial")
class DataTest
/**
 * Creates new form VDFTest
 */
private() : JFrame() {
    private var jTree1: JTree? = null

    {
        initComponents()
        setDropTarget(object : DropTarget() {
            override fun drop(e: DropTargetDropEvent) {
                try {
                    val context = e.getDropTargetContext()
                    e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
                    val t = e.getTransferable()
                    var file: File? = null
                    if (OS.isLinux()) {
                        val nixFileDataFlavor = DataFlavor("text/uri-list;class=java.lang.String")
                        val data = t.getTransferData(nixFileDataFlavor) as String
                        run {
                            val st = StringTokenizer(data, "\r\n")
                            while (st.hasMoreTokens()) {
                                val token = st.nextToken().trim()
                                if (token.startsWith("#") || token.isEmpty()) {
                                    // comment line, by RFC 2483
                                    continue
                                }
                                try {
                                    file = File(URI(token))
                                } catch (ignored: Exception) {
                                }

                            }
                        }
                    } else {
                        val data = t.getTransferData(DataFlavor.javaFileListFlavor)
                        if (data is Iterable<*>) {
                            for (o in data) {
                                if (o is File) {
                                    file = o
                                }
                            }
                        }
                    }
                    if (file != null) {
                        open(file)
                    }
                } catch (ex: ClassNotFoundException) {
                    LOG.log(Level.SEVERE, null, ex)
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, null, ex)
                } catch (ex: UnsupportedFlavorException) {
                    LOG.log(Level.SEVERE, null, ex)
                } catch (ex: InvalidDnDOperationException) {
                    LOG.log(Level.SEVERE, null, ex)
                } finally {
                    e.dropComplete(true)
                    repaint()
                }
            }
        })
    }

    private fun initComponents() {
        val jScrollPane1 = JScrollPane()
        jTree1 = JTree()
        val jMenuBar1 = JMenuBar()
        val jMenu1 = JMenu()
        val jMenuItem1 = JMenuItem()
        val jMenuItem2 = JMenuItem()
        val jMenuItem3 = JMenuItem()
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        setTitle("Data viewer")
        setMinimumSize(Dimension(300, 300))
        val treeNode1 = DefaultMutableTreeNode("root")
        jTree1!!.setModel(DefaultTreeModel(treeNode1))
        jTree1!!.setEditable(true)
        jTree1!!.setLargeModel(true)
        jTree1!!.setRootVisible(false)
        jScrollPane1.setViewportView(jTree1)
        getContentPane().add(jScrollPane1, BorderLayout.CENTER)
        jMenu1.setText("File")
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
        jMenuItem1.setMnemonic('O')
        jMenuItem1.setText("Open")
        jMenuItem1.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                openFile(e)
            }
        })
        jMenu1.add(jMenuItem1)
        jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK))
        jMenuItem2.setMnemonic('A')
        jMenuItem2.setText("AppInfo")
        jMenuItem2.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                appInfo(e)
            }
        })
        jMenu1.add(jMenuItem2)
        jMenuItem3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK))
        jMenuItem3.setMnemonic('P')
        jMenuItem3.setText("PackageInfo")
        jMenuItem3.addActionListener(object : ActionListener {
            override fun actionPerformed(evt: ActionEvent) {
                packageInfo(evt)
            }
        })
        jMenu1.add(jMenuItem3)
        jMenuBar1.add(jMenu1)
        setJMenuBar(jMenuBar1)
        pack()
    }

    private fun openFile(e: ActionEvent) {
        try {
            val fs = NativeFileChooser().setDirectory(SteamUtils.getSteam()).setParent(this).setTitle("Open VDF").choose()
            if (fs == null) {
                return
            }
            open(fs[0])
        } catch (ex: IOException) {
            Logger.getLogger(javaClass<DataTest>().getName()).log(Level.SEVERE, null, ex)
        }

    }

    private fun open(f: File?) {
        if (f == null) {
            LOG.info("File is null")
            return
        }
        LOG.log(Level.INFO, "File is {0}", f)
        val model = jTree1!!.getModel() as DefaultTreeModel
        val pseudo = DefaultMutableTreeNode(f.getPath())
        model.setRoot(pseudo)
        object : SwingWorker<DefaultMutableTreeNode, Void>() {
            throws(javaClass<Exception>())
            override fun doInBackground(): DefaultMutableTreeNode? {
                var n: DefaultMutableTreeNode? = null
                try {
                    if (f.getName().toLowerCase().endsWith(".blob")) {
                        val bin = Blob()
                        bin.readExternal(DataUtils.mapFile(f))
                        n = bin.root
                    } else if (f.getName().toLowerCase().matches("^.*(vdf|res|bin|txt|styles)$")) {
                        if (VDF.isBinary(f)) {
                            val bin = BVDF()
                            bin.readExternal(DataUtils.mapFile(f))
                            n = bin.root
                        } else {
                            n = VDF.load(FileInputStream(f)).toTreeNode()
                        }
                    } else {
                        JOptionPane.showMessageDialog(this@DataTest, MessageFormat.format("{0} is not supported", f.getAbsolutePath()), "Invalid file", JOptionPane.ERROR_MESSAGE)
                    }
                } catch (e: StackOverflowError) {
                    LOG.warning("Stack Overflow")
                } catch (e: Exception) {
                    LOG.log(Level.SEVERE, null, e)
                }

                return n
            }

            override fun done() {
                try {
                    val n = get()
                    if (n != null) {
                        pseudo.add(n)
                    }
                    model.reload()
                    //                    TreeUtils.expand(DataTest.this.jTree1);
                } catch (ex: InterruptedException) {
                    Logger.getLogger(javaClass<DataTest>().getName()).log(Level.SEVERE, null, ex)
                } catch (ex: ExecutionException) {
                    Logger.getLogger(javaClass<DataTest>().getName()).log(Level.SEVERE, null, ex)
                }

            }
        }.execute()
    }

    private fun appInfo(evt: ActionEvent) {
        open(File(SteamUtils.getSteam(), "appcache/appinfo.vdf"))
    }

    private fun packageInfo(evt: ActionEvent) {
        open(File(SteamUtils.getSteam(), "appcache/packageinfo.vdf"))
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<DataTest>().getName())

        /**
         * @param args the command line arguments
         */
        public fun main(args: Array<String>) {
            EventQueue.invokeLater {
                DataTest().setVisible(true)
            }
        }
    }
}

fun main(args: Array<String>) = DataTest.main(args)
