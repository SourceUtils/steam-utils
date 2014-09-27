package com.timepath.steam.io;

import com.timepath.FileUtils;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.gcf.GCF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.VPK;
import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
public class ArchiveExplorer extends JPanel {

    private static final Logger LOG = Logger.getLogger(ArchiveExplorer.class.getName());
    protected final List<SimpleVFile> archives = new LinkedList<>();
    protected ArchiveTreeTableModel tableModel;
    protected JPopupMenu popupMenu;
    protected JMenuItem extractMenuItem;
    protected JXTreeTable treeTable;
    protected JMenuBar menuBar;

    public ArchiveExplorer() {
        this.add(initComponents(), BorderLayout.CENTER);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                @NotNull JFrame f = new JFrame();
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                f.setTitle("Archive Explorer");
                f.setPreferredSize(new Dimension(800, 500));
                @NotNull ArchiveExplorer ae = new ArchiveExplorer();
                f.setContentPane(ae);
                f.setJMenuBar(ae.getMenuBar());
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    protected void addArchive(SimpleVFile a) {
        archives.add(a);
        treeTable.setTreeTableModel(tableModel = new ArchiveTreeTableModel(archives));
        // hide the last few columns
        for (int i = 0; i < 3; i++) {
            treeTable.getColumnExt(4).setVisible(false);
        }
    }

    protected void load(@NotNull File f) throws IOException {
        @Nullable String ext = FileUtils.extension(f);
        @Nullable ExtendedVFile a;
        switch (ext) {
            case "gcf":
                a = new GCF(f);
                break;
            case "vpk":
                a = VPK.loadArchive(f);
                break;
            default:
                LOG.log(Level.WARNING, "Unrecognised archive: {0}", f);
                return;
        }
        addArchive(a);
    }

    protected void search(final String search) {
        @NotNull final SimpleVFile searchNode = new SimpleVFile() {
            @Override
            public boolean isDirectory() {
                return true;
            }

            @NotNull
            @Override
            public String getName() {
                return "Search: " + search;
            }

            @Nullable
            @Override
            public InputStream openStream() {
                return null;
            }
        };
        new SwingWorker<Void, SimpleVFile>() {
            @Nullable
            @Override
            protected Void doInBackground() throws Exception {
                @NotNull Collection<SimpleVFile> children = new LinkedList<>();
                for (@NotNull SimpleVFile a : archives) {
                    children.addAll(a.find(search));
                }
                addArchive(searchNode);
                for (@NotNull SimpleVFile c : children) {
                    if (!c.isDirectory()) {
                        publish(c);
                    }
                }
                return null;
            }

            @Override
            protected void process(@NotNull List<SimpleVFile> chunks) {
                for (SimpleVFile c : chunks) {
                    searchNode.add(c);
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(ArchiveExplorer.this, "Done");
            }
        }.execute();
    }

    @NotNull
    protected Component initComponents() {
        setLayout(new BorderLayout());
        menuBar = new JMenuBar() {{
            add(new JMenu("File") {{
                setMnemonic('F');
                add(new JMenuItem("Open") {{
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            open();
                        }
                    });
                }});
                add(new JMenuItem("Mount TF2") {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            mount(440);
                        }
                    });
                }});
            }});
        }};
        popupMenu = new JPopupMenu() {{
            add(extractMenuItem = new JMenuItem("Extract") {{
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        extract(getSelected());
                    }
                });
            }});
            add(new JMenuItem("Properties") {{
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        properties(getSelected());
                    }
                });
            }});
        }};
        return new JPanel(new BorderLayout()) {{
            add(new JPanel(new BorderLayout()) {{
                @NotNull final JTextField field;
                add(field = new JTextField() {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            search(getText());
                        }
                    });
                }}, BorderLayout.CENTER);
                add(new JButton("Search") {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            search(field.getText());
                        }
                    });
                }}, BorderLayout.EAST);
            }}, BorderLayout.NORTH);
            add(new JScrollPane(treeTable = new JXTreeTable() {{
                setLargeModel(true);
                setColumnControlVisible(true);
                setHorizontalScrollEnabled(true);
                setAutoCreateRowSorter(true);
                setFillsViewportHeight(true);
                getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(@NotNull MouseEvent evt) {
                        tableClicked(evt);
                    }
                });
                addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(@NotNull final KeyEvent evt) {
                        int selectedRow = treeTable.getSelectedRow();
                        ListSelectionModel selection = treeTable.getSelectionModel();
                        switch (evt.getKeyCode()) {
                            case KeyEvent.VK_RIGHT:
                                treeTable.expandRow(selectedRow);
                                selection.setSelectionInterval(selectedRow + 1, selectedRow + 1);
                                break;
                            case KeyEvent.VK_LEFT:
                                if (treeTable.isExpanded(selectedRow)) {
                                    treeTable.collapseRow(selectedRow);
                                } else {
                                    // find last deepest node above this one
                                    TreePath pathForRow = treeTable.getPathForRow(selectedRow);
                                    TreePath parentPath = pathForRow.getParentPath();
                                    int currentDepth = pathForRow.getPathCount();
                                    for (int i = selectedRow; i >= 0; i--) {
                                        selection.setSelectionInterval(i, i);
                                        TreePath pathForPrevious = treeTable.getPathForRow(i);
                                        if (pathForPrevious.getPathCount() > currentDepth) break;
                                        if (pathForPrevious == parentPath) break;
                                    }
                                }
                                break;
                        }
                    }
                });
            }}), BorderLayout.CENTER);
        }};
    }

    /**
     * @return the selected files, last in, first out
     */
    @NotNull
    protected List<? extends SimpleVFile> getSelected() {
        @NotNull LinkedList<SimpleVFile> ret = new LinkedList<>();
        for (@NotNull TreePath treePath : treeTable.getTreeSelectionModel().getSelectionPaths()) {
            Object lastPathComponent = treePath.getLastPathComponent();
            if (lastPathComponent instanceof SimpleVFile) {
                ret.addFirst((SimpleVFile) lastPathComponent);
            }
        }
        return ret;
    }

    @Nullable
    protected Frame getFrame(@Nullable Component c) {
        if (c == null) {
            return JOptionPane.getRootFrame();
        } else if (c instanceof Frame) {
            return (Frame) c;
        } else {
            return getFrame(c.getParent());
        }
    }

    protected void open() {
        try {
            @Nullable File[] fs = new NativeFileChooser().setParent(getFrame(this))
                    .setTitle("Open archive")
                    .setDirectory(SteamUtils.getSteamApps())
                    .setMultiSelectionEnabled(true)
                    .addFilter(new BaseFileChooser.ExtensionFilter("VPK directory files", "_dir.vpk"))
                    .addFilter(new BaseFileChooser.ExtensionFilter("VPK files", ".vpk"))
                    .addFilter(new BaseFileChooser.ExtensionFilter("GCF files", ".gcf"))
                    .choose();
            if (fs == null) {
                return;
            }
            for (@Nullable File f : fs) {
                if (f == null) {
                    LOG.warning("File is null");
                    return;
                }
                load(f);
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void extract(@NotNull List<? extends SimpleVFile> items) {
        try {
            @Nullable File[] outs = new NativeFileChooser().setParent(getFrame(this))
                    .setTitle("Select extraction directory")
                    .setMultiSelectionEnabled(false)
                    .setDialogType(BaseFileChooser.DialogType.OPEN_DIALOG)
                    .setFileMode(BaseFileChooser.FileMode.DIRECTORIES_ONLY)
                    .choose();
            if (outs == null) {
                return;
            }
            File out = outs[0];
            for (@NotNull SimpleVFile e : items) {
                try {
                    e.extract(out);
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            LOG.info("Done");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void open(@NotNull SimpleVFile e) {
        @NotNull File dir = new File(System.getProperty("java.io.tmpdir"));
        @NotNull File f = new File(dir, e.getName());
        try {
            e.extract(dir);
            f.deleteOnExit();
            if (!OS.isLinux()) {
                Desktop.getDesktop().open(f);
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", f.getPath()});
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void tableClicked(@NotNull MouseEvent evt) {
        @NotNull List<? extends SimpleVFile> selected = getSelected();
        if (selected.isEmpty()) return;
        if (SwingUtilities.isRightMouseButton(evt)) {
            @NotNull Point p = evt.getPoint();
            int rowNumber = treeTable.rowAtPoint(p);
            ListSelectionModel model = treeTable.getSelectionModel();
            model.setSelectionInterval(rowNumber, rowNumber);
            extractMenuItem.setEnabled(true);
            popupMenu.show(treeTable, evt.getX(), evt.getY());
        } else if (SwingUtilities.isLeftMouseButton(evt) && (evt.getClickCount() >= 2)) {
            SimpleVFile file = selected.get(0);
            if (file.isDirectory()) {
                TreePath path = treeTable.getPathForLocation(evt.getX(), evt.getY());
                if (treeTable.isExpanded(path)) {
                    treeTable.collapsePath(path);
                } else {
                    treeTable.expandPath(path);
                }
            } else {
                open(file);
            }
        }
    }

    protected void properties(@NotNull List<? extends SimpleVFile> list) {
        if (list.isEmpty()) return;
        SimpleVFile selected = list.get(0);
        if (selected instanceof ExtendedVFile) {
            @NotNull ExtendedVFile ext = (ExtendedVFile) selected;
            @Nullable String title;
            @NotNull String message = "";
            //        if(selectedArchive != null) {
            //            title = selectedArchive.toString();
            //            //            message = "V" + selectedArchive.header.applicationVersion + "\n";
            //        } else {
            title = ext.getName();
            message += "Entry: " + ext.getAbsoluteName() + '\n';
            message += ext.getChecksum() + " vs " + ext.calculateChecksum() + '\n';
            //        }
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected void mount(int appID) {
        try {
            addArchive(ACF.fromManifest(appID));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private class ArchiveTreeTableModel extends AbstractTreeTableModel {

        final String[] columns = new String[]{
                "Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete"
        };
        final Class[] types = new Class[]{
                String.class, Integer.class, String.class, Object.class, String.class, Object.class, Boolean.class
        };
        private List<? extends SimpleVFile> archives = Collections.emptyList();

        public ArchiveTreeTableModel(List<? extends SimpleVFile> top) {
            this();
            this.archives = top;
        }

        public ArchiveTreeTableModel() {
            super(new Object());
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Nullable
        @Override
        public Object getValueAt(Object node, int column) {
            if (node instanceof SimpleVFile) {
                @NotNull SimpleVFile simple = (SimpleVFile) node;
                switch (column) {
                    case 0:
                        return simple;
                    case 1:
                        return simple.length();
                    case 2:
                        return FileUtils.extension(simple.getName());
                    case 4:
                        return simple.getPath();
                }
                if (simple instanceof ExtendedVFile) {
                    @NotNull ExtendedVFile extended = (ExtendedVFile) simple;
                    switch (column) {
                        case 3:
                            return extended.getRoot();
                        case 5:
                            return extended.getAttributes();
                        case 6:
                            return extended.isComplete();
                    }
                }
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return types[column];
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public boolean isCellEditable(Object node, int columnIndex) {
            return false;
        }

        @Override
        public boolean isLeaf(Object node) {
            if (node instanceof SimpleVFile) {
                return ((SimpleVFile) node).list().size() == 0;
            }
            return false;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof SimpleVFile) {
                @NotNull SimpleVFile dir = (SimpleVFile) parent;
                return new ArrayList<>(dir.list()).get(index);
            }
            return archives.get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof SimpleVFile) {
                @NotNull SimpleVFile dir = (SimpleVFile) parent;
                return dir.list().size();
            }
            return archives.size();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public int getIndexOfChild(final Object parent, final Object child) {
            if (parent instanceof SimpleVFile) {
                @NotNull SimpleVFile dir = (SimpleVFile) parent;
                return new ArrayList<>(dir.list()).indexOf(child);
            }
            return archives.indexOf(child);
        }
    }
}
