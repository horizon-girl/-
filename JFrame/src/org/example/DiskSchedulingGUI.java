package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author 12525
 */
public class DiskSchedulingGUI {
    private JFrame mainFrame;
    private JComboBox<String> algorithmComboBox;
    private JSpinner seekTimeSpinner;
    private JSpinner startTimeSpinner;
    private JSpinner diskSpeedSpinner;
    private JSpinner sectorsPerTrackSpinner;
    private JSpinner bytesPerSectorSpinner;
    public static void main(String[] args) {
        DiskSchedulingGUI diskSchedulingGUI = new DiskSchedulingGUI();
        diskSchedulingGUI.showParameterSettingWindow();
    }
    public void showParameterSettingWindow() {
        mainFrame = new JFrame("参数设置");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLayout(new GridLayout(7, 2));
        mainFrame.setLocationRelativeTo(null);

        JLabel seekTimeLabel = new JLabel("跨越1个磁道所用时间（毫秒）：");
        seekTimeLabel.setFont(new Font("宋体", Font.BOLD, 24));
        mainFrame.add(seekTimeLabel);

        seekTimeSpinner = createSpinner();// 设置微调器的尺寸
        seekTimeSpinner.setValue(50);
        mainFrame.add(seekTimeSpinner);

        JLabel startTimeLabel = new JLabel("启动时间（毫秒）：");
        startTimeLabel.setFont(new Font("宋体", Font.BOLD, 24));
        mainFrame.add(startTimeLabel);

        startTimeSpinner = createSpinner();
        startTimeSpinner.setValue(50);
        mainFrame.add(startTimeSpinner);

        JLabel diskSpeedLabel = new JLabel("磁盘转速（转/分钟）：");
        diskSpeedLabel.setFont(new Font("宋体", Font.BOLD, 24));
        mainFrame.add(diskSpeedLabel);

        diskSpeedSpinner = createSpinner();
        diskSpeedSpinner.setValue(7200);
        mainFrame.add(diskSpeedSpinner);

        JLabel sectorsPerTrackLabel = new JLabel("每磁道扇区数：");
        sectorsPerTrackLabel.setFont(new Font("宋体", Font.BOLD, 24));
        mainFrame.add(sectorsPerTrackLabel);

        sectorsPerTrackSpinner = createSpinner();
        sectorsPerTrackSpinner.setValue(64);
        mainFrame.add(sectorsPerTrackSpinner);

        JLabel bytesPerSectorLabel = new JLabel("每扇区字节数：");
        bytesPerSectorLabel.setFont(new Font("宋体", Font.BOLD, 24));
        mainFrame.add(bytesPerSectorLabel);

        bytesPerSectorSpinner = createSpinner();
        bytesPerSectorSpinner.setValue(512);

        mainFrame.add(bytesPerSectorSpinner);

        mainFrame.setVisible(true);
        JLabel algorithmLabel = new JLabel("选择磁盘算法：");
        algorithmLabel.setFont(new Font("宋体", Font.BOLD, 24)); // 设置字体为宋体，加粗，字号为18
        String[] algorithms = {"先到先服务（FCFS）算法", "最短查找时间优先（SSTF）算法", "扫描算法（SCAN）", "电梯算法（LOOK）"};
        algorithmComboBox = new JComboBox<>(algorithms);
        algorithmComboBox.setFont(new Font("宋体", Font.PLAIN, 24)); // 设置下拉框中文字的字体大小为 16

        mainFrame.add(algorithmLabel);
        mainFrame.add(algorithmComboBox);

        JButton startButton = new JButton("开始");
        startButton.setPreferredSize(new Dimension(780, 78));
        startButton.setFont(new Font("宋体", Font.BOLD, 24)); // 设置按钮字体为宋体，加粗，字号为18
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startDiskScheduling();
            }
        });
        mainFrame.add(startButton);

        mainFrame.setVisible(true);
    }

    private JSpinner createSpinner() {
        SpinnerNumberModel numberModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1); // 设置初始值、最小值和步长
        JSpinner spinner = new JSpinner(numberModel);
        spinner.setFont(spinner.getFont().deriveFont(24f));
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) spinner.getEditor();
        spinnerEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        spinnerEditor.getTextField().setColumns(10);

        return spinner;
    }
    private void startDiskScheduling() {
        int seekTimePerTrack = (int) seekTimeSpinner.getValue();
        int startTime = (int) startTimeSpinner.getValue();
        int diskSpeed = (int) diskSpeedSpinner.getValue();
        int sectorsPerTrack = (int) sectorsPerTrackSpinner.getValue();
        int bytesPerSector = (int) bytesPerSectorSpinner.getValue();

        List<Integer> ioSequence = generateIOSequence();

        int choice = algorithmComboBox.getSelectedIndex() + 1;
        // 生成随机的当前磁道数
        Random random = new Random();
        int currentTrack = random.nextInt(201); // 生成0到200之间的随机数
        Random random2 = new Random();
        boolean isElevatorUp = random2.nextBoolean(); // 随机生成 true 或 false
        List<Integer> schedule;
        switch (choice) {
            case 1:
                schedule = fcfsScheduling(ioSequence, currentTrack);
                break;
            case 2:
                schedule = sstfScheduling(ioSequence, currentTrack);
                break;
            case 3:
                schedule = scanScheduling(ioSequence, currentTrack, isElevatorUp);
                break;
            case 4:
                schedule = lookScheduling(ioSequence, currentTrack, isElevatorUp);
                break;
            default:
                JOptionPane.showMessageDialog(mainFrame, "无效的选择。");
                return;
        }

        int totalSeekTime = 0;
        int seekDistance = 0;
        for (int i = 1; i < schedule.size(); i++) {
            seekDistance = Math.abs(schedule.get(i) - schedule.get(i - 1));
            totalSeekTime += seekDistance * seekTimePerTrack;
        }

        int totalSeekTimeInSeconds = (int) Math.ceil(totalSeekTime / 1000.0);
        int rotationDelay = diskSpeed / 60;
        double averageRotationDelay = rotationDelay / 2.0;
        int transferTime = ioSequence.size() * sectorsPerTrack * bytesPerSector / diskSpeed;
        int totalProcessingTime = totalSeekTime + startTime + transferTime;

        showOutputWindow(seekDistance, totalSeekTime, totalSeekTimeInSeconds, averageRotationDelay, transferTime, totalProcessingTime,ioSequence,schedule,currentTrack,isElevatorUp,choice);

        // Show animation dialog
    }
    private List<Integer> generateIOSequence() {
        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            sequence.add((int) (Math.random() * 200));
        }
        return sequence;
    }
    private List<Integer> fcfsScheduling(List<Integer> ioSequence, int currentTrack) {
        return new ArrayList<>(ioSequence);
    }
    private List<Integer> sstfScheduling(List<Integer> ioSequence, int currentTrack) {
        List<Integer> schedule = new ArrayList<>(ioSequence);
        schedule.sort((a, b) -> Math.abs(a - currentTrack) - Math.abs(b - currentTrack));
        return schedule;
    }
    private List<Integer> scanScheduling(List<Integer> ioSequence, int currentTrack, boolean isElevatorUp) {
        List<Integer> schedule = new ArrayList<>(ioSequence);
        if (isElevatorUp) {
            schedule.sort(Integer::compareTo);
        } else {
            schedule.sort((a, b) -> b.compareTo(a));
        }
        return schedule;
    }
    private List<Integer> lookScheduling(List<Integer> ioSequence, int currentTrack, boolean isElevatorUp) {
        List<Integer> schedule = new ArrayList<>(ioSequence);
        if (isElevatorUp) {
            schedule.sort(Integer::compareTo);
        } else {
            schedule.sort((a, b) -> b.compareTo(a));
        }
        return schedule;
    }

    private void showAnimationDialog2(List<Integer> schedule,int currenttrack){
        JDialog animationDialog = new JDialog(mainFrame, "调度算法演示2", true);
        animationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        animationDialog.setSize(1080, 630); // 增加窗口尺寸
        animationDialog.setLocationRelativeTo(null);

        int[] currentTrack = {0};
        int[] currentIndex = {0};
        currentTrack[0] = currenttrack;
        JPanel animationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                g2d.setStroke(new BasicStroke(2));

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.BLACK);
                // 绘制垂直坐标轴线及标签
                g.drawLine(30, 30, 30, 480);
                g.drawString("次数", 20, 23);
                int y=42,y1=20;
                for(int i=0;i<=20;i++){
                    g.drawString(String.valueOf(y1), 15, y);// 坐标轴端点的数值
                    y+=22;
                    y1--;
                }
                // 绘制水平坐标轴线及标签
                g.drawLine(30, 480, 1030, 480);
                g.drawString("磁道数", 520, 510);
                int x=50,x1=5;
                for(int i=1;i<=40;i++){
                    g.drawString(String.valueOf(x1), x, 495);// 坐标轴端点的数值
                    x+=25;
                    x1+=5;
                }

                int startX = 30+currenttrack*5;
                int startY = 38;

                for (int i = 0; i < currentIndex[0]; i++) {
                    int endX = 30 + schedule.get(i)*5;
                    int endY =startY+22; // 修改此处的索引
                    g.setColor(Color.BLUE);
                    g2d.setStroke(new BasicStroke(1.8f));
                    g.drawLine(startX, startY, endX, endY);
                    g.fillOval(startX-2 , startY-2, 5, 5);
                    startX = endX;
                    startY = endY;
                }

                g.setColor(Color.RED);
                int currentX = startX;
                int currentY = startY;
                g.fillOval(currentX - 3, currentY - 3, 6, 6);

            }
        };


        // 创建用于显示当前磁道位置的 JLabel
        JLabel currentTrackLabel = new JLabel("currentTrack: ");
        currentTrackLabel.setFont(new Font("黑体", Font.PLAIN, 18)); // 设置标签字体和大小
        currentTrackLabel.setHorizontalAlignment(SwingConstants.CENTER); // 设置标签的水平对齐方式
        JLabel trackPositionLabel = new JLabel(String.valueOf(currentTrack[0]));
        trackPositionLabel.setFont(new Font("黑体", Font.PLAIN, 18));

        // 创建放置当前磁道位置的 JPanel
        JPanel trackPositionPanel = new JPanel(new BorderLayout());
        trackPositionPanel.add(currentTrackLabel, BorderLayout.WEST);
        trackPositionPanel.add(trackPositionLabel, BorderLayout.CENTER);

        // 创建动画面板
        animationPanel.setBackground(Color.WHITE);
        animationPanel.setPreferredSize(new Dimension(1080, 630));
        animationDialog.add(trackPositionPanel, BorderLayout.NORTH);
        animationDialog.add(animationPanel, BorderLayout.CENTER);


        // 更新动画
        Timer timer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex[0] < schedule.size()) {
                    currentTrack[0] = schedule.get(currentIndex[0]);
                    currentIndex[0]++;
                    trackPositionLabel.setText(String.valueOf(currentTrack[0])); // 更新磁道位置文本
                    animationPanel.repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();

        // 创建按钮面板
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop(); // 关闭时停止动画
                animationDialog.dispose();
            }
        });
        buttonPanel.add(closeButton);
        animationDialog.add(buttonPanel, BorderLayout.SOUTH);

        // 显示动画窗口
        animationDialog.setVisible(true);
    }
    private void showAnimationDialog(List<Integer> schedule,int currenttrack) {
        JDialog animationDialog = new JDialog(mainFrame, "调度算法演示1", true);
        animationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        animationDialog.setSize(600, 600); // 增加窗口尺寸
        animationDialog.setLocationRelativeTo(null);
        int[] currentTrack = {0};
        int[] currentIndex = {0};
        int trackCount = 200;
        currentTrack[0] = currenttrack;
        JPanel animationPanel = new JPanel() {


            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                int circleRadius = 200; // 增加圆的半径

                // 绘制圆圈
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(centerX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
                Font font = new Font("Arial", Font.PLAIN, 12); // 修改字体大小
                g2d.setFont(font);
                g2d.setColor(Color.BLACK);

                // 绘制磁道数值
                int interval = 5; // 设置间隔
                int count = 0;
                for (int i = 0; i <= 200; i++) {
                    if (i % interval == 0 && i != 200) { // 每隔十个显示一次，不显示200
                        double angle = i / 200.0 * 2 * Math.PI;
                        int x = (int) (centerX + Math.cos(angle) * (circleRadius + 20)); // 增加偏移距离
                        int y = (int) (centerY - Math.sin(angle) * (circleRadius + 20)); // 增加偏移距离
                        String number = String.valueOf(i ); // 数值变大
                        int stringWidth = g2d.getFontMetrics().stringWidth(number);
                        g2d.drawString(number, x - stringWidth / 2, y + font.getSize() / 2);
                    }
                }
                // 计算磁道的角度
                double angle = (double) currentTrack[0] / trackCount * 2 * Math.PI;

                // 计算磁道在圆圈上的位置
                int x = (int) (centerX + Math.cos(angle) * circleRadius);
                int y = (int) (centerY - Math.sin(angle) * circleRadius);

                // 绘制磁道上的红色圆点
                int dotRadius = 10;
                g2d.setColor(Color.RED);
                g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                // 绘制移动路径
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 0; i < currentIndex[0]; i++) {
                    if(i==0)
                    {
                        double pathAngle = (double) schedule.get(i) / trackCount * 2 * Math.PI;
                        int pathX = (int) (centerX + Math.cos(pathAngle) * circleRadius);
                        int pathY = (int) (centerY - Math.sin(pathAngle) * circleRadius);
                        int prevPathX = (int) (centerX + Math.cos((double) currenttrack / trackCount * 2 * Math.PI) * circleRadius);
                        int prevPathY = (int) (centerY - Math.sin((double) currenttrack / trackCount * 2 * Math.PI) * circleRadius);
                        g2d.drawLine(prevPathX, prevPathY, pathX, pathY);
                    }
                    else
                    {double pathAngle = (double) schedule.get(i) / trackCount * 2 * Math.PI;
                        int pathX = (int) (centerX + Math.cos(pathAngle) * circleRadius);
                        int pathY = (int) (centerY - Math.sin(pathAngle) * circleRadius);
                        int prevPathX = (int) (centerX + Math.cos((double) schedule.get(i-1) / trackCount * 2 * Math.PI) * circleRadius);
                        int prevPathY = (int) (centerY - Math.sin((double) schedule.get(i-1) / trackCount * 2 * Math.PI) * circleRadius);
                        g2d.drawLine(prevPathX, prevPathY, pathX, pathY);}
                }
            }
        };
