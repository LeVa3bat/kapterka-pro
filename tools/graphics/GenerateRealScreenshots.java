import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateRealScreenshots {
    static int W = 1080;
    static int H = 2400; // Exact aspect ratio matching real phone

    static Color BG = new Color(230, 235, 230);
    static Color SURFACE = new Color(241, 246, 241);
    static Color CARD_BG = new Color(245, 248, 245);
    static Color TEXT_DARK = new Color(17, 34, 27);
    static Color TEXT_MUTED = new Color(74, 90, 80);
    static Color BRAND_GREEN = new Color(14, 118, 77);
    static Color BRAND_GREEN_LIGHT = new Color(220, 240, 228);
    static Color ACCENT_GOLD = new Color(180, 130, 20);
    static Color ACCENT_RED_BG = new Color(254, 238, 238);
    static Color ACCENT_RED_TEXT = new Color(185, 28, 28);
    static Color BORDER = new Color(205, 220, 210);

    public static void main(String[] args) throws Exception {
        createSplash();
        createMainDashboard();
        createJournal();
        createRequests();
        createCatalog();
        createExcel();
        createSettings();
        System.out.println("ALL 7 REAL SCREENSHOTS GENERATED!");
    }

    static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // 1. SPLASH SCREEN (Screenshot_20260903_150452)
    static void createSplash() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(new Color(232, 238, 232));
        g.fillRect(0, 0, W, H);

        // Top bar
        g.setColor(BRAND_GREEN);
        g.fillOval(70, 100, 16, 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("АСУ «КАПТЁРКА» PRO", 105, 115);

        // Timer badge
        g.setColor(new Color(200, 225, 210));
        g.fillRoundRect(820, 80, 190, 65, 20, 20);
        g.setColor(ACCENT_GOLD);
        g.setFont(new Font("Monospaced", Font.BOLD, 30));
        g.drawString("00:03.85", 845, 125);

        // Center Radar
        int cx = W / 2;
        int cy = 820;
        int rad = 300;
        g.setColor(new Color(17, 45, 34));
        g.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);

        g.setColor(new Color(30, 80, 58));
        g.setStroke(new BasicStroke(4f));
        g.drawOval(cx - rad + 50, cy - rad + 50, (rad - 50) * 2, (rad - 50) * 2);
        g.drawOval(cx - rad + 140, cy - rad + 140, (rad - 140) * 2, (rad - 140) * 2);
        g.drawLine(cx - rad, cy, cx + rad, cy);
        g.drawLine(cx, cy - rad, cx, cy + rad);

        // Radar beam
        g.setColor(new Color(50, 140, 95, 120));
        g.fillArc(cx - rad, cy - rad, rad * 2, rad * 2, 270, 90);

        // Radar points
        g.setColor(new Color(245, 190, 80));
        g.fillOval(cx + 120, cy - 130, 24, 24);
        g.setColor(new Color(210, 240, 220));
        g.fillOval(cx - 150, cy + 80, 20, 20);
        g.setColor(new Color(220, 100, 90));
        g.fillOval(cx + 60, cy + 160, 20, 20);

        // Center shield
        g.setColor(new Color(28, 90, 65));
        g.fillOval(cx - 48, cy - 48, 96, 96);
        g.setColor(new Color(14, 50, 36));
        g.fillOval(cx - 30, cy - 30, 60, 60);

        // Title
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 74));
        String title = "КАПТЁРКАПРО";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, cx - fm.stringWidth(title)/2, 1220);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 32));
        String sub = "Автоматизированный воинский учет и снабжение";
        fm = g.getFontMetrics();
        g.drawString(sub, cx - fm.stringWidth(sub)/2, 1280);

        // Audio bars / equalizer
        int barY = 1340;
        int[] barH = {14,24,36,44,52,58,64,62,54,42,32,20,16,14,18,28,40,54,62,64,56,44,32,20,16};
        int barStart = cx - (barH.length * 28)/2;
        for (int i = 0; i < barH.length; i++) {
            g.setColor(i % 5 == 0 ? new Color(230, 180, 70) : new Color(130, 190, 155));
            g.fillRoundRect(barStart + i * 28, barY - barH[i]/2, 14, barH[i], 6, 6);
        }

        // Crypto verified
        g.setColor(new Color(14, 80, 56));
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.drawString("SEC_CRYPTO_256 // VERIFIED", 100, 1470);
        g.drawString("35%", 920, 1470);

        // Progress bar
        g.setColor(new Color(205, 220, 210));
        g.fillRoundRect(100, 1495, 880, 16, 8, 8);
        g.setColor(BRAND_GREEN);
        g.fillRoundRect(100, 1495, 330, 16, 8, 8);
        g.fillOval(970, 1497, 12, 12);

        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        String prText = "ПРОВЕРКА КРИПТОКЛЮЧА И БАЗЫ ДАННЫХ ROOM...";
        fm = g.getFontMetrics();
        g.drawString(prText, cx - fm.stringWidth(prText)/2, 1565);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("Monospaced", Font.PLAIN, 22));
        String gridText = "GRID: 48.019° N  37.802° E • AES-256 ROOM DB";
        fm = g.getFontMetrics();
        g.drawString(gridText, cx - fm.stringWidth(gridText)/2, 1615);

        // Skip button
        g.setColor(BRAND_GREEN);
        g.fillRoundRect(240, 1670, 600, 95, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String skip = "Пропустить (6 сек)";
        fm = g.getFontMetrics();
        g.drawString(skip, cx - fm.stringWidth(skip)/2, 1730);

        // Developer footer
        g.setColor(new Color(14, 100, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String dev = "Разработчик: Васев Алексей Евгеньевич";
        fm = g.getFontMetrics();
        g.drawString(dev, cx - fm.stringWidth(dev)/2, 2230);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("Monospaced", Font.PLAIN, 24));
        String ver = "Версия программы: v3.0.0 PRO (Tactical Edition)";
        fm = g.getFontMetrics();
        g.drawString(ver, cx - fm.stringWidth(ver)/2, 2280);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_1.jpg"));
    }

    // 2. MAIN DASHBOARD (Screenshot_20260903_150501)
    static void createMainDashboard() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        // Top badges
        drawPill(g, 50, 40, 420, 70, "● лева • 1-е Подразделение", new Color(210, 230, 215), TEXT_DARK);
        drawPill(g, 490, 40, 260, 70, "❔ ИНСТРУКЦИЯ", new Color(210, 235, 220), BRAND_GREEN);
        drawPill(g, 770, 40, 260, 70, "ТАРИФ PRO", new Color(210, 235, 220), BRAND_GREEN);

        // Title row
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        g.drawString("КАПТЁРКАПРО", 50, 175);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("СКЛАД • УЧЕТ • СНАБЖЕНИЕ", 50, 220);

        // Secret code badge
        drawPill(g, 710, 125, 320, 75, "● kapt_59e13b", new Color(215, 240, 225), BRAND_GREEN);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("КОД ПОДРАЗДЕЛЕНИЯ / СКЛАДА", 650, 230);

        // Sync & Report wide buttons
        drawWideButton(g, 50, 270, 470, 90, "🔄 Синхронизация", new Color(220, 235, 225), TEXT_DARK);
        drawWideButton(g, 550, 270, 470, 90, "📥 Отчеты Excel", new Color(220, 235, 225), TEXT_DARK);

        // 4 Action cards: Приход, Перенос, Выдача, Расход
        drawActionCard(g, 40, 390, 230, 210, "🚚", "Приход", "Поступление", false);
        drawActionCard(g, 290, 390, 230, 210, "➤", "Перенос", "Локации", false);
        drawActionCard(g, 540, 390, 230, 210, "🪖", "Выдача", "В руки / Бойцу", false);
        drawActionCard(g, 790, 390, 230, 210, "↗", "Расход", "Акт ф.8", true);

        // Search bar
        drawSearchBar(g, 40, 630, 980, 110, "Введите название");

        // Filter chips
        drawPill(g, 40, 770, 180, 80, "Все виды", BRAND_GREEN, Color.WHITE);
        drawPill(g, 240, 770, 220, 80, "Служба РАВ", Color.WHITE, TEXT_DARK);
        drawPill(g, 480, 770, 450, 80, "Служба БПЛА и робототехники", Color.WHITE, TEXT_DARK);

        // Warehouse section header
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("🏠 ТОЧКИ И СКЛАДЫ УЧЕТА", 50, 900);
        drawPill(g, 740, 850, 280, 70, "+ Склад / Точка", new Color(210, 240, 225), BRAND_GREEN);

        // Warehouse tabs
        drawPill(g, 40, 940, 250, 70, "Все склады (3)", BRAND_GREEN_LIGHT, BRAND_GREEN);
        drawPill(g, 310, 940, 390, 70, "Базовый склад ★ (15) ✏️", Color.WHITE, TEXT_DARK);
        drawPill(g, 720, 940, 290, 70, "ОП «Скала» (0) ✏️", Color.WHITE, TEXT_DARK);

        // Warehouse Cards
        drawWarehouseItem(g, 40, 1030, 980, 140, "Базовый склад ★ Базовый", "15 наим. • остаток: 529 ед.", "529 ед.", new Color(254, 243, 199), new Color(180, 120, 20));
        drawWarehouseItem(g, 40, 1200, 980, 140, "ОП «Скала»", "0 наим. • остаток: 0 ед.", "0 ед.", new Color(215, 245, 230), BRAND_GREEN);
        drawWarehouseItem(g, 40, 1370, 980, 140, "ОП «Заря»", "0 наим. • остаток: 0 ед.", "0 ед.", new Color(215, 245, 230), BRAND_GREEN);

        // Bottom total summary
        g.setColor(CARD_BG);
        g.fillRoundRect(40, 1550, 980, 85, 24, 24);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(40, 1550, 980, 85, 24, 24);
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("ВСЕГО ПО ПОДРАЗДЕЛЕНИЮ: 15 ПОЗ.", 70, 1605);
        g.setColor(BRAND_GREEN);
        g.drawString("ОСТАТОК:  529  ЕД.", 660, 1605);

        // Bottom Nav Bar
        drawBottomNavigation(g, 0);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_2.jpg"));
    }

    // 3. JOURNAL (Screenshot_20260903_150506)
    static void createJournal() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("ЖУРНАЛ ОПЕРАЦИЙ", 40, 80);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g.drawString("История приходов, перемещений, выдач и расхода (ф.8)", 40, 130);

        drawSearchBar(g, 40, 180, 980, 110, "Введите название");

        // Filter chips
        drawPill(g, 40, 320, 310, 80, "Все операции (0)", BRAND_GREEN, Color.WHITE);
        drawPill(g, 370, 320, 240, 80, "Расход (ф. 8)", new Color(230, 235, 230), TEXT_DARK);
        drawPill(g, 630, 320, 200, 80, "Привезли", new Color(230, 235, 230), TEXT_DARK);
        drawPill(g, 850, 320, 190, 80, "Перемещение", new Color(230, 235, 230), TEXT_DARK);

        // Center Empty Icon
        g.setColor(new Color(140, 165, 150));
        g.setFont(new Font("SansSerif", Font.PLAIN, 120));
        g.drawString("⏱", W/2 - 60, 680);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        String empty = "Операций по заданным критериям не найдено";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(empty, W/2 - fm.stringWidth(empty)/2, 760);

        drawBottomNavigation(g, 1);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_3.jpg"));
    }

    // 4. REQUESTS (Screenshot_20260903_150509)
    static void createRequests() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("ЗАЯВКИ НА СНАБЖЕНИЕ", 40, 75);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.drawString("Новая заявка поступает на склад. При сборке переводится в", 40, 120);
        g.drawString("статус «Собрана», затем «Выдана».", 40, 155);

        // Container card
        int cardY = 190;
        int cardH = 1550;
        g.setColor(CARD_BG);
        g.fillRoundRect(40, cardY, 980, cardH, 36, 36);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(40, cardY, 980, cardH, 36, 36);

        // Header inside card
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString("📋  ФОРМИРОВАНИЕ ЗАЯВКИ", 80, 260);

        // Field 1: Куда доставить
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Куда доставить / Назначение", 80, 330);
        drawDropdown(g, 80, 360, 900, 110, "Базовый склад");

        // Stock preview card inside
        g.setColor(new Color(230, 242, 235));
        g.fillRoundRect(80, 500, 900, 200, 24, 24);
        g.setColor(new Color(180, 215, 195));
        g.drawRoundRect(80, 500, 900, 200, 24, 24);
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("📦 Наличие на «Базовый склад» (15 поз., всего: 529 ед.):", 105, 545);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.drawString("DJI Mavic 3 Enterprise Pro: 2 компл. • DJI Mavic 3T (Тепловизор): 1", 105, 590);
        g.drawString("компл. • FPV-дрон 7\" Ударный «Камикадзе»: 10 шт. • Аккумулятор", 105, 630);
        g.drawString("LiPo 6S 4500mAh 100C: 20 шт. • Бензин автомобильный АИ-92: 200", 105, 670);

        // Field 2: Позывной заявителя
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Позывной заявителя", 80, 750);
        drawDropdown(g, 80, 780, 900, 110, "лева");

        // Item row container
        g.drawString("ПОЗИЦИИ ЗАЯВКИ", 80, 950);
        drawPill(g, 720, 905, 260, 65, "+  позиция", BRAND_GREEN_LIGHT, BRAND_GREEN);

        g.setColor(new Color(236, 242, 238));
        g.fillRoundRect(80, 990, 900, 360, 24, 24);
        g.setColor(BORDER);
        g.drawRoundRect(80, 990, 900, 360, 24, 24);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Строка #1", 110, 1035);
        drawDropdown(g, 105, 1060, 850, 100, "Введите название");
        drawQtyInput(g, 105, 1190, 850, 110, "1", "ед.");

        // Comment
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Комментарий (срочность, координаты, примечание)", 80, 1400);
        g.setColor(new Color(230, 238, 232));
        g.fillRoundRect(80, 1430, 900, 110, 20, 20);
        g.setColor(BORDER);
        g.drawRoundRect(80, 1430, 900, 110, 20, 20);
        g.setColor(TEXT_MUTED);
        g.drawString("Срочно для 2-го расчета / на вечерний рейс", 120, 1495);

        // Send Button
        g.setColor(BRAND_GREEN);
        g.fillRoundRect(80, 1590, 900, 100, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String send = "➤  ОТПРАВИТЬ ЗАЯВКУ НА СКЛАД";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(send, W/2 - fm.stringWidth(send)/2, 1655);

        // Bottom label
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("СПИСОК ЗАЯВОК (0)", 40, 1800);

        drawBottomNavigation(g, 2);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_4.jpg"));
    }

    // 5. CATALOG (Screenshot_20260903_150513)
    static void createCatalog() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("НОМЕНКЛАТУРА ИМУЩЕСТВА", 40, 75);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.drawString("Справочник и категории (75 позиций)", 40, 120);

        drawPill(g, 730, 35, 290, 80, "+  Позиция", BRAND_GREEN_LIGHT, BRAND_GREEN);

        drawSearchBar(g, 40, 170, 980, 110, "Введите название");

        drawPill(g, 40, 310, 270, 80, "Все службы (75)", BRAND_GREEN, Color.WHITE);
        drawPill(g, 330, 310, 270, 80, "Служба РАВ (32)", Color.WHITE, TEXT_DARK);
        drawPill(g, 620, 310, 420, 80, "Служба БПЛА и робототехн...", Color.WHITE, TEXT_DARK);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("СПИСКИ ПО СЛУЖБАМ ОБЕСПЕЧЕНИЯ", 40, 440);
        drawPill(g, 780, 405, 240, 60, "↕ Развернуть", Color.WHITE, TEXT_DARK);

        String[] services = {
            "Автомобильная и БТ служба (2)",
            "Вещевая служба и СИБЗ (5)",
            "Инженерная служба (6)",
            "Медицинская служба (5)",
            "Продовольственная служба (4)",
            "Служба БПЛА и робототехники (8)",
            "Служба ГСМ (4)",
            "Служба РАВ (32)",
            "Служба РХБЗ (2)",
            "Служба связи и РЭБ (5)",
            "Топографическая и штабная (2)"
        };

        int sy = 480;
        for (String s : services) {
            drawAccordion(g, 40, sy, 980, 90, s);
            sy += 110;
        }

        drawBottomNavigation(g, 3);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_5.jpg"));
    }

    // 6. EXCEL REPORT MODAL (Screenshot_20260903_150534)
    static void createExcel() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        // Dimmed background
        g.setColor(new Color(20, 30, 25, 210));
        g.fillRect(0, 0, W, H);

        // White Dialog Card
        int diaY = 240;
        int diaH = 1750;
        g.setColor(Color.WHITE);
        g.fillRoundRect(30, diaY, 1020, diaH, 40, 40);

        // Header
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.drawString("ГЕНЕРАТОР ВЕДОМОСТЕЙ EXCEL", 70, diaY + 80);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("Строгий белый бланк • Черный шрифт • Печатная форма", 70, diaY + 125);

        // Close X
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString("✕", 970, diaY + 85);

        // Tabs
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Полная ведомость (Все позиции)", 80, diaY + 230);
        g.fillRect(50, diaY + 260, 600, 6);

        g.setColor(TEXT_MUTED);
        g.drawString("Точка: Базовый склад", 680, diaY + 230);

        // Subtitle inside table
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("ПОЛНАЯ СВОДНАЯ ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА", 60, diaY + 330);

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Подразделение: 1-е Подразделение", 60, diaY + 380);

        // Excel Table Mockup
        int ty = diaY + 410;
        g.setColor(new Color(240, 240, 240));
        g.fillRect(50, ty, 980, 70);
        g.setColor(Color.BLACK);
        g.drawRect(50, ty, 980, 850);

        g.setFont(new Font("SansSerif", Font.BOLD, 19));
        g.drawString("№", 65, ty + 40);
        g.drawString("Наименование", 150, ty + 40);
        g.drawString("Категория", 280, ty + 40);
        g.drawString("Ед.", 390, ty + 40);
        g.drawString("Приход", 450, ty + 40);
        g.drawString("Расход", 550, ty + 40);
        g.drawString("Остаток на точках", 660, ty + 40);
        g.drawString("Всего", 860, ty + 40);

        String[][] rows = {
            {"1", "DJI Mavic 3 Enterprise Pro", "БПЛА", "компл.", "2", "0", "Базовый склад: 2", "2"},
            {"2", "DJI Mavic 3T (Тепловизор)", "БПЛА", "компл.", "1", "0", "Базовый склад: 1", "1"},
            {"3", "FPV-дрон 7\" Ударный", "БПЛА", "шт.", "10", "0", "Базовый склад: 10", "10"},
            {"4", "Аккумулятор LiPo 6S", "БПЛА", "шт.", "20", "0", "Базовый склад: 20", "20"},
            {"5", "Бензин автомобильный АИ-92", "ГСМ", "л.", "200", "0", "Базовый склад: 200", "200"},
            {"6", "Жгут-турникет кровоостан.", "Мед.", "шт.", "25", "0", "Базовый склад: 25", "25"},
            {"7", "Гемостатический z-бинт", "Мед.", "шт.", "20", "0", "Базовый склад: 20", "20"},
            {"8", "Индивидуальный рацион", "Прод.", "шт.", "50", "0", "Базовый склад: 50", "50"},
            {"9", "Мина 120-мм ОФ-843Б", "РАВ", "шт.", "48", "0", "Базовый склад: 48", "48"},
            {"10", "Мина 82-мм О-832ДУ", "РАВ", "шт.", "64", "0", "Базовый склад: 64", "64"}
        };

        int rY = ty + 70;
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        for (String[] r : rows) {
            g.setColor(new Color(220, 220, 220));
            g.drawLine(50, rY, 1030, rY);
            g.setColor(Color.BLACK);
            g.drawString(r[0], 65, rY + 45);
            g.drawString(r[1], 120, rY + 45);
            g.drawString(r[2], 280, rY + 45);
            g.drawString(r[3], 390, rY + 45);
            g.drawString(r[4], 460, rY + 45);
            g.drawString(r[5], 560, rY + 45);
            g.drawString(r[6], 660, rY + 45);
            g.drawString(r[7], 880, rY + 45);
            rY += 75;
        }

        // Export button
        g.setColor(BRAND_GREEN);
        g.fillRoundRect(80, diaY + 1550, 920, 110, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String exp = "СКАЧАТЬ / ПОДЕЛИТЬСЯ EXCEL (.TSV)";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(exp, W/2 - fm.stringWidth(exp)/2, diaY + 1620);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_6.jpg"));
    }

    // 7. SETTINGS & LICENSING (Screenshot_20260903_150517)
    static void createSettings() throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setup(g);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("НАСТРОЙКИ И ОТЧЕТНОСТЬ", 40, 75);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.drawString("Компактное меню управления подразделением и группами", 40, 120);

        int sy = 160;
        drawSettingsTile(g, 40, sy, 980, 140, "☁️", "Облачная база Google Firebase", "Канал: kapt_59e13b • Онлайн синхронизация");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "🔑", "Код подключения бойцов (без QR)", "Секретный код: kapt_59e13b");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "★", "Лицензия бойца (ЮKassa / 30 дней)", "Активна (Осталось 30 дн.) • Персональный ключ");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "📊", "Армейская отчетность и ведомости", "Форма № 8, Форма № 18, Сводные отчеты в Excel");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "👥", "Управление штатными группами", "Активно служб: 12 • Удаление ненужных");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "❔", "Инструкция и руководство пользователя", "Подробное иллюстрированное руководство по всем");
        sy += 165;
        drawSettingsTile(g, 40, sy, 980, 140, "🗑️", "Опасная зона", "Сброс всех операций и остатков базы");

        // Action buttons at bottom
        sy += 175;
        drawWideButton(g, 40, sy, 980, 95, "Сменить позывной / Выйти из подразделения", new Color(225, 235, 228), TEXT_DARK);
        sy += 115;
        drawWideButton(g, 40, sy, 980, 95, "🔄 Выйти и сбросить лицензию (для проверки)", new Color(225, 235, 228), TEXT_DARK);

        // Footer
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String dev = "Разработчик: Васев Алексей Евгеньевич";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(dev, W/2 - fm.stringWidth(dev)/2, 2240);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("Monospaced", Font.PLAIN, 24));
        String ver = "Версия программы: v3.0.0 PRO (Tactical Edition)";
        fm = g.getFontMetrics();
        g.drawString(ver, W/2 - fm.stringWidth(ver)/2, 2290);

        drawBottomNavigation(g, 4);

        g.dispose();
        ImageIO.write(img, "jpg", new File("docs/real_screen_7.jpg"));
    }

    // Helper UI methods
    static void drawPill(Graphics2D g, int x, int y, int w, int h, String text, Color bgCol, Color textCol) {
        g.setColor(bgCol);
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, h, h);
        g.setColor(textCol);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text))/2, y + (h + fm.getAscent() - 8)/2);
    }

    static void drawWideButton(Graphics2D g, int x, int y, int w, int h, String text, Color bgCol, Color textCol) {
        g.setColor(bgCol);
        g.fillRoundRect(x, y, w, h, 28, 28);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 28, 28);
        g.setColor(textCol);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text))/2, y + (h + fm.getAscent() - 8)/2);
    }

    static void drawActionCard(Graphics2D g, int x, int y, int w, int h, String icon, String title, String sub, boolean isRed) {
        g.setColor(isRed ? ACCENT_RED_BG : CARD_BG);
        g.fillRoundRect(x, y, w, h, 28, 28);
        g.setColor(isRed ? new Color(252, 165, 165) : BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 28, 28);

        // Icon circle
        g.setColor(isRed ? new Color(254, 202, 202) : new Color(220, 240, 228));
        g.fillOval(x + w/2 - 35, y + 25, 70, 70);
        g.setColor(isRed ? ACCENT_RED_TEXT : BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.PLAIN, 34));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(icon, x + w/2 - fm.stringWidth(icon)/2, y + 72);

        g.setColor(isRed ? ACCENT_RED_TEXT : TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        fm = g.getFontMetrics();
        g.drawString(title, x + w/2 - fm.stringWidth(title)/2, y + 145);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        fm = g.getFontMetrics();
        g.drawString(sub, x + w/2 - fm.stringWidth(sub)/2, y + 185);
    }

    static void drawSearchBar(Graphics2D g, int x, int y, int w, int h, String ph) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 24, 24);
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.PLAIN, 36));
        g.drawString("🔍", x + 35, y + 70);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g.drawString(ph, x + 110, y + 70);
    }

    static void drawWarehouseItem(Graphics2D g, int x, int y, int w, int h, String name, String sub, String qty, Color tagBg, Color tagText) {
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, w, h, 28, 28);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 28, 28);

        // House icon
        g.setColor(new Color(254, 243, 199));
        g.fillRoundRect(x + 25, y + 25, 90, 90, 20, 20);
        g.setColor(new Color(180, 120, 20));
        g.setFont(new Font("SansSerif", Font.PLAIN, 44));
        g.drawString("🏠", x + 45, y + 85);

        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(name, x + 140, y + 60);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(sub, x + 140, y + 105);

        // Qty pill
        g.setColor(tagBg);
        g.fillRoundRect(x + w - 260, y + 35, 170, 70, 20, 20);
        g.setColor(BORDER);
        g.drawRoundRect(x + w - 260, y + 35, 170, 70, 20, 20);
        g.setColor(tagText);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(qty, x + w - 245, y + 80);

        // Arrow
        g.setColor(BRAND_GREEN);
        g.drawString("⌵", x + w - 60, y + 80);
    }

    static void drawDropdown(Graphics2D g, int x, int y, int w, int h, String val) {
        g.setColor(new Color(225, 235, 228));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 20, 20);
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(val, x + 100, y + 65);
        g.setColor(BRAND_GREEN);
        g.drawString("🔍", x + 35, y + 65);
        g.drawString("▼", x + w - 60, y + 65);
    }

    static void drawQtyInput(Graphics2D g, int x, int y, int w, int h, String qty, String unit) {
        g.setColor(new Color(225, 235, 228));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 20, 20);
        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("Кол-во", x + 35, y + 30);
        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(qty, x + 35, y + 85);
        g.setColor(BRAND_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(unit, x + w - 80, y + 75);
    }

    static void drawAccordion(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setColor(BRAND_GREEN);
        g.fillRect(x + 30, y + 35, 18, 18);

        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(title, x + 70, y + 55);

        g.setColor(BRAND_GREEN);
        g.drawString("⌵", x + w - 50, y + 55);
    }

    static void drawSettingsTile(Graphics2D g, int x, int y, int w, int h, String icon, String title, String sub) {
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setColor(new Color(22, 50, 36));
        g.fillRoundRect(x + 25, y + 25, 90, 90, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(icon, x + 25 + (90 - fm.stringWidth(icon))/2, y + 80);

        g.setColor(TEXT_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(title, x + 140, y + 60);

        g.setColor(TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.drawString(sub, x + 140, y + 100);

        g.setColor(BRAND_GREEN);
        g.drawString("⌵", x + w - 50, y + 75);
    }

    static void drawBottomNavigation(Graphics2D g, int activeIdx) {
        int navH = 170;
        int navY = H - navH;
        g.setColor(new Color(236, 242, 238));
        g.fillRect(0, navY, W, navH);
        g.setColor(BORDER);
        g.drawLine(0, navY, W, navY);

        String[] labels = {"Главная", "Журнал", "Заявки", "Каталог", "Ещё"};
        String[] icons = {"◫", "📋", "☑", "📦", "⚙"};
        int colW = W / 5;

        for (int i = 0; i < 5; i++) {
            boolean active = (i == activeIdx);
            int cx = i * colW + colW/2;

            if (active) {
                g.setColor(new Color(205, 240, 222));
                g.fillRoundRect(cx - 75, navY + 15, 150, 70, 35, 35);
            }

            g.setColor(active ? BRAND_GREEN : TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(icons[i], cx - fm.stringWidth(icons[i])/2, navY + 60);

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            fm = g.getFontMetrics();
            g.drawString(labels[i], cx - fm.stringWidth(labels[i])/2, navY + 125);
        }
    }
}
