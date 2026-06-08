// frontend/src/TrueToneApp.java
// Complete Java Swing frontend for TrueTone (EchoShield AI)
// Compile & run instructions at the bottom of this file.

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class TrueToneApp extends JFrame {

    // ── colours ──────────────────────────────────────────────────────────────
    static final Color BG_DARK    = new Color(10,  12,  20);
    static final Color BG_PANEL   = new Color(18,  22,  36);
    static final Color BG_CARD    = new Color(26,  32,  50);
    static final Color ACCENT     = new Color(82, 186, 255);
    static final Color GREEN      = new Color(52, 211, 153);
    static final Color RED        = new Color(248, 113, 113);
    static final Color TEXT_MAIN  = new Color(226, 232, 240);
    static final Color TEXT_MUTED = new Color(100, 116, 139);
    static final Color BORDER_COL = new Color(40,  50,  75);

    static final String API_BASE  = "http://localhost:5000";

    // ── state ─────────────────────────────────────────────────────────────────
    private File      selectedFile   = null;
    private String    lastPredId     = null;
    private double[]  waveformData   = null;
    private double[][]spectroData   = null;

    // ── UI components ─────────────────────────────────────────────────────────
    private JLabel    lblFileName, lblResult, lblConfidence, lblStatus;
    private JButton   btnSelect, btnAnalyze, btnWaveform, btnSpectro, btnHistory;
    private JProgressBar progressBar;
    private JPanel    resultCard, vizPanel;
    private JTable    historyTable;
    private DefaultTableModel tableModel;
    private CardLayout cardLayout;
    private JPanel    mainCards;

    // ─────────────────────────────────────────────────────────────────────────
    public TrueToneApp() {
        super("TrueTone — EchoShield AI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);
        setBackground(BG_DARK);
        buildUI();
        setVisible(true);
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildSidebar(),   BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainCards  = new JPanel(cardLayout);
        mainCards.setBackground(BG_DARK);
        mainCards.add(buildAnalyzePanel(), "ANALYZE");
        mainCards.add(buildHistoryPanel(), "HISTORY");
        root.add(mainCards, BorderLayout.CENTER);

        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_COL),
            new EmptyBorder(14, 24, 14, 24)
        ));

        JLabel title = new JLabel("🛡  TrueTone  |  EchoShield AI");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT);

        JLabel sub = new JLabel("Real vs AI-Generated Voice Detector");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(sub);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        p.setPreferredSize(new Dimension(180, 0));
        p.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER_COL),
            new EmptyBorder(20, 12, 20, 12)
        ));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JButton btnAnalyzeNav = sidebarBtn("🔬  Analyze Audio", "ANALYZE");
        JButton btnHistNav    = sidebarBtn("📋  History",       "HISTORY");
        p.add(btnAnalyzeNav);
        p.add(Box.createVerticalStrut(8));
        p.add(btnHistNav);
        p.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("v1.0.0");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ver.setForeground(TEXT_MUTED);
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(ver);
        return p;
    }

    private JButton sidebarBtn(String text, String card) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(TEXT_MAIN);
        b.setBackground(BG_CARD);
        b.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COL, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> cardLayout.show(mainCards, card));
        return b;
    }

    // ── Analyze Panel ─────────────────────────────────────────────────────────
    private JPanel buildAnalyzePanel() {
        JPanel outer = new JPanel(new BorderLayout(16, 16));
        outer.setBackground(BG_DARK);
        outer.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Upload card
        JPanel uploadCard = card();
        uploadCard.setLayout(new BoxLayout(uploadCard, BoxLayout.Y_AXIS));

        JLabel uploadTitle = cardTitle("Upload Audio File");
        lblFileName = new JLabel("No file selected");
        lblFileName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblFileName.setForeground(TEXT_MUTED);
        lblFileName.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSelect  = accentBtn("📂  Select Audio File");
        btnAnalyze = primaryBtn("⚡  Analyze Voice");
        btnAnalyze.setEnabled(false);

        btnSelect.addActionListener(e -> selectFile());
        btnAnalyze.addActionListener(e -> analyzeFile());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnSelect);
        btnRow.add(btnAnalyze);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setBackground(BG_DARK);
        progressBar.setForeground(ACCENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));

        uploadCard.add(uploadTitle);
        uploadCard.add(Box.createVerticalStrut(12));
        uploadCard.add(lblFileName);
        uploadCard.add(Box.createVerticalStrut(14));
        uploadCard.add(btnRow);
        uploadCard.add(Box.createVerticalStrut(14));
        uploadCard.add(progressBar);

        // Result card
        resultCard = card();
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
        resultCard.setVisible(false);

        lblResult = new JLabel("—");
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblResult.setForeground(TEXT_MAIN);
        lblResult.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblConfidence = new JLabel("Confidence: —");
        lblConfidence.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblConfidence.setForeground(TEXT_MUTED);
        lblConfidence.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel vizBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        vizBtnRow.setOpaque(false);
        btnWaveform = accentBtn("📈  Waveform");
        btnSpectro  = accentBtn("🎨  Spectrogram");
        btnWaveform.addActionListener(e -> showWaveform());
        btnSpectro.addActionListener(e -> showSpectrogram());
        vizBtnRow.add(btnWaveform);
        vizBtnRow.add(btnSpectro);

        resultCard.add(cardTitle("Prediction Result"));
        resultCard.add(Box.createVerticalStrut(12));
        resultCard.add(lblResult);
        resultCard.add(Box.createVerticalStrut(6));
        resultCard.add(lblConfidence);
        resultCard.add(Box.createVerticalStrut(14));
        resultCard.add(vizBtnRow);

        // Viz panel (waveform / spectrogram drawn here)
        vizPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawVisualization((Graphics2D) g);
            }
        };
        vizPanel.setBackground(BG_CARD);
        vizPanel.setPreferredSize(new Dimension(0, 200));
        vizPanel.setBorder(new LineBorder(BORDER_COL, 1, true));

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        top.add(uploadCard);
        top.add(resultCard);

        outer.add(top,      BorderLayout.NORTH);
        outer.add(vizPanel, BorderLayout.CENTER);
        return outer;
    }

    // ── History Panel ─────────────────────────────────────────────────────────
    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel title = new JLabel("Prediction History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_MAIN);

        btnHistory = accentBtn("🔄  Refresh");
        btnHistory.addActionListener(e -> loadHistory());
        topRow.add(title,      BorderLayout.WEST);
        topRow.add(btnHistory, BorderLayout.EAST);

        String[] cols = {"#", "Filename", "Result", "Confidence", "Timestamp"};
        tableModel  = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(tableModel);
        historyTable.setBackground(BG_CARD);
        historyTable.setForeground(TEXT_MAIN);
        historyTable.setGridColor(BORDER_COL);
        historyTable.setRowHeight(32);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.getTableHeader().setBackground(BG_PANEL);
        historyTable.getTableHeader().setForeground(ACCENT);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        historyTable.setSelectionBackground(new Color(40, 60, 100));
        historyTable.setDefaultRenderer(Object.class, new HistoryTableRenderer());

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(new LineBorder(BORDER_COL, 1, true));

        p.add(topRow, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_COL),
            new EmptyBorder(6, 16, 6, 16)
        ));
        lblStatus = new JLabel("Ready. Select an audio file to begin.");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_MUTED);
        p.add(lblStatus, BorderLayout.WEST);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void selectFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
            "Audio Files (wav, mp3, flac, ogg, m4a)", "wav","mp3","flac","ogg","m4a"));
        fc.setDialogTitle("Select Audio File");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            lblFileName.setText("📎  " + selectedFile.getName());
            lblFileName.setForeground(TEXT_MAIN);
            btnAnalyze.setEnabled(true);
            waveformData = null;
            spectroData  = null;
            vizPanel.repaint();
            setStatus("File selected: " + selectedFile.getName());
        }
    }

    private void analyzeFile() {
        if (selectedFile == null) return;
        btnAnalyze.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        setStatus("Uploading and analyzing…");

        SwingWorker<JSONObject, Void> worker = new SwingWorker<>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return uploadAndPredict(selectedFile);
            }
            @Override protected void done() {
                progressBar.setVisible(false);
                btnAnalyze.setEnabled(true);
                try {
                    JSONObject resp = get();
                    if (resp.has("error")) {
                        setStatus("Error: " + resp.getString("error"));
                        return;
                    }
                    String result     = resp.getString("result");
                    double confidence = resp.getDouble("confidence");
                    lastPredId        = resp.getString("prediction_id");

                    lblResult.setText(result);
                    lblResult.setForeground("REAL".equals(result) ? GREEN : RED);
                    lblConfidence.setText(String.format("Confidence: %.1f%%", confidence));
                    resultCard.setVisible(true);
                    setStatus("Analysis complete — " + result + " (" + String.format("%.1f%%", confidence) + ")");

                    // auto-fetch waveform
                    fetchWaveform(lastPredId);
                } catch (Exception ex) {
                    setStatus("Failed: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showWaveform() {
        if (waveformData != null) { vizPanel.repaint(); return; }
        if (lastPredId == null) return;
        fetchWaveform(lastPredId);
    }

    private void showSpectrogram() {
        if (spectroData != null) { vizPanel.repaint(); return; }
        if (lastPredId == null) return;
        SwingWorker<double[][], Void> w = new SwingWorker<>() {
            @Override protected double[][] doInBackground() throws Exception {
                String url  = API_BASE + "/spectrogram/" + lastPredId;
                String body = httpGet(url);
                JSONObject j = new JSONObject(body);
                JSONArray rows = j.getJSONArray("data");
                double[][] mat = new double[rows.length()][];
                for (int i = 0; i < rows.length(); i++) {
                    JSONArray row = rows.getJSONArray(i);
                    mat[i] = new double[row.length()];
                    for (int k = 0; k < row.length(); k++) mat[i][k] = row.getDouble(k);
                }
                return mat;
            }
            @Override protected void done() {
                try { spectroData = get(); vizPanel.repaint(); }
                catch (Exception ex) { setStatus("Spectrogram error: " + ex.getMessage()); }
            }
        };
        w.execute();
    }

    private void fetchWaveform(String predId) {
        SwingWorker<double[], Void> w = new SwingWorker<>() {
            @Override protected double[] doInBackground() throws Exception {
                String url  = API_BASE + "/waveform/" + predId;
                String body = httpGet(url);
                JSONObject j = new JSONObject(body);
                JSONArray arr = j.getJSONArray("waveform");
                double[] d = new double[arr.length()];
                for (int i = 0; i < arr.length(); i++) d[i] = arr.getDouble(i);
                return d;
            }
            @Override protected void done() {
                try { waveformData = get(); spectroData = null; vizPanel.repaint(); }
                catch (Exception ex) { /* silent */ }
            }
        };
        w.execute();
    }

    private void loadHistory() {
        setStatus("Loading history…");
        SwingWorker<JSONArray, Void> w = new SwingWorker<>() {
            @Override protected JSONArray doInBackground() throws Exception {
                return new JSONArray(httpGet(API_BASE + "/history"));
            }
            @Override protected void done() {
                try {
                    JSONArray arr = get();
                    tableModel.setRowCount(0);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject row = arr.getJSONObject(i);
                        tableModel.addRow(new Object[]{
                            row.getInt("id"),
                            row.getString("filename"),
                            row.getString("result"),
                            String.format("%.1f%%", row.getDouble("confidence") * 100),
                            row.getString("created_at")
                        });
                    }
                    setStatus("History loaded — " + arr.length() + " records.");
                } catch (Exception ex) { setStatus("History error: " + ex.getMessage()); }
            }
        };
        w.execute();
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    private void drawVisualization(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = vizPanel.getWidth(), h = vizPanel.getHeight();

        g.setColor(BG_CARD);
        g.fillRect(0, 0, w, h);

        if (spectroData != null) {
            drawSpectrogram(g, w, h);
        } else if (waveformData != null) {
            drawWaveform(g, w, h);
        } else {
            g.setColor(TEXT_MUTED);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String msg = "Waveform & spectrogram will appear here after analysis";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
        }
    }

    private void drawWaveform(Graphics2D g, int w, int h) {
        // Label
        g.setColor(ACCENT);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.drawString("WAVEFORM", 12, 18);

        // Axis
        g.setColor(BORDER_COL);
        g.drawLine(0, h / 2, w, h / 2);

        int n    = waveformData.length;
        double xStep = (double) w / n;
        float  mid   = h / 2f;

        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(1.2f));

        Path2D path = new Path2D.Float();
        path.moveTo(0, mid);
        for (int i = 0; i < n; i++) {
            float x = (float)(i * xStep);
            float y = (float)(mid - waveformData[i] * (h * 0.42));
            if (i == 0) path.moveTo(x, y);
            else        path.lineTo(x, y);
        }
        g.draw(path);
    }

    private void drawSpectrogram(Graphics2D g, int w, int h) {
        g.setColor(ACCENT);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.drawString("MEL SPECTROGRAM", 12, 18);

        int rows = spectroData.length;
        int cols = spectroData[0].length;
        double minDb = -80, maxDb = 0;

        double cellW = (double) w / cols;
        double cellH = (double) h / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double val = (spectroData[r][c] - minDb) / (maxDb - minDb);
                val = Math.max(0, Math.min(1, val));
                g.setColor(spectroColor(val));
                int px = (int)(c * cellW);
                int py = h - (int)((r + 1) * cellH);
                g.fillRect(px, py, (int)Math.ceil(cellW), (int)Math.ceil(cellH));
            }
        }
    }

    /** Blue → Cyan → Green → Yellow → Red colour map for spectrogram. */
    private Color spectroColor(double v) {
        float[] r = {0,0,0,1,1};
        float[] gr= {0,1,1,1,0};
        float[] b = {1,1,0,0,0};
        double scaled = v * 4;
        int i  = (int) scaled;
        double t  = scaled - i;
        i = Math.min(i, 3);
        return new Color(
            (float)(r[i] + t * (r[i+1] - r[i])),
            (float)(gr[i]+ t * (gr[i+1]- gr[i])),
            (float)(b[i] + t * (b[i+1] - b[i]))
        );
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────
    private JSONObject uploadAndPredict(File file) throws Exception {
        String boundary = "----TrueTone" + System.currentTimeMillis();
        URL url = new URL(API_BASE + "/predict");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            pw.append("--").append(boundary).append("\r\n");
            pw.append("Content-Disposition: form-data; name=\"audio\"; filename=\"")
              .append(file.getName()).append("\"").append("\r\n");
            pw.append("Content-Type: application/octet-stream").append("\r\n\r\n");
            pw.flush();
            Files.copy(file.toPath(), os);
            os.flush();
            pw.append("\r\n--").append(boundary).append("--\r\n");
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        String body = new String(is.readAllBytes());
        return new JSONObject(body);
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        return new String(conn.getInputStream().readAllBytes());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(msg));
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COL, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        return p;
    }

    private JLabel cardTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(ACCENT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton accentBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(ACCENT);
        b.setBackground(new Color(20, 30, 55));
        b.setBorder(new CompoundBorder(
            new LineBorder(ACCENT, 1, true),
            new EmptyBorder(7, 14, 7, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(37, 99, 200));
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Table renderer ────────────────────────────────────────────────────────
    static class HistoryTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            setBackground(sel ? new Color(40, 60, 100) : (row % 2 == 0 ? BG_CARD : new Color(22,28,44)));
            setForeground(TEXT_MAIN);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            if (col == 2 && value != null) {
                setForeground("REAL".equals(value.toString()) ? GREEN : RED);
                setFont(getFont().deriveFont(Font.BOLD));
            }
            return this;
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(TrueToneApp::new);
    }
}

/*
 HOW TO COMPILE & RUN (from project root):
 ------------------------------------------
 1. Download org.json JAR:
    cd frontend
    curl -L -o json.jar https://search.maven.org/remotecontent?filepath=org/json/json/20240303/json-20240303.jar

 2. Compile:
    cd frontend/src
    javac -cp .:../json.jar TrueToneApp.java

 3. Run:
    java -cp .:../json.jar TrueToneApp
*/
