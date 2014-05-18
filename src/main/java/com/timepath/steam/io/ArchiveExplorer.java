package com.timepath.steam.io;

import com.timepath.FileUtils;
import com.timepath.io.utils.ViewableData;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.VPK;
import com.timepath.steam.io.storage.gcf.GCF;
import com.timepath.steam.io.storage.util.ExtendedVFile;
import com.timepath.swing.DirectoryTreeCellRenderer;
import com.timepath.vfs.SimpleVFile;

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
class ArchiveExplorer extends JFrame {

    private static final Logger                    LOG       = Logger.getLogger(ArchiveExplorer.class.getName());
    private final        Collection<ExtendedVFile> archives  = new LinkedList<>();
    private final        List<ExtendedVFile>       toExtract = new LinkedList<>();
    private final DefaultTreeModel  tree;
    private final DefaultTableModel table;
    private       ExtendedVFile     selectedArchive;
    private       JPopupMenu        jPopupMenu1;
    private       JMenuItem         jPopupMenuItem1;
    private       JTable            jTable1;
    private       JTextField        jTextField1;
    private       JTree             jTree1;

    private ArchiveExplorer() {
        initComponents();
        jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree1.setCellRenderer(new DirectoryTreeCellRenderer());
        tree = (DefaultTreeModel) jTree1.getModel();
        jTable1.setDefaultEditor(Object.class, new CellSelectionListener());
        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private JLabel label = new JLabel();

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
                    label = (JLabel) comp;
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
        table = (DefaultTableModel) jTable1.getModel();
    }

