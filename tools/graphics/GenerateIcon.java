import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateIcon {
    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Enable Anti-Aliasing and high quality rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark tactical background
        g2.setColor(new Color(16, 21, 18));
        g2.fillRect(0, 0, size, size);

        // Rounded tactical container
        int margin = 28;
        RoundRectangle2D card = new RoundRectangle2D.Float(margin, margin, size - 2*margin, size - 2*margin, 96, 96);
        
        // Gradient background for card
        GradientPaint gp = new GradientPaint(margin, margin, new Color(28, 38, 30), size - margin, size - margin, new Color(18, 24, 20));
        g2.setPaint(gp);
        g2.fill(card);

        // Gold border
        g2.setColor(new Color(218, 165, 32)); // Accent Gold
        g2.setStroke(new BasicStroke(6f));
        g2.draw(card);

        // Subtle inner border
        g2.setColor(new Color(141, 170, 89, 90));
        g2.setStroke(new BasicStroke(2f));
        RoundRectangle2D innerBorder = new RoundRectangle2D.Float(margin + 8, margin + 8, size - 2*(margin + 8), size - 2*(margin + 8), 84, 84);
        g2.draw(innerBorder);

        // Draw Tactical Chevron Shield Emblem in center
        int cx = size / 2;
        int cy = 210;

        // Shield / Chevron
        Polygon shield = new Polygon();
        shield.addPoint(cx, cy - 110);       // Top point
        shield.addPoint(cx + 95, cy - 50);    // Top right
        shield.addPoint(cx + 70, cy + 60);    // Mid right
        shield.addPoint(cx, cy + 120);       // Bottom point
        shield.addPoint(cx - 70, cy + 60);    // Mid left
        shield.addPoint(cx - 95, cy - 50);    // Top left

        // Shield fill
        GradientPaint shieldPaint = new GradientPaint(cx, cy - 110, new Color(141, 170, 89), cx, cy + 120, new Color(92, 114, 57));
        g2.setPaint(shieldPaint);
        g2.fill(shield);

        // Shield gold outline
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shield);

        // Tactical 5-point star inside shield
        drawStar(g2, cx, cy - 15, 36, 17, new Color(255, 255, 255));
        drawStar(g2, cx, cy - 15, 36, 17, new Color(255, 215, 0), false);

        // Small chevrons under the star inside shield
        Polygon chevron1 = new Polygon();
        chevron1.addPoint(cx - 35, cy + 30);
        chevron1.addPoint(cx, cy + 60);
        chevron1.addPoint(cx + 35, cy + 30);
        chevron1.addPoint(cx + 35, cy + 45);
        chevron1.addPoint(cx, cy + 75);
        chevron1.addPoint(cx - 35, cy + 45);
        g2.setColor(new Color(255, 215, 0));
        g2.fill(chevron1);

        // Typography: KAPTERKA
        g2.setColor(Color.WHITE);
        Font mainFont = new Font("SansSerif", Font.BOLD, 46);
        g2.setFont(mainFont);
        FontMetrics fm = g2.getFontMetrics();
        String title = "КАПТЁРКА";
        int titleX = (size - fm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, 395);

        // Subtitle: PRO · СКЛАД
        Font subFont = new Font("SansSerif", Font.BOLD, 22);
        g2.setFont(subFont);
        FontMetrics fmSub = g2.getFontMetrics();
        String sub = "ПРО  ★  СКЛАДСКОЙ УЧЁТ";
        int subX = (size - fmSub.stringWidth(sub)) / 2;
        g2.setColor(new Color(218, 165, 32));
        g2.drawString(sub, subX, 435);

        g2.dispose();

        File out = new File("docs/rustore_icon_512.png");
        ImageIO.write(image, "png", out);
        System.out.println("Saved icon to " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int outerR, int innerR, Color color) {
        drawStar(g, cx, cy, outerR, innerR, color, true);
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int outerR, int innerR, Color color, boolean fill) {
        Polygon p = new Polygon();
        double angle = -Math.PI / 2.0;
        double step = Math.PI / 5.0;
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outerR : innerR;
            p.addPoint((int)(cx + r * Math.cos(angle)), (int)(cy + r * Math.sin(angle)));
            angle += step;
        }
        g.setColor(color);
        if (fill) {
            g.fill(p);
        } else {
            g.setStroke(new BasicStroke(2.5f));
            g.draw(p);
        }
    }
}
