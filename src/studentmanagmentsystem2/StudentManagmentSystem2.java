package studentmanagmentsystem2;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Self-contained Java Swing Student Management System (StudentManagmentSystem2).
 * Includes:
 * - User Authentication (Login & Registration with validation)
 * - Session Context (logged-in user & role)
 * - Student Management (Add, Update, Delete, Search)
 * - Course Management (Add, Delete)
 * - Attendance Tracking (Mark Present/Absent per course & date)
 * - Grades & Marks (Internal, Practical, Final calculation & grade letter)
 * - User Management (Create & Delete user accounts)
 * - Academic Overview (Live summary statistics)
 */
public class StudentManagmentSystem2 extends JFrame {

    // ==================== DOMAIN MODELS ====================

    static class User {
        final int id;
        String fullName;
        String email;
        String username;
        String password;
        String role;

        User(int id, String fullName, String email, String username, String password, String role) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    static class Student {
        final int id;
        String enrollmentNo;
        String fullName;
        String email;
        String branch;
        int semester;
        String status;

        Student(int id, String enrollmentNo, String fullName, String email, String branch, int semester, String status) {
            this.id = id;
            this.enrollmentNo = enrollmentNo;
            this.fullName = fullName;
            this.email = email;
            this.branch = branch;
            this.semester = semester;
            this.status = status;
        }
    }

    static class Course {
        final int id;
        String code;
        String name;
        int credits;
        String faculty;

        Course(int id, String code, String name, int credits, String faculty) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.credits = credits;
            this.faculty = faculty;
        }

        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    static class GradeRecord {
        final int id;
        int studentId;
        String courseCode;
        int internalMarks;  // out of 30
        int practicalMarks; // out of 20
        int finalMarks;     // out of 50

        GradeRecord(int id, int studentId, String courseCode, int internalMarks, int practicalMarks, int finalMarks) {
            this.id = id;
            this.studentId = studentId;
            this.courseCode = courseCode;
            this.internalMarks = internalMarks;
            this.practicalMarks = practicalMarks;
            this.finalMarks = finalMarks;
        }

        int getTotal() {
            return internalMarks + practicalMarks + finalMarks;
        }

        String getGradeLetter() {
            int total = getTotal();
            if (total >= 90) return "O";
            if (total >= 80) return "A+";
            if (total >= 70) return "A";
            if (total >= 60) return "B+";
            if (total >= 50) return "B";
            return "F";
        }
    }

    // ==================== IN-MEMORY DATA STORE ====================

    private final List<User> users = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<Course> courses = new ArrayList<>();
    private final List<GradeRecord> grades = new ArrayList<>();
    // Key: "<courseCode>_<yyyy-MM-dd>", Value: Map<studentId, "Present" | "Absent">
    private final Map<String, Map<Integer, String>> attendanceMap = new LinkedHashMap<>();

    private int nextUserId = 10;
    private int nextStudentId = 110;
    private int nextCourseId = 10;
    private int nextGradeId = 10;

    private User currentSessionUser = null;

    // ==================== ROOT LAYOUT & VIEWS ====================

    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootCardLayout);

    private final CardLayout moduleCardLayout = new CardLayout();
    private final JPanel modulePanel = new JPanel(moduleCardLayout);

    // Session header labels
    private final JLabel lblSessionInfo = new JLabel();
    private final JLabel lblPageTitle = new JLabel("Academic Overview");

    // Overview stat labels
    private final JLabel lblStatStudents = new JLabel("0");
    private final JLabel lblStatCourses = new JLabel("0");
    private final JLabel lblStatAttendance = new JLabel("0%");
    private final JLabel lblStatCgpa = new JLabel("0.00");

