import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class RenderExactInstalledIcon {
    public static void main(String[] args) throws Exception {
        int SIZE = 512;

        // -------------------------------------------------------------
        // We render based on the 72dp active viewport (from (18,18) to (90,90))
        // zoomed to fill the squircle (diameter 72dp mapped to ~440px inside 512px)
        // -------------------------------------------------------------

        // 1. VERSION A: Exact Squircle matching Samsung One UI installed app icon (Transparent background outside squircle)
        BufferedImage squircleTrans = renderInstalledIcon(SIZE, true, false);
        ImageIO.write(squircleTrans, "PNG", new File("docs/icon_installed_oneui.png"));

        // 2. VERSION B: Exact Squircle on dark background (just like user's phone screenshot)
        BufferedImage squircleDarkBg = renderInstalledIcon(SIZE, false, true);
        ImageIO.write(squircleDarkBg, "PNG", new File("docs/icon_phone_screenshot.png"));

        // 3. VERSION C: RuStore Official 512x512 Square (Full bleed, scaled properly so elements are large and fill canvas)
        BufferedImage squareStore = renderFullSquareStoreIcon(SIZE);
        ImageIO.write(squareStore, "PNG", new File("docs/rustore_icon_512.png"));
        ImageIO.write(squareStore, "PNG", new File("docs/app_icon_512.png"));

        System.out.println("ALL ICONS GENERATED SUCCESSFULLY!");
    }

    // Renders the icon elements scaled to fit a target box
    static void drawArtwork(Graphics2D g, double cx, double cy, double scale) {
        // scale: maps from 108dp coordinates, where center is (54,54)
        // Center is at cx, cy

        AffineTransform oldTx = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-54, -54);

        // Grid lines inside (centered around 54, from 18 to 90)
        g.setColor(new Color(0x23, 0x41, 0x2E, 220));
        g.setStroke(new BasicStroke(0.7f));
        int[] gridCoords = {18, 27, 36, 45, 54, 63, 72, 81, 90};
        for (int c : gridCoords) {
            g.draw(new Line2D.Double(10, c, 98, c));
            g.draw(new Line2D.Double(c, 10, c, 98));
        }

        // Radar Outer Ring (radius 32, stroke 1.5)
        g.setColor(new Color(0x2F, 0x5C, 0x40));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(54 - 32, 54 - 32, 64, 64));

        // Radar Inner Ring (radius 26, stroke 0.8)
        g.setColor(new Color(0x3D, 0x75, 0x53));
        g.setStroke(new BasicStroke(0.9f));
        g.draw(new Ellipse2D.Double(54 - 26, 54 - 26, 52, 52));

        // Reticle Crosshair Ticks (#8EBF9F, stroke 2.0)
        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(54, 17, 54, 25)); // Top
        g.draw(new Line2D.Double(54, 83, 54, 91)); // Bottom
        g.draw(new Line2D.Double(17, 54, 25, 54)); // Left
        g.draw(new Line2D.Double(83, 54, 91, 54)); // Right

        // Military Supply Depot Outer Shield
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

        // Tactical Crate / Container Core
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

        // Inner crate Y lines
        Path2D.Double crateInner = new Path2D.Double();
        crateInner.moveTo(39, 47);
        crateInner.lineTo(54, 55);
        crateInner.lineTo(69, 47);
        crateInner.moveTo(54, 55);
        crateInner.lineTo(54, 75);

        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crateInner);

        // Tactical Chevrons (#E5C468)
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

        // Gold Tactical Star (#E5C468)
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

    // Creates the exact Samsung One UI squircle path
    static Shape createSquircle(double x, double y, double size, double radius) {
        return new RoundRectangle2D.Double(x, y, size, size, radius, radius);
    }

    // VERSION A & B: Installed Squircle Icon
    static BufferedImage renderInstalledIcon(int size, boolean transparentBg, boolean darkPhoneBg) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (darkPhoneBg) {
            // Dark phone background like screenshot
            g.setColor(new Color(0x2B, 0x2B, 0x2E));
            g.fillRect(0, 0, size, size);
        }

        // Squircle bounds: inset by 16px
        double margin = 18;
        double sqSize = size - margin * 2;
        double cornerRadius = sqSize * 0.44; // smooth One UI squircle
        RoundRectangle2D.Double squircle = new RoundRectangle2D.Double(margin, margin, sqSize, sqSize, cornerRadius, cornerRadius);

        // Clip to squircle for drawing background & artwork
        Shape oldClip = g.getClip();
        g.setClip(squircle);

        // Background inside squircle
        GradientPaint bgGrad = new GradientPaint(
                (float) margin, (float) margin, new Color(0x11, 0x1E, 0x17),
                (float)(margin + sqSize), (float)(margin + sqSize), new Color(0x1B, 0x2B, 0x22)
        );
        g.setPaint(bgGrad);
        g.fill(squircle);

        // Draw artwork zoomed to fill the squircle (scale = sqSize / 76.0)
        double scale = sqSize / 74.0;
        drawArtwork(g, size / 2.0, size / 2.0, scale);

        // Restore clip
        g.setClip(oldClip);

        // Subtle squircle border
        g.setColor(new Color(0x3B, 0x4D, 0x42, 180));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(squircle);

        g.dispose();
        return img;
    }

    // VERSION C: Full Bleed 512x512 Square for RuStore
    static BufferedImage renderFullSquareStoreIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Background
        GradientPaint bgGrad = new GradientPaint(
                0f, 0f, new Color(0x0F, 0x1B, 0x14),
                (float) size, (float) size, new Color(0x1A, 0x2B, 0x21)
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        // Scale artwork so active elements fill 85% of the 512 canvas
        double scale = size / 76.0;
        drawArtwork(g, size / 2.0, size / 2.0, scale);

        g.dispose();
        return img;
    }
}
