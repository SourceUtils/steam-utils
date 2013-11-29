package com.timepath.steam.io;

import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.http.HTTPFS;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author TimePath
 */
public class ArchiveHost {

    private static final Logger LOG = Logger.getLogger(ArchiveHost.class.getName());

    public static void main(String... args) {
        int appID = 440;
        try {
            final ACF acf = ACF.fromManifest(appID);
            Collection<? extends SimpleVFile> files = acf.list();
            try {
                HTTPFS http = new HTTPFS();
                http.copyFrom(files);
                new Thread(http).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            try {
                FTPFS ftp = FTPFS.create();
                ftp.copyFrom(files);
                new Thread(ftp).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(null,
                                              "Navigate to ftp://localhost:2121. The files will stop being hosted when you close all running instances",
                                              "Files hosted",
                                              JOptionPane.INFORMATION_MESSAGE,
                                              null);
            }
        });

    }

}
