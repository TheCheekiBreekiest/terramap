package fr.thesmyler.smylibgui.util;

import net.smyler.smylib.Color;
import net.smyler.smylib.gui.DrawContext;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import static net.smyler.smylib.SmyLib.getGameClient;

public final class RenderUtil {

    @Deprecated
    public static void drawRect(int z, double xLeft, double yTop, double xRight, double yBottom, Color color) {
        drawGradientRect(z, xLeft, yTop, xRight, yBottom, color, color, color, color);
    }

    @Deprecated
    public static void drawRect(double xLeft, double yTop, double xRight, double yBottom, Color color) {
        drawGradientRect(0, xLeft, yTop, xRight, yBottom, color, color, color, color);
    }

    @Deprecated
    public static void drawRectWithContour(int z, double xLeft, double yTop, double xRight, double yBottom, Color color, float contourSize, Color contourColor) {
        DrawContext context = getGameClient().guiDrawContext();
        context.drawRectangleWithContours(z, xLeft, yTop, xRight, yBottom, color, contourSize, contourColor);
    }

    @Deprecated
    public static void drawRectWithContour(double xLeft, double yTop, double xRight, double yBottom, Color color, float contourSize, Color contourColor) {
        drawRectWithContour(0, xLeft, yTop, xRight, yBottom, color, contourSize, contourColor);
    }

    @Deprecated
    public static void drawGradientRect(int z, double xLeft, double yTop, double xRight, double yBottom, Color upperLeftColor, Color lowerLeftColor, Color lowerRightColor, Color upperRightColor) {
        DrawContext context = getGameClient().guiDrawContext();
        context.drawGradientRectangle(z, xLeft, yTop, xRight, yBottom, upperLeftColor, lowerLeftColor, lowerRightColor, upperRightColor);
    }

    @Deprecated
    public static void drawGradientRect(double xLeft, double yTop, double xRight, double yBottom, Color upperLeftColor, Color lowerLeftColor, Color lowerRightColor, Color upperRightColor) {
        drawGradientRect(0, xLeft, yTop, xRight, yBottom, upperLeftColor, lowerLeftColor, lowerRightColor, upperRightColor);
    }

    public static void drawModalRectWithCustomSizedTexture(double x, double y, double z, double u, double v, double width, double height, double textureWidth, double textureHeight) {
        double f = 1.0f / textureWidth;
        double f1 = 1.0f / textureHeight;
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(7, DefaultVertexFormats.POSITION_TEX);
        builder.pos(x, y + height, z).tex(u * f, (v + height) * f1).endVertex();
        builder.pos(x + width, y + height, z).tex((u + width) * f, (v + height) * f1).endVertex();
        builder.pos(x + width, y, z).tex((u + width) * f, v * f1).endVertex();
        builder.pos(x, y, z).tex(u * f, v * f1).endVertex();
        tessellator.draw();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    public static void drawModalRectWithCustomSizedTexture(double x, double y, double u, double v, double width, double height, double textureWidth, double textureHeight) {
        drawModalRectWithCustomSizedTexture(x, y, 0d, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawTexturedModalRect(double x, double y, double z, double minU, double minV, double maxU, double maxV) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(x, y + maxV, z).tex(minU * 0.00390625, (minV + maxV) * 0.00390625).endVertex();
        builder.pos(x + maxU, y + maxV, z).tex((minU + maxU) * 0.00390625, (minV + maxV) * 0.00390625).endVertex();
        builder.pos(x + maxU, y, z).tex((minU + maxU) * 0.00390625, minV * 0.00390625).endVertex();
        builder.pos(x, y, z).tex(minU * 0.00390625, minV * 0.00390625).endVertex();
        tessellator.draw();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    public static void drawTexturedModalRect(double x, double y, double minU, double minV, double maxU, double maxV) {
        drawTexturedModalRect(x, y, 0, minU, minV, maxU, maxV);
    }

    @Deprecated
    public static void drawPolygon(double z, Color color, double... points) {
        DrawContext context = getGameClient().guiDrawContext();
        context.drawPolygon(z, color, points);
    }

    @Deprecated
    public static void drawPolygon(Color color, double... points) {
        drawPolygon(0d, color, points);
    }

    @Deprecated
    public static void drawStrokeLine(double z, Color color, float size, double... points) {
        DrawContext context = getGameClient().guiDrawContext();
        context.drawStrokeLine(z, color, size, points);
    }

    @Deprecated
    public static void drawStrokeLine(Color color, float size, double... points) {
        drawStrokeLine(0, color, size, points);
    }

    @Deprecated
    public static void drawClosedStrokeLine(double z, Color color, float size, double... points) {
        DrawContext context = getGameClient().guiDrawContext();
        context.drawClosedStrokeLine(z, color, size, points);
    }

    @Deprecated
    public static void drawClosedStrokeLine(Color color, float size, double... points) {
        drawClosedStrokeLine(0d, color, size, points);
    }

    public static void applyColor(Color color) {
        GlStateManager.color(
                color.redf(),
                color.greenf(),
                color.bluef(),
                color.alphaf()
        );
    }

    public static Color currentColor() {
        return new Color(GL11.glGetInteger(GL11.GL_CURRENT_COLOR));
    }

}
