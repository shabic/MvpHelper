package com.doomsday.mvphelper.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by hsx on 16/4/29.
 */
public class MVPSupportAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        new InputNameDialog(this, anActionEvent).setVisible(true);
    }

    void generate(AnActionEvent anActionEvent, String name, int type) {
        String pkg = getParentPackage(anActionEvent);
        generateContract(anActionEvent, name, pkg);
        generatePresenter(anActionEvent, name, pkg);
        if (type == InputNameDialog.TypeActivity) {
            generateLayout(anActionEvent, name, "activity");
            generateActivity(anActionEvent, name, pkg);
        } else {
            generateLayout(anActionEvent, name, "fragment");
            generateFragment(anActionEvent, name, pkg);
        }
        anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE).getParent().refresh(true, true);
    }

void generateLayout(AnActionEvent anActionEvent, String fileName, String type) {
        String midStr = "/app/src/main/res/";
        File file = new File(anActionEvent.getProject().getBasePath(), midStr+"/layout/");
        if (!file.exists()) {
            try {
                file = new File(anActionEvent.getProject().getBasePath(), midStr);
                VirtualFile[] children = LocalFileSystem.getInstance().findFileByIoFile(file).getChildren();
                if (children == null || children.length == 0) return;
                ArrayList<String> al = new ArrayList<>();
                for (VirtualFile virtualFile : children) {
                    if (virtualFile.isDirectory())
                        al.add(virtualFile.getName());
                }
                if (al.size() == 1) {
                    midStr += al.get(0);
                } else {
                    new LayoutDirectorySelectDialog(al, anActionEvent, this, type + "_" + fileName.toLowerCase() + ".xml").setVisible(true);
                    return;
                }
            } catch (Exception e) {
                return;
            }
        }
        file = new File(file, type + "_" + fileName.toLowerCase() + ".xml");
        generateLayout(file);
    }

    void generateLayout(File file) {
        try {
            InputStream is = getClass().getResourceAsStream("../template/TemplateLayout");
            String content = readStream(is);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("generateLayout", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    void generateFragment(AnActionEvent anActionEvent, String fileName, String pkg) {
        File file = new File(anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE).getPath(), fileName + "Fragment.java");
        InputStream is = getClass().getResourceAsStream("../template/TemplateFragment");
        String content = readStream(is);
        content = content.replace("$packagename", getCurPackage(anActionEvent))
                .replace("$imp_packagename", pkg)
                .replace("$package", getPackageName(anActionEvent))
                .replace("$layouname", fileName.toLowerCase())
                .replace("$classname", fileName)
                .replace("$date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .replace("$username", System.getenv().get("USERNAME"));
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            MessageDialogBuilder.yesNo("generateFragment", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    void generateActivity(AnActionEvent anActionEvent, String fileName, String pkg) {
        File file = new File(anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE).getPath(), fileName + "Activity.java");
        InputStream is = getClass().getResourceAsStream("../template/TemplateActivity");
        String content = readStream(is);
        content = content.replace("$packagename", getCurPackage(anActionEvent))
                .replace("$imp_packagename", pkg)
                .replace("$package", getPackageName(anActionEvent))
                .replace("$layouname", fileName.toLowerCase())
                .replace("$classname", fileName)
                .replace("$date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .replace("$username", System.getenv().get("USERNAME"));
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            MessageDialogBuilder.yesNo("generateActivity", e.getMessage()).show();
            e.printStackTrace();
            return;
        }

        String activityPackage = getCurPackage(anActionEvent) + "." + fileName + "Activity";
        File fileManifest = new File(anActionEvent.getProject().getBasePath(), "/app/src/main/AndroidManifest.xml");
        insertActivityToManifest(activityPackage.replace(getPackageName(anActionEvent), ""), fileManifest);
    }

    private void insertActivityToManifest(String activityPackage, File fileManifest) {
        try {
            RandomAccessFile rw = new RandomAccessFile(fileManifest, "rw");
            String s;
            while (true) {
                s = rw.readLine();
                if (s == null) {
                    return;
                }
                if (s.contains("</application>")) {
                    int length = s.length();
                    byte[] writeBytes = ("\t\t<activity android:name=\"" + activityPackage + "\"/>\n").getBytes();
                    rw.seek(rw.getFilePointer() - 2 - s.length());
                    long pos = rw.getFilePointer();
                    byte[] bytes = new byte[(int) (rw.length() - rw.getFilePointer())];
                    rw.read(bytes);
                    rw.seek(pos);
                    rw.write(writeBytes);
                    rw.write(bytes);
                    rw.close();
                    return;
                }

            }
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("insertActivityToManifest", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    void generatePresenter(AnActionEvent anActionEvent, String fileName, String pkg) {
        File file = new File(getParentFilePath(anActionEvent), "presenter");
        //contract文件夹不存在就创建
        if (file != null && !file.exists()) {
            file.mkdir();
        }
        file = new File(file, fileName + "Presenter.java");
        try {
            InputStream is = getClass().getResourceAsStream("../template/TemplatePresenter");
            write(new FileOutputStream(file), is, pkg, fileName);
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("generatePresenter", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    void generateContract(AnActionEvent anActionEvent, String fileName, String pkg) {
        File file = new File(getParentFilePath(anActionEvent), "contract");
        //contract文件夹不存在就创建
        if (file != null && !file.exists()) {
            file.mkdir();
        }
        try {
            file = new File(file, fileName + "Contract.java");
            InputStream is = getClass().getResourceAsStream("../template/TemplateContract");
            write(new FileOutputStream(file), is, pkg, fileName);
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("generateContract", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    void write(FileOutputStream fos, InputStream is, String pkg, String fileName) {
        String content = readStream(is);
        content = content.replace("$packagename", pkg)
                .replace("$classname", fileName)
                .replace("$date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .replace("$username", System.getenv().get("USERNAME"))
                .replace("$username", System.getenv().get("USERNAME"));
        try {
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            MessageDialogBuilder.yesNo("write", e.getMessage()).show();
            e.printStackTrace();
        }
    }

    String getParentPackage(AnActionEvent anActionEvent) {
        String projectPath = anActionEvent.getProject().getBasePath() + "/app/src/main/java/";
//        String projectPath = anActionEvent.getProject().getBasePath() + "/src/";
        return getParentFilePath(anActionEvent).replace(projectPath, "").replace("/", ".");
    }

    String getCurPackage(AnActionEvent anActionEvent) {
        String projectPath = anActionEvent.getProject().getBasePath() + "/app/src/main/java/";
//        String projectPath = anActionEvent.getProject().getBasePath() + "/src/";
        return getCurFilePath(anActionEvent).replace(projectPath, "").replace("/", ".");
    }

    //读取InputStream里面的数据
    private String readStream(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int len = -1;

        try {
            while ((len = is.read(bytes)) != -1) {
                baos.write(bytes, 0, len);
            }
            return new String(baos.toByteArray());
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("readStream", e.getMessage()).show();
            e.printStackTrace();
        }
        return null;
    }

    //获取文件名，带后缀
    String getFileName(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        if (editor == null) return null;
        PsiFile psiFileInEditor = PsiUtilBase.getPsiFileInEditor(editor, anActionEvent.getProject());
        return psiFileInEditor.getName();
    }

    //获取当前路径
    String getCurFilePath(AnActionEvent anActionEvent) {
        VirtualFile file = anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
        return file.getPath();
    }

    //获取当前路径
    String getParentFilePath(AnActionEvent anActionEvent) {
        VirtualFile file = anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE).getParent();
        return file.getPath();
    }

    @Override
    public void update(AnActionEvent e) {

    }


    //从AndroidManifest.xml文件中获取当前app的包名
    private String getPackageName(AnActionEvent anActionEvent) {
        String package_name = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(anActionEvent.getProject().getBasePath() + "/App/src/main/AndroidManifest.xml");
            NodeList nodeList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                package_name = element.getAttribute("package");
            }
        } catch (Exception e) {
            MessageDialogBuilder.yesNo("getPackageName", e.getMessage()).show();
            e.printStackTrace();
        }
        return package_name;
    }
}