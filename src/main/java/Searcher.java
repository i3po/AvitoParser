import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;


class Searcher {
  private File file;
  private final String mainUrl;
  private File data = ParserManager.getData();
  private Document mainDoc = null;
  private static List<Proxy> proxyList = new ArrayList<>();
  private static final String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";
  private static ProxySetter proxySetter;

  public static void setProxyList(List<Proxy> proxyList) {
    Searcher.proxyList = proxyList;
  }

  public static List<Proxy> getProxyList() {
    return proxyList;
  }

  public static void setProxySetter(ProxySetter proxySetter) {
    Searcher.proxySetter = proxySetter;
  }

  public Document getMainDoc() {
    return mainDoc;
  }

  public Searcher(String mainUrl) {
    System.out.println(data + "search constructor");
    this.mainUrl = mainUrl;
    int begin = mainUrl.lastIndexOf('/') + 1;
    int end = begin + 5;
    if (!data.exists()) try {
      Files.createDirectories(data.toPath());
    } catch (IOException e) {
      e.printStackTrace();
    }
    file = new File(data + File.separator + mainUrl.substring(begin, end).trim());
  }

  private Map<URL, Date> getUrlsAndDates(Elements elements) throws MalformedURLException {

    Map<URL, Date> urlAndTitles = new HashMap<>();
    String linkElement = "href";
    String dateElement = "c-2";//!!!!!!!!

    for (Element element : elements) {
//            System.out.println("elementelement&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&" + element.text());
      String link = element.getElementsByClass("item-description-title-link").attr("href");

      URL url = new URL("https://www.avito.ru" + link);

      String date = element.getElementsByClass(dateElement).first().text().toLowerCase();
      date = insertVafterToday(date);
      //System.out.println(date + " getUrlsAnd Dates!!!");
      Date date1 = AdFactoryFromAdUrl.parseAvitoDate(date);

      urlAndTitles.put(url, date1);
    }
    return urlAndTitles;
  }

  private String insertVafterToday(String date) {
    StringBuffer stringBuffer = new StringBuffer(date);
    stringBuffer.insert(date.length() - 6, " в");
    //System.out.println(stringBuffer.toString() + "!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    return stringBuffer.toString();
  }

  Map<URL, Date> getUrlsAndDatesMapFromFile() throws IOException {
    Map<URL, Date> result;
    try {
      TimeUnit.MILLISECONDS.sleep(ParserManager.rnd(300, 1000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if (!file.exists()) try {
      Files.createFile(file.toPath());
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println(file.getAbsolutePath() + "absolute path");
//       Document doc = Jsoup.parse(file, "UTF-8", mainUrl);
    Document doc = parse(mainUrl);

    Elements sellBoardElements = new Elements();
    sellBoardElements = doc.select(".item_table-description");

    result = getUrlsAndDates(sellBoardElements);
//        System.out.println("result <<<<<<<<<<<<<<<<<<<<" + result);
    if (result != null) return result;

    return result;
  }


  public static Document parse(String url) {
    try {
      if (!proxyList.isEmpty()) {
        System.out.println("USING PROXY....................................................." + " " + proxyList.get(0));
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .proxy(proxyList.get(0))
                .get();

      } else {
        System.out.println("WITHOUT PROXY.....................................................");
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .get();
      }

    } catch (Exception e) {
      if (!proxyList.isEmpty()) {
        deleteLastProxy();
      }
      e.printStackTrace();
    }
    return null;
  }

  static void deleteLastProxy() {
    if (proxyList.size() > 0) {
      System.out.println("deleting proxy..." + proxyList.get(0));
      proxySetter.deleteProxy(proxyList.remove(0));
//            TimeUnit.HOURS.sleep(1);
      System.out.println("Proxy was removed");
    } else {
      try {
        TimeUnit.MILLISECONDS.sleep(20000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }
}