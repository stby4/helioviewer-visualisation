package timelines.renderer;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import timelines.api.web.DiagramAPI;
import timelines.config.Config;
import timelines.database.TimelinesDB;
import timelines.utils.ImageUtils;
import timelines.utils.TimeUtils;

public class CacheRenderer {

  private static final Logger logger = Logger.getLogger(DiagramAPI.class.getName());

  public static final int CACHE_ZOOM_START = 8;
  public static final int CACHE_ZOOM_END = DiagramAPI.ZOOM_LEVEL_MAX;

  private Config config;

  private DiagramRenderer renderer;

  public CacheRenderer() throws Exception {
    config = new Config();
    renderer = new DiagramRenderer();
  }

  /**
   * Used to initialize the cache
   */
  public void createCache() {

    logger.log(Level.INFO, "Creating cache");

    Map<String, String> customData = new HashMap<String, String>();

    for (int i = CACHE_ZOOM_START; i <= CACHE_ZOOM_END; i++) {

      logger.log(Level.INFO, "Creating cache for zoom level {0}", new Object[]{i});

      customData.put("zoomLevel", "" + i);
      createCacheForZoomLevel(customData, i, TimelinesDB.DB_START_DATE);
    }
  }

  /**
   * Calculates the amount time represented by a diagram in ms on the given zoom level
   * @param zoomLevel the zoom level
   * @return the amount time represented by a diagram in ms on the given zoom level
   */
  public static long getTimePerImage(int zoomLevel) {
    return (long) (DiagramRenderer.IMAGE_WIDTH * Math.pow(2, zoomLevel) * 1000);
  }


  private void createCacheForZoomLevel(Map<String, String> customData, int zoomLevel, Date currentStartDate) {
    BufferedImage img = null;
    File f;
    int alphaFactor = 1 + 1 / (CACHE_ZOOM_END - CACHE_ZOOM_START);
    long currentTimePerImage = getTimePerImage(zoomLevel);

    while (currentStartDate.before(new Date())) {
      try {

        Date endDate = new Date(currentStartDate.getTime() + currentTimePerImage);
        customData.put("startDate", TimeUtils.toString(currentStartDate, "yyyy-MM-dd:HH:mm:ss"));
        customData.put("endDate", TimeUtils.toString(endDate, "yyyy-MM-dd:HH:mm:ss"));

        // get image the normal way for the lowest zoom level
        if(zoomLevel == CACHE_ZOOM_START) {
          img = renderer.getDiagramForTimespan(currentStartDate, endDate);


        // merge two images from the lower level for higher cache levels
        } else {
          try {

            BufferedImage left = ImageIO.read(new File(getCacheImagePath(zoomLevel - 1, currentStartDate.getTime())));
            long previousTimePerImage = (long) (DiagramRenderer.IMAGE_WIDTH * Math.pow(2, (zoomLevel - 1)) * 1000);
            BufferedImage right = new BufferedImage(DiagramRenderer.IMAGE_WIDTH, DiagramRenderer.IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            if (new Date(currentStartDate.getTime() + previousTimePerImage).before(new Date())) {
              right = ImageIO.read(new File(getCacheImagePath(zoomLevel - 1, currentStartDate.getTime() + previousTimePerImage)));
            }
            img = new BufferedImage(DiagramRenderer.IMAGE_WIDTH * 2, DiagramRenderer.IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(left, null, 0, 0);
            g.drawImage(right, null, left.getWidth(), 0);
            g.dispose();
            img = ImageUtils.multiplyAlpha(alphaFactor, ImageUtils.getScaledInstance(img, DiagramRenderer.IMAGE_WIDTH, DiagramRenderer.IMAGE_HEIGHT, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true));

          } catch (IIOException e) {
            // occurs when there's no images for the current time frame on lower zoom levels.
            // can be ignored, simply results in the inexistence of images for the current zoom level
          }
        }

        f = new File(getCacheImagePath(zoomLevel, currentStartDate.getTime()));
        ImageUtils.writeWithCustomData(new FileOutputStream(f), img, customData);

        currentStartDate = new Date(currentStartDate.getTime() + currentTimePerImage);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private String getCacheImagePath(int zoomLevel, long timestamp) {
    return config.getCachePath() + zoomLevel + "/" + timestamp + ".png";
  }

  /**
   * Used to update the last image in the cache as well as to
   * add images for the time passed since the last images end date
   */
  public void updateCache() {

    Map<String, String> customData = new HashMap<String, String>();

    for(int i = CACHE_ZOOM_START; i <= CACHE_ZOOM_END; i++) {

      // get last diagram file in the lowest cache level
      File dir = new File(config.getCachePath() + i);
      File[] files = dir.listFiles();
      long lastLong = Long.MIN_VALUE;
      for (File f : files) {
        if (f.isFile()) {
          long fileNr = Long.parseLong(f.getName().substring(0, f.getName().indexOf(".")));
          if (fileNr > lastLong) {
            lastLong = fileNr;
            if (lastLong >= new Date().getTime()) {
              break;
            }
          }
        }
      }

      customData.put("zoomLevel", "" + i);
      createCacheForZoomLevel(customData, i, new Date(lastLong));
    }
  }

  /**
   * Use this to create an initial cache
   * @param args
   */
  public static void main(String[] args) {

    try {

      CacheRenderer cacheRenderer = new CacheRenderer();
      cacheRenderer.createCache();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
