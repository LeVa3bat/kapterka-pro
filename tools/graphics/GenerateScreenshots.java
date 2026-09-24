import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateScreenshots {
    public static void main(String[] args) throws Exception {
        int w = 1080;
        int h = 1920;

        createScreen1(w, h);
        createScreen2(w, h);
        createScreen3(w, h);
        System.out.println("Screenshots 1, 2, 3 generated successfully!");
    }

    private static void createScreen1(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        // Background
        g.setColor(new Color(16, 21, 18));
        g.fillRect(0, 0, w, h);

        // Header
        drawTacticalHeader(g, w, "СКЛАДСКОЙ УЧЁТ И ОСТАТКИ", "Базовый склад подразделения");

        // Top KPI Cards
        drawKpiCard(g, 60, 240, 290, 160, "ВСЕГО ПОЗИЦИЙ", "248", new Color(141, 170, 89));
        drawKpiCard(g, 390, 240, 290, 160, "НА ВЫДАЧЕ", "64", new Color(218, 165, 32));
        drawKpiCard(g, 720, 240, 290, 160, "КРИТИЧЕСКИ", "3", new Color(220, 53, 69));

        // Category Filter chips
        String[] chips = {"Все склады", "СИЗ и Броня", "Связь", "Медицина", "Оптика"};
        int chipX = 60;
        for (int i = 0; i < chips.length; i++) {
            boolean active = (i == 0);
            g.setColor(active ? new Color(141, 170, 89) : new Color(30, 40, 32));
            g.fillRoundRect(chipX, 430, 180, 60, 30, 30);
            g.setColor(active ? Color.BLACK : Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(chips[i], chipX + (180 - fm.stringWidth(chips[i])) / 2, 468);
            chipX += 195;
        }

        // Inventory Items List
        int startY = 530;
        drawInventoryRow(g, 60, startY, 960, 180, "Бронежилет 6Б45 (размер 2)", "СИЗ и Бронезащита • Ячейка А-14", "18 шт.", "Остаток в норме", new Color(141, 170, 89));
        drawInventoryRow(g, 60, startY + 200, 960, 180, "Шлем защитный 6Б47", "СИЗ и Экипировка • Ячейка А-15", "12 шт.", "Остаток в норме", new Color(141, 170, 89));
        drawInventoryRow(g, 60, startY + 400, 960, 180, "Радиостанция цифровой связи", "Связь и спецсредства • Сейф №2", "4 шт.", "Малый остаток", new Color(218, 165, 32));
        drawInventoryRow(g, 60, startY + 600, 960, 180, "Аптечка первой помощи (групповая)", "Тактическая медицина • Бокс М-3", "28 шт.", "Остаток в норме", new Color(141, 170, 89));
        drawInventoryRow(g, 60, startY + 800, 960, 180, "Аккумуляторы усиленные Li-Ion", "Энергообеспечение • Стеллаж Э-1", "2 шт.", "Критический остаток", new Color(220, 53, 69));

        // Floating Action Button
        drawFAB(g, w - 240, h - 340, "+ ПРИХОД / РАСХОД");

        // Bottom Navigation Bar
        drawBottomNav(g, w, h, 0);

        g.dispose();
        ImageIO.write(img, "png", new File("docs/screenshot_1.png"));
    }

    private static void createScreen2(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setColor(new Color(16, 21, 18));
        g.fillRect(0, 0, w, h);

        drawTacticalHeader(g, w, "ЭКСПОРТ В EXCEL И НАКЛАДНЫЕ", "Мгновенное формирование отчётов");

        // Info Banner
        g.setColor(new Color(24, 34, 27));
        g.fillRoundRect(60, 240, 960, 220, 36, 36);
        g.setColor(new Color(218, 165, 32));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(60, 240, 960, 220, 36, 36);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("★ ОФФЛАЙН-ГЕНЕРАТОР ТАБЛИЦ XLSX", 100, 300);
        g.setColor(new Color(200, 215, 200));
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("Формируйте ведомости наличия, раздаточные ведомости", 100, 350);
        g.drawString("и акты списания прямо в полевых условиях без связи.", 100, 390);

        // Report Templates
        int startY = 500;
        drawReportRow(g, 60, startY, 960, 190, "Ведомость наличия и остатков (Форма №1)", "Полная опись по всем складам и ячейкам", "Сформировать XLSX");
        drawReportRow(g, 60, startY + 220, 960, 190, "Раздаточная ведомость (под роспись)", "Учёт закрепления имущества за бойцами", "Сформировать XLSX");
        drawReportRow(g, 60, startY + 440, 960, 190, "Акт инвентаризации и списания", "Сверка фактических остатков и расхода", "Сформировать XLSX");
        drawReportRow(g, 60, startY + 660, 960, 190, "Сводный отчёт движения ТМЦ", "Приход, расход и перемещение за период", "Сформировать XLSX");

        // Action Buttons at bottom
        g.setColor(new Color(141, 170, 89));
        g.fillRoundRect(60, 1550, 960, 90, 45, 45);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String expText = "📁 ОТКРЫТЬ ПАПКУ С ОТЧЁТАМИ (XLSX)";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(expText, 60 + (960 - fm.stringWidth(expText)) / 2, 1605);

        drawBottomNav(g, w, h, 2);

        g.dispose();
        ImageIO.write(img, "png", new File("docs/screenshot_2.png"));
    }

    private static void createScreen3(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setColor(new Color(16, 21, 18));
        g.fillRect(0, 0, w, h);

        drawTacticalHeader(g, w, "КАРТОЧКА ЛИЧНОГО СОСТАВА", "Закрепление имущества за бойцами");

        // Soldier Info Card
        g.setColor(new Color(25, 35, 28));
        g.fillRoundRect(60, 240, 960, 260, 36, 36);
        g.setColor(new Color(141, 170, 89));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(60, 240, 960, 260, 36, 36);

        // Avatar circle
        g.setColor(new Color(45, 60, 48));
        g.fillOval(100, 280, 120, 120);
        g.setColor(new Color(218, 165, 32));
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("Б", 145, 360);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString("Позывной: «БЕРКУТ»", 250, 325);
        g.setColor(new Color(180, 200, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("Должность: Старший стрелок 1-го взвода", 250, 370);
        g.setColor(new Color(218, 165, 32));
        g.drawString("Закреплено имущества: 8 наименований", 250, 410);

        // Section Title
        g.setColor(new Color(141, 170, 89));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("СПИСОК ВЫДАННОГО ИМУЩЕСТВА ПОД РОСПИСЬ:", 60, 560);

        // Issued items list
        int startY = 600;
        drawIssuedItemRow(g, 60, startY, 960, 160, "Бронежилет тактический 6Б45", "Сер. номер: №БЖ-8841 • Кат. 1", "Выдано: 1 шт. (01.09)", "На руках");
        drawIssuedItemRow(g, 60, startY + 180, 960, 160, "Шлем кевларовый 6Б47", "Сер. номер: №ШЛ-1029 • Кат. 1", "Выдано: 1 шт. (01.09)", "На руках");
        drawIssuedItemRow(g, 60, startY + 360, 960, 160, "Рация переносная цифровой связи", "Сер. номер: №РЦ-4412 • Гарнитура", "Выдано: 1 компл.", "На руках");
        drawIssuedItemRow(g, 60, startY + 540, 960, 160, "Прибор ночного видения ПНВ", "Инв. номер: №ОПТ-052 • Бокс", "Выдано: 1 шт.", "Срочный возврат");

        // Return / Issue action buttons
        g.setColor(new Color(141, 170, 89));
        g.fillRoundRect(60, 1420, 460, 90, 45, 45);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("+ ВЫДАТЬ ИМУЩЕСТВО", 130, 1475);

        g.setColor(new Color(40, 50, 42));
        g.fillRoundRect(560, 1420, 460, 90, 45, 45);
        g.setColor(Color.WHITE);
        g.drawString("ВЕДОМОСТЬ СДАЧИ", 640, 1475);

        drawBottomNav(g, w, h, 1);

        g.dispose();
        ImageIO.write(img, "png", new File("docs/screenshot_3.png"));
    }

    private static void setupHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void drawTacticalHeader(Graphics2D g, int w, String title, String subtitle) {
        g.setColor(new Color(24, 32, 26));
        g.fillRect(0, 0, w, 200);
        g.setColor(new Color(218, 165, 32));
        g.fillRect(0, 196, w, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(title, 60, 110);

        g.setColor(new Color(160, 185, 160));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(subtitle, 60, 155);

        // Status badge
        g.setColor(new Color(141, 170, 89));
        g.fillRoundRect(w - 280, 85, 220, 55, 28, 28);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("● ОФФЛАЙН", w - 240, 120);
    }

    private static void drawKpiCard(Graphics2D g, int x, int y, int w, int h, String title, String val, Color valCol) {
        g.setColor(new Color(24, 32, 26));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(45, 58, 48));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setColor(new Color(170, 190, 170));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, x + 25, y + 45);

        g.setColor(valCol);
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        g.drawString(val, x + 25, y + 120);
    }

    private static void drawInventoryRow(Graphics2D g, int x, int y, int w, int h, String name, String sub, String qty, String status, Color stCol) {
        g.setColor(new Color(24, 32, 26));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(38, 50, 40));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(name, x + 35, y + 55);

        g.setColor(new Color(170, 190, 170));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(sub, x + 35, y + 100);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(qty, x + w - 160, y + 70);

        g.setColor(stCol);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(status, x + 35, y + 145);
    }

    private static void drawReportRow(Graphics2D g, int x, int y, int w, int h, String title, String sub, String btnText) {
        g.setColor(new Color(24, 32, 26));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(40, 55, 42));
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(title, x + 35, y + 60);

        g.setColor(new Color(170, 190, 170));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(sub, x + 35, y + 105);

        // Green button
        g.setColor(new Color(141, 170, 89));
        g.fillRoundRect(x + 35, y + 125, 340, 45, 22, 22);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(btnText, x + 55, y + 155);
    }

    private static void drawIssuedItemRow(Graphics2D g, int x, int y, int w, int h, String name, String sub, String qty, String status) {
        g.setColor(new Color(24, 32, 26));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(40, 55, 42));
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(name, x + 30, y + 50);

        g.setColor(new Color(170, 190, 170));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString(sub, x + 30, y + 90);

        g.setColor(new Color(218, 165, 32));
        g.drawString(qty, x + 30, y + 130);

        g.setColor(new Color(141, 170, 89));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(status, x + w - 180, y + 90);
    }

    private static void drawFAB(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(218, 165, 32));
        g.fillRoundRect(x, y, 180, 80, 40, 40);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(label, x + 25, y + 50);
    }

    private static void drawBottomNav(Graphics2D g, int w, int h, int activeIdx) {
        int navH = 140;
        g.setColor(new Color(20, 27, 22));
        g.fillRect(0, h - navH, w, navH);
        g.setColor(new Color(40, 55, 42));
        g.drawLine(0, h - navH, w, h - navH);

        String[] tabs = {"Склад", "Личный состав", "Отчёты Excel", "Настройки"};
        int tabW = w / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == activeIdx);
            g.setColor(active ? new Color(218, 165, 32) : new Color(140, 160, 140));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            int tx = i * tabW + (tabW - fm.stringWidth(tabs[i])) / 2;
            g.drawString(tabs[i], tx, h - 55);
        }
    }
}
