package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.service.ClassService;
import com.company.ems.service.ResultService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Panel hiển thị & quản lý kết quả học tập của toàn bộ học viên.
 */
public class ResultAdminPanel extends JPanel {

    private static final String[] COLS = {
        "STT", "Mã HV", "Họ và tên", "Lớp học", "Khóa học", "Điểm", "Xếp loại", "Nhận xét"
    };

    private final ResultService resultService;
    private final ClassService  classService;
    private final boolean       readOnly;

    private List<Result> allResults      = new ArrayList<>();
    private List<Result> filteredResults = new ArrayList<>();
    private Runnable     onDataChanged;
    private boolean      suppressFilter  = false;

    private JLabel            lblTotal, lblAvg, lblPassRate, lblFailCnt;
    private DefaultTableModel tableModel;
    private JTable            table;
    private JComboBox<Object> cbClass;
    private JTextField        tfSearch;

    public ResultAdminPanel(ResultService resultService, ClassService classService, boolean readOnly) {
        this.resultService = resultService;
        this.classService  = classService;
        this.readOnly      = readOnly;

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_PAGE);

        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);

        loadData();
    }

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    // ── Build UI ──────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 24, 0, 24));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiRow.setOpaque(false);

        lblTotal    = new JLabel("—");
        lblAvg      = new JLabel("—");
        lblPassRate = new JLabel("—");
        lblFailCnt  = new JLabel("—");

        kpiRow.add(buildKpiCard("📋  Tổng đã chấm điểm", lblTotal,    Theme.PRIMARY));
        kpiRow.add(buildKpiCard("📈  Điểm trung bình",    lblAvg,      Theme.GREEN));
        kpiRow.add(buildKpiCard("✅  Tỉ lệ đạt (≥ 5.0)", lblPassRate, Theme.AMBER));
        kpiRow.add(buildKpiCard("❌  Không đạt (< 5.0)", lblFailCnt,  Theme.RED));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.setBorder(new EmptyBorder(10, 0, 10, 0));

        cbClass = new JComboBox<>();
        cbClass.setFont(Theme.FONT_PLAIN);
        cbClass.setPreferredSize(new Dimension(220, 32));
        cbClass.setRenderer(new ClassComboRenderer());
        cbClass.addActionListener(e -> { if (!suppressFilter) filterAndDisplay(); });

        tfSearch = new JTextField();
        tfSearch.setFont(Theme.FONT_PLAIN);
        tfSearch.setPreferredSize(new Dimension(200, 32));
        tfSearch.putClientProperty("JTextField.placeholderText", "Tìm tên / mã học viên...");
        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filterAndDisplay(); }
            public void removeUpdate(DocumentEvent e)  { filterAndDisplay(); }
            public void changedUpdate(DocumentEvent e) { filterAndDisplay(); }
        });

        JButton btnRefresh = ComponentFactory.secondaryButton("🔄  Làm mới");
        btnRefresh.addActionListener(e -> loadData());

        JLabel lbl1 = styledMutedLabel("Lọc theo lớp:");
        JLabel lbl2 = styledMutedLabel("Tìm kiếm:");

        filterRow.add(lbl1); filterRow.add(cbClass);
        filterRow.add(Box.createHorizontalStrut(8));
        filterRow.add(lbl2); filterRow.add(tfSearch);
        filterRow.add(Box.createHorizontalStrut(8));
        filterRow.add(btnRefresh);

        wrapper.add(kpiRow,    BorderLayout.NORTH);
        wrapper.add(filterRow, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildMainArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(12, 24, 20, 24));

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? Theme.ROW_SELECT
                        : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                c.setForeground(Theme.TEXT_MAIN);
                return c;
            }
        };
        table.setFont(Theme.FONT_PLAIN);
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        int[] colWidths = {44, 80, 160, 130, 160, 60, 72, 200};
        for (int i = 0; i < colWidths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        table.getColumnModel().getColumn(0).setMaxWidth(44);

        JTableHeader hdr = table.getTableHeader();
        hdr.setFont(Theme.FONT_BOLD);
        hdr.setBackground(Theme.BG_HEADER);
        hdr.setForeground(Theme.TEXT_MUTED);
        hdr.setPreferredSize(new Dimension(0, 36));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        if (!readOnly) {
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0)
                        openEditDialog(getSelectedResult());
                }
            });
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        JLabel lblCount = new JLabel("Hiển thị 0 kết quả");
        lblCount.setFont(Theme.FONT_SMALL);
        lblCount.setForeground(Theme.TEXT_MUTED);
        tableModel.addTableModelListener(ev ->
                lblCount.setText("Hiển thị " + tableModel.getRowCount() + " kết quả"));

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(8, 0, 0, 0));
        bottomBar.add(lblCount, BorderLayout.WEST);

        if (!readOnly) {
            JButton btnEdit = ComponentFactory.primaryButton("✏️  Sửa điểm");
            btnEdit.addActionListener(e -> openEditDialog(getSelectedResult()));
            JPanel ep = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            ep.setOpaque(false);
            ep.add(btnEdit);
            bottomBar.add(ep, BorderLayout.EAST);
        }

        JLabel sectionTitle = new JLabel("✎   Bảng kết quả học tập");
        sectionTitle.setFont(Theme.FONT_SECTION);
        sectionTitle.setForeground(Theme.TEXT_MAIN);

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        card.add(sectionTitle, BorderLayout.NORTH);
        card.add(scroll,       BorderLayout.CENTER);
        card.add(bottomBar,    BorderLayout.SOUTH);

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{resultService.findAll(), classService.findAll()};
            }
            @Override protected void done() {
                try {
                    Object[] data = get();
                    @SuppressWarnings("unchecked") List<Result> results = (List<Result>) data[0];
                    @SuppressWarnings("unchecked") List<Class>  classes = (List<Class>)  data[1];
                    allResults = results;
                    rebuildClassFilter(classes);
                    filterAndDisplay();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void rebuildClassFilter(List<Class> classes) {
        Object prevSel = cbClass.getSelectedItem();
        suppressFilter = true;
        cbClass.removeAllItems();
        cbClass.addItem("— Tất cả lớp —");
        classes.stream()
               .sorted(Comparator.comparing(Class::getClassName, String.CASE_INSENSITIVE_ORDER))
               .forEach(cbClass::addItem);
        if (prevSel instanceof Class prev) {
            for (int i = 1; i < cbClass.getItemCount(); i++) {
                if (cbClass.getItemAt(i) instanceof Class c
                        && c.getClassId().equals(prev.getClassId())) {
                    cbClass.setSelectedIndex(i); break;
                }
            }
        }
        suppressFilter = false;
    }

    private void filterAndDisplay() {
        String keyword  = tfSearch.getText().trim().toLowerCase();
        Object selClass = cbClass.getSelectedItem();

        filteredResults = allResults.stream()
            .filter(r -> {
                if (!(selClass instanceof Class c)) return true;
                return r.getClazz() != null && r.getClazz().getClassId().equals(c.getClassId());
            })
            .filter(r -> {
                if (keyword.isEmpty()) return true;
                String name = r.getStudent() != null ? r.getStudent().getFullName().toLowerCase() : "";
                String code = r.getStudent() != null && r.getStudent().getStudentId() != null
                        ? ("hv" + String.format("%04d", r.getStudent().getStudentId())) : "";
                return name.contains(keyword) || code.contains(keyword);
            })
            .sorted(Comparator.comparing(
                r -> r.getStudent() != null ? r.getStudent().getFullName() : "",
                String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

        updateKpis(filteredResults);
        updateTable(filteredResults);
    }

    private void updateKpis(List<Result> results) {
        long totalGraded = results.stream().filter(r -> r.getScore() != null).count();
        OptionalDouble avg = results.stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(r -> r.getScore().doubleValue()).average();
        long passCount = results.stream()
                .filter(r -> r.getScore() != null && r.getScore().doubleValue() >= 5.0).count();
        long failCount = results.stream()
                .filter(r -> r.getScore() != null && r.getScore().doubleValue() < 5.0).count();
        double passRate = totalGraded > 0 ? (passCount * 100.0 / totalGraded) : 0.0;

        DecimalFormat df = new DecimalFormat("0.##");
        lblTotal.setText(String.valueOf(totalGraded));
        lblAvg.setText(avg.isPresent() ? df.format(avg.getAsDouble()) : "—");
        lblPassRate.setText(df.format(passRate) + "%");
        lblFailCnt.setText(String.valueOf(failCount));
    }

    private void updateTable(List<Result> results) {
        tableModel.setRowCount(0);
        int stt = 1;
        for (Result r : results) {
            String code   = r.getStudent() != null ? "HV" + String.format("%04d", r.getStudent().getStudentId()) : "—";
            String name   = r.getStudent() != null ? r.getStudent().getFullName() : "—";
            String cls    = r.getClazz()   != null ? r.getClazz().getClassName()  : "—";
            String course = (r.getClazz() != null && r.getClazz().getCourse() != null)
                    ? r.getClazz().getCourse().getCourseName() : "—";
            String score  = r.getScore() != null ? r.getScore().stripTrailingZeros().toPlainString() : "—";
            String grade  = r.getGrade()   != null ? r.getGrade()   : "—";
            String comment = r.getComment() != null ? r.getComment() : "";
            tableModel.addRow(new Object[]{stt++, code, name, cls, course, score, grade, comment});
        }
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────

    private Result getSelectedResult() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredResults.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một học viên trong danh sách.",
                    "Chú ý", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return filteredResults.get(row);
    }

    private void openEditDialog(Result result) {
        if (result == null) return;
        String studentName = result.getStudent() != null ? result.getStudent().getFullName() : "—";
        String className   = result.getClazz()   != null ? result.getClazz().getClassName()  : "—";

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Sửa điểm — " + studentName, true);
        dlg.setSize(440, 340);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.BG_CARD);
        content.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        infoPanel.setOpaque(false);
        infoPanel.add(styledMutedLabel("Học viên:"));  infoPanel.add(boldLabel(studentName));
        infoPanel.add(styledMutedLabel("Lớp:"));        infoPanel.add(boldLabel(className));

        JTextField tfScore = new JTextField(
                result.getScore() != null ? result.getScore().toPlainString() : "");
        tfScore.setFont(Theme.FONT_PLAIN);

        JLabel lblComputedGrade = new JLabel(result.getGrade() != null ? result.getGrade() : "—");
        lblComputedGrade.setFont(Theme.FONT_BOLD);
        lblComputedGrade.setForeground(gradeColor(result.getScore()));

        tfScore.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                try {
                    double v = Double.parseDouble(tfScore.getText().trim().replace(",", "."));
                    if (v >= 0 && v <= 10) {
                        lblComputedGrade.setText(ResultService.autoGrade(v));
                        lblComputedGrade.setForeground(v >= 5.0 ? Theme.GREEN : Theme.RED);
                    } else {
                        lblComputedGrade.setText("Ngoài khoảng");
                        lblComputedGrade.setForeground(Theme.RED);
                    }
                } catch (NumberFormatException ex) {
                    lblComputedGrade.setText("—");
                    lblComputedGrade.setForeground(Theme.TEXT_MUTED);
                }
            }
            public void insertUpdate(DocumentEvent e)  { update(); }
            public void removeUpdate(DocumentEvent e)  { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        JTextArea taComment = new JTextArea(result.getComment() != null ? result.getComment() : "", 3, 20);
        taComment.setFont(Theme.FONT_PLAIN);
        taComment.setLineWrap(true);
        taComment.setWrapStyleWord(true);
        JScrollPane cmtScroll = new JScrollPane(taComment);
        cmtScroll.setPreferredSize(new Dimension(0, 64));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST; lc.insets = new Insets(4, 0, 4, 10);
        lc.gridx = 0; lc.weightx = 0;
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL; fc.insets = new Insets(4, 0, 4, 0);
        fc.gridx = 1; fc.weightx = 1;

        int row = 0;
        lc.gridy = fc.gridy = row++;
        form.add(styledMutedLabel("Điểm (0 – 10) *:"), lc); form.add(tfScore, fc);
        lc.gridy = fc.gridy = row++;
        form.add(styledMutedLabel("Xếp loại tự động:"), lc); form.add(lblComputedGrade, fc);
        lc.gridy = fc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST; fc.weighty = 1;
        form.add(styledMutedLabel("Nhận xét:"), lc); form.add(cmtScroll, fc);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JButton btnCancel = ComponentFactory.secondaryButton("Hủy");
        JButton btnSave   = ComponentFactory.primaryButton("Lưu điểm");

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            String raw = tfScore.getText().trim().replace(",", ".");
            double scoreVal;
            try { scoreVal = Double.parseDouble(raw); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Điểm phải là số thực trong khoảng 0 đến 10.",
                        "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
                tfScore.requestFocus(); return;
            }
            if (scoreVal < 0 || scoreVal > 10) {
                JOptionPane.showMessageDialog(dlg, "Điểm phải nằm trong khoảng 0 đến 10.",
                        "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
                tfScore.requestFocus(); return;
            }
            result.setScore(BigDecimal.valueOf(scoreVal).setScale(2, RoundingMode.HALF_UP));
            result.setGrade(ResultService.autoGrade(scoreVal));
            String cmt = taComment.getText().trim();
            result.setComment(cmt.isEmpty() ? null : cmt);
            try {
                resultService.update(result);
                dlg.dispose();
                loadData();
                if (onDataChanged != null) onDataChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi khi lưu: " + ex.getMessage(),
                        "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel); btnBar.add(btnSave);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(infoPanel, BorderLayout.NORTH);
        body.add(form,      BorderLayout.CENTER);

        content.add(body,   BorderLayout.CENTER);
        content.add(btnBar, BorderLayout.SOUTH);
        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(16, 20, 16, 20)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_KPI_LBL);
        lbl.setForeground(Theme.TEXT_MUTED);
        valueLabel.setFont(Theme.FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        JPanel inner = new JPanel(new GridLayout(2, 1, 0, 8));
        inner.setOpaque(false);
        inner.add(lbl); inner.add(valueLabel);
        card.add(inner, BorderLayout.CENTER);

        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(0, 3));
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel styledMutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        return lbl;
    }

    private static JLabel boldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_MAIN);
        return lbl;
    }

    private static Color gradeColor(BigDecimal score) {
        if (score == null) return Theme.TEXT_MUTED;
        return score.doubleValue() >= 5.0 ? Theme.GREEN : Theme.RED;
    }

    private static class ClassComboRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Class c) setText(c.getClassName());
            setFont(Theme.FONT_PLAIN);
            return this;
        }
    }
}

