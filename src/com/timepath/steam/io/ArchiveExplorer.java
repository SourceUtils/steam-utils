package com.timepath.steam.io;

import com.timepath.FileUtils;
import com.timepath.io.utils.ViewableData;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.BaseFileChooser.ExtensionFilter;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.GCF;
import com.timepath.steam.io.storage.VPK;
import com.timepath.steam.io.storage.util.ExtendedVFile;
import com.timepath.swing.DirectoryTreeCellRenderer;
import com.timepath.vfs.SimpleVFile;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;

/**
 *
 * @author TimePath
 */
@SuppressWarnings("serial")
public class ArchiveExplorer extends JFrame {
    
    private static final Logger LOG = Logger.getLogger(ArchiveExplorer.class.getName());

    private final List<ExtendedVFile> archives = new LinkedList<ExtendedVFile>();
    
    private final List<ExtendedVFile> toExtract = new LinkedList<ExtendedVFile>();
    
    private ExtendedVFile selectedArchive;

    private final DefaultTreeModel tree;

    private final DefaultTableModel table;

    public ArchiveExplorer() {
        initComponents();
        jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree1.setCellRenderer(new DirectoryTreeCellRenderer());
        tree = (DefaultTreeModel) jTree1.getModel();
        jTable1.setDefaultEditor(Object.class, new CellSelectionListener());
        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private JLabel label = new JLabel();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected,
                                                                     hasFocus, row, column);
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

    private class CellSelectionListener extends DefaultCellEditor {

        CellSelectionListener() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            Object val = table.getValueAt(row, 0);
            if(val instanceof ExtendedVFile) {
                directoryChanged((ExtendedVFile) val);
            }
            return null;
        }

    }

    private void addArchive(ExtendedVFile a) {
        archives.add(a);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(a);
        a.analyze(node, false);
        tree.insertNodeInto(
            node,
            (MutableTreeNode) tree.getRoot(),
            tree.getChildCount(tree.getRoot()));
        tree.reload();
    }

    private Object[] attrs(SimpleVFile f) {
        Object[] attrs = new Object[] {
            f,
            f.length(),
            FileUtils.extension(f.getName()),
            null,
            f.getPath(),
            null,
            null
        };
        if(f instanceof ExtendedVFile) {
            ExtendedVFile de = (ExtendedVFile) f;
            attrs[3] = de.getRoot();
            
            attrs[5] = de.getAttributes();
            attrs[6] = de.isComplete();
        }
        return attrs;
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
        if(ext.equals("gcf")) {
            a = new GCF(f);
        } else if(ext.equals("vpk")) {
            a = VPK.loadArchive(f);
        } else {
            LOG.log(Level.WARNING, "Unrecognised archive: {0}", f);
            return;
        }
        addArchive(a);
    }
        
    private void search() {
        jTree1.setSelectionPath(null);
        List<SimpleVFile> children = new LinkedList<SimpleVFile>();
        for(ExtendedVFile a : archives) {
            children.addAll(a.find(jTextField1.getText(), a));
        }
        table.setRowCount(0);
        for(SimpleVFile c : children) {
            if(!c.isDirectory()) {
                table.addRow(attrs(c));
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ArchiveExplorer().setVisible(true);
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupMenuItem1 = new javax.swing.JMenuItem();
        jPopupMenuItem2 = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();

        jPopupMenuItem1.setText("Extract");
        jPopupMenuItem1.setEnabled(false);
        jPopupMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupMenuItem1);

        jPopupMenuItem2.setText("Properties");
        jPopupMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showProperties(evt);
            }
        });
        jPopupMenu1.add(jPopupMenuItem2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Archive Explorer");
        setPreferredSize(new java.awt.Dimension(800, 500));

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setOneTouchExpandable(true);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setRootVisible(false);
        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
        });
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                directoryChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jTree1);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jPanel2.add(jTextField1, java.awt.BorderLayout.CENTER);

        jButton1.setText("Search");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1, java.awt.BorderLayout.EAST);

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Size", "Type", "Archive", "Path", "Attributes", "Complete"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Integer.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTable1.setColumnSelectionAllowed(true);
        jTable1.setFillsViewportHeight(true);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        jPanel1.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Mount TF2");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void open(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open
        try {
            File[] fs = new NativeFileChooser()
                .setParent(this)
                .setTitle("Open archive")
                .setDirectory(SteamUtils.getSteamApps())
                .setMultiSelectionEnabled(true)
                .addFilter(new ExtensionFilter("VPK directory files", "_dir.vpk"))
                .addFilter(new ExtensionFilter("VPK files", ".vpk"))
                .addFilter(new ExtensionFilter("GCF files", ".gcf"))
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
    }//GEN-LAST:event_open

    private void directoryChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_directoryChanged
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            table.setRowCount(0);
            return;
        }
        Object node = selection.getLastPathComponent();
        if(!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        ExtendedVFile dir = null;
        if(obj instanceof ExtendedVFile) {
            dir = (ExtendedVFile) obj;
        }
        if(dir == null) {
            return;
        }
        directoryChanged(dir);
    }//GEN-LAST:event_directoryChanged
    
    private void jPopupMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupMenuItem1ActionPerformed
        try {
            File[] outs = new NativeFileChooser()
                .setParent(this)
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
    }//GEN-LAST:event_jPopupMenuItem1ActionPerformed
    
    private void jTree1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MouseClicked
        if(SwingUtilities.isRightMouseButton(evt)) {
            TreePath clicked = jTree1.getPathForLocation(evt.getX(), evt.getY());
            if(clicked == null) {
                return;
            }
            if(jTree1.getSelectionPaths() == null || !Arrays.asList(jTree1.getSelectionPaths())
                .contains(
                    clicked)) {
                jTree1.setSelectionPath(clicked);
            }
            toExtract.clear();
            TreePath[] paths = jTree1.getSelectionPaths();
            for(TreePath p : paths) {
                if(!(p.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                    return;
                }
                Object userObject = ((DefaultMutableTreeNode) p.getLastPathComponent())
                    .getUserObject();
                if(userObject instanceof ExtendedVFile) {
                    selectedArchive = (ExtendedVFile) userObject;
                    toExtract.add(selectedArchive);
                }
            }
            extractablesUpdated();
            jPopupMenu1.show(jTree1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTree1MouseClicked

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
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
        } else if(SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() >= 2) {
            ExtendedVFile e = toExtract.get(0);
            File dir = new File(System.getProperty("java.io.tmpdir"));
            File f = new File(dir, e.getName());
            try {
                e.extract(dir);
                f.deleteOnExit();
                if(!OS.isLinux()) {
                    Desktop.getDesktop().open(f);
                } else {
                    Runtime.getRuntime().exec(new String[] {"xdg-open", f.getPath()});
                }
            } catch(IOException ex) {
                Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        search();
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        search();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void showProperties(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showProperties
        String title;
        String message = "";
        if(selectedArchive != null) {
            title = selectedArchive.toString();
//            message = "V" + selectedArchive.header.applicationVersion + "\n";
        } else {
            ExtendedVFile last = toExtract.get(toExtract.size() - 1);
            title = last.getName();
            message += "Entry: " + last.getAbsoluteName() + "\n";
            message += last.getChecksum() + " vs " + last.calculateChecksum() + "\n";
        }
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_showProperties

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        try {
            addArchive(ACF.fromManifest(440));
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ArchiveExplorer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JMenuItem jPopupMenuItem1;
    private javax.swing.JMenuItem jPopupMenuItem2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables

}
