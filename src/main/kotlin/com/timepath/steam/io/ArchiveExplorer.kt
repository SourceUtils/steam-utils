package com.timepath.steam.io

import com.timepath.FileUtils
import com.timepath.plaf.OS
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.gcf.GCF
import com.timepath.steam.io.storage.ACF
import com.timepath.steam.io.storage.VPK
import com.timepath.vfs.provider.ExtendedVFile
import com.timepath.vfs.SimpleVFile
import org.jdesktop.swingx.JXTreeTable
import org.jdesktop.swingx.treetable.AbstractTreeTableModel

import javax.swing.*
import java.awt.event.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.awt.BorderLayout
import kotlin.platform.platformStatic
import java.awt.EventQueue
import java.awt.Dimension
import java.awt.Desktop
import java.awt.Frame
import java.awt.Component

/**
 * @author TimePath
 */
SuppressWarnings("serial")
public class ArchiveExplorer : JPanel() {
    protected val archives: MutableList<SimpleVFile> = LinkedList()
    protected var tableModel: ArchiveTreeTableModel? = null
    protected var popupMenu: JPopupMenu? = null
    protected var extractMenuItem: JMenuItem? = null
    protected var treeTable: JXTreeTable? = null
    public var menuBar: JMenuBar? = null
        protected set

    {
        this.add(initComponents(), BorderLayout.CENTER)
    }

    protected fun addArchive(a: SimpleVFile) {
        archives.add(a)
        tableModel = ArchiveTreeTableModel(archives)
        val table = treeTable!!
        table.setTreeTableModel(tableModel)
        // hide the last few columns
        for (i in 3.indices) {
            table.getColumnExt(4).setVisible(false)
        }
    }

    throws(javaClass<IOException>())
    protected fun load(f: File) {
        val ext = FileUtils.extension(f)
        val a: ExtendedVFile?
        when (ext) {
            "gcf" -> a = GCF(f)
            "vpk" -> a = VPK.loadArchive(f)
            else -> {
                LOG.log(Level.WARNING, "Unrecognised archive: {0}", f)
                return
            }
        }
        addArchive(a!!)
    }

    protected fun search(search: String) {
        val searchNode = object : SimpleVFile() {
            override val isDirectory = true

            override val name = "Search: $search"

            override fun openStream() = null
        }
        object : SwingWorker<Void, SimpleVFile>() {
            throws(javaClass<Exception>())
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

            override fun done() {
                JOptionPane.showMessageDialog(this@ArchiveExplorer, "Done")
            }
        }.execute()
    }

