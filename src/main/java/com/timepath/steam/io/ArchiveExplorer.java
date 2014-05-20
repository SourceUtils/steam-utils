package com.timepath.steam.io;

import com.timepath.FileUtils;
import com.timepath.io.utils.ViewableData;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.gcf.GCF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.VPK;
import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.swing.DirectoryTreeCellRenderer;
import com.timepath.vfs.SimpleVFile;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
public class ArchiveExplorer extends JPanel {

    private static final Logger                    LOG       = Logger.getLogger(ArchiveExplorer.class.getName());
    protected final        Collection<ExtendedVFile> archives  = new LinkedList<>();
    protected final        List<ExtendedVFile>       toExtract = new LinkedList<>();
    protected DefaultTreeModel  treeModel;
    protected DefaultTableModel tableModel;
    protected ExtendedVFile     selectedArchive;
    protected JPopupMenu        popupMenu;
    protected JMenuItem         extractMenuItem;
    protected JXTable           table;
    protected JMenuBar          menuBar;
    protected JTree             tree;

    public ArchiveExplorer() {
        this.setLayout(new BorderLayout());
        this.add(initComponents(), BorderLayout.CENTER);
    }

    /**
     * @param args
     *         the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                f.setTitle("Archive Explorer");
                f.setPreferredSize(new Dimension(800, 500));
                ArchiveExplorer ae = new ArchiveExplorer();
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

    protected static Object[] attrs(SimpleVFile simple) {
        Object[] attrs = { simple, simple.length(), FileUtils.extension(simple.getName()), null, simple.getPath(), null, null };
        if(simple instanceof ExtendedVFile) {
            ExtendedVFile extended = (ExtendedVFile) simple;
            attrs[3] = extended.getRoot();
            attrs[5] = extended.getAttributes();
            attrs[6] = extended.isComplete();
        }
        return attrs;
    }

    protected void addArchive(ExtendedVFile a) {
        archives.add(a);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(a);
        a.analyze(node, false);
        treeModel.insertNodeInto(node, (MutableTreeNode) treeModel.getRoot(), treeModel.getChildCount(treeModel.getRoot()));
        treeModel.reload();
    }

    protected void directoryChanged(ExtendedVFile dir) {
        if(!dir.isDirectory()) {
            return;
        }
        tableModel.setRowCount(0);
        for(SimpleVFile c : dir.children()) {
            if(c.isDirectory()) {
                continue;
            }
            tableModel.addRow(attrs(c));
        }
        table.packAll();
    }

    protected void extractablesUpdated() {
        extractMenuItem.setEnabled(!toExtract.isEmpty());
    }

    protected void load(File f) throws IOException {
        String ext = FileUtils.extension(f);
        ExtendedVFile a;
        switch(ext) {
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
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                tree.setSelectionPath(null);
                tableModel.setRowCount(0);
                Collection<SimpleVFile> children = new LinkedList<>();
                for(ExtendedVFile a : archives) {
                    children.addAll(a.find(search, a));
                }
                for(SimpleVFile c : children) {
                    if(!c.isDirectory()) {
                        publish(attrs(c));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for(Object[] rowData : chunks) {
                    tableModel.addRow(rowData);
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(ArchiveExplorer.this, "Done");
            }
        }.execute();
    }

    protected Component initComponents() {
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
                setEnabled(false);
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        extract(toExtract);
                    }
                });
            }});
            add(new JMenuItem("Properties") {{
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        properties();
                    }
                });
            }});
        }};
        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true) {{
            setDividerLocation(200);
            setOneTouchExpandable(true);
            setLeftComponent(new JScrollPane(tree = new JTree(new DefaultMutableTreeNode("root")) {{
                setRootVisible(false);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        treeClicked(evt);
                    }
                });
                addTreeSelectionListener(new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent evt) {
                        directoryChanged(evt);
                    }
                });
                getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                setCellRenderer(new DirectoryTreeCellRenderer());
                ArchiveExplorer.this.treeModel = (DefaultTreeModel) getModel();
            }}));
            setRightComponent(new JPanel(new BorderLayout()) {{
                add(new JPanel(new BorderLayout()) {{
                    final JTextField field;
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
                add(new JScrollPane(table = new JXTable() {{
                    setColumnControlVisible(true);
                    setHorizontalScrollEnabled(true);
                    setAutoCreateRowSorter(true);
                    setFillsViewportHeight(true);
                    getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                            tableClicked(evt);
                        }
                    });
                    setModel(tableModel = new DefaultTableModel(new Object[][] { }, new String[] {
                            "Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete"
                    })
                    {
                        final Class[] types = {
                                Object.class, Integer.class, String.class, Object.class, String.class, Object.class, Boolean.class
                        };
                        final boolean[] canEdit = { false, false, false, false, false, false, false };

                        @Override
                        public Class getColumnClass(int columnIndex) {
                            return types[columnIndex];
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                            return canEdit[columnIndex];
                        }
                    });
                    setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
                        @Override
                        public Component getTableCellEditorComponent(JTable table,
                                                                     Object value,
                                                                     boolean isSelected,
                                                                     int row,
                                                                     int column)
                        {
                            Object val = table.getValueAt(row, 0);
                            if(val instanceof ExtendedVFile) {
                                directoryChanged((ExtendedVFile) val);
                            }
                            return null;
                        }
                    });
                    setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table,
                                                                       Object value,
                                                                       boolean isSelected,
                                                                       boolean hasFocus,
                                                                       int row,
                                                                       int column)
                        {
                            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            if(comp instanceof JLabel) {
                                JLabel label = (JLabel) comp;
                                if(value instanceof ViewableData) {
                                    ViewableData data = (ViewableData) value;
                                    label.setIcon(null);
                                    label.setIcon(data.getIcon());
                                    label.setText(data.toString());
                                    return label;
                                }
                            }
                            return comp;
                        }
                    });
                }}), BorderLayout.CENTER);
            }});
        }};
    }

    protected Frame getFrame() {
        return null;
    }

    protected void open() {
        try {
            File[] fs = new NativeFileChooser().setParent(getFrame())
                                               .setTitle("Open archive")
                                               .setDirectory(SteamUtils.getSteamApps())
                                               .setMultiSelectionEnabled(true)
                                               .addFilter(new BaseFileChooser.ExtensionFilter("VPK directory files", "_dir.vpk"))
                                               .addFilter(new BaseFileChooser.ExtensionFilter("VPK files", ".vpk"))
                                               .addFilter(new BaseFileChooser.ExtensionFilter("GCF files", ".gcf"))
                                               .choose();
            if(fs == null) {
                return;
            }
            for(File f : fs) {
                if(f == null) {
                    LOG.warning("File is null");
                    return;
                }
                load(f);
            }
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void directoryChanged(TreeSelectionEvent evt) {
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            tableModel.setRowCount(0);
            return;
        }
        Object node = selection.getLastPathComponent();
        if(!( node instanceof DefaultMutableTreeNode )) {
            return;
        }
        Object obj = ( (DefaultMutableTreeNode) node ).getUserObject();
        ExtendedVFile dir = null;
        if(obj instanceof ExtendedVFile) {
            dir = (ExtendedVFile) obj;
        }
        if(dir == null) {
            return;
        }
        directoryChanged(dir);
    }

    protected void extract(List<ExtendedVFile> items) {
        try {
            File[] outs = new NativeFileChooser().setParent(getFrame())
                                                 .setTitle("Select extraction directory")
                                                 .setMultiSelectionEnabled(false)
                                                 .setDialogType(BaseFileChooser.DialogType.OPEN_DIALOG)
                                                 .setFileMode(BaseFileChooser.FileMode.DIRECTORIES_ONLY)
                                                 .choose();
            if(outs == null) {
                return;
            }
            File out = outs[0];
            for(ExtendedVFile e : items) {
                try {
                    e.extract(out);
                } catch(IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            LOG.info("Done");
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void treeClicked(MouseEvent evt) {
        if(SwingUtilities.isRightMouseButton(evt)) {
            TreePath clicked = tree.getPathForLocation(evt.getX(), evt.getY());
            if(clicked == null) {
                return;
            }
            if(( tree.getSelectionPaths() == null ) || !Arrays.asList(tree.getSelectionPaths()).contains(clicked)) {
                tree.setSelectionPath(clicked);
            }
            toExtract.clear();
            TreePath[] paths = tree.getSelectionPaths();
            for(TreePath p : paths) {
                if(!( p.getLastPathComponent() instanceof DefaultMutableTreeNode )) {
                    return;
                }
                Object userObject = ( (DefaultMutableTreeNode) p.getLastPathComponent() ).getUserObject();
                if(userObject instanceof ExtendedVFile) {
                    selectedArchive = (ExtendedVFile) userObject;
                    toExtract.add(selectedArchive);
                }
            }
            extractablesUpdated();
            popupMenu.show(tree, evt.getX(), evt.getY());
        }
    }

    protected void tableClicked(MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        if(row == -1) {
            return;
        }
        int[] selectedRows = table.getSelectedRows();
        Arrays.sort(selectedRows);
        if(Arrays.binarySearch(selectedRows, row) < 0) {
            table.setRowSelectionInterval(row, row);
        }
        toExtract.clear();
        int[] selected = table.getSelectedRows();
        for(int r : selected) {
            Object userObject = tableModel.getValueAt(table.convertRowIndexToModel(r), 0);
            if(userObject instanceof ExtendedVFile) {
                toExtract.add((ExtendedVFile) userObject);
            }
        }
        selectedArchive = null;
        extractablesUpdated();
        if(SwingUtilities.isRightMouseButton(evt)) {
            popupMenu.show(table, evt.getX(), evt.getY());
        } else if(SwingUtilities.isLeftMouseButton(evt) && ( evt.getClickCount() >= 2 )) {
            ExtendedVFile e = toExtract.get(0);
            File dir = new File(System.getProperty("java.io.tmpdir"));
            File f = new File(dir, e.getName());
            try {
                e.extract(dir);
                f.deleteOnExit();
                if(!OS.isLinux()) {
                    Desktop.getDesktop().open(f);
                } else {
                    Runtime.getRuntime().exec(new String[] { "xdg-open", f.getPath() });
                }
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void properties() {
        String title;
        String message = "";
        if(selectedArchive != null) {
            title = selectedArchive.toString();
            //            message = "V" + selectedArchive.header.applicationVersion + "\n";
        } else {
            ExtendedVFile last = toExtract.get(toExtract.size() - 1);
            title = last.getName();
            message += "Entry: " + last.getAbsoluteName() + '\n';
            message += last.getChecksum() + " vs " + last.calculateChecksum() + '\n';
        }
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    protected void mount(int appID) {
        try {
            addArchive(ACF.fromManifest(appID));
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
