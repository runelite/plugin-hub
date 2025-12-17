package cc.nocturne.eventsmadeeasy.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class BoardViewerDialog extends JDialog
{
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final JScrollPane scrollPane = new JScrollPane(imageLabel);

    private BufferedImage original;

    public BoardViewerDialog(Window owner, String title)
    {
        super(owner, title, ModalityType.MODELESS);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // Niceties
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        setPreferredSize(new Dimension(1100, 800));
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(owner);

        // Re-fit image when the window resizes
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                fitToWindow();
            }
        });
    }

    public void setImagePngBytes(byte[] pngBytes) throws Exception
    {
        if (pngBytes == null || pngBytes.length == 0)
        {
            throw new IllegalArgumentException("PNG bytes were empty");
        }

        original = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (original == null)
        {
            throw new IllegalStateException("Could not decode PNG image");
        }

        fitToWindow();
    }

    private void fitToWindow()
    {
        if (original == null) return;

        // Available viewport size
        int vw = scrollPane.getViewport().getWidth();
        int vh = scrollPane.getViewport().getHeight();

        if (vw <= 0 || vh <= 0)
        {
            imageLabel.setIcon(new ImageIcon(original));
            return;
        }

        int iw = original.getWidth();
        int ih = original.getHeight();

        // Scale to fit (never upscale above 100%)
        double scale = Math.min((double) vw / iw, (double) vh / ih);
        scale = Math.min(scale, 1.0);

        int nw = Math.max(1, (int) Math.round(iw * scale));
        int nh = Math.max(1, (int) Math.round(ih * scale));

        Image scaled = original.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaled));
        imageLabel.revalidate();
        imageLabel.repaint();
    }
}