    protected fun initComponents(): Component {
        setLayout(BorderLayout())
        menuBar = object : JMenuBar() {
            {
                add(object : JMenu("File") {
                    {
                        setMnemonic('F')
                        add(object : JMenuItem("Open") {
                            {
                                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(evt: ActionEvent) {
                                        open()
                                    }
                                })
                            }
                        })
                        add(object : JMenuItem("Mount TF2") {
                            {
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(evt: ActionEvent) {
                                        mount(440)
                                    }
                                })
                            }
                        })
                    }
                })
            }
        }
        popupMenu = object : JPopupMenu() {
            {
                extractMenuItem = object : JMenuItem("Extract") {
                    {
                        addActionListener(object : ActionListener {
                            override fun actionPerformed(evt: ActionEvent) {
                                extract(selected)
                            }
                        })
                    }
                }
                add(extractMenuItem)
                add(object : JMenuItem("Properties") {
                    {
                        addActionListener(object : ActionListener {
                            override fun actionPerformed(evt: ActionEvent) {
                                properties(selected)
                            }
                        })
                    }
                })
            }
        }
        return object : JPanel(BorderLayout()) {
            {
                add(object : JPanel(BorderLayout()) {
                    {
                        val field: JTextField
                        field = object : JTextField() {
                            {
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(evt: ActionEvent) {
                                        search(getText())
                                    }
                                })
                            }
                        }
                        add(field, BorderLayout.CENTER)
                        add(object : JButton("Search") {
                            {
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(evt: ActionEvent) {
                                        search(field.getText())
                                    }
                                })
                            }
                        }, BorderLayout.EAST)
                    }
                }, BorderLayout.NORTH)
                treeTable = object : JXTreeTable() {
                    {
                        setLargeModel(true)
                        setColumnControlVisible(true)
                        setHorizontalScrollEnabled(true)
                        setAutoCreateRowSorter(true)
                        setFillsViewportHeight(true)
                        getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(evt: MouseEvent) {
                                tableClicked(evt)
                            }
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
                }
                add(JScrollPane(treeTable), BorderLayout.CENTER)
            }
        }
    }

    /**
     * @return the selected files, last in, first out
     */
    protected val selected: List<SimpleVFile> get() {
        val ret = LinkedList<SimpleVFile>()
        for (treePath in treeTable!!.getTreeSelectionModel().getSelectionPaths()) {
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
            val fs = NativeFileChooser().setParent(getFrame(this)).setTitle("Open archive").setDirectory(SteamUtils.getSteamApps()).setMultiSelectionEnabled(true).addFilter(BaseFileChooser.ExtensionFilter("VPK directory files", "_dir.vpk")).addFilter(BaseFileChooser.ExtensionFilter("VPK files", ".vpk")).addFilter(BaseFileChooser.ExtensionFilter("GCF files", ".gcf")).choose()
            if (fs == null) {
                return
            }
            for (f in fs) {
                if (f == null) {
                    LOG.warning("File is null")
                    return
                }
                load(f)
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    protected fun extract(items: List<SimpleVFile>) {
        try {
            val outs = NativeFileChooser().setParent(getFrame(this)).setTitle("Select extraction directory").setMultiSelectionEnabled(false).setDialogType(BaseFileChooser.DialogType.OPEN_DIALOG).setFileMode(BaseFileChooser.FileMode.DIRECTORIES_ONLY).choose()
            if (outs == null) {
                return
            }
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
                Runtime.getRuntime().exec(array("xdg-open", f.getPath()))
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    protected fun tableClicked(evt: MouseEvent) {
        val selected = selected
        if (selected.isEmpty()) return
        val table = treeTable!!
        if (SwingUtilities.isRightMouseButton(evt)) {
            val p = evt.getPoint()
            val rowNumber = table.rowAtPoint(p)
            val model = table.getSelectionModel()
            model.setSelectionInterval(rowNumber, rowNumber)
            extractMenuItem!!.setEnabled(true)
            popupMenu!!.show(table, evt.getX(), evt.getY())
        } else if (SwingUtilities.isLeftMouseButton(evt) && (evt.getClickCount() >= 2)) {
            val file = selected[0]
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

        val columns = array("Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete")
        val types = array<Class<*>>(javaClass<String>(), javaClass<Int>(), javaClass<String>(), javaClass<Any>(), javaClass<String>(), javaClass<Any>(), javaClass<Boolean>())

        override fun getColumnCount() = columns.size()

        override fun getValueAt(node: Any, column: Int): Any? = when {
            node !is SimpleVFile -> null
            column == 0 -> node
            column == 1 -> node.length
            column == 2 -> node.name.substringBeforeLast('.')
            column == 4 -> node.path
            node !is ExtendedVFile -> null
            column == 3 -> node.root
            column == 5 -> node.attributes
            column == 6 -> node.isComplete
            else -> null
        }

        override fun getColumnClass(column: Int): Class<*> {
            return types[column]
        }

        override fun getColumnName(column: Int): String {
            return columns[column]
        }

        override fun isCellEditable(node: Any?, columnIndex: Int): Boolean {
            return false
        }

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

        SuppressWarnings("SuspiciousMethodCalls")
        override fun getIndexOfChild(parent: Any, child: Any): Int {
            if (parent is SimpleVFile) {
                return ArrayList(parent.list()).indexOf(child)
            }
            return archives.indexOf(child)
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<ArchiveExplorer>().getName())

        /**
         * @param args the command line arguments
         */
        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater(object : Runnable {
                override fun run() {
                    val f = JFrame()
                    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                    f.setTitle("Archive Explorer")
                    f.setPreferredSize(Dimension(800, 500))
                    val ae = ArchiveExplorer()
                    f.setContentPane(ae)
                    f.setJMenuBar(ae.menuBar)
                    f.pack()
                    f.setLocationRelativeTo(null)
                    f.setVisible(true)
                }
            })
        }
    }
}
