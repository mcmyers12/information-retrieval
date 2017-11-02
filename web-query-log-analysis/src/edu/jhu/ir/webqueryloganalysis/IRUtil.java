package edu.jhu.ir.webqueryloganalysis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

	public static boolean queryContainsURL(String query) {
		List<String> tokens = tokenize(query);

		for (String token : tokens) {
			if (isValidURL(token)) {
				return true;
			}
		}

		return false;
	}
}
