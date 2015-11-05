package timelines.importer.csv;

import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import timelines.utils.TimeUtils;
import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

public class CsvToGoesSxrLeafConverter {

  //	private static final Logger LOGGER = LoggerFactory.getLogger(CsvToGoesSxrLeafConverter.class);

  private final int timestampColumn;
  private final int lowChannelColumn;
  private final int highChannelColumn;
  private final SimpleDateFormat dateTimeFormatter;

  public CsvToGoesSxrLeafConverter(int timestampColumn, int lowChannelColumn, int highChannelColumn, SimpleDateFormat dateTimeFormatter) {
    this.timestampColumn = timestampColumn;
    this.lowChannelColumn = lowChannelColumn;
    this.highChannelColumn = highChannelColumn;
    this.dateTimeFormatter = dateTimeFormatter;
  }

  public List<GoesSxrLeaf> parseFile(Date startTimestamp, Date endTimestamp, Reader fileReader) throws IOException {
    List<GoesSxrLeaf> goesSxrLeafs = new ArrayList<GoesSxrLeaf>();
    String[] nextLine;
    CSVReader csvReader = new CSVReader(fileReader, CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);

    try {
      boolean foundValid = false;
      System.out.println("reading");
      while ((nextLine = csvReader.readNext()) != null) {
        GoesSxrLeaf goesSxrLeaf = parseDataRow(nextLine);


        if(!foundValid) {
          foundValid = true;
          System.out.println("first line in file: " + Arrays.toString(nextLine));
          System.out.println("valid: " + isValid(goesSxrLeaf));
          System.out.println("fitting in interval " + startTimestamp + " to " + endTimestamp + ": " + TimeUtils.isFittingInInterval(goesSxrLeaf.getTimestamp(), startTimestamp, endTimestamp));
          // TODO remove debugging stuff
        }

        if (isValid(goesSxrLeaf) && TimeUtils.isFittingInInterval(goesSxrLeaf.getTimestamp(), startTimestamp, endTimestamp)) {
          goesSxrLeafs.add(goesSxrLeaf);

        } else {
//          System.out.println(goesSxrLeaf.getTimestamp() + " " + startTimestamp + " " + endTimestamp);
//          System.out.println(TimeUtils.isFittingInInterval(goesSxrLeaf.getTimestamp(), startTimestamp, endTimestamp));
        }
      }
    } finally {
      csvReader.close();
//      IOUtils.closeQuietly(csvReader);
    }

    return goesSxrLeafs;
  }

  private GoesSxrLeaf parseDataRow(String[] line) {
    try {
      GoesSxrLeaf goesSxrLeaf = new GoesSxrLeaf();

      Date timestamp = dateTimeFormatter.parse(line[timestampColumn]);
//      Date timestamp = Date. (line[timestampColumn], dateTimeFormatter);
      float lowChannel = Float.parseFloat(line[lowChannelColumn]);
      float highChannel = Float.parseFloat(line[highChannelColumn]);

      // sanitize data
      if (lowChannel < 1E-10 || lowChannel > 1E-2) {
        lowChannel = 0;
      }
      if (highChannel < 1E-10 || highChannel > 1E-2) {
        highChannel = 0;
      }

      goesSxrLeaf.setTimestamp(timestamp);
      goesSxrLeaf.setLowChannel(lowChannel);
      goesSxrLeaf.setHighChannel(highChannel);

      return goesSxrLeaf;
    } catch (Exception e) {
      e.printStackTrace();
      //LOGGER.info(e.toString() + " at line: " + Arrays.toString(line));
      return null;
    }
  }

  /**
   * Validate GoesSxrLeaf.
   *
   * @param goesSxrLeaf
   * @return is valid
   */
  private boolean isValid(GoesSxrLeaf goesSxrLeaf) {
    if (goesSxrLeaf == null) {
      return false;
    } else if (goesSxrLeaf.getTimestamp() == null) {
      return false;
    } else if (goesSxrLeaf.getLowChannel() < 1E-10 || goesSxrLeaf.getLowChannel() > 1E-2) {
      return false;
    } else if (goesSxrLeaf.getHighChannel() < 1E-10 || goesSxrLeaf.getHighChannel() > 1E-2) {
      return false;
    }
    return true;
  }
}