// 创建用于显示当前磁道位置的 JLabel
        JLabel currentTrackLabel = new JLabel("currentTrack: ");
        JLabel trackPositionLabel = new JLabel(String.valueOf(currentTrack[0]));

        // 创建放置当前磁道位置的 JPanel
        JPanel trackPositionPanel = new JPanel(new BorderLayout());
        trackPositionPanel.add(currentTrackLabel, BorderLayout.WEST);
        trackPositionPanel.add(trackPositionLabel, BorderLayout.CENTER);

        // 创建动画面板
        animationPanel.setBackground(Color.WHITE);
        animationPanel.setPreferredSize(new Dimension(600, 600));
        animationDialog.add(animationPanel, BorderLayout.CENTER);
        // 将 JPanel 和磁道位置面板添加到 animationDialog
        animationDialog.add(trackPositionPanel, BorderLayout.NORTH);
        animationDialog.add(animationPanel, BorderLayout.CENTER);


        // 更新动画
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex[0] < schedule.size()) {
                    currentTrack[0] = schedule.get(currentIndex[0]);
                    currentIndex[0]++;
                    trackPositionLabel.setText(String.valueOf(currentTrack[0])); // 更新磁道位置文本
                    animationPanel.repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();

        // 创建按钮面板
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop(); // 关闭时停止动画
                animationDialog.dispose();
            }
        });
        buttonPanel.add(closeButton);
        animationDialog.add(buttonPanel, BorderLayout.SOUTH);

        // 显示动画窗口
        animationDialog.setVisible(true);
    }
    private void showOutputWindow(int seekDistance, int totalSeekTime, int totalSeekTimeInSeconds, double averageRotationDelay, int transferTime, int totalProcessingTime,List<Integer> ioSequence,List<Integer> schedule,int currentTrack,boolean isElevatorUp,int choice) {
        JFrame outputFrame = new JFrame("输出结果");
        outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        outputFrame.setSize(800, 600);
        outputFrame.setLayout(new GridLayout(11, 2));
        // Generate random IO track access sequence
        //  List<Integer> ioSequence = generateIOSequence();

        // Choose scheduling algorithm
        //   int choice = algorithmComboBox.getSelectedIndex() + 1;
        // Perform scheduling based on the chosen algorithm
        //   List<Integer> schedule = null;
// Create output labels
        JLabel ioSequenceLabel = new JLabel("磁道I/O访问序列：" );
        ioSequenceLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel ioSequenceValue = new JLabel(ioSequence.toString());
        // Create scroll pane for long I/O sequence
        JScrollPane scrollPane = new JScrollPane(ioSequenceValue);
        scrollPane.setPreferredSize(new Dimension(380, 100));

        Font textFieldFont10 = ioSequenceValue .getFont();
        ioSequenceValue .setFont(textFieldFont10.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
// Add output labels to the output frame
        outputFrame.add(ioSequenceLabel);
        outputFrame.add(scrollPane);

        outputFrame.setVisible(true);
        // Add output labels to the output frame
        //  outputFrame.add(ioSequenceLabel);
        //   outputFrame.add(ioSequenceValue);
        JLabel scheduleLabel = new JLabel("引臂移动序列：");

        scheduleLabel.setFont(new Font("宋体", Font.BOLD, 24)); // 设置字体为宋体，加粗，字号为18
        JLabel scheduleValue = new JLabel(schedule.toString());
        // Create scroll pane for long I/O sequence
        JScrollPane scrollPane2 = new JScrollPane(scheduleValue);
        scrollPane.setPreferredSize(new Dimension(380, 100));
        Font textFieldFont11 = scheduleValue.getFont();
        scheduleValue.setFont(textFieldFont11.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
// Add output labels to the output frame
        outputFrame.add(scheduleLabel);
        outputFrame.add(scrollPane2);

        outputFrame.setVisible(true);
        //  outputFrame.add(scheduleLabel);
        //    outputFrame.add(scheduleValue);
        JLabel currentTrackLabel = new JLabel("当前磁头所在磁道：");
        currentTrackLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel currentTrackValue = new JLabel(String.valueOf(currentTrack));

        Font textFieldFont00 = currentTrackValue.getFont();
        currentTrackValue.setFont(textFieldFont00.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(currentTrackLabel);
        outputFrame.add(currentTrackValue);

        JLabel isElevatorUpLabel = new JLabel("磁头移动方向：");
        isElevatorUpLabel.setFont(new Font("宋体", Font.BOLD, 24));
        String label;

        if (isElevatorUp) {
            label = "向外";
        } else {
            label = "向内";
        }
        switch (choice) {
            case 3:
                // 选择3时的代码块

                break;
            case 4:
                // 选择4时的代码块
                break;
            default:
                label="NULL";
        }
        JLabel isElevatorUpValue = new JLabel(label);
        Font textFieldFont02 = isElevatorUpValue.getFont();
        isElevatorUpValue.setFont(textFieldFont02.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(isElevatorUpLabel);
        outputFrame.add(isElevatorUpValue);



        JLabel seekDistanceLabel = new JLabel("引臂移动量：");
        seekDistanceLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel seekDistanceValue = new JLabel(String.valueOf(seekDistance));

        Font textFieldFont12 = seekDistanceValue.getFont();
        seekDistanceValue.setFont(textFieldFont12.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(seekDistanceLabel);
        outputFrame.add(seekDistanceValue);


        JLabel totalSeekTimeLabel = new JLabel("寻道时间：");

        totalSeekTimeLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel totalSeekTimeValue = new JLabel(totalSeekTime + " 毫秒");

        Font textFieldFont13 = totalSeekTimeValue.getFont();
        totalSeekTimeValue.setFont(textFieldFont13.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(totalSeekTimeLabel);
        outputFrame.add(totalSeekTimeValue);

        JLabel totalSeekTimeInSecondsLabel = new JLabel("总的寻道时间：");

        totalSeekTimeInSecondsLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel totalSeekTimeInSecondsValue = new JLabel(totalSeekTimeInSeconds + " 秒");
        Font textFieldFont14 = totalSeekTimeInSecondsValue.getFont();
        totalSeekTimeInSecondsValue.setFont(textFieldFont14.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(totalSeekTimeInSecondsLabel);
        outputFrame.add(totalSeekTimeInSecondsValue);

        JLabel averageRotationDelayLabel = new JLabel("平均旋转延迟时间：");

        averageRotationDelayLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel averageRotationDelayValue = new JLabel(averageRotationDelay + " 毫秒");
        Font textFieldFont15 = averageRotationDelayValue.getFont();
        averageRotationDelayValue.setFont(textFieldFont15.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(averageRotationDelayLabel);
        outputFrame.add(averageRotationDelayValue);

        JLabel transferTimeLabel = new JLabel("传输时间：");

        transferTimeLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel transferTimeValue = new JLabel(transferTime + " 毫秒");
        Font textFieldFont16 = transferTimeValue.getFont();
        transferTimeValue.setFont(textFieldFont16.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(transferTimeLabel);
        outputFrame.add(transferTimeValue);

        JLabel totalProcessingTimeLabel = new JLabel("所有访问处理时间：");

        totalProcessingTimeLabel.setFont(new Font("宋体", Font.BOLD, 24));
        JLabel totalProcessingTimeValue = new JLabel(totalProcessingTime + " 毫秒");
        Font textFieldFont17= totalProcessingTimeValue.getFont();
        totalProcessingTimeValue.setFont(textFieldFont17.deriveFont(24f)); // 设置输入框内文字的字体大小为 16
        outputFrame.add(totalProcessingTimeLabel);
        outputFrame.add(totalProcessingTimeValue);

        outputFrame.setVisible(true);
        // 创建动画按钮
        JButton animationButton = new JButton("动画1");
        animationButton.setFont(new Font("宋体", Font.BOLD, 24));
        animationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAnimationDialog(schedule,currentTrack);
            }
        });
        outputFrame.add(animationButton, BorderLayout.SOUTH);

        outputFrame.setVisible(true);
        // 创建动画按钮
        JButton animationButton2 = new JButton("动画2");
        animationButton2.setFont(new Font("宋体", Font.BOLD, 24));
        animationButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //  LineChartAnimation.showLineChartAnimation(schedule);
                //  ChartDrawer.drawLineChart(schedule);
                showAnimationDialog2(schedule,currentTrack);
            }
        });
        outputFrame.add(animationButton2, BorderLayout.SOUTH);
        // Set the position of the window to the center of the screen
        outputFrame.setLocationRelativeTo(null);
        outputFrame.setVisible(true);
    }
}

