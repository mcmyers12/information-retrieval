package edu.jhu.ir.binarytextclassification;

import java.io.Serializable;
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
	 * Class representing a term that is used to build a lexicon and list of terms
	 */
	public static class Term implements Serializable {
		private static final long serialVersionUID = -1947264671039701464L;
		private String text;	//Required in memory
		private int invertedFileLocation;	//Required in memory
		private int documentFrequency = 0; // Number of documents which the word occurs in	// Useful in memory

		public int getDocumentFrequency() {
			return documentFrequency;
		}

		public void setDocumentFrequency(int documentFrequency) {
			this.documentFrequency = documentFrequency;
		}

		public int getInvertedFileLocation() {
			return invertedFileLocation;
		}

		public void setInvertedFileLocation(int invertedFileLocation) {
			this.invertedFileLocation = invertedFileLocation;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text + "\t\t - document frequency: " + documentFrequency;
		}
	}


	/**
	 * Class representing an inverted file record that stores the document ID and
	 * count for the number of times a term appears in that document
	 * This class is used to hold the information that is stored to the inverted file
	 */
	public static class InvertedFileRecord {
		private int documentId;
		private int termFrequency;

		public InvertedFileRecord(int documentId, int termFrequency) {
			this.documentId = documentId;
			this.termFrequency = termFrequency;
		}

		public int getTermFrequency() {
			return termFrequency;
		}

		public int getDocumentId() {
			return documentId;
		}

		@Override
		public String toString() {
			return "(documentID: " + documentId
					+ ", count: " + termFrequency + ")";
		}
	}


	/**
	 * Tokenize a given string using the following approaches
	 * Split on spaces
	 * Lower case only if not all upper case
	 * Remove leading and trailing punctuation from each token
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
			normalizedToken = normalizedToken.replaceFirst("^[^a-zA-Z]+", "");
			normalizedToken = normalizedToken.replaceAll("[^a-zA-Z]+$", "");

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
	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
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

}
