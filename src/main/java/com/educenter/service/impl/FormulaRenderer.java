package com.educenter.service.impl;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FormulaRenderer {
    private static final Pattern MATH = Pattern.compile("\\$\\$(.+?)\\$\\$|\\\\\\[(.+?)\\\\\\]|\\\\\\((.+?)\\\\\\)|(?<!\\$)\\$([^\\r\\n$]+?)\\$(?!\\$)", Pattern.DOTALL);

    List<Segment> segments(String value) {
        String text = value == null ? "" : value;
        Matcher matcher = MATH.matcher(text); List<Segment> result = new ArrayList<>(); int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) result.add(new Segment(text.substring(cursor, matcher.start()), null));
            String expression = first(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            byte[] image = render(expression);
            if (image == null) result.add(new Segment(matcher.group(), null));
            else result.add(new Segment(null, image));
            cursor = matcher.end();
        }
        if (cursor < text.length() || result.isEmpty()) result.add(new Segment(text.substring(cursor), null));
        return result;
    }

    private byte[] render(String expression) {
        if (expression == null || expression.isBlank() || expression.length() > 2000) return null;
        try {
            TeXFormula formula = new TeXFormula(expression);
            TeXIcon icon = formula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_TEXT).setSize(13).build();
            icon.setInsets(new Insets(1, 1, 1, 1));
            BufferedImage image = new BufferedImage(Math.max(1, icon.getIconWidth() + 2), Math.max(1, icon.getIconHeight() + 2), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(Color.BLACK); icon.paintIcon(null, graphics, 1, 1);
            } finally { graphics.dispose(); }
            ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output); return output.toByteArray();
        } catch (RuntimeException e) { return null; }
        catch (Exception e) { return null; }
    }

    private String first(String... values) { for (String value : values) if (value != null) return value; return ""; }
    record Segment(String text, byte[] png) { boolean isFormula() { return png != null; } }
}
