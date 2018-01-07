package com.doomsday.mvphelper.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;

import javax.swing.*;
import javax.xml.soap.Text;
import java.awt.event.*;

public class InputNameDialog extends JDialog implements ItemListener {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField text_name;
    private JRadioButton activity;
    private JRadioButton fragment;

    MVPSupportAction mvpSupportAction;
    AnActionEvent anActionEvent;

    public InputNameDialog(MVPSupportAction mvpSupportAction, AnActionEvent anActionEvent) {
        this.mvpSupportAction = mvpSupportAction;
        this.anActionEvent = anActionEvent;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setSize(250, 150);
        setLocation(500, 500);
        setLocationRelativeTo(null);//居中
        setTitle("请输入模块名称");
        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        initRadioButton();
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initRadioButton() {
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(activity);
        buttonGroup.add(fragment);
        fragment.addItemListener(this);
        activity.addItemListener(this);
        activity.setSelected(true);
    }

    private void onOK() {
        String text = text_name.getText();
        if (text == null || "".equals(text)) {
            return;
        }
        dispose();
        mvpSupportAction.generate(anActionEvent, text, moduleType);
    }

    private void onCancel() {
        dispose();
    }

    int moduleType;

    public static final int TypeActivity = 1;
    public static final int TypeFragent = 2;

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getItem() == activity) {
                moduleType = TypeActivity;
            } else {
                moduleType = TypeFragent;
            }
        }
        text_name.requestFocus();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        text_name.requestFocus();
    }
}
