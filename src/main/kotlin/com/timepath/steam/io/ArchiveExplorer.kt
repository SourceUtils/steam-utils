package com.timepath.steam.io

import com.timepath.plaf.OS
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.gcf.GCF
import com.timepath.steam.io.storage.ACF
import com.timepath.steam.io.storage.VPK
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ExtendedVFile
import com.timepath.with
import org.jdesktop.swingx.JXTreeTable
import org.jdesktop.swingx.treetable.AbstractTreeTableModel
import java.awt.*
import java.awt.event.*
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.*
import kotlin.platform.platformStatic

public class ArchiveExplorer : JPanel() {
    protected var tableModel: ArchiveTreeTableModel? = null
    protected val archives: MutableList<SimpleVFile> = LinkedList()
    protected val popupMenu: JPopupMenu
    protected val extractMenuItem: JMenuItem
    protected val treeTable: JXTreeTable
    public val menuBar: JMenuBar

    protected fun addArchive(a: SimpleVFile) {
        archives.add(a)
        tableModel = ArchiveTreeTableModel(archives)
        val table = treeTable
        table.setTreeTableModel(tableModel)
        // hide the last few columns
        repeat(3) {
            table.getColumnExt(4).setVisible(false)
        }
    }

    throws(IOException::class)
    protected fun load(f: File) {
        when (f.name.substringAfterLast('.')) {
            "gcf" -> GCF(f)
            "vpk" -> VPK.loadArchive(f)
            else -> {
                LOG.log(Level.WARNING, "Unrecognised archive: {0}", f)
                return
            }
        }?.let { addArchive(it) }
    }

    protected fun search(search: String) {
        val searchNode = object : SimpleVFile() {
            override val isDirectory = true

            override val name = "Search: $search"

            override fun openStream() = null
        }
        object : SwingWorker<Void, SimpleVFile>() {
            override fun doInBackground(): Void? {
                val children = LinkedList<SimpleVFile>()
                for (a in archives) {
                    children.addAll(a.find(search))
                }
                addArchive(searchNode)
                for (c in children) {
                    if (!c.isDirectory) {
                        publish(c)
                    }
                }
                return null
            }

            override fun process(chunks: List<SimpleVFile>) {
                for (c in chunks) {
                    searchNode.add(c)
                }
            }

            override fun done() = JOptionPane.showMessageDialog(this@ArchiveExplorer, "Done")
        }.execute()
    }

