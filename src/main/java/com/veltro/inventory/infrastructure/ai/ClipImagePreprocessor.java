package com.veltro.inventory.infrastructure.ai;

import com.veltro.inventory.exception.ai.ClipImageProcessingException;
import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

@Component
public class ClipImagePreprocessor {

    private static final int TARGET_SIZE = 224;
    private static final float[] MEAN = {0.48145466f, 0.4578275f, 0.40821073f};
    private static final float[] STD = {0.26862954f, 0.26130258f, 0.27577711f};

    /**
     * Preprocesses an image for CLIP ViT-B/32.
     * Steps:
     * 1. Ensure RGB
     * 2. Resize shortest edge to 224
     * 3. Center crop to 224x224
     * 4. Normalize and convert to NCHW flat float array
     *
     * @param image Original image
     * @return Flat array of 3 * 224 * 224 floats
     */
    public float[] preprocess(BufferedImage image) {
        try {
            BufferedImage rgbImage = ensureRGB(image);
            BufferedImage resized = resizeShortestEdge(rgbImage, TARGET_SIZE);
            BufferedImage cropped = centerCrop(resized, TARGET_SIZE);
            return normalizeToNCHW(cropped);
        } catch (Exception e) {
            throw new ClipImageProcessingException("Failed to preprocess image for CLIP inference", e);
        }
    }

    private BufferedImage ensureRGB(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgbImage;
    }

    private BufferedImage resizeShortestEdge(BufferedImage source, int targetSize) {
        int w = source.getWidth();
        int h = source.getHeight();
        
        if (w == targetSize && h == targetSize) {
            return source;
        }

        int targetW, targetH;
        if (w < h) {
            targetW = targetSize;
            targetH = (int) (h * ((double) targetSize / w));
        } else {
            targetH = targetSize;
            targetW = (int) (w * ((double) targetSize / h));
        }

        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, targetW, targetH, null);
        g.dispose();
        
        return resized;
    }

    private BufferedImage centerCrop(BufferedImage source, int targetSize) {
        int w = source.getWidth();
        int h = source.getHeight();
        
        if (w == targetSize && h == targetSize) {
            return source;
        }

        int x = (w - targetSize) / 2;
        int y = (h - targetSize) / 2;

        return source.getSubimage(x, y, targetSize, targetSize);
    }

    private float[] normalizeToNCHW(BufferedImage image) {
        float[] tensor = new float[3 * TARGET_SIZE * TARGET_SIZE];
        int stride = TARGET_SIZE * TARGET_SIZE;

        for (int y = 0; y < TARGET_SIZE; y++) {
            for (int x = 0; x < TARGET_SIZE; x++) {
                int rgb = image.getRGB(x, y);
                
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;

                int i = y * TARGET_SIZE + x;
                
                // NCHW format: R channel first, then G, then B
                tensor[i] = (r - MEAN[0]) / STD[0];
                tensor[stride + i] = (g - MEAN[1]) / STD[1];
                tensor[2 * stride + i] = (b - MEAN[2]) / STD[2];
            }
        }
        
        return tensor;
    }
}
