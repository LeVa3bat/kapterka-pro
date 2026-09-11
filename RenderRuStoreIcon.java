import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class RenderRuStoreIcon {
    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // High quality rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double s = (double) size / 108.0;

        // 1. Background Gradient (0F1713 -> 1A2720)
        GradientPaint bgGrad = new GradientPaint(
                0f, 0f, new Color(0x0F, 0x17, 0x13),
                (float) size, (float) size, new Color(0x1A, 0x27, 0x20)
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        // 2. Tactical Grid Lines (#1F3D2B, width 0.5 * s)
        g.setColor(new Color(0x1F, 0x3D, 0x2B));
        g.setStroke(new BasicStroke((float) (0.8 * s)));
        int[] gridCoords = {18, 36, 54, 72, 90};
        for (int c : gridCoords) {
            // Horizontal
            g.draw(new Line2D.Double(18 * s, c * s, 90 * s, c * s));
            // Vertical
            g.draw(new Line2D.Double(c * s, 18 * s, c * s, 90 * s));
        }

        // 3. Outer Tactical Radar Ring (#2F5C40, radius 32, width 1.5 * s)
        g.setColor(new Color(0x2F, 0x5C, 0x40));
        g.setStroke(new BasicStroke((float) (1.5 * s)));
        g.draw(new Ellipse2D.Double((54 - 32) * s, (54 - 32) * s, 64 * s, 64 * s));

        // Secondary Radar Ring (#3D7553, radius 26, width 0.8 * s)
        g.setColor(new Color(0x3D, 0x75, 0x53));
        g.setStroke(new BasicStroke((float) (0.9 * s)));
        g.draw(new Ellipse2D.Double((54 - 26) * s, (54 - 26) * s, 52 * s, 52 * s));

        // 4. Reticle Crosshair Ticks (#8EBF9F, stroke 2.0 * s)
        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke((float) (2.2 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(54 * s, 18 * s, 54 * s, 25 * s)); // Top
        g.draw(new Line2D.Double(54 * s, 83 * s, 54 * s, 90 * s)); // Bottom
        g.draw(new Line2D.Double(18 * s, 54 * s, 25 * s, 54 * s)); // Left
        g.draw(new Line2D.Double(83 * s, 54 * s, 90 * s, 54 * s)); // Right

        // 5. Military Supply Depot Outer Shield
        // M54,27 L76,36 L76,57 C76,71 54,83 54,83 C54,83 32,71 32,57 L32,36 Z
        Path2D.Double shield = new Path2D.Double();
        shield.moveTo(54 * s, 27 * s);
        shield.lineTo(76 * s, 36 * s);
        shield.lineTo(76 * s, 57 * s);
        shield.curveTo(76 * s, 71 * s, 54 * s, 83 * s, 54 * s, 83 * s);
        shield.curveTo(54 * s, 83 * s, 32 * s, 71 * s, 32 * s, 57 * s);
        shield.lineTo(32 * s, 36 * s);
        shield.closePath();

        GradientPaint shieldGrad = new GradientPaint(
                (float)(54 * s), (float)(27 * s), new Color(0x1A, 0x33, 0x24),
                (float)(54 * s), (float)(83 * s), new Color(0x0F, 0x1E, 0x15)
        );
        g.setPaint(shieldGrad);
        g.fill(shield);

        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke((float) (2.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shield);

        // 6. Tactical Crate / Container Core
        // M39,47 L54,39 L69,47 L69,67 L54,75 L39,67 Z
        Path2D.Double crate = new Path2D.Double();
        crate.moveTo(39 * s, 47 * s);
        crate.lineTo(54 * s, 39 * s);
        crate.lineTo(69 * s, 47 * s);
        crate.lineTo(69 * s, 67 * s);
        crate.lineTo(54 * s, 75 * s);
        crate.lineTo(39 * s, 67 * s);
        crate.closePath();

        g.setColor(new Color(0x24, 0x47, 0x33));
        g.fill(crate);

        g.setColor(new Color(0xA2, 0xCE, 0xB5));
        g.setStroke(new BasicStroke((float) (1.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crate);

        // Inner crate Y lines: M39,47 L54,55 L69,47 M54,55 L54,75
        Path2D.Double crateInner = new Path2D.Double();
        crateInner.moveTo(39 * s, 47 * s);
        crateInner.lineTo(54 * s, 55 * s);
        crateInner.lineTo(69 * s, 47 * s);
        crateInner.moveTo(54 * s, 55 * s);
        crateInner.lineTo(54 * s, 75 * s);

        g.setColor(new Color(0x8E, 0xBF, 0x9F));
        g.setStroke(new BasicStroke((float) (1.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crateInner);

        // 7. Tactical Military Chevrons (#E5C468)
        // M46,45 L54,41 L62,45
        Path2D.Double chev1 = new Path2D.Double();
        chev1.moveTo(46 * s, 45 * s);
        chev1.lineTo(54 * s, 41 * s);
        chev1.lineTo(62 * s, 45 * s);
        g.setColor(new Color(0xE5, 0xC4, 0x68));
        g.setStroke(new BasicStroke((float) (2.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev1);

        // M47,50 L54,46 L61,50
        Path2D.Double chev2 = new Path2D.Double();
        chev2.moveTo(47 * s, 50 * s);
        chev2.lineTo(54 * s, 46 * s);
        chev2.lineTo(61 * s, 50 * s);
        g.setStroke(new BasicStroke((float) (2.1 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chev2);

        // 8. Gold Tactical Star (#E5C468)
        // M54,57 L55.8,61.8 L61,61.8 L56.8,64.8 L58.4,69.5 L54,66.5 L49.6,69.5 L51.2,64.8 L47,61.8 L52.2,61.8 Z
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
        star.moveTo(starPts[0][0] * s, starPts[0][1] * s);
        for (int i = 1; i < starPts.length; i++) {
            star.lineTo(starPts[i][0] * s, starPts[i][1] * s);
        }
        star.closePath();
        g.setColor(new Color(0xE5, 0xC4, 0x68));
        g.fill(star);

        g.dispose();

        // Save 512x512 square icon for RuStore (MANDATORY REQUIREMENT OF RUSTORE CONSOLE)
        ImageIO.write(img, "PNG", new File("docs/rustore_icon_512.png"));
        ImageIO.write(img, "PNG", new File("docs/app_icon_512.png"));

        // Also create 192x192 for 4PDA
        BufferedImage img192 = new BufferedImage(192, 192, BufferedImage.TYPE_INT_RGB);
        Graphics2D g192 = img192.createGraphics();
        g192.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g192.drawImage(img, 0, 0, 192, 192, null);
        g192.dispose();
        ImageIO.write(img192, "PNG", new File("docs/icon_4pda_192.png"));

        System.out.println("SUCCESS: RuStore icon rendered to docs/rustore_icon_512.png (512x512 PNG)!");
    }
}
