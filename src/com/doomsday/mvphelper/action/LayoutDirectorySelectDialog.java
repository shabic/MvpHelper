package com.doomsday.mvphelper.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class LayoutDirectorySelectDialog extends JDialog {
    private JPanel contentPane;
    private JList list1;

    public LayoutDirectorySelectDialog(List<String> list ,AnActionEvent anActionEvent,MVPSupportAction mvpSupportAction,String name) {
        setContentPane(contentPane);
        setModal(true);
        setSize(200,300);
        setLocation(500, 500);
        setLocationRelativeTo(null);//居中
        setTitle("请选择layout存放目录");
        DefaultListModel<String> model = new DefaultListModel<>();
        list1.setModel(model);
        for (String s : list) {
            model.addElement(s);
        }
        list1.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                File file = new File(anActionEvent.getProject().getBasePath(), "/app/src/main/res/" + list1.getSelectedValue().toString() + "/layout/" + name);
                mvpSupportAction.generateLayout(file);
                dispose();
            }
        });
    }
}
