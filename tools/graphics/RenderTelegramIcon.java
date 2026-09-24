import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

// Генерирует иконку для Telegram-канала "Каптёрка PRO" в фирменном тактическом стиле.
public class RenderTelegramIcon {
    public static void main(String[] args) throws Exception {
        int SIZE = 512;

        // 1. Квадратная full-bleed версия для загрузки в Telegram (само TG обрежет в круг)
        BufferedImage square = renderSquare(SIZE);
        ImageIO.write(square, "PNG", new File("docs/telegram_channel_icon_512.png"));

        // 2. Превью, как иконка будет выглядеть после круглой обрезки Telegram
        BufferedImage circlePreview = renderCirclePreview(square, SIZE);
        ImageIO.write(circlePreview, "PNG", new File("docs/telegram_channel_icon_circle_preview.png"));

        System.out.println("Telegram channel icon generated.");
    }

    static void drawArtwork(Graphics2D g, double cx, double cy, double scale) {
        AffineTransform oldTx = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-54, -54);

        // Тактическая сетка
        g.setColor(new Color(0x23, 0x41, 0x2E, 220));
        g.setStroke(new BasicStroke(0.7f));
        int[] gridCoords = {18, 27, 36, 45, 54, 63, 72, 81, 90};
        for (int c : gridCoords) {
            g.draw(new Line2D.Double(10, c, 98, c));
            g.draw(new Line2D.Double(c, 10, c, 98));
        }

        // Внешнее кольцо радара
        g.setColor(new Color(0x2F, 0x5C, 0x40));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(54 - 32, 54 - 32, 64, 64));

        // Внутреннее кольцо
        g.setColor(new Color(0x3D, 0x75, 0x53));
        g.setStroke(new BasicStroke(0.9f));
        g.draw(new Ellipse2D.Double(54 - 26, 54 - 26, 52, 52));

        // Метки прицела
        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(54, 17, 54, 25));
        g.draw(new Line2D.Double(54, 83, 54, 91));
        g.draw(new Line2D.Double(17, 54, 25, 54));
        g.draw(new Line2D.Double(83, 54, 91, 54));

        // Щит склада
        Path2D.Double shield = new Path2D.Double();
        shield.moveTo(54, 27);
        shield.lineTo(76, 36);
        shield.lineTo(76, 57);
        shield.curveTo(76, 71, 54, 83, 54, 83);
        shield.curveTo(54, 83, 32, 71, 32, 57);
        shield.lineTo(32, 36);
        shield.closePath();

        GradientPaint shieldGrad = new GradientPaint(
                54f, 27f, new Color(0x1B, 0x36, 0x27),
                54f, 83f, new Color(0x0E, 0x1E, 0x15)
        );
        g.setPaint(shieldGrad);
        g.fill(shield);

        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shield);

        // Ящик
        Path2D.Double crate = new Path2D.Double();
        crate.moveTo(39, 47);
        crate.lineTo(54, 39);
        crate.lineTo(69, 47);
        crate.lineTo(69, 67);
        crate.lineTo(54, 75);
        crate.lineTo(39, 67);
        crate.closePath();

        g.setColor(new Color(0x20, 0x44, 0x30));
        g.fill(crate);

        g.setColor(new Color(0xA2, 0xCE, 0xB5));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crate);

        Path2D.Double crateInner = new Path2D.Double();
        crateInner.moveTo(39, 47);
        crateInner.lineTo(54, 55);
        crateInner.lineTo(69, 47);
        crateInner.moveTo(54, 55);
        crateInner.lineTo(54, 75);

        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crateInner);

        // Шевроны
        Path2D.Double chev1 = new Path2D.Double();
        chev1.moveTo(46, 45);
        chev1.lineTo(54, 41);
        chev1.lineTo(62, 45);
        g.setColor(new Color(0xE5, 0xC4, 0x68));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev1);

        Path2D.Double chev2 = new Path2D.Double();
        chev2.moveTo(47, 50);
        chev2.lineTo(54, 46);
        chev2.lineTo(61, 50);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev2);

        // Звезда
        double[][] starPts = {
                {54, 57},
                {55.8, 61.8},
                {61, 61.8},
                {56.8, 64.8},
                {58.4, 69.5},
                {54, 66.5},
                {49.6, 69.5},
                {51.2, 64.8},
                {47, 61.8},
                {52.2, 61.8}
        };
        Path2D.Double star = new Path2D.Double();
        star.moveTo(starPts[0][0], starPts[0][1]);
        for (int i = 1; i < starPts.length; i++) {
            star.lineTo(starPts[i][0], starPts[i][1]);
        }
        star.closePath();
        g.setColor(new Color(0xE5, 0xC4, 0x68));
        g.fill(star);

        g.setTransform(oldTx);
    }

    // Квадрат 512x512, full-bleed, элементы в пределах вписанного круга (safe zone для Telegram)
    static BufferedImage renderSquare(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        GradientPaint bgGrad = new GradientPaint(
                0f, 0f, new Color(0x0F, 0x1B, 0x14),
                (float) size, (float) size, new Color(0x1A, 0x2B, 0x21)
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        // scale=size/76 держит все элементы (включая метки прицела на радиусе 37 в 108-координатах)
        // внутри вписанного в квадрат круга -> иконка не обрезается при круглом аватаре Telegram
        double scale = size / 76.0;
        drawArtwork(g, size / 2.0, size / 2.0, scale);

        g.dispose();
        return img;
    }

    // Круглое превью, как это будет выглядеть в интерфейсе Telegram
    static BufferedImage renderCirclePreview(BufferedImage square, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, size, size);
        g.setClip(circle);
        g.drawImage(square, 0, 0, null);
        g.dispose();
        return img;
    }
}
