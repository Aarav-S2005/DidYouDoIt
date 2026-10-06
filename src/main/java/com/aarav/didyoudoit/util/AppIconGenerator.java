package com.aarav.didyoudoit.util;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for generating high-DPI application icon assets for taskbar, system tray, and packaging.
 */
public final class AppIconGenerator {

    private AppIconGenerator() {}

    public static BufferedImage generateIconImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int margin = Math.max(2, size / 16);
        int cornerRadius = Math.max(6, size / 4);

        // Warm Coral Gradient
        GradientPaint gp = new GradientPaint(
                margin, margin, new Color(242, 142, 114),
                size - margin, size - margin, new Color(212, 99, 71)
        );
        g.setPaint(gp);
        g.fillRoundRect(margin, margin, size - (margin * 2), size - (margin * 2), cornerRadius, cornerRadius);

        // Crisp White Checkmark
        g.setColor(Color.WHITE);
        float strokeWidth = Math.max(2.5f, size * 0.10f);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int x1 = (int) Math.round(size * 0.28);
        int y1 = (int) Math.round(size * 0.52);
        int x2 = (int) Math.round(size * 0.44);
        int y2 = (int) Math.round(size * 0.68);
        int x3 = (int) Math.round(size * 0.72);
        int y3 = (int) Math.round(size * 0.36);

        int[] xPoints = {x1, x2, x3};
        int[] yPoints = {y1, y2, y3};
        g.drawPolyline(xPoints, yPoints, 3);

        // Golden accent dot
        g.setColor(new Color(255, 229, 143));
        int dotSize = Math.max(3, (int) Math.round(size * 0.08));
        int dotX = (int) Math.round(size * 0.73);
        int dotY = (int) Math.round(size * 0.24);
        g.fillOval(dotX, dotY, dotSize, dotSize);

        g.dispose();
        return image;
    }

    public static void generateAndSave(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        BufferedImage icon256 = generateIconImage(256);
        BufferedImage icon32 = generateIconImage(32);
        BufferedImage icon16 = generateIconImage(16);

        ImageIO.write(icon256, "PNG", outputDir.resolve("app-icon.png").toFile());
        ImageIO.write(icon32, "PNG", outputDir.resolve("app-icon-32.png").toFile());
        ImageIO.write(icon16, "PNG", outputDir.resolve("app-icon-16.png").toFile());
    }

    public static void main(String[] args) throws IOException {
        generateAndSave(Path.of("src/main/resources/icons"));
        System.out.println("Generated icons in src/main/resources/icons");
    }
}
