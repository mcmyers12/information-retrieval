package edu.jhu.ir.webqueryloganalysis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class contains utility methods and classes for general information retrieval related tasks
 * @author Miranda Myers
 *
 */
public class IRUtil {

	/**
	 * Tokenize a given string using the following approaches
	 * Split on spaces
	 * Lower case only if not all upper case						REMOVED
	 * Remove leading and trailing punctuation from each token		REMOVED
	 *
	 * @param line
	 * @return list of tokens
	 */
	public static List<String> tokenize(String line) {
		String[] tokens = line.split("\\s+"); // Split string on whitespace
		List<String> normalizedTokens = new ArrayList<>();
		for (String token : tokens) {
			String normalizedToken = token.toLowerCase();

			//Remove external punctuation
			//normalizedToken = normalizedToken.replaceFirst("^[^a-zA-Z]+", "");
			//normalizedToken = normalizedToken.replaceAll("[^a-zA-Z]+$", "");

			if (!normalizedToken.equals("")) {
				normalizedTokens.add(normalizedToken);
			}
		}

		return normalizedTokens;
	}


	/**
	 * Generic utility method to sort a map by value in ascending order
	 * (credit: https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java)
	 * @param map
	 * @return Map<K, V> result
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueAscending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue() );
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}


	/**
	 * Generic utility method to sort a map by value in descending order
	 * (credit: https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java)
	 * @param map
	 * @return Map<K, V> result
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue() );
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}


	/**
	 * Determine whether a given string is a valid URL
	 * @param urlString
	 * @return
	 */
	public static boolean isValidURL(String urlString)
	{
		try {
			URI url = new URI(urlString);
			url.toURL();
			return true;
		} catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
			return false;
		}
	}


	/**
	 * Determine whether a query contains a url
	 * @param query
	 * @return
	 */
	public static boolean queryContainsURL(String query) {
		List<String> tokens = tokenize(query);

		for (String token : tokens) {
			if (isValidURL(token)) {
				return true;
			}
		}

		return false;
	}


	public static boolean queryContainsStopword(String query) {
		for (String token : IRUtil.tokenize(query)) {
			if (isStopword(token)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Determine whether a term is a stopword
	 * @param term
	 * @return
	 */
	public static boolean isStopword(String term) {
		String[] stopwords = getStopwords();
		if (Arrays.asList(stopwords).contains(term)) {
			return true;
		}
		return false;
	}


	/**
	 * Get a list of stop words
	 */
	public static String[] getStopwords() {
		String[] stopwords = {
				"a",
				"about",
				"above",
				"after",
				"again",
				"against",
				"all",
				"am",
				"an",
				"and",
				"any",
				"are",
				"aren't",
				"as",
				"at",
				"be",
				"because",
				"been",
				"before",
				"being",
				"below",
				"between",
				"both",
				"but",
				"by",
				"can't",
				"cannot",
				"could",
				"couldn't",
				"did",
				"didn't",
				"do",
				"does",
				"doesn't",
				"doing",
				"don't",
				"down",
				"during",
				"each",
				"few",
				"for",
				"from",
				"further",
				"had",
				"hadn't",
				"has",
				"hasn't",
				"have",
				"haven't",
				"having",
				"he",
				"he'd",
				"he'll",
				"he's",
				"her",
				"here",
				"here's",
				"hers",
				"herself",
				"him",
				"himself",
				"his",
				"how",
				"how's",
				"i",
				"i'd",
				"i'll",
				"i'm",
				"i've",
				"if",
				"in",
				"into",
				"is",
				"isn't",
				"it",
				"it's",
				"its",
				"itself",
				"let's",
				"me",
				"more",
				"most",
				"mustn't",
				"my",
				"myself",
				"no",
				"nor",
				"not",
				"of",
				"off",
				"on",
				"once",
				"only",
				"or",
				"other",
				"ought",
				"our",
				"ours",
				"ourselves",
				"out",
				"over",
				"own",
				"same",
				"shan't",
				"she",
				"she'd",
				"she'll",
				"she's",
				"should",
				"shouldn't",
				"so",
				"some",
				"such",
				"than",
				"that",
				"that's",
				"the",
				"their",
				"theirs",
				"them",
				"themselves",
				"then",
				"there",
				"there's",
				"these",
				"they",
				"they'd",
				"they'll",
				"they're",
				"they've",
				"this",
				"those",
				"through",
				"to",
				"too",
				"under",
				"until",
				"up",
				"very",
				"was",
				"wasn't",
				"we",
				"we'd",
				"we'll",
				"we're",
				"we've",
				"were",
				"weren't",
				"what",
				"what's",
				"when",
				"when's",
				"where",
				"where's",
				"which",
				"while",
				"who",
				"who's",
				"whom",
				"why",
				"why's",
				"with",
				"won't",
				"would",
				"wouldn't",
				"you",
				"you'd",
				"you'll",
				"you're",
				"you've",
				"your",
				"yours",
				"yourself",
				"yourselves"
		};

		return stopwords;
	}
}