    init {
        setLayout(BorderLayout())
        menuBar = JMenuBar() with {
            add(JMenu("File") with {
                setMnemonic('F')
                add(JMenuItem("Open") with {
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
                    addActionListener { open() }
                })
                add(JMenuItem("Mount TF2") with {
                    addActionListener { mount(440) }
                })
            })
        }
        extractMenuItem = JMenuItem("Extract") with {
            addActionListener { extract(selected) }
        }
        popupMenu = JPopupMenu() with {
            add(extractMenuItem)
            add(JMenuItem("Properties") with {
                addActionListener { properties(selected) }
            })
        }
        treeTable = JXTreeTable() with {
            setLargeModel(true)
            setColumnControlVisible(true)
            setHorizontalScrollEnabled(true)
            setAutoCreateRowSorter(true)
            setFillsViewportHeight(true)
            getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(evt: MouseEvent) = tableClicked(evt)
            })
            val self = this
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(evt: KeyEvent) {
                    val selectedRow = self.getSelectedRow()
                    val selection = self.getSelectionModel()
                    when (evt.getKeyCode()) {
                        KeyEvent.VK_RIGHT -> {
                            self.expandRow(selectedRow)
                            selection.setSelectionInterval(selectedRow + 1, selectedRow + 1)
                        }
                        KeyEvent.VK_LEFT -> if (self.isExpanded(selectedRow)) {
                            self.collapseRow(selectedRow)
                        } else {
                            // find last deepest node above this one
                            val pathForRow = self.getPathForRow(selectedRow)
                            val parentPath = pathForRow.getParentPath()
                            val currentDepth = pathForRow.getPathCount()
                            run {
                                var i = selectedRow
                                while (i >= 0) {
                                    selection.setSelectionInterval(i, i)
                                    val pathForPrevious = self.getPathForRow(i)
                                    if (pathForPrevious.getPathCount() > currentDepth) break
                                    if (pathForPrevious == parentPath) break
                                    i--
                                }
                            }
                        }
                    }
                }
            })
        }
        val content = JPanel(BorderLayout()).with {
            add(JPanel(BorderLayout()) with {
                val field = JTextField() with {
                    addActionListener { search(getText()) }
                }
                add(field, BorderLayout.CENTER)
                add(JButton("Search") with {
                    addActionListener { search(field.getText()) }
                }, BorderLayout.EAST)
            }, BorderLayout.NORTH)
            add(JScrollPane(treeTable), BorderLayout.CENTER)
        }
        add(content, BorderLayout.CENTER)
    }

    /**
     * @return the selected files, last in, first out
     */
    protected val selected: List<SimpleVFile> get() {
        val ret = LinkedList<SimpleVFile>()
        for (treePath in treeTable.getTreeSelectionModel().getSelectionPaths()) {
            val lastPathComponent = treePath.getLastPathComponent()
            if (lastPathComponent is SimpleVFile) {
                ret.addFirst(lastPathComponent)
            }
        }
        return ret
    }

    protected fun getFrame(c: Component?): Frame? {
        if (c == null) {
            return JOptionPane.getRootFrame()
        } else if (c is Frame) {
            return c
        } else {
            return getFrame(c.getParent())
        }
    }

    protected fun open() {
        try {
            val fs = NativeFileChooser()
                    .setParent(getFrame(this))
                    .setTitle("Open archive")
                    .setDirectory(SteamUtils.getSteamApps())
                    .setMultiSelectionEnabled(true)
                    .addFilter(BaseFileChooser.ExtensionFilter("VPK directory files", "_dir.vpk"))
                    .addFilter(BaseFileChooser.ExtensionFilter("VPK files", ".vpk"))
                    .addFilter(BaseFileChooser.ExtensionFilter("GCF files", ".gcf"))
                    .choose() ?: return
            for (f in fs) {
                load(f)
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    protected fun extract(items: List<SimpleVFile>) {
        try {
            val outs = NativeFileChooser()
                    .setParent(getFrame(this))
                    .setTitle("Select extraction directory")
                    .setMultiSelectionEnabled(false)
                    .setDialogType(BaseFileChooser.DialogType.OPEN_DIALOG)
                    .setFileMode(BaseFileChooser.FileMode.DIRECTORIES_ONLY)
                    .choose() ?: return
            val out = outs[0]
            for (e in items) {
                try {
                    e.extract(out)
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, null, ex)
                }
            }
            LOG.info("Done")
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }
    }

    protected fun open(e: SimpleVFile) {
        val dir = File(System.getProperty("java.io.tmpdir"))
        val f = File(dir, e.name)
        try {
            e.extract(dir)
            f.deleteOnExit()
            if (!OS.isLinux()) {
                Desktop.getDesktop().open(f)
            } else {
                Runtime.getRuntime().exec(arrayOf("xdg-open", f.getPath()))
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    protected fun tableClicked(evt: MouseEvent) {
        val selected = selected
        if (selected.isEmpty()) return
        val table = treeTable
        if (SwingUtilities.isRightMouseButton(evt)) {
            val p = evt.getPoint()
            val rowNumber = table.rowAtPoint(p)
            val model = table.getSelectionModel()
            model.setSelectionInterval(rowNumber, rowNumber)
            extractMenuItem.setEnabled(true)
            popupMenu.show(table, evt.getX(), evt.getY())
        } else if (SwingUtilities.isLeftMouseButton(evt) && (evt.getClickCount() >= 2)) {
            val file = selected.first()
            if (file.isDirectory) {
                val path = table.getPathForLocation(evt.getX(), evt.getY())
                if (table.isExpanded(path)) {
                    table.collapsePath(path)
                } else {
                    table.expandPath(path)
                }
            } else {
                open(file)
            }
        }
    }

    protected fun properties(list: List<SimpleVFile>) {
        if (list.isEmpty()) return
        val selected = list[0]
        if (selected is ExtendedVFile) {
            val title: String?
            var message = ""
            //        if(selectedArchive != null) {
            //            title = selectedArchive.toString();
            //            //            message = "V" + selectedArchive.header.applicationVersion + "\n";
            //        } else {
            title = selected.name
            message += "Entry: ${selected.absoluteName}\n"
            message += "${selected.checksum} vs ${selected.calculateChecksum()}"
            //        }
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE)
        }
    }

    protected fun mount(appID: Int) {
        try {
            addArchive(ACF.fromManifest(appID))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }
    }

    private inner class ArchiveTreeTableModel(
            private val archives: List<SimpleVFile> = emptyList()
    ) : AbstractTreeTableModel(Object()) {

        val columns = arrayOf("Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete")
        val types = arrayOf(javaClass<String>(), javaClass<Int>(), javaClass<String>(), javaClass<Any>(), javaClass<String>(), javaClass<Any>(), javaClass<Boolean>())

        override fun getColumnCount() = columns.size()

        override fun getValueAt(node: Any, column: Int): Any? = when {
            node !is SimpleVFile -> null
            column == 0 -> node
            column == 1 -> node.length
            column == 2 -> node.name.substringAfterLast('.')
            column == 4 -> node.path
            node !is ExtendedVFile -> null
            column == 3 -> node.root
            column == 5 -> node.attributes
            column == 6 -> node.isComplete
            else -> null
        }

        override fun getColumnClass(column: Int) = types[column]

        override fun getColumnName(column: Int) = columns[column]

        override fun isCellEditable(node: Any?, columnIndex: Int) = false

        override fun isLeaf(node: Any): Boolean {
            if (node is SimpleVFile) {
                return node.list().size() == 0
            }
            return false
        }

        override fun getChild(parent: Any, index: Int): Any {
            if (parent is SimpleVFile) {
                return ArrayList(parent.list())[index]
            }
            return archives[index]
        }

        override fun getChildCount(parent: Any): Int {
            if (parent is SimpleVFile) {
                return parent.list().size()
            }
            return archives.size()
        }

        override fun getIndexOfChild(parent: Any, child: Any): Int {
            if (parent is SimpleVFile) {
                return ArrayList(parent.list()).indexOf(child)
            }
            return archives.indexOf(child)
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<ArchiveExplorer>().getName())

        /**
         * @param args the command line arguments
         */
        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater {
                JFrame().with {
                    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                    setTitle("Archive Explorer")
                    this.setPreferredSize(Dimension(800, 500))
                    ArchiveExplorer().let {
                        setContentPane(it)
                        setJMenuBar(it.menuBar)
                    }
                    pack()
                    setLocationRelativeTo(null)
                }.setVisible(true)
            }
        }
    }
}
