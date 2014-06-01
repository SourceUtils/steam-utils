package com.timepath.steam.net;

import com.timepath.DateUtils;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.VDFNode;
import com.timepath.steam.net.MasterServer.Region;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class ServerTest extends JPanel {

    private static final Logger LOG = Logger.getLogger(ServerTest.class.getName());
    private JScrollPane server;
    private JTabbedPane tabs;

    public ServerTest() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true) {{
            this.setLeftComponent(server = new JScrollPane());
            this.setRightComponent(tabs = new JTabbedPane());
        }}, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame f = new JFrame("Server browser");
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                f.setPreferredSize(new Dimension(400, 600));
                f.setContentPane(new ServerTest().loadMaster().loadSaved().loadServer(new SourceServer("27.50.71.201", 27016)));
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    public ServerTest loadSaved() {
        final JTextArea favorites = new JTextArea();
        tabs.add("Favorites", new JScrollPane(favorites));
        final JTextArea history = new JTextArea();
        tabs.add("History", new JScrollPane(history));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                VDFNode vdf = VDF.load(new File(SteamUtils.getUserData(), "7/remote/serverbrowser_hist.vdf"));
                VDFNode filters = vdf.get("Filters");
                for(VDFNode n : filters.get("Favorites").getNodes()) {
                    favorites.append("Favorite " + n.getCustom() + '\n');
                    favorites.append("Name: " + n.getValue("name") + '\n');
                    favorites.append("Address: " + n.getValue("address") + '\n');
                    long lastPlayed = Long.parseLong((String) n.getValue("LastPlayed"));
                    favorites.append("Last Played: " + DateUtils.parse(lastPlayed) + '\n' + '\n');
                }
                for(VDFNode n : filters.get("History").getNodes()) {
                    history.append("History " + n.getCustom() + '\n');
                    history.append("Name: " + n.getValue("name") + '\n');
                    history.append("Address: " + n.getValue("address") + '\n');
                    long lastPlayed = Long.parseLong((String) n.getValue("LastPlayed"));
                    history.append("Last Played: " + DateUtils.parse(lastPlayed) + '\n' + '\n');
                }
                return null;
            }
        }.execute();
        return this;
    }

    public ServerTest loadMaster() {
        final JTextArea net = new JTextArea();
        tabs.add("Internet", new JScrollPane(net));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    LOG.info("Querying master ...");
                    MasterServer.SOURCE.query(Region.AUSTRALIA, "\\gamedir\\tf", new ServerListener() {
                        @Override
                        public void inform(String update) {
                            net.append(update + '\n');
                        }
                    });
                } catch(IOException ex) {
                    LOG.log(Level.WARNING, null, ex);
                }
                return null;
            }
        }.execute();
        return this;
    }

    public ServerTest loadServer(SourceServer ss) {
        final JTextArea test = new JTextArea();
        server.setViewportView(test);
        test.append("Server: " + ss.address + '\n');
        try {
            LOG.info("Getting info ...");
            ss.getInfo(new ServerListener() {
                @Override
                public void inform(String update) {
                    test.append(update + '\n');
                }
            });
            LOG.info("Getting rules ...");
            test.append("Rules: \n");
            ss.getRules(new ServerListener() {
                @Override
                public void inform(String update) {
                    test.append(update + '\n');
                }
            });
        } catch(IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return this;
    }
}
