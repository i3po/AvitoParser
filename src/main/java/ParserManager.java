import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ParserManager {
  private boolean flag = true;
  private String mainUrl;
  private Map<Ad, Date> ads = new HashMap<>();
  private File file;
  private ObjectInputStream ois;
  private static final File data;
  private String emailTo;
  private String searchValue;
  private String emailFrom;
  private String password;
  private int[] interval = new int[] { 60 * 2, 60 * 3 };

  static {
    data = new File(CurrentDir() + File.separator + "ParsersData");
    System.out.println(data + File.separator + "ParserData" + " data static Manger");
  }

  public static File getData() {
    return data;
  }

  private static String CurrentDir() {
    String path = System.getProperty("user.dir");
    String FileSeparator = System.getProperty("file.separator");
    return path.substring(0, path.lastIndexOf(FileSeparator) + 1);
  }

  public String getSearchValue() {
    return searchValue;
  }

  public void setSearchValue(String searchValue) {
    this.searchValue = searchValue;
  }

  public boolean isFlag() {
    return flag;
  }

  public void setFlag(boolean flag) {
    this.flag = flag;
  }

  public String getMainUrl() {
    return mainUrl;
  }

  public void setMainUrl(String mainUrl) {
    this.mainUrl = mainUrl;
  }

  public String getEmailTo() {
    return emailTo;
  }

  public void setEmailTo(String emailTo) {
    this.emailTo = emailTo;
  }

  static ParserManager getInstance() {
    return Instance.getInstance();

  }

  public void startProxySearchThread() {
    final Thread proxyThread = new Thread(new ProxyController());
    proxyThread.setDaemon(true);
    proxyThread.start();
    proxyThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable throwable) {
        System.out.println("error in proxy thread, relaunching....");
        if (!proxyThread.isAlive() || proxyThread.isInterrupted()) {
          startProxySearchThread();
        }
      }
    });
  }

  private ParserManager() {
    if (!data.exists())
      try {
        Files.createDirectories(data.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    file = new File(data + File.separator + "admap.ser");
    if (Files.notExists(file.toPath()))
      try {
        System.out.println("file creating " + file);
        Files.createFile(file.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }

    try {
      FileInputStream fis = new FileInputStream(file);
      ois = new ObjectInputStream(fis);

      ads = (Map<Ad, Date>) ois.readObject();
      fis.close();
      ois.close();
      System.out.println("Object history READING...Done");

    } catch (EOFException e) {
      ads = new HashMap<>();
      System.out.println("Object history READING...not Done, new file object history created");
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    if (ads == null) {
      ads = new HashMap<Ad, Date>();
      System.out.println("new ads");
    }

    System.out.println("constructor ads(object history HashMap) size: " + ads.size());
  }

  public void startPars() {

    while (flag) {
      try {
        Date before = new Date();
        System.out.printf("Start Processing URL: %s\n", mainUrl);
        start(mainUrl);
        int rand = (int) rnd(interval[0], interval[1]);
        TimeUnit.SECONDS.sleep(rand);
        Date after = new Date();
        long time = after.getTime() - before.getTime();
        System.out.println("parsing page done, time reamaning ms: " + time);

      } catch (Exception e) {
        e.printStackTrace();
      }
      if (!flag)
        break;
    }
  }

  private boolean containsURL(URL key) {
    boolean result = false;
    for (Map.Entry<Ad, Date> pair : ads.entrySet()) {
      if (pair.getKey().getUrl().equals(key)) {
        result = true;
        break;
      }
    }
    // System.out.println(result + " url contains : true");
    return result;
  }

  // important method
  private void start(String mainUrl) throws MalformedURLException {
    Searcher searcher = new Searcher(mainUrl);
    Map<URL, Date> urlDateMap = null;
    try {
      urlDateMap = searcher.getUrlsAndDatesMapFromMainUrl();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (urlDateMap == null || urlDateMap.isEmpty()) {
      System.out.println("urlDateMap is empty ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
      Searcher.deleteLastProxy();
    }

    for (Map.Entry<URL, Date> urlStringEntry : urlDateMap.entrySet()) {
      URL url = urlStringEntry.getKey();
      Date dateFromBoard = urlStringEntry.getValue();
      System.out.println("Url seen: " + url + " date: " + (dateFromBoard == null ? "Not Today" : dateFromBoard));
      if (!AvitoDateParser.isToday(dateFromBoard))
        continue;

      if (!containsURL(url)) {
        Ad ad = AdFactoryFromAdUrl.createNewAd(urlStringEntry.getKey());

        if (ad == null)
          continue;

        String title = ad.getTitle();
        int price = ad.getPrice();
        String description = ad.getDescription();
        Date date = ad.getDate();

        if ((descriptionContainsSearchVal(description, title, searchValue)) && !ads.containsKey(ad)
            && !ads.containsValue(date)) {
          String messsgeText = price + " \n" + description + " время: \n" + "" + date + "\n" + ad.getUrl();
          String[] messageImgs = ad.getPhotos();
          MessageHTML messageHTML = new MessageHTML(title, messsgeText, messageImgs);
          new Thread(new EmailDemon(messageHTML, emailTo, emailFrom, password)).start();

        }
        System.out.println("##########################################################################");
        System.out.println(ad);
        System.out.println(ads.size() + " ads.size()");
        System.out.println("##########################################################################");

        if (!ads.containsKey(ad)) {
          ads.put(ad, date);
          setSerialize();
          if (ads.size() > 2000) {
            try {
              Path bakFile = saveSetAndCreateNewFile();
              System.out.println("BAK FILE AND NEW HashSet CREATED!!! " + bakFile);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

        }

      }

    }
  }

  private boolean descriptionContainsSearchVal(String description, String title, String searchValue) {
    return searchValue.equals("ns") || description.toLowerCase().contains(searchValue)
        || title.toLowerCase().contains(searchValue);
  }

  private Path saveSetAndCreateNewFile() throws IOException {
    String stringPath = data + File.separator + file.getName().trim() + ".bak";
    Path path = Paths.get(stringPath);
    Path target = Files.createFile(path);
    Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
    ads = new HashMap<>();
    return target;
  }

  private void setSerialize() {
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
      oos.writeObject(ads);
      oos.flush();
      oos.close();
      System.out.println("ads written");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Метод получения псевдослучайного целого числа от min до max (включая max);
   */
  static long rnd(long min, long max) {
    max -= min;
    final double random = Math.random();
    return Math.round((random * max) + min);
  }

  public void setEmailFrom(String emailFrom) {
    this.emailFrom = emailFrom;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  private static class Instance {
    private static final ParserManager parsmanager = new ParserManager();

    static ParserManager getInstance() {
      return parsmanager;
    }
  }
}