    // Table models
    private final DefaultTableModel studentTableModel = new DefaultTableModel(
            new String[]{"ID", "Enrollment No", "Full Name", "Email", "Branch", "Semester", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel courseTableModel = new DefaultTableModel(
            new String[]{"ID", "Course Code", "Course Name", "Credits", "Faculty"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel attendanceTableModel = new DefaultTableModel(
            new String[]{"Student ID", "Enrollment No", "Student Name", "Overall %", "Session Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel gradesTableModel = new DefaultTableModel(
            new String[]{"ID", "Student", "Course", "Internal (30)", "Practical (20)", "Final (50)", "Total (100)", "Grade"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel usersTableModel = new DefaultTableModel(
            new String[]{"ID", "Full Name", "Username", "Email", "Role"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // Attendance controls
    private final JComboBox<Course> cmbAttendanceCourse = new JComboBox<>();
    private final JTextField txtAttendanceDate = new JTextField(LocalDate.now().toString(), 12);
    private JTable attendanceTable;

    // Student search control
    private final JTextField txtStudentSearch = new JTextField(18);

    public StudentManagmentSystem2() {
        super("EduTrack Pro - Student Management System (StudentManagmentSystem2)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);

        seedInitialData();
        buildUi();
        rootCardLayout.show(rootPanel, "AUTH");
    }

    private void seedInitialData() {
        users.clear();
        students.clear();
        courses.clear();
        grades.clear();
        attendanceMap.clear();

        users.add(new User(1, "Dr. Rajesh Kulkarni", "rajesh.kulkarni@fet.edu.in", "admin", "password123", "Admin"));
        users.add(new User(2, "Prof. N. K. Mehta", "nk.mehta@fet.edu.in", "nkmehta", "password123", "Faculty"));
        users.add(new User(3, "Riya Sharma", "riya.sharma@fet.edu.in", "riya", "password123", "Student"));

        students.add(new Student(101, "92500119001", "Riya Sharma", "riya.sharma@fet.edu.in", "B.Tech Computer Engineering", 3, "Active"));
        students.add(new Student(102, "92500119014", "Aarav Vikram Deshmukh", "aarav.deshmukh@fet.edu.in", "B.Tech Computer Engineering", 3, "Active"));
        students.add(new Student(103, "92500119031", "Priya Maheshwari Joshi", "priya.joshi@fet.edu.in", "B.Tech AI & Machine Learning", 3, "Active"));
        students.add(new Student(104, "92500119042", "Rohan Siddharth Nair", "rohan.nair@fet.edu.in", "B.Tech Information Technology", 5, "Active"));
        students.add(new Student(105, "92500119055", "Ananya Sanjay Patel", "ananya.patel@fet.edu.in", "B.Tech AI & Machine Learning", 3, "Active"));

        courses.add(new Course(1, "01CE0308", "Advance Java Technology", 4, "Prof. N. K. Mehta"));
        courses.add(new Course(2, "01CE0302", "Data Structures & Algorithms", 4, "Dr. Kavita Trivedi"));
        courses.add(new Course(3, "01CE0305", "Database Management Systems", 4, "Prof. Amitabh Shah"));
        courses.add(new Course(4, "01AI0301", "Foundations of Machine Learning", 3, "Dr. S. R. Rao"));

        String today = LocalDate.now().toString();
        Map<Integer, String> session1 = new LinkedHashMap<>();
        session1.put(101, "Present");
        session1.put(102, "Present");
        session1.put(103, "Present");
        session1.put(104, "Absent");
        session1.put(105, "Present");
        attendanceMap.put("01CE0308_" + today, session1);

        grades.add(new GradeRecord(1, 101, "01CE0308", 28, 19, 46));
        grades.add(new GradeRecord(2, 102, "01CE0308", 24, 17, 40));
        grades.add(new GradeRecord(3, 103, "01AI0301", 29, 19, 45));
        grades.add(new GradeRecord(4, 104, "01CE0305", 22, 16, 37));
    }

    // ==================== UI CONSTRUCTION ====================

    private void buildUi() {
        rootPanel.add(buildAuthPanel(), "AUTH");
        rootPanel.add(buildMainAppPanel(), "APP");
        setContentPane(rootPanel);
    }

    private JPanel buildAuthPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(15, 23, 42));

        JTabbedPane authTabs = new JTabbedPane();
        authTabs.setPreferredSize(new Dimension(430, 390));

        // Login Tab
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel lblTitle = new JLabel("EduTrack Pro Login", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(lblTitle, gbc);

        JLabel lblSub = new JLabel("Student Management System", SwingConstants.CENTER);
        lblSub.setForeground(Color.GRAY);
        gbc.gridy = 1;
        loginPanel.add(lblSub, gbc);

        gbc.gridwidth = 1;
        JTextField txtLoginUser = new JTextField();
        JPasswordField txtLoginPass = new JPasswordField();

        gbc.gridy = 2; gbc.gridx = 0;
        loginPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(txtLoginUser, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        loginPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(txtLoginPass, gbc);

        JButton btnLogin = new JButton("Login");
        JButton btnDemoAdmin = new JButton("Demo Admin Login");
        btnLogin.addActionListener(e -> performLogin(txtLoginUser.getText().trim(), new String(txtLoginPass.getPassword())));
        btnDemoAdmin.addActionListener(e -> performLogin("admin", "password123"));

        gbc.gridy = 4; gbc.gridx = 0;
        loginPanel.add(btnLogin, gbc);
        gbc.gridx = 1;
        loginPanel.add(btnDemoAdmin, gbc);

        // Register Tab
        JPanel registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));
        GridBagConstraints rgbc = new GridBagConstraints();
        rgbc.insets = new Insets(5, 4, 5, 4);
        rgbc.fill = GridBagConstraints.HORIZONTAL;
        rgbc.weightx = 1.0;

        JTextField txtRegName = new JTextField();
        JTextField txtRegEmail = new JTextField();
        JTextField txtRegUser = new JTextField();
        JPasswordField txtRegPass = new JPasswordField();
        JComboBox<String> cmbRegRole = new JComboBox<>(new String[]{"Student", "Faculty", "Admin"});

        addLabeledRow(registerPanel, rgbc, 0, "Full Name *:", txtRegName);
        addLabeledRow(registerPanel, rgbc, 1, "Email *:", txtRegEmail);
        addLabeledRow(registerPanel, rgbc, 2, "Username *:", txtRegUser);
        addLabeledRow(registerPanel, rgbc, 3, "Password (8+ chars) *:", txtRegPass);
        addLabeledRow(registerPanel, rgbc, 4, "Role:", cmbRegRole);

        JButton btnRegister = new JButton("Create Account");
        btnRegister.addActionListener(e -> performRegistration(
                txtRegName.getText().trim(),
                txtRegEmail.getText().trim(),
                txtRegUser.getText().trim(),
                new String(txtRegPass.getPassword()),
                (String) cmbRegRole.getSelectedItem(),
                txtRegName, txtRegEmail, txtRegUser, txtRegPass
        ));

        rgbc.gridy = 5; rgbc.gridx = 0; rgbc.gridwidth = 2;
        registerPanel.add(btnRegister, rgbc);

        authTabs.addTab("Login", loginPanel);
        authTabs.addTab("Register", registerPanel);
        outer.add(authTabs);
        return outer;
    }

    private JPanel buildMainAppPanel() {
        JPanel app = new JPanel(new BorderLayout());

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(15, 41, 66));
        topBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel leftTop = new JPanel(new GridLayout(2, 1));
        leftTop.setOpaque(false);
        lblPageTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblPageTitle.setForeground(Color.WHITE);
        lblSessionInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSessionInfo.setForeground(new Color(186, 230, 253));
        leftTop.add(lblPageTitle);
        leftTop.add(lblSessionInfo);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> performLogout());

        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(btnLogout, BorderLayout.EAST);
        app.add(topBar, BorderLayout.NORTH);

        // Sidebar Navigation
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 41, 59));
        sidebar.setBorder(new EmptyBorder(14, 10, 14, 10));
        sidebar.setPreferredSize(new Dimension(210, 0));

        String[][] navItems = {
                {"DASHBOARD", "Academic Overview"},
                {"STUDENTS", "Student Management"},
                {"COURSES", "Course Management"},
                {"ATTENDANCE", "Attendance Tracker"},
                {"GRADES", "Grades & Marks"},
                {"USERS", "User Management"}
        };

        for (String[] item : navItems) {
            JButton btn = new JButton(item[1]);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> switchModule(item[0], item[1]));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(8));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton btnReset = new JButton("Reset Demo Data");
        btnReset.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnReset.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReset.addActionListener(e -> {
            seedInitialData();
            refreshAllViews();
            JOptionPane.showMessageDialog(this, "Demo data restored.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        });
        sidebar.add(btnReset);

        app.add(sidebar, BorderLayout.WEST);

        // Module Cards
        modulePanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        modulePanel.add(buildDashboardPanel(), "DASHBOARD");
        modulePanel.add(buildStudentsPanel(), "STUDENTS");
        modulePanel.add(buildCoursesPanel(), "COURSES");
        modulePanel.add(buildAttendancePanel(), "ATTENDANCE");
        modulePanel.add(buildGradesPanel(), "GRADES");
        modulePanel.add(buildUsersPanel(), "USERS");

        app.add(modulePanel, BorderLayout.CENTER);
        return app;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 14, 14));
        statsGrid.add(createStatCard("Total Students", lblStatStudents));
        statsGrid.add(createStatCard("Active Courses", lblStatCourses));
        statsGrid.add(createStatCard("Avg. Attendance", lblStatAttendance));
        statsGrid.add(createStatCard("Average CGPA", lblStatCgpa));

        JTextArea infoArea = new JTextArea(
                "System Summary:\n" +
                "• Use the left navigation menu to manage Students, Courses, Attendance, Grades, and User Accounts.\n" +
                "• All changes update live statistics across the Academic Overview.\n" +
                "• Click 'Reset Demo Data' in the sidebar anytime to restore sample college records."
        );
        infoArea.setEditable(false);
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        infoArea.setBackground(new Color(248, 250, 252));

        panel.add(statsGrid, BorderLayout.NORTH);
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setBackground(new Color(241, 245, 249));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(Color.DARK_GRAY);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("Search:"));
        txtStudentSearch.addActionListener(e -> refreshStudentsTable());
        JButton btnSearch = new JButton("Filter");
        btnSearch.addActionListener(e -> refreshStudentsTable());

        JButton btnAdd = new JButton("+ Add Student");
        JButton btnEdit = new JButton("Edit Selected");
        JButton btnDelete = new JButton("Delete Selected");

        JTable table = new JTable(studentTableModel);
        btnAdd.addActionListener(e -> openStudentDialog(null));
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a student row first.");
                return;
            }
            int id = (int) studentTableModel.getValueAt(row, 0);
            findStudentById(id).ifPresent(this::openStudentDialog);
        });
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a student row first.");
                return;
            }
            int id = (int) studentTableModel.getValueAt(row, 0);
            students.removeIf(s -> s.id == id);
            refreshAllViews();
        });

        toolbar.add(txtStudentSearch);
        toolbar.add(btnSearch);
        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("+ Add Course");
        JButton btnDelete = new JButton("Delete Selected");
        JTable table = new JTable(courseTableModel);

        btnAdd.addActionListener(e -> openCourseDialog());
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a course to delete.");
                return;
            }
            int id = (int) courseTableModel.getValueAt(row, 0);
            courses.removeIf(c -> c.id == id);
            refreshAllViews();
        });

        toolbar.add(btnAdd);
        toolbar.add(btnDelete);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        cmbAttendanceCourse.addActionListener(e -> refreshAttendanceTable());
        JButton btnLoad = new JButton("Load Sheet");
        btnLoad.addActionListener(e -> refreshAttendanceTable());

        JButton btnPresent = new JButton("Mark Present");
        JButton btnAbsent = new JButton("Mark Absent");
        JButton btnSave = new JButton("Save Attendance");

        attendanceTable = new JTable(attendanceTableModel);

        btnPresent.addActionListener(e -> setSelectedAttendanceStatus("Present"));
        btnAbsent.addActionListener(e -> setSelectedAttendanceStatus("Absent"));
        btnSave.addActionListener(e -> saveAttendanceFromTable());

        toolbar.add(new JLabel("Course:"));
        toolbar.add(cmbAttendanceCourse);
        toolbar.add(new JLabel("Date (YYYY-MM-DD):"));
        toolbar.add(txtAttendanceDate);
        toolbar.add(btnLoad);
        toolbar.add(btnPresent);
        toolbar.add(btnAbsent);
        toolbar.add(btnSave);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("+ Record Marks");
        JButton btnDelete = new JButton("Delete Selected");
        JTable table = new JTable(gradesTableModel);

        btnAdd.addActionListener(e -> openGradeDialog());
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a grade record to delete.");
                return;
            }
            int id = (int) gradesTableModel.getValueAt(row, 0);
            grades.removeIf(g -> g.id == id);
            refreshAllViews();
        });

        toolbar.add(btnAdd);
        toolbar.add(btnDelete);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("+ Add User");
        JButton btnDelete = new JButton("Delete Selected");
        JTable table = new JTable(usersTableModel);

        btnAdd.addActionListener(e -> openUserDialog());
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a user to delete.");
                return;
            }
            int id = (int) usersTableModel.getValueAt(row, 0);
            if (id == 1) {
                JOptionPane.showMessageDialog(this, "Primary admin account cannot be deleted.");
                return;
            }
            users.removeIf(u -> u.id == id);
            refreshUsersTable();
        });

        toolbar.add(btnAdd);
        toolbar.add(btnDelete);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ==================== DIALOGS & ACTIONS ====================

    private void openStudentDialog(Student existing) {
        JTextField txtEnroll = new JTextField(existing != null ? existing.enrollmentNo : "");
        JTextField txtName = new JTextField(existing != null ? existing.fullName : "");
        JTextField txtEmail = new JTextField(existing != null ? existing.email : "");
        JComboBox<String> cmbBranch = new JComboBox<>(new String[]{
                "B.Tech Computer Engineering",
                "B.Tech AI & Machine Learning",
                "B.Tech Information Technology"
        });
        if (existing != null) cmbBranch.setSelectedItem(existing.branch);

        JSpinner spnSem = new JSpinner(new SpinnerNumberModel(existing != null ? existing.semester : 3, 1, 8, 1));
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Active", "Probation", "Graduated"});
        if (existing != null) cmbStatus.setSelectedItem(existing.status);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Enrollment No *:")); form.add(txtEnroll);
        form.add(new JLabel("Full Name *:")); form.add(txtName);
        form.add(new JLabel("Email *:")); form.add(txtEmail);
        form.add(new JLabel("Branch:")); form.add(cmbBranch);
        form.add(new JLabel("Semester:")); form.add(spnSem);
        form.add(new JLabel("Status:")); form.add(cmbStatus);

        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add New Student" : "Edit Student",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String enroll = txtEnroll.getText().trim();
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            if (enroll.isEmpty() || name.isEmpty() || !isValidEmail(email)) {
                JOptionPane.showMessageDialog(this, "Enter valid Enrollment No, Full Name, and Email.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int sem = (int) spnSem.getValue();
            String branch = (String) cmbBranch.getSelectedItem();
            String status = (String) cmbStatus.getSelectedItem();

            if (existing == null) {
                students.add(new Student(nextStudentId++, enroll, name, email, branch, sem, status));
            } else {
                existing.enrollmentNo = enroll;
                existing.fullName = name;
                existing.email = email;
                existing.branch = branch;
                existing.semester = sem;
                existing.status = status;
            }
            refreshAllViews();
        }
    }

    private void openCourseDialog() {
        JTextField txtCode = new JTextField();
        JTextField txtName = new JTextField();
        JSpinner spnCredits = new JSpinner(new SpinnerNumberModel(4, 1, 6, 1));
        JTextField txtFaculty = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course Code *:")); form.add(txtCode);
        form.add(new JLabel("Course Name *:")); form.add(txtName);
        form.add(new JLabel("Credits:")); form.add(spnCredits);
        form.add(new JLabel("Faculty *:")); form.add(txtFaculty);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Course",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String code = txtCode.getText().trim().toUpperCase();
            String name = txtName.getText().trim();
            String faculty = txtFaculty.getText().trim();
            if (code.isEmpty() || name.isEmpty() || faculty.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All course fields are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            courses.add(new Course(nextCourseId++, code, name, (int) spnCredits.getValue(), faculty));
            refreshAllViews();
        }
    }

    private void openGradeDialog() {
        if (students.isEmpty() || courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ensure at least one student and course exist.");
            return;
        }
        JComboBox<Student> cmbStudent = new JComboBox<>(students.toArray(new Student[0]));
        cmbStudent.setRenderer((list, value, index, isSelected, cellHasFocus) ->
                new JLabel(value == null ? "" : value.enrollmentNo + " - " + value.fullName));

        JComboBox<Course> cmbCourse = new JComboBox<>(courses.toArray(new Course[0]));
        JTextField txtInternal = new JTextField("25");
        JTextField txtPractical = new JTextField("18");
        JTextField txtFinal = new JTextField("40");

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Student:")); form.add(cmbStudent);
        form.add(new JLabel("Course:")); form.add(cmbCourse);
        form.add(new JLabel("Internal (0-30):")); form.add(txtInternal);
        form.add(new JLabel("Practical (0-20):")); form.add(txtPractical);
        form.add(new JLabel("Final (0-50):")); form.add(txtFinal);

        int result = JOptionPane.showConfirmDialog(this, form, "Record Marks",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int internal = Integer.parseInt(txtInternal.getText().trim());
                int practical = Integer.parseInt(txtPractical.getText().trim());
                int finalM = Integer.parseInt(txtFinal.getText().trim());
                if (internal < 0 || internal > 30 || practical < 0 || practical > 20 || finalM < 0 || finalM > 50) {
                    throw new NumberFormatException();
                }
                Student s = (Student) cmbStudent.getSelectedItem();
                Course c = (Course) cmbCourse.getSelectedItem();
                grades.add(new GradeRecord(nextGradeId++, s.id, c.code, internal, practical, finalM));
                refreshAllViews();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers within range (0-30, 0-20, 0-50).",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openUserDialog() {
        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"Faculty", "Admin", "Student"});

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Full Name *:")); form.add(txtName);
        form.add(new JLabel("Email *:")); form.add(txtEmail);
        form.add(new JLabel("Username *:")); form.add(txtUser);
        form.add(new JLabel("Password (8+) *:")); form.add(txtPass);
        form.add(new JLabel("Role:")); form.add(cmbRole);

        int result = JOptionPane.showConfirmDialog(this, form, "Create User Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            performRegistration(
                    txtName.getText().trim(),
                    txtEmail.getText().trim(),
                    txtUser.getText().trim(),
                    new String(txtPass.getPassword()),
                    (String) cmbRole.getSelectedItem(),
                    null, null, null, null
            );
            refreshUsersTable();
        }
    }

    // ==================== AUTH, SESSION & REFRESH LOGIC ====================

    private void performLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.", "Login Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Optional<User> match = users.stream()
                .filter(u -> u.username.equalsIgnoreCase(username) && u.password.equals(password))
                .findFirst();

        if (match.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Try 'admin' / 'password123' or Demo Admin Login.",
                    "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentSessionUser = match.get();
        lblSessionInfo.setText("Logged in as: " + currentSessionUser.fullName + " (" + currentSessionUser.username + ") | Role: " + currentSessionUser.role);
        refreshAllViews();
        switchModule("DASHBOARD", "Academic Overview");
        rootCardLayout.show(rootPanel, "APP");
    }

    private void performRegistration(String fullName, String email, String username, String password, String role,
                                     JTextField f1, JTextField f2, JTextField f3, JPasswordField f4) {
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.length() < 8 || !isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid details. Email must be valid and password must be at least 8 characters.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean duplicate = users.stream().anyMatch(u -> u.username.equalsIgnoreCase(username));
        if (duplicate) {
            JOptionPane.showMessageDialog(this, "Username already exists.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        users.add(new User(nextUserId++, fullName, email, username, password, role));
        if (f1 != null) { f1.setText(""); f2.setText(""); f3.setText(""); f4.setText(""); }
        JOptionPane.showMessageDialog(this, "Account created for '" + username + "'. You can now log in.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void performLogout() {
        currentSessionUser = null;
        rootCardLayout.show(rootPanel, "AUTH");
    }

    private void switchModule(String cardName, String title) {
        lblPageTitle.setText(title);
        moduleCardLayout.show(modulePanel, cardName);
        refreshAllViews();
    }

    private void refreshAllViews() {
        refreshCourseDropdown();
        refreshStudentsTable();
        refreshCourseTable();
        refreshAttendanceTable();
        refreshGradesTable();
        refreshUsersTable();
        refreshDashboardStats();
    }

    private void refreshDashboardStats() {
        lblStatStudents.setText(String.valueOf(students.size()));
        lblStatCourses.setText(String.valueOf(courses.size()));

        int avgAtt = students.isEmpty() ? 0 : (int) Math.round(
                students.stream().mapToInt(s -> calculateAttendancePercentage(s.id)).average().orElse(0.0));
        lblStatAttendance.setText(avgAtt + "%");

        double avgCgpa = grades.isEmpty() ? 0.0 :
                grades.stream().mapToDouble(g -> g.getTotal() / 10.0).average().orElse(0.0);
        lblStatCgpa.setText(String.format(Locale.US, "%.2f", avgCgpa));
    }

    private void refreshStudentsTable() {
        String q = txtStudentSearch.getText().trim().toLowerCase();
        studentTableModel.setRowCount(0);
        for (Student s : students) {
            if (q.isEmpty()
                    || s.fullName.toLowerCase().contains(q)
                    || s.enrollmentNo.toLowerCase().contains(q)
                    || s.branch.toLowerCase().contains(q)) {
                studentTableModel.addRow(new Object[]{
                        s.id, s.enrollmentNo, s.fullName, s.email, s.branch, "Sem " + s.semester, s.status
                });
            }
        }
    }

    private void refreshCourseDropdown() {
        Course selected = (Course) cmbAttendanceCourse.getSelectedItem();
        cmbAttendanceCourse.removeAllItems();
        for (Course c : courses) {
            cmbAttendanceCourse.addItem(c);
        }
        if (selected != null) {
            for (int i = 0; i < cmbAttendanceCourse.getItemCount(); i++) {
                if (cmbAttendanceCourse.getItemAt(i).code.equals(selected.code)) {
                    cmbAttendanceCourse.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshCourseTable() {
        courseTableModel.setRowCount(0);
        for (Course c : courses) {
            courseTableModel.addRow(new Object[]{c.id, c.code, c.name, c.credits, c.faculty});
        }
    }

    private void refreshAttendanceTable() {
        attendanceTableModel.setRowCount(0);
        Course c = (Course) cmbAttendanceCourse.getSelectedItem();
        String dateStr = txtAttendanceDate.getText().trim();
        String key = (c != null ? c.code : "NONE") + "_" + dateStr;
        Map<Integer, String> sessionRec = attendanceMap.getOrDefault(key, Collections.emptyMap());

        for (Student s : students) {
            String status = sessionRec.getOrDefault(s.id, "Present");
            int pct = calculateAttendancePercentage(s.id);
            attendanceTableModel.addRow(new Object[]{s.id, s.enrollmentNo, s.fullName, pct + "%", status});
        }
    }

    private void setSelectedAttendanceStatus(String status) {
        int row = attendanceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a student row first.");
            return;
        }
        attendanceTableModel.setValueAt(status, row, 4);
    }

    private void saveAttendanceFromTable() {
        Course c = (Course) cmbAttendanceCourse.getSelectedItem();
        String dateStr = txtAttendanceDate.getText().trim();
        if (c == null || dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a course and valid date.");
            return;
        }
        String key = c.code + "_" + dateStr;
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < attendanceTableModel.getRowCount(); i++) {
            int studentId = (int) attendanceTableModel.getValueAt(i, 0);
            String status = String.valueOf(attendanceTableModel.getValueAt(i, 4));
            map.put(studentId, status);
        }
        attendanceMap.put(key, map);
        refreshAllViews();
        JOptionPane.showMessageDialog(this, "Attendance saved for " + c.code + " (" + dateStr + ").");
    }

    private int calculateAttendancePercentage(int studentId) {
        int total = 0;
        int present = 0;
        for (Map<Integer, String> rec : attendanceMap.values()) {
            if (rec.containsKey(studentId)) {
                total++;
                if ("Present".equalsIgnoreCase(rec.get(studentId))) {
                    present++;
                }
            }
        }
        return total == 0 ? 100 : (int) Math.round((present * 100.0) / total);
    }

    private void refreshGradesTable() {
        gradesTableModel.setRowCount(0);
        for (GradeRecord g : grades) {
            String studentLabel = findStudentById(g.studentId)
                    .map(s -> s.enrollmentNo + " - " + s.fullName)
                    .orElse("Student #" + g.studentId);
            gradesTableModel.addRow(new Object[]{
                    g.id, studentLabel, g.courseCode,
                    g.internalMarks, g.practicalMarks, g.finalMarks,
                    g.getTotal(), g.getGradeLetter()
            });
        }
    }

    private void refreshUsersTable() {
        usersTableModel.setRowCount(0);
        for (User u : users) {
            usersTableModel.addRow(new Object[]{u.id, u.fullName, u.username, u.email, u.role});
        }
    }

    private Optional<Student> findStudentById(int id) {
        return students.stream().filter(s -> s.id == id).findFirst();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void addLabeledRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(comp, gbc);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new StudentManagmentSystem2().setVisible(true);
        });
    }
}