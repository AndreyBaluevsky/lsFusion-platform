package platform.client.form;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OSUtils;
import platform.client.ClientResourceBundle;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.remote.proxy.RemoteFormProxy;
import platform.interop.KeyStrokes;
import platform.interop.action.*;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteDialogInterface;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;

public class ClientFormActionDispatcher implements ClientActionDispatcher {

    private final ClientFormController form;

    public ClientFormActionDispatcher() {
        this(null);
    }

    public ClientFormActionDispatcher(ClientFormController form) {
        this.form = form;
    }

    private Container getDialogParentContainer() {
        return form == null ? Main.frame : form.getComponent();
    }

    public Object[] dispatchActions(ClientAction... actions) throws IOException {
        if (actions == null) {
            return null;
        }

        Object[] results = new Object[actions.length];
        for (int i = 0; i < actions.length; i++) {
            results[i] = actions[i].dispatch(this);
        }
        return results;
    }

    public void execute(DenyCloseFormClientAction action) {
        denyFormClose();
    }

    private void denyFormClose() {
        if (form != null) {
            form.setCanClose(false);
        }
    }

    public void execute(FormClientAction action) {
        try {
            RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm);
            if (action.isPrintForm) {
                Main.frame.runReport(remoteForm, action.isModal, form == null ? null : form.getUserPreferences());
            } else {
                if (!action.isModal) {
                    Main.frame.runForm(remoteForm);
                } else {
                    new ClientModalForm(Main.frame, remoteForm, action.newSession).showDialog(false);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(DialogClientAction action) {
        AWTEvent currentEvent = EventQueue.getCurrentEvent();

        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        RemoteDialogInterface dialog = action.dialog;
        ClientDialog dlg;
        if (KeyStrokes.isSpaceEvent(currentEvent) || KeyStrokes.isObjectEditorDialogEvent(currentEvent)) {
            dlg = new ClientNavigatorDialog(owner, dialog, true);
        } else {
            dlg = new ClientDialog(owner, dialog, currentEvent, true);
        }

        dlg.showDialog(false);
    }

    public Object execute(RuntimeClientAction action) {

        try {

            Process p = Runtime.getRuntime().exec(action.command, action.environment, (action.directory == null ? null : new File(action.directory)));

            if (action.input != null && action.input.length > 0) {
                OutputStream inStream = p.getOutputStream();
                inStream.write(action.input);
            }

            if (action.waitFor) {
                p.waitFor();
            }

            InputStream outStream = p.getInputStream();
            InputStream errStream = p.getErrorStream();

            byte[] output = new byte[outStream.available()];
            outStream.read(output);

            byte[] error = new byte[errStream.available()];
            outStream.read(error);

            return new RuntimeClientActionResult(output, error);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(ExportFileClientAction action) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(OSUtils.loadCurrentDirectory());
            boolean singleFile;
            if (action.files.size() > 1) {
                singleFile = false;
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);
            } else {
                singleFile = true;
                fileChooser.setSelectedFile(new File(action.files.keySet().iterator().next()));
            }
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                for (String file : action.files.keySet()) {
                    IOUtils.putFileBytes(new File(singleFile ? path : path + "\\" + file), action.files.get(file));
                }
                OSUtils.saveCurrentDirectory(!singleFile ? new File(path) : new File(path.substring(0, path.lastIndexOf("\\"))));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(ImportFileClientAction action) {

        try {

            File file = new File(action.fileName);
            FileInputStream fileStream;

            try {
                fileStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return new ImportFileClientActionResult(false, "");
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);
            fileStream.close();

            if (action.erase) {
                file.delete();
            }

            return new ImportFileClientActionResult(true, action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(SleepClientAction action) {

        try {
            Thread.sleep(action.millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(MessageFileClientAction action) {

        try {

            File file = new File(action.fileName);
            FileInputStream fileStream = null;

            try {
                fileStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                if (action.mustExist) {
                    throw new RuntimeException(e);
                } else {
                    return null;
                }
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);
            fileStream.close();

            if (action.erase) {
                file.delete();
            }

            String fileText = action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName);
            if (action.multiplier > 0) {
                fileText = ((Double) (Double.parseDouble(fileText) * 100)).toString();
            }

            if (action.mask != null) {
                fileText = new DecimalFormat(action.mask).format((Double) (Double.parseDouble(fileText)));
            }

            JOptionPane.showMessageDialog(getDialogParentContainer(), fileText,
                                          action.caption, JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(UserChangedClientAction action) {
        try {
            Main.frame.updateUser();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(UserReloginClientAction action) {
        try {
            final JPasswordField jpf = new JPasswordField();
            JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = jop.createDialog(getDialogParentContainer(), ClientResourceBundle.getString("form.enter.password"));
            dialog.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    jpf.requestFocusInWindow();
                }
            });
            dialog.setVisible(true);
            int result = (jop.getValue() != null) ? (Integer) jop.getValue() : JOptionPane.CANCEL_OPTION;
            dialog.dispose();
            String password = null;
            if (result == JOptionPane.OK_OPTION) {
                password = new String(jpf.getPassword());
                boolean check = Main.remoteLogics.checkUser(action.login, password);
                if (check) {
                    Main.frame.remoteNavigator.relogin(action.login);
                    Main.frame.updateUser();
                } else {
                    throw new RuntimeException();
                }
            }
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(MessageClientAction action) {
        if (!action.extended) {
            JOptionPane.showMessageDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.INFORMATION_MESSAGE);
        } else {
            new ExtendedMessageDialog(getDialogParentContainer(), action.caption, action.message).setVisible(true);
        }
    }

    public int execute(ConfirmClientAction action) {
        return JOptionPane.showConfirmDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.YES_NO_OPTION);
    }

    public class ExtendedMessageDialog extends JDialog {
        public ExtendedMessageDialog(Container owner, String title, String message) {
            super(SwingUtils.getWindow(owner), title, ModalityType.DOCUMENT_MODAL);
            JTextArea textArea = new JTextArea(message);
            textArea.setFont(textArea.getFont().deriveFont((float) 12));
            add(new JScrollPane(textArea));
            setMinimumSize(new Dimension(400, 200));
            setLocationRelativeTo(owner);
            ActionListener escListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    setVisible(false);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
    }

    public void execute(LogMessageClientAction action) {
        if (action.failed) {
            Log.error(action.message);
            denyFormClose();
        } else {
            Log.message(action.message);
        }
    }

    public void execute(ApplyClientAction action) {
        if (form != null) {
            form.apply(false);
        }
    }

    public void execute(OpenFileClientAction action) {
        try {
            if (action.file != null) {
                BaseUtils.openFile(action.file, action.extension);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(AudioClientAction action) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(action.audio));
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
