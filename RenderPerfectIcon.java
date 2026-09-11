import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class RenderPerfectIcon {
    public static void main(String[] args) throws Exception {
        int SIZE = 512;

        // 1. Exact squircle on transparent background (512x512 PNG)
        BufferedImage imgSquircleTrans = renderIcon(SIZE, 1);
        ImageIO.write(imgSquircleTrans, "PNG", new File("docs/kapterka_rustore_squircle.png"));

        // 2. Exact squircle on dark phone wallpaper background (matching user screenshot 1:1)
        BufferedImage imgScreenshot = renderIcon(SIZE, 2);
        ImageIO.write(imgScreenshot, "PNG", new File("docs/kapterka_phone_icon.png"));

        // 3. Official RuStore Full Square Bleed (512x512 PNG)
        BufferedImage imgFullSquare = renderIcon(SIZE, 3);
        ImageIO.write(imgFullSquare, "PNG", new File("docs/rustore_icon_512.png"));
        ImageIO.write(imgFullSquare, "PNG", new File("docs/app_icon_512.png"));

        System.out.println("PERFECT ICONS RENDERED!");
    }

    // mode: 1 = Squircle transparent, 2 = Squircle on dark wallpaper, 3 = Full square bleed
    static BufferedImage renderIcon(int size, int mode) {
        BufferedImage img = new BufferedImage(size, size, (mode == 1) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (mode == 2) {
            // Dark One UI desktop wallpaper color from user's screenshot
            g.setColor(new Color(0x44, 0x42, 0x47));
            g.fillRect(0, 0, size, size);
        }

        Shape clipShape = null;
        double inset = 24.0;
        double sqSize = size - inset * 2;
        double radius = sqSize * 0.42; // Samsung One UI squircle curve

        if (mode == 1 || mode == 2) {
            clipShape = new RoundRectangle2D.Double(inset, inset, sqSize, sqSize, radius, radius);
            g.setClip(clipShape);
        }

        // --- BACKGROUND ---
        // Dark military tactical green gradient
        GradientPaint bgGrad = new GradientPaint(
                0f, 0f, new Color(0x10, 0x1E, 0x16),
                (float) size, (float) size, new Color(0x18, 0x2A, 0x20)
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        // --- ARTWORK ---
        // Scale to fit nicely
        double contentSize = (mode == 3) ? (size * 0.88) : (sqSize * 0.94);
        double scale = contentSize / 72.0;
        double cx = size / 2.0;
        double cy = size / 2.0;

        AffineTransform oldTx = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-54, -54);

        // 1. Subtle Tactical Grid Lines (exact matching screenshot)
        g.setColor(new Color(0x20, 0x36, 0x28, 200));
        g.setStroke(new BasicStroke(0.7f));
        // Vertical and horizontal lines
        int[] gridLines = {18, 27, 36, 45, 54, 63, 72, 81, 90};
        for (int p : gridLines) {
            g.draw(new Line2D.Double(8, p, 100, p));
            g.draw(new Line2D.Double(p, 8, p, 100));
        }

        // 2. Outer Tactical Radar Ring (#2E543A, radius 32)
        g.setColor(new Color(0x2E, 0x54, 0x3A));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(54 - 32, 54 - 32, 64, 64));

        // Secondary Inner Radar Ring (#25442F, radius 26)
        g.setColor(new Color(0x25, 0x44, 0x2F));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Ellipse2D.Double(54 - 26, 54 - 26, 52, 52));

        // 3. Reticle Crosshair Ticks (#8EBF9F, stroke 2.2)
        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(54, 15, 54, 25)); // Top
        g.draw(new Line2D.Double(54, 83, 54, 93)); // Bottom
        g.draw(new Line2D.Double(15, 54, 25, 54)); // Left
        g.draw(new Line2D.Double(83, 54, 93, 54)); // Right

        // 4. Military Shield Silhouette (NO bright stroke, dark tactical green solid shape)
        Path2D.Double shield = new Path2D.Double();
        shield.moveTo(54, 27);
        shield.lineTo(76, 36);
        shield.lineTo(76, 57);
        shield.curveTo(76, 71, 54, 83, 54, 83);
        shield.curveTo(54, 83, 32, 71, 32, 57);
        shield.lineTo(32, 36);
        shield.closePath();

        GradientPaint shieldGrad = new GradientPaint(
                54f, 27f, new Color(0x15, 0x2C, 0x20),
                54f, 83f, new Color(0x0C, 0x19, 0x12)
        );
        g.setPaint(shieldGrad);
        g.fill(shield);

        // Very subtle dark edge for shield (not bright!)
        g.setColor(new Color(0x1F, 0x3D, 0x2D));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(shield);

        // 5. Tactical Crate / Container Core (Crisp Mint Wireframe #8EBF9F)
        Path2D.Double crate = new Path2D.Double();
        crate.moveTo(39, 47);
        crate.lineTo(54, 39);
        crate.lineTo(69, 47);
        crate.lineTo(69, 67);
        crate.lineTo(54, 75);
        crate.lineTo(39, 67);
        crate.closePath();

        // Dark green inside crate
        g.setColor(new Color(0x19, 0x36, 0x26));
        g.fill(crate);

        // Outer box wireframe
        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crate);

        // Inner Y-lines (connecting 3D faces)
        Path2D.Double crateY = new Path2D.Double();
        crateY.moveTo(39, 47);
        crateY.lineTo(54, 55);
        crateY.lineTo(69, 47);
        crateY.moveTo(54, 55);
        crateY.lineTo(54, 75);
        g.draw(crateY);

        // 6. Tactical Military Chevrons (Gold #E5C468)
        g.setColor(new Color(0xE5, 0xC4, 0x68));
        Path2D.Double chev1 = new Path2D.Double();
        chev1.moveTo(46, 45);
        chev1.lineTo(54, 41);
        chev1.lineTo(62, 45);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev1);

        Path2D.Double chev2 = new Path2D.Double();
        chev2.moveTo(47, 50);
        chev2.lineTo(54, 46);
        chev2.lineTo(61, 50);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev2);

        // 7. Gold Tactical Star (#E5C468)
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

        // If squircle, draw subtle outer icon border
        if (mode == 1 || mode == 2) {
            g.setClip(null);
            g.setColor(new Color(0x35, 0x48, 0x3D, 180));
            g.setStroke(new BasicStroke(2.0f));
            g.draw(clipShape);
        }

        g.dispose();
        return img;
    }
}
