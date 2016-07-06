package wikipediaGame;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikipediaGame {

	static Scanner reader = new Scanner(System.in);  // Reading from System.in

	static HashSet<Article> visitedArticles = new HashSet<Article>();
	static HashSet<String> seenLinks = new HashSet<String>();

	static String startArticleURL;
	static Article startArticle;
	static String endArticleURL;
	static Article endArticle;

	static boolean useNavBox = false;

	public static void main(String[] args){

		String userInput = getUserInput("Find a path between two wikipedia articles:\n"
				+ "Use non-article navigation template links? y/n "
				+ "\n(Off by default, may run faster if y. Input H for details)").
				toLowerCase();
		
		if(userInput.equals("h")){
			System.out.println("\"A navigation template is a grouping of links used in multiple"
						+ "\n related articles to facilitate navigation between those articles."
						+ "\"\n - See en.wikipedia.org/wiki/Wikipedia:Navigation_templates\n");
			main(args);
		}
		
		useNavBox = userInput.equals("y");

		startArticleURL = parseUserInput(
				getUserInput("\nEnter the start article URL or title: "));
		endArticleURL = parseUserInput(
				getUserInput("\nEnter the end article URL or title: "));

		if(parseUserInput(startArticleURL) == null || 
				parseUserInput(endArticleURL) == null){
			System.out.println("\nInvalid input!");
			main(args);
			
		}else{
			Document startDoc;
			Document endDoc;
			try {
				if(isValidArticleURL(startArticleURL) && isValidArticleURL(endArticleURL)){
					//get the article and find the canonical url
					startDoc = Jsoup.connect(startArticleURL).get();
					endDoc = Jsoup.connect(endArticleURL).get();
					startArticleURL = getCanonicalLink(startDoc);
					endArticleURL = getCanonicalLink(endDoc);
				}else{
					System.out.println("URL(s) not valid. Try using the article url.\n"
							+ "Article URLs should be in the format "
							+ "en.wikipedia.org/wiki/ArticleName");
					main(args);
				}
			} catch (IOException e) {
				System.out.println("Article(s) not valid. Try using the page url.\n"
						+ "URLs should be in the format en.wikipedia.org/wiki/ArticleName");
				main(args);
				//e.printStackTrace();
			}

			System.out.println("\nFinding shortest path from \n    "+startArticleURL+" \nto "
					+ "\n    " +endArticleURL + " ...\n");
			
			endArticle = new Article(Integer.MAX_VALUE, null, endArticleURL);
			
			startArticle = new Article(0, null, startArticleURL);

			runSearch();
		}

		reader.close();
	}

	/**
	 * Run breadth-first search until the end article is found.
	 */
	public static void runSearch(){

		Article currentArticle = null;
		
		PriorityQueue<Article> pQueue = new PriorityQueue<Article>(128);
		pQueue.add(new Article(0, null, startArticleURL));
		
		pQueue.addAll(visitedArticles);

		bfs:
		while(!pQueue.isEmpty()){
			currentArticle = pQueue.poll();
			visitedArticles.add(currentArticle);
			
			currentArticle.children = getArticleChildren(currentArticle, pQueue);
			
			for(Article url : currentArticle.children){
				if(url.equals(endArticle)){
					currentArticle = url;
					break bfs;
				}
			}
			
			pQueue.addAll(currentArticle.children);
			
		}

		System.out.println("\n\n" + endArticleURL + "\nFOUND " + currentArticle.distance + 
				" ARTICLE(S) AWAY"+" AFTER PROCESSING " + visitedArticles.size() + 
				" ARTICLES(S) AND " + seenLinks.size() + " LINK(S)!");
		System.out.println(currentArticle.lineageToString());
		System.out.println("Connected to a total of " + accessedWikiArticles + 
				" Wikipedia article(s) during this search and used " + usedCachedWikiArticles +
				" cached article(s).");
		
		System.out.println("\n----------------------------------------"
				+ "-------------------------------------------------\n");
		
		reset();
	}

	private static void reset() {
		seenLinks.clear();
		for(Article a : visitedArticles){
			a.reset();
		}
		accessedWikiArticles = 0;
		usedCachedWikiArticles = 0;
		main(null);
	}

	static int accessedWikiArticles = 0;
	static int usedCachedWikiArticles = 0;
	public static HashSet<Article> getArticleChildren(Article articleURL, 
			PriorityQueue<Article> pQueue){
		if(articleURL != null && articleURL.children!=null
				&& (visitedArticles.contains(articleURL) || pQueue.contains(articleURL))){
			for(Article a : articleURL.children){
				a.distance = articleURL.distance + 1;
				a.parent = articleURL;
			}
			usedCachedWikiArticles++;
			return articleURL.children;
		}
		HashSet<Article> linkSet = new HashSet<Article>();
		Document doc;
		Elements links = null;
		try {
			if(isValidArticleURL(articleURL.url)){
				doc = Jsoup.connect(articleURL.url).get();
				accessedWikiArticles++;
			}else{
				return linkSet;
			}
			if(!useNavBox){
				doc.select("td").remove();
			}
			doc.select("[href^=#cite]").remove();
			links = doc.select("a");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		int numLinks = 0;
		if (links != null) {
			Article a;
			String s;
			for(Element link : links){
				s = link.attr("abs:href");
				if(isValidArticleURL(s) && !seenLinks.contains(s)){
					a = new Article(articleURL, s);
					linkSet.add(a);
					if(visitedArticles.remove(a) || pQueue.contains(a)){
						System.out.println("Using cached article " +
					a.url.replace("https://en.wikipedia.org/wiki/", ""));
						visitedArticles.add(a);
						pQueue.add(a);
						usedCachedWikiArticles++;
					}
					seenLinks.add(s);
					numLinks++;
				}
			}
		}
		if(numLinks>0)System.out.println(numLinks + " new links found in article " + 
					articleURL.url.replace("https://en.wikipedia.org/wiki/", ""));
		return linkSet;
	}

	public static HashSet<String> getArticleLinks(HashSet<Article> articles){
		HashSet<String> linkSet = new HashSet<String>();
		for(Article a : articles){
			linkSet.add(a.url);
		}
		return linkSet;
	}

	public static String parseUserInput(String input){
		if(input == null || input.length() == 0){
			return null;
		}
		input = input.replace("\n", "").replace("\r", "");
		if(isValidArticleURL(input)){
			return input;
		} else {
			input = input.replaceAll(" ", "%20");
			if(!input.contains("wikipedia.org") && 
					isValidArticleURL("https://en.wikipedia.org/wiki/" + input)){
				return "https://en.wikipedia.org/wiki/" + input;
			}else if(input.contains("wikipedia.org/") && isValidArticleURL(
					input.replace("wikipedia.org/", "wikipedia.org/wiki/"))) { 
				//check and correction if .../wiki/... was not included
				return input.replace("wikipedia.org/", "wikipedia.org/wiki/");
			}
		}
		return null;
	}

	public static boolean isValidArticleURL(String url){
		if(url.contains("en.wikipedia.org/wiki/")){
			if(!(url.contains("/wiki/Wikipedia:")||url.contains("/wiki/Special:")||
					url.contains("/wiki/Category:")
					||url.contains("/wiki/Portal:")||url.contains("/wiki/File:")
					||url.contains("/wiki/Random:")||url.contains("/wiki/Help:")
					||url.contains("/wiki/Talk:"))){
				try {
					@SuppressWarnings("unused")
					URL test = new URL(url);//simple way to check for malformed URL
					return true;
				} catch (MalformedURLException e) {}
			}
		}
		return false;
	}

	public static HashSet<String> getIntersection(HashSet<String> set1, 
			HashSet<String> set2) {
		boolean set1IsLarger = set1.size() > set2.size();
		HashSet<String> cloneSet = new HashSet<String>(set1IsLarger ? set2 : set1);
		cloneSet.retainAll(set1IsLarger ? set1 : set2);
		return cloneSet;
	}

	public static String getUserInput(String message){
		String line = null;
		System.out.println(message);
		if(reader.hasNextLine())line = reader.nextLine();
		return line;
	}

	public static String getCanonicalLink(Document article){
		return article.select("link[rel=canonical]").attr("href");
	}
	
	
	/**
	 * Node for the article PriorityQueue
	 * @author nate
	 *
	 */
	public static class Article implements Comparable<Article>{
		public int distance;
		public Article parent;
		public String url;
		public HashSet<Article> children;
		
		public Article(int distance, Article parent, String url, HashSet<Article> children){
			this.distance = distance;
			this.parent = parent;
			this.url = url;
			this.children = children;
		}
		
		public Article(int distance, Article parent, String url){
			this(distance, parent, url, null);
		}
		
		public Article(Article parent, String url){
			this(parent.distance + 1, parent, url, null);
		}

		@Override
		public int compareTo(Article o) {
			if(o.distance < distance) {
				return 1;
			}
			if(o.distance > distance) {
				return -1;
			}
			return 0;
		}
		
		@Override
		public boolean equals(Object o){
			return o instanceof Article && url.equals(((Article) o).url);
		}
		
		@Override
		public String toString(){
			return url;
		}
		
		/**
		 * reset for reuse in a later run
		 */
		public void reset(){
			this.distance = Integer.MAX_VALUE;
			this.parent = null;
			if(this.children!=null){
				for(Article a : children){
					a.reset();
				}
			}
		}
		
		public String lineageToString(){
			if(parent!=null){
				if(children == null){
					return "  ---> " + url + "\n  ^--- " + parent.lineageToString();
				}
				return url + "\n  ^--- " + parent.lineageToString();
			} else {
				return url;
			}
			
		}
		
		@Override
		public int hashCode(){
			return url.hashCode();
		}
	}
}
