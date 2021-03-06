/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ueg.watchdog.view;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.dbutils.DbUtils;
import ueg.watchdog.Constants;
import ueg.watchdog.api.PersonRecognizedCallback;
import ueg.watchdog.api.Startable;
import ueg.watchdog.core.WatchDog;
import ueg.watchdog.database.DbConnect;
import ueg.watchdog.model.Profile;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author erandi
 */
public class PersonDetection extends WatchDogBaseFrame implements PersonRecognizedCallback {

    private WatchDog watchDog;

    public PersonDetection(WatchDogBaseFrame parentFrame) {
        super(parentFrame);
        initComponents();
        super.setCloseOperation();
        watchDog = WatchDog.getInstance();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelDetectPerson = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButtonCapture = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        javax.swing.GroupLayout jPanelDetectPersonLayout = new javax.swing.GroupLayout(jPanelDetectPerson);
        jPanelDetectPerson.setLayout(jPanelDetectPersonLayout);
        jPanelDetectPersonLayout.setHorizontalGroup(
                jPanelDetectPersonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 594, Short.MAX_VALUE)
        );
        jPanelDetectPersonLayout.setVerticalGroup(
                jPanelDetectPersonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 355, Short.MAX_VALUE)
        );

        jLabel1.setFont(new java.awt.Font("DejaVu Serif", 1, 18)); // NOI18N
        jLabel1.setText("WatchDog - Detect Person");

        jButtonCapture.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jButtonCapture.setText("Capture");
        jButtonCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCaptureActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(36, 36, 36)
                                .addComponent(jPanelDetectPerson, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(41, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(187, 187, 187))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(jButtonCapture, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(252, 252, 252))))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanelDetectPerson, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(jButtonCapture, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCaptureActionPerformed
        if (watchDog.getState() != Startable.State.STARTED) {
            JOptionPane.showMessageDialog(this, "WatchDog must be started to capture the image", "WatchDog hasn't started", JOptionPane.INFORMATION_MESSAGE);
            logger.warn("Attempting to capture the image while WatchDog is in state : {}", watchDog.getState());
            return;
        }
        watchDog.detectPerson(jPanelDetectPerson, this);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                watchDog.stopDetectPerson();
            }
        });
    }//GEN-LAST:event_jButtonCaptureActionPerformed

    @Override
    public void onRecognized(Profile profile) {
        //if person recognized, ask to print report. else return to mainWindow.
        if ((JOptionPane.showConfirmDialog(null, "Person recognized in existing videos. Do you want to print detailed report?", "Recognized!", 0, 3) == 0)) {
            //if yes, print report
            Connection connection = DbConnect.getDBConnection();
            String query = "SELECT video_stat.occurred_timestamp, video.file_name, person_profile.first_name, video_stat.face " +
                    "FROM  `video_stat` LEFT JOIN  `person_profile` ON video_stat.profile_id = person_profile.id LEFT JOIN video ON " +
                    "video.id=video_stat.video_id WHERE video_stat.profile_id=?";
            try {
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, profile.getId());
                ResultSet resultSet = statement.executeQuery();

                JasperReport jr = JasperCompileManager.compileReport(
                        this.getClass().getClassLoader().getResourceAsStream(Constants.RECOGNIZED_PERSON_REPORT_PATH));
                JRDataSource dataSource = new JRResultSetDataSource(resultSet);
                JasperPrint jp = JasperFillManager.fillReport(jr, null, dataSource);
                JasperViewer.viewReport(jp, false);
            } catch (Exception e) {
                logger.error("Error occurred when printing report for profile : {}", profile.getId(), e);
            } finally {
                DbUtils.closeQuietly(connection);
            }
        }
        this.openGivenWindow(new MainWindow());
    }

    @Override
    public void onNotRecognized() {
        JOptionPane.showMessageDialog(null, "Person doesn't exist in recorded videos!", "Unrecognized", 1);
        this.openGivenWindow(new MainWindow());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCapture;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanelDetectPerson;
    // End of variables declaration//GEN-END:variables
}