    /**
     * @param args
     *         the command line arguments
     */
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ArchiveExplorer().setVisible(true);
            }
        });
    }

    private static Object[] attrs(SimpleVFile f) {
        Object[] attrs = {
                f, f.length(), FileUtils.extension(f.getName()), null, f.getPath(), null, null
        };
        if(f instanceof ExtendedVFile) {
            ExtendedVFile de = (ExtendedVFile) f;
            attrs[3] = de.getRoot();
            attrs[5] = de.getAttributes();
            attrs[6] = de.isComplete();
        }
        return attrs;
    }

    private void addArchive(ExtendedVFile a) {
        archives.add(a);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(a);
        a.analyze(node, false);
        tree.insertNodeInto(node, (MutableTreeNode) tree.getRoot(), tree.getChildCount(tree.getRoot()));
        tree.reload();
    }

    private void directoryChanged(ExtendedVFile dir) {
        if(!dir.isDirectory()) {
            return;
        }
        table.setRowCount(0);
        for(SimpleVFile c : dir.children()) {
            if(c.isDirectory()) {
                continue;
            }
            table.addRow(attrs(c));
        }
    }

    private void extractablesUpdated() {
        jPopupMenuItem1.setEnabled(!toExtract.isEmpty());
    }

    private void load(File f) throws IOException {
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

    private void search() {
        SwingWorker<Void, Object[]> sw = new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                jTree1.setSelectionPath(null);
                table.setRowCount(0);
                Collection<SimpleVFile> children = new LinkedList<>();
                for(ExtendedVFile a : archives) {
                    children.addAll(a.find(jTextField1.getText(), a));
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
                    table.addRow(rowData);
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(ArchiveExplorer.this, "Done");
            }
        };
        sw.execute();
    }

    private void initComponents() {
        jPopupMenu1 = new JPopupMenu();
        jPopupMenuItem1 = new JMenuItem();
        JMenuItem jPopupMenuItem2 = new JMenuItem();
        JSplitPane jSplitPane1 = new JSplitPane();
        JScrollPane jScrollPane2 = new JScrollPane();
        jTree1 = new JTree();
        JPanel jPanel1 = new JPanel();
        JPanel jPanel2 = new JPanel();
        jTextField1 = new JTextField();
        JButton jButton1 = new JButton();
        JScrollPane jScrollPane3 = new JScrollPane();
        jTable1 = new JTable();
        JMenuBar jMenuBar1 = new JMenuBar();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem1 = new JMenuItem();
        JMenuItem jMenuItem2 = new JMenuItem();
        jPopupMenuItem1.setText("Extract");
        jPopupMenuItem1.setEnabled(false);
        jPopupMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jPopupMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupMenuItem1);
        jPopupMenuItem2.setText("Properties");
        jPopupMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showProperties(evt);
            }
        });
        jPopupMenu1.add(jPopupMenuItem2);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Archive Explorer");
        setPreferredSize(new Dimension(800, 500));
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setOneTouchExpandable(true);
        DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("root");
        jTree1.setModel(new DefaultTreeModel(treeNode1));
        jTree1.setRootVisible(false);
        jTree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
        });
        jTree1.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent evt) {
                directoryChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jTree1);
        jSplitPane1.setLeftComponent(jScrollPane2);
        jPanel1.setLayout(new BorderLayout());
        jPanel2.setLayout(new BorderLayout());
        jTextField1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jPanel2.add(jTextField1, BorderLayout.CENTER);
        jButton1.setText("Search");
        jButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1, BorderLayout.EAST);
        jPanel1.add(jPanel2, BorderLayout.NORTH);
        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new DefaultTableModel(new Object[][] {
        }, new String[] {
                "Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete"
        }
        )
        {
            Class[] types = {
                    Object.class, Integer.class, String.class, Object.class, String.class, Object.class, Boolean.class
            };
            boolean[] canEdit = {
                    false, false, false, false, false, false, false
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jTable1.setFillsViewportHeight(true);
        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jPanel1.add(jScrollPane3, BorderLayout.CENTER);
        jSplitPane1.setRightComponent(jPanel1);
        getContentPane().add(jSplitPane1, BorderLayout.CENTER);
        jMenu1.setText("File");
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                open(evt);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setText("Mount TF2");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        pack();
    }

    private void open(ActionEvent evt) {
        try {
            File[] fs = new NativeFileChooser().setParent(this)
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
            Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void directoryChanged(TreeSelectionEvent evt) {
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            table.setRowCount(0);
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

    private void jPopupMenuItem1ActionPerformed(ActionEvent evt) {
        try {
            File[] outs = new NativeFileChooser().setParent(this)
                                                 .setTitle("Select extraction directory")
                                                 .setMultiSelectionEnabled(false)
                                                 .setDialogType(BaseFileChooser.DialogType.OPEN_DIALOG)
                                                 .setFileMode(BaseFileChooser.FileMode.DIRECTORIES_ONLY)
                                                 .choose();
            if(outs == null) {
                return;
            }
            File out = outs[0];
            for(ExtendedVFile e : toExtract) {
                try {
                    e.extract(out);
                } catch(IOException ex) {
                    Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            LOG.info("Done");
        } catch(IOException ex) {
            Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void jTree1MouseClicked(MouseEvent evt) {
        if(SwingUtilities.isRightMouseButton(evt)) {
            TreePath clicked = jTree1.getPathForLocation(evt.getX(), evt.getY());
            if(clicked == null) {
                return;
            }
            if(( jTree1.getSelectionPaths() == null ) || !Arrays.asList(jTree1.getSelectionPaths()).contains(clicked)) {
                jTree1.setSelectionPath(clicked);
            }
            toExtract.clear();
            TreePath[] paths = jTree1.getSelectionPaths();
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
            jPopupMenu1.show(jTree1, evt.getX(), evt.getY());
        }
    }

    private void jTable1MouseClicked(MouseEvent evt) {
        int row = jTable1.rowAtPoint(evt.getPoint());
        if(row == -1) {
            return;
        }
        int[] selectedRows = jTable1.getSelectedRows();
        Arrays.sort(selectedRows);
        if(Arrays.binarySearch(selectedRows, row) < 0) {
            jTable1.setRowSelectionInterval(row, row);
        }
        toExtract.clear();
        int[] selected = jTable1.getSelectedRows();
        for(int r : selected) {
            Object userObject = table.getValueAt(jTable1.convertRowIndexToModel(r), 0);
            if(userObject instanceof ExtendedVFile) {
                toExtract.add((ExtendedVFile) userObject);
            }
        }
        extractablesUpdated();
        if(SwingUtilities.isRightMouseButton(evt)) {
            jPopupMenu1.show(jTable1, evt.getX(), evt.getY());
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
                Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jTextField1ActionPerformed(ActionEvent evt) {
        search();
    }

    private void jButton1ActionPerformed(ActionEvent evt) {
        search();
    }

    private void showProperties(ActionEvent evt) {
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

    private void jMenuItem2ActionPerformed(ActionEvent evt) {
        try {
            addArchive(ACF.fromManifest(440));
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class CellSelectionListener extends DefaultCellEditor {

        CellSelectionListener() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Object val = table.getValueAt(row, 0);
            if(val instanceof ExtendedVFile) {
                directoryChanged((ExtendedVFile) val);
            }
            return null;
        }
    }
}
