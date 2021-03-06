import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdFactoryFromAdUrl {
  private static final int[] interval = new int[] { 2500, 6000 };

  private AdFactoryFromAdUrl() {
  }

  public static Ad createNewAd(URL url) {
    Document document = null;
    try {
      document = Searcher.parse(url.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    sleep();

    if (document != null) {
      // достаём цену и содержание
      String priceText = document.getElementsByClass("js-item-price").text();
      String temp = priceText.substring(0, priceText.length() / 2);

      if (temp.length() > 0) {
        temp = temp.replaceAll(" ", "");
      }

      else
        temp = "0";
      
      temp = temp.replaceAll("[^0-9]+", "");
      int price = Integer.parseInt(temp);
      String description = document.getElementsByClass("item-description").tagName("p").text();
      String title = document.getElementsByClass("title-info-title-text").text();
      String temp2 = document.getElementsByClass("title-info-metadata-item-redesign").text();
      String stringDate = getTimeFromString(temp2);
      Date date = AvitoDateParser.avitoDatePars(stringDate);
      if (date == null)
        return null;

      List<String> urls = new ArrayList<>();
      Elements imageEls = document.getElementsByClass("gallery-list-item-link");
      for (Element el : imageEls) {
        String imgUrl = el.attr("src");
        imgUrl = imgUrl.replaceAll("\\d+x\\d+", "640x480");
        urls.add(imgUrl);
      }

      return new Ad(price, description, title, url, date, urls.toArray(new String[0]));
    }
    return null;
  }

  public static Date parseAvitoDate(String date) {

    Date result;

    result = AvitoDateParser.avitoDatePars(date);
    return result;

  }

  private static void sleep() {
    try {
      int a = (int) ParserManager.rnd(interval[0], interval[1]);

      TimeUnit.MILLISECONDS.sleep(a);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
  }

  private static String getTimeFromString(String line) {
    String string = line.toLowerCase();

    String pattern = "сегодня.*(\\d{2}:\\d{2})";

    Pattern r = Pattern.compile(pattern);

    Matcher m = r.matcher(string);

    if (m.find()) {
      return m.group(1).trim();
    } else {
      System.out.println("Time not found in string: " + line);
    }

    return null;
  }
}