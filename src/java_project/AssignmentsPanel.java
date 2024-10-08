package java_project;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import components.RoundedPanel;
import java_project.models.ClassMember;
import java_project.models.ClassRoom;
import java_project.models.User;

public class AssignmentsPanel extends JPanel {
    private ClassRoom classRoom;
    private User user;
    private JPanel assignmentsPanel;
    private JLabel loadingLabel;
    private boolean isAdmin;
    @SuppressWarnings("unused")
    private boolean isModerator;

    public AssignmentsPanel(ClassRoom classRoom, User user) {
        this.classRoom = classRoom;
        this.user = user;
        checkUserRole();

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        ImageIcon backIcon = new ImageIcon(getClass().getResource("/java_project/back-icon.png"));
        ImageIcon scaledBackIcon = new ImageIcon(
                backIcon.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_AREA_AVERAGING));

        JButton backButton = new JButton(scaledBackIcon);

        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setOpaque(false);

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateToClassDetails();
            }
        });
        topPanel.add(backButton, BorderLayout.WEST);

        if (isAdmin) {
            JButton setGradesButton = new JButton("Grades");
            setGradesButton.setContentAreaFilled(false);
            setGradesButton.setBorderPainted(false);
            setGradesButton.setFocusPainted(false);
            setGradesButton.setOpaque(false);
            setGradesButton.addActionListener(e -> {
                navigateToAssignmentGradesPanel();
            });
            topPanel.add(setGradesButton, BorderLayout.EAST);
        }

        add(topPanel, BorderLayout.NORTH);

        assignmentsPanel = new JPanel();
        assignmentsPanel.setLayout(new BoxLayout(assignmentsPanel, BoxLayout.Y_AXIS));
        assignmentsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        loadingLabel = new JLabel("Loading assignments...");
        assignmentsPanel.add(loadingLabel);

        JScrollPane scrollPane = new JScrollPane(assignmentsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        fetchAndDisplayAssignments();
    }

    private void navigateToClassDetails() {
        removeAll();
        add(new ClassDetails(classRoom, user));
        revalidate();
        repaint();
    }

    private void navigateToAssignmentGradesPanel() {
        removeAll();
        add(new AssignmentGradesPanel(classRoom, user));
        revalidate();
        repaint();
    }

    private void checkUserRole() {
        for (ClassMember member : classRoom.getMembers()) {
            String memberId = member.getUserId();
            String role = member.getRole();
            if (user.getId().equals(memberId)) {
                if ("ADMIN".equals(role)) {
                    isAdmin = true;
                } else if ("MODERATOR".equals(role)) {
                    isModerator = true;
                }
            }
        }
    }

    private void fetchAndDisplayAssignments() {
        String url = "http://localhost:5000/api/classrooms/" + classRoom.getId() + "/" + user.getId() + "/assignments";
        SwingUtilities.invokeLater(() -> {
            assignmentsPanel.removeAll();
            assignmentsPanel.add(loadingLabel);
            revalidate();
            repaint();
        });

        new Thread(() -> {
            try {
                String response = Utility.executeGet(url);
                JSONParser parser = new JSONParser();
                JSONArray assignmentsArray = (JSONArray) parser.parse(response);

                SwingUtilities.invokeLater(() -> {
                    assignmentsPanel.remove(loadingLabel);
                    if (assignmentsArray.isEmpty()) {
                        assignmentsPanel.add(new JLabel("No assignments found for this class."));
                    } else {
                        displayAssignments(assignmentsArray);
                    }
                    revalidate();
                    repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    assignmentsPanel.remove(loadingLabel);
                    assignmentsPanel.add(new JLabel("An error occurred while fetching assignments: " + e.getMessage()));
                    revalidate();
                    repaint();
                });
            }
        }).start();
    }

    private void displayAssignments(JSONArray assignmentsArray) {
        assignmentsPanel.removeAll(); // Clear previous content

        for (Object obj : assignmentsArray) {
            if (!(obj instanceof JSONObject)) {
                continue; // Skip if it's not a JSONObject
            }

            JSONObject assignmentObj = (JSONObject) obj;
            String assignmentId = String.valueOf(assignmentObj.get("id"));
            String assignmentTitle = (String) assignmentObj.get("title");
            String assignmentDescription = (String) assignmentObj.get("description");
            JSONArray submissionsArray = (JSONArray) assignmentObj.get("submissions");
            JSONObject submission = submissionsArray.isEmpty() ? null : (JSONObject) submissionsArray.get(0);
            JSONObject file = (JSONObject) assignmentObj.get("file");

            // Create a wrapper panel to center the RoundedPanel
            JPanel wrapperPanel = new JPanel();
            wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.X_AXIS));
            wrapperPanel.setOpaque(false); // Ensure the wrapper panel is transparent
            wrapperPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Create a rounded card panel for each assignment
            RoundedPanel cardPanel = new RoundedPanel(20); // Adjust the radius as needed
            cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

            // Center align components within the cardPanel
            cardPanel.setBackground(Color.WHITE);
            cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Optional padding for the card

            JLabel titleLabel = new JLabel(assignmentTitle);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center alignment
            cardPanel.add(titleLabel);

            JLabel descriptionLabel = new JLabel(assignmentDescription);
            descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center alignment
            cardPanel.add(descriptionLabel);

            if (submission != null) {
                displaySubmissionDetails(cardPanel, submission);
            } else if (!isAdmin) { // Disable submission for admins
                JButton submitButton = new JButton("Submit Assignment");
                submitButton.addActionListener(e -> {
                    submitButton.setEnabled(false); // Disable submit button
                    submitButton.setText("Loading..."); // Show loading text
                    openFileChooser(assignmentId, submitButton, cardPanel);
                });
                submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                cardPanel.add(submitButton);
            }

            if (file != null) {
                String filePath = (String) file.get("filePath");
                components.Button viewFileButton = new components.Button("View Assignment");
                viewFileButton.setBackground(new java.awt.Color(157, 153, 255));
                viewFileButton.setForeground(new java.awt.Color(255, 255, 255));
                viewFileButton.setPreferredSize(new Dimension(400, 100));
                viewFileButton.setEffectColor(new java.awt.Color(199, 196, 255));
                viewFileButton.addActionListener(e -> openFile(filePath));
                viewFileButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                cardPanel.add(viewFileButton);
            }

            cardPanel.setPreferredSize(new Dimension(400, 200));
            cardPanel.setMaximumSize(new Dimension(400, 200));
            wrapperPanel.add(Box.createHorizontalGlue());
            wrapperPanel.add(cardPanel);
            wrapperPanel.add(Box.createHorizontalGlue());

            assignmentsPanel.add(wrapperPanel);
            assignmentsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        SwingUtilities.invokeLater(() -> {
            assignmentsPanel.revalidate();
            assignmentsPanel.repaint();
        });
    }


    private void displaySubmissionDetails(JPanel assignmentPanel, JSONObject submission) {
        String fileName = (String) submission.get("fileName");
        String filePath = (String) submission.get("filePath");
        String grade = submission.get("grade") != null ? submission.get("grade").toString() : "Pending";

        JLabel submittedLabel = new JLabel("Submitted File: " + fileName);
        assignmentPanel.add(submittedLabel);

        JButton openFileButton = new JButton("Open Submitted File");
        openFileButton.addActionListener(e -> openFile(filePath));
        assignmentPanel.add(openFileButton);

        JLabel gradeLabel = new JLabel("Grade: " + grade);
        assignmentPanel.add(gradeLabel);
    }

    @SuppressWarnings("unchecked")
    private void openFileChooser(String assignmentId, JButton submitButton, JPanel assignmentPanel) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF and DOCX files", "pdf", "docx");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null) {
                new Thread(() -> {
                    String uploadResult = uploadAssignmentSubmission(assignmentId, selectedFile);
                    SwingUtilities.invokeLater(() -> {
                        if (uploadResult.startsWith("Upload successful:")) {
                            assignmentPanel.removeAll();
                            JSONObject submissionResponse = new JSONObject();
                            submissionResponse.put("fileName", selectedFile.getName());
                            submissionResponse.put("filePath", selectedFile.getAbsolutePath());
                            submissionResponse.put("grade", "Pending");
                            displaySubmissionDetails(assignmentPanel, submissionResponse);
                            assignmentPanel.revalidate();
                            assignmentPanel.repaint();
                        } else {
                            submitButton.setEnabled(true);
                            submitButton.setText("Submit Assignment");
                            JOptionPane.showMessageDialog(this, "Failed to submit the assignment: " + uploadResult,
                                    "Submission Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }).start();
            }
        } else {
            submitButton.setEnabled(true);
            submitButton.setText("Submit Assignment");
        }
    }

    @SuppressWarnings("deprecation")
    private String uploadAssignmentSubmission(String assignmentId, File file) {
        String url = "http://localhost:5000/api/classrooms/" + assignmentId + "/submit";
        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream output = connection.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)) {

                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"userId\"").append(CRLF);
                writer.append(CRLF).append(user.getId()).append(CRLF).flush();

                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName())
                        .append("\"").append(CRLF);
                writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(file.getName()))
                        .append(CRLF).append(CRLF).flush();

                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    output.flush();
                }

                writer.append(CRLF).append("--").append(boundary).append("--").append(CRLF).flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                // Handle success response (e.g., parse JSON response)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // Parse and process response here (optional)
                    return "Upload successful: " + response.toString();
                }
            } else {
                return "Upload failed: " + responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while uploading file: " + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void openFile(String filePath) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URL(filePath).toURI());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while opening the file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
