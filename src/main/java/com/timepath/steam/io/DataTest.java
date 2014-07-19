package com.timepath.steam.io;

import com.timepath.DataUtils;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.blob.Blob;
import com.timepath.steam.io.bvdf.BVDF;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
class DataTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(DataTest.class.getName());
    private JTree jTree1;

    /**
     * Creates new form VDFTest
     */
    private DataTest() {
        initComponents();
        setDropTarget(new DropTarget() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    DropTargetContext context = e.getDropTargetContext();
                    e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    Transferable t = e.getTransferable();
                    File file = null;
                    if(OS.isLinux()) {
                        DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
                        String data = (String) t.getTransferData(nixFileDataFlavor);
                        for(StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
                            String token = st.nextToken().trim();
                            if(token.startsWith("#") || token.isEmpty()) {
                                // comment line, by RFC 2483
                                continue;
                            }
                            try {
                                file = new File(new URI(token));
                            } catch(Exception ignored) {
                            }
                        }
                    } else {
                        Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                        if(data instanceof List) {
                            for(Object o : (Iterable<?>) data) {
                                if(o instanceof File) {
                                    file = (File) o;
                                }
                            }
                        }
                    }
                    if(file != null) {
                        open(file);
                    }
                } catch(ClassNotFoundException | IOException | UnsupportedFlavorException | InvalidDnDOperationException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } finally {
                    e.dropComplete(true);
                    repaint();
                }
            }
        });
    }

    private void initComponents() {
        JScrollPane jScrollPane1 = new JScrollPane();
        jTree1 = new JTree();
        JMenuBar jMenuBar1 = new JMenuBar();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem1 = new JMenuItem();
        JMenuItem jMenuItem2 = new JMenuItem();
        JMenuItem jMenuItem3 = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Data viewer");
        setMinimumSize(new Dimension(300, 300));
        DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("root");
        jTree1.setModel(new DefaultTreeModel(treeNode1));
        jTree1.setEditable(true);
        jTree1.setLargeModel(true);
        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);
        getContentPane().add(jScrollPane1, BorderLayout.CENTER);
        jMenu1.setText("File");
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile(e);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('A');
        jMenuItem2.setText("AppInfo");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appInfo(e);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuItem3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        jMenuItem3.setMnemonic('P');
        jMenuItem3.setText("PackageInfo");
        jMenuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                packageInfo(evt);
            }
        });
        jMenu1.add(jMenuItem3);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        pack();
    }

    private void openFile(ActionEvent e) {
        try {
            File[] fs = new NativeFileChooser().setDirectory(SteamUtils.getSteam()).setParent(this).setTitle("Open VDF").choose();
            if(fs == null) {
                return;
            }
            open(fs[0]);
        } catch(IOException ex) {
            Logger.getLogger(DataTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void open(final File f) {
        if(f == null) {
            LOG.info("File is null");
            return;
        }
        LOG.log(Level.INFO, "File is {0}", f);
        final DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
        final DefaultMutableTreeNode pseudo = new DefaultMutableTreeNode(f.getPath());
        model.setRoot(pseudo);
        new SwingWorker<DefaultMutableTreeNode, Void>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                DefaultMutableTreeNode n = null;
                try {
                    if(f.getName().toLowerCase().endsWith(".blob")) {
                        Blob bin = new Blob();
                        bin.readExternal(DataUtils.mapFile(f));
                        n = bin.getRoot();
                    } else if(f.getName().toLowerCase().matches("^.*(vdf|res|bin|txt|styles)$")) {
                        if(VDF.isBinary(f)) {
                            BVDF bin = new BVDF();
                            bin.readExternal(DataUtils.mapFile(f));
                            n = bin.getRoot();
                        } else {
                            n = VDF.load(new FileInputStream(f)).toTreeNode();
                        }
                    } else {
                        JOptionPane.showMessageDialog(DataTest.this,
                                                      MessageFormat.format("{0} is not supported", f.getAbsolutePath()),
                                                      "Invalid file",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                } catch(StackOverflowError e) {
                    LOG.warning("Stack Overflow");
                } catch(Exception e) {
                    LOG.log(Level.SEVERE, null, e);
                }
                return n;
            }

            @Override
            protected void done() {
                try {
                    DefaultMutableTreeNode n = get();
                    if(n != null) {
                        pseudo.add(n);
                    }
                    model.reload();
                    //                    TreeUtils.expand(DataTest.this.jTree1);
                } catch(InterruptedException | ExecutionException ex) {
                    Logger.getLogger(DataTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    private void appInfo(ActionEvent evt) {
        open(new File(SteamUtils.getSteam() + "/appcache/appinfo.vdf"));
    }

    private void packageInfo(ActionEvent evt) {
        open(new File(SteamUtils.getSteam() + "/appcache/packageinfo.vdf"));
    }

    /**
     * @param args
     *         the command line arguments
     */
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DataTest().setVisible(true);
            }
        });
    }
}
