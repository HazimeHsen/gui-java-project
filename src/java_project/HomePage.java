package java_project;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java_project.models.ClassMember;
import java_project.models.ClassRoom;
import java_project.models.User;

public class HomePage extends JFrame {
    private User user;

    public HomePage(User user) {
        this.user = user;

        setTitle("Home Page - Welcome " + user.getName());
        setSize(800, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        JButton createClassButton = new JButton("Create Class");
        createClassButton.setPreferredSize(new Dimension(150, 30));
        createClassButton.addActionListener(e -> openCreateClassDialog());
        buttonPanel.add(createClassButton);
        add(buttonPanel, BorderLayout.NORTH);

        JPanel classesPanel = new JPanel();
        classesPanel.setLayout(new GridLayout(0, 3, 10, 10)); 
        JScrollPane scrollPane = new JScrollPane(classesPanel); 
        scrollPane.setPreferredSize(new Dimension(800, 400)); 
        add(scrollPane, BorderLayout.CENTER);

        loadUserClasses(classesPanel);
    }

    private void openCreateClassDialog() {
        JTextField nameField = new JTextField(10);
        JTextField descriptionField = new JTextField(10);

        JPanel panel = new JPanel();
        panel.add(new JLabel("Class Name:"));
        panel.add(nameField);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(new JLabel("Description:"));
        panel.add(descriptionField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Create a New Class", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String className = nameField.getText();
            String description = descriptionField.getText();

            if (!className.isEmpty() && !description.isEmpty()) {
                createClass(className, description);
            } else {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createClass(String className, String description) {
        String url = "http://localhost:5000/api/classrooms";

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name", className);
        jsonParam.put("description", description);
        jsonParam.put("creatorId", user.getId());

        try {
            String response = Utility.executePost(url, jsonParam.toString());

            if (response != null && !response.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Class created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUserClasses((JPanel) ((JScrollPane) getContentPane().getComponent(1)).getViewport().getView());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create class. No response from server.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadUserClasses(JPanel classesPanel) {
        String url = "http://localhost:5000/api/classrooms/" + user.getId() + "/classes";

        try {
            String response = Utility.executeGet(url);

            if (response != null && !response.isEmpty()) {
                JSONParser parser = new JSONParser();
                JSONArray classArray = (JSONArray) parser.parse(response);
                classesPanel.removeAll();

                for (Object obj : classArray) {
                    JSONObject classObj = (JSONObject) obj;
                    String className = (String) classObj.get("name");
                    String classDescription = (String) classObj.get("description");
                    String classId = String.valueOf(classObj.get("id"));

                    JSONArray membersArray = (JSONArray) classObj.get("members");
                    List<ClassMember> members = new ArrayList<>();
                    for (Object memberObj : membersArray) {
                        JSONObject memberJson = (JSONObject) memberObj;
                        String userId = String.valueOf(memberJson.get("userId"));
                        String role = (String) memberJson.get("role");
                        members.add(new ClassMember(userId, role, classId));
                    }

                    ClassRoom classRoom = new ClassRoom(classId, className, classDescription, members);
                    JPanel classCard = createClassCard(classRoom);
                    classesPanel.add(classCard);
                }

                classesPanel.revalidate();
                classesPanel.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load classes. No response from server.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private JPanel createClassCard(ClassRoom classRoom) {
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        cardPanel.setPreferredSize(new Dimension(250, 150));
        cardPanel.setMinimumSize(new Dimension(250, 150));

        JLabel nameLabel = new JLabel("Class Name: " + classRoom.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel descriptionLabel = new JLabel("Description: " + classRoom.getDescription());

        cardPanel.add(nameLabel);
        cardPanel.add(descriptionLabel);

        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    new ClassDetails(classRoom, user).setVisible(true);
                });
            }
        });

        return cardPanel;
    }

    public static void main(String[] args) {
        User user = new User("1", "John Doe", "john.doe@example.com");
        SwingUtilities.invokeLater(() -> new HomePage(user).setVisible(true));
    }
}
