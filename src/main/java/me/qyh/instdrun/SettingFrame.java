package me.qyh.instdrun;

import me.qyh.instd4j.parser.exception.LogicException;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;

import javax.swing.*;
import java.io.File;

class SettingFrame extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final JTextField locationText;
    private final JTextField threadNumText;
    private final JTextField proxyPortText;
    private final JTextField proxyAddrText;
    private final JTextField usernameText;
    private final JPasswordField passwordText;
    private final JTextField socketTimeoutText;
    private final JTextField connTimeoutText;
    private final JTextField getConnTimeoutText;

    public SettingFrame() {

        super();
        DowninsConfig config = Configure.get().getConfig();
        setResizable(false);
        setLayout(null);
        setSize(400, 650);
        setLocationRelativeTo(null);

        JLabel locationLabel = new JLabel("存储文件夹路径");
        locationLabel.setBounds(10, 10, 400, 30);
        add(locationLabel);

        locationText = new JTextField();
        locationText.setText(config.getLocation());
        locationText.setBounds(10, 50, 300, 30);
        add(locationText);

        JButton locationBtn = new JButton("选择");
        locationBtn.setBounds(320, 50, 60, 30);
        add(locationBtn);
        locationBtn.addActionListener(e -> SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFileChooser jfc = new JFileChooser(locationText.getText());
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = jfc.showOpenDialog(SettingFrame.this);
                if (JFileChooser.APPROVE_OPTION == returnVal) {
                    File file = jfc.getSelectedFile();
                    locationText.setText(file.getAbsolutePath());
                }
            }
        }));

        JLabel threadNumLabel = new JLabel("下载线程数");
        threadNumLabel.setBounds(10, 90, 100, 30);
        add(threadNumLabel);
        threadNumText = new JTextField(String.valueOf(config.getThreadNum()));
        threadNumText.setBounds(10, 130, 370, 30);
        add(threadNumText);

        JLabel proxyAddrLabel = new JLabel("代理地址");
        proxyAddrLabel.setBounds(10, 170, 300, 30);
        add(proxyAddrLabel);

        String proxyAddr = config.getProxyAddr() == null ? "" : config.getProxyAddr();
        proxyAddrText = new JTextField(proxyAddr);
        proxyAddrText.setBounds(10, 210, 370, 30);
        add(proxyAddrText);

        JLabel proxyPortLabel = new JLabel("代理端口");
        proxyPortLabel.setBounds(10, 250, 300, 30);
        add(proxyPortLabel);

        String proxyPort = config.getProxyPort() == null ? "" : String.valueOf(config.getProxyPort());
        proxyPortText = new JTextField(proxyPort);
        proxyPortText.setBounds(10, 290, 370, 30);
        add(proxyPortText);


        JLabel usernameLabel = new JLabel("用户名");
        usernameLabel.setBounds(10, 330, 300, 30);
        add(usernameLabel);

        String username = config.getUsername() == null ? "" : String.valueOf(config.getUsername());
        usernameText = new JTextField(username);
        usernameText.setBounds(10, 370, 370, 30);
        add(usernameText);

        JLabel passwordLabel = new JLabel("密码");
        passwordLabel.setBounds(10, 410, 300, 30);
        add(passwordLabel);

        String password = config.getPassword() == null ? "" : String.valueOf(config.getPassword());
        passwordText = new JPasswordField(password);
        passwordText.setBounds(10, 450, 370, 30);
        add(passwordText);

        JLabel timeoutLabel = new JLabel("超时(秒)");
        timeoutLabel.setBounds(10, 490, 300, 30);
        add(timeoutLabel);

        JLabel connTimeoutLabel = new JLabel("连接超时");
        connTimeoutLabel.setBounds(10, 530, 60, 30);
        add(connTimeoutLabel);

        connTimeoutText = new JTextField(String.valueOf(config.getConnTimeout()));
        connTimeoutText.setBounds(70, 530, 40, 30);
        add(connTimeoutText);

        JLabel getConnTimeoutLabel = new JLabel("获取连接超时");
        getConnTimeoutLabel.setBounds(120, 530, 80, 30);
        add(getConnTimeoutLabel);

        getConnTimeoutText = new JTextField(String.valueOf(config.getGetConnTimeout()));
        getConnTimeoutText.setBounds(200, 530, 40, 30);
        add(getConnTimeoutText);

        JLabel socketTimeoutLabel = new JLabel("读取数据超时");
        socketTimeoutLabel.setBounds(250, 530, 80, 30);
        add(socketTimeoutLabel);

        socketTimeoutText = new JTextField(String.valueOf(config.getSocketTimeout()));
        socketTimeoutText.setBounds(340, 530, 40, 30);
        add(socketTimeoutText);

        JButton saveBtn = new JButton("保存");
        saveBtn.setBounds(280, 570, 100, 30);
        add(saveBtn);

        saveBtn.addActionListener(e -> SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                DowninsConfig config1 = Configure.get().getConfig();
                config1.setLocation(locationText.getText());
                try {
                    config1.setThreadNum(Integer.parseInt(threadNumText.getText()));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "下载线程必须为一个数字");
                    return;
                }
                config1.setProxyAddr(proxyAddrText.getText());
                if (!proxyPortText.getText().isEmpty()) {
                    try {
                        config1.setProxyPort(Integer.parseInt(proxyPortText.getText()));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "代理端口必须为一个数字");
                        return;
                    }
                }
                if (!connTimeoutText.getText().isEmpty()) {
                    try {
                        config1.setConnTimeout(Integer.parseInt(connTimeoutText.getText()));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "连接超时必须为一个数字");
                        return;
                    }
                }

                if (!getConnTimeoutText.getText().isEmpty()) {
                    try {
                        config1.setGetConnTimeout(Integer.parseInt(getConnTimeoutText.getText()));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "获取连接超时必须为一个数字");
                        return;
                    }
                }

                if (!socketTimeoutText.getText().isEmpty()) {
                    try {
                        config1.setSocketTimeout(Integer.parseInt(socketTimeoutText.getText()));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "读取数据超时必须为一个数字");
                        return;
                    }
                }
                config1.setUsername(usernameText.getText());
                config1.setPassword(new String(passwordText.getPassword()));
                try {
                    config1.store();
                    JOptionPane.showMessageDialog(null, "保存成功");
                } catch (LogicException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "保存失败");
                }
            }
        }));

        setVisible(true);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